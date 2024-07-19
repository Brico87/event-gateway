package com.demo.eventregistry.controller;

import com.demo.eventregistry.configuration.EventRegistryConfiguration;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import io.apicurio.datamodels.Library;
import io.apicurio.datamodels.models.Document;
import io.apicurio.datamodels.models.asyncapi.AsyncApiChannelItem;
import io.apicurio.datamodels.models.asyncapi.AsyncApiDocument;
import io.apicurio.datamodels.models.asyncapi.AsyncApiMessage;
import io.apicurio.datamodels.validation.DefaultSeverityRegistry;
import io.apicurio.datamodels.validation.ValidationProblem;
import io.apicurio.registry.rest.client.RegistryClient;
import io.apicurio.registry.rest.client.RegistryClientFactory;
import io.apicurio.registry.rest.v2.beans.ArtifactMetaData;
import io.confluent.kafka.schemaregistry.avro.AvroSchema;
import io.confluent.kafka.schemaregistry.client.CachedSchemaRegistryClient;
import io.confluent.kafka.schemaregistry.client.SchemaRegistryClient;
import org.apache.commons.io.IOUtils;
import org.apache.kafka.clients.admin.Admin;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.common.errors.UnknownTopicOrPartitionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.util.CollectionUtils;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;

@RestController
public class EventRegistryController {

    private static final Logger LOGGER = LoggerFactory.getLogger(EventRegistryController.class);
    private static final Pattern TOPIC_PATTERN = Pattern.compile("[A-Za-z]+\\.event.[A-Za-z]+\\.[A-Za-z]+");

    private final Admin kafkaAdmin;
    private final SchemaRegistryClient schemaRegistryClient;
    private final RegistryClient apicurioRegistryClient;

    @Autowired
    public EventRegistryController(EventRegistryConfiguration configuration) {
        this.kafkaAdmin = Admin.create(configuration.getKafkaProperties());
        this.schemaRegistryClient = new CachedSchemaRegistryClient(configuration.getSchemaRegistryUrl(), 10);
        this.apicurioRegistryClient = RegistryClientFactory.create(configuration.getApicurioRegistryUrl());
    }

    @PostMapping(value = "/contract", consumes = "application/x-yaml")
    public ResponseEntity<String> processContract(@RequestBody String contract) {
        try {
            // READ THE ASYNCAPI CONTRACT FROM YAML
            String contractJson = convertYamlToJson(contract);
            Document document = Library.readDocumentFromJSONString(contractJson);

            // VALIDATE THE CONTRACT
            LOGGER.info("Received a contract of type: '{}'", document.root().modelType().name());
            AsyncApiDocument contractDocument = (AsyncApiDocument) document;
            List<ValidationProblem> validationProblems = Library.validate(contractDocument, new DefaultSeverityRegistry());
            if (!CollectionUtils.isEmpty(validationProblems)) {
                ObjectMapper objectMapper = new ObjectMapper();
                StringBuilder validationProblemsStr = new StringBuilder();
                for (ValidationProblem problem : validationProblems) {
                    validationProblemsStr.append(objectMapper.writeValueAsString(problem));
                }
                LOGGER.warn("Validation issues detected: {}", validationProblemsStr);
                return ResponseEntity.badRequest().body(validationProblemsStr.toString());
            }

            // EXTRACT CHANNEL NAME = TOPIC
            // - validate format using regex
            // - create topic using Kafka AdminClient
            List<NewTopic> topicsToCreate = new ArrayList<>();
            List<String> channels = contractDocument.getChannels().getItemNames();
            for (String channel : channels) {
                boolean matches = TOPIC_PATTERN.matcher(channel).matches();
                if (!matches) {
                    LOGGER.warn("Topic name is not matching the expected format: '{}'", channel);
                    return ResponseEntity.badRequest().body("Topic name not valid: " + channel);
                }
                try {
                    kafkaAdmin.describeTopics(List.of(channel)).topicNameValues().get(channel).get();
                } catch (UnknownTopicOrPartitionException e) {
                    topicsToCreate.add(new NewTopic(channel, Optional.empty(), Optional.empty()));
                }
            }
            kafkaAdmin.createTopics(topicsToCreate).all().get();
            LOGGER.info("The following topics have been created successfully: {}", String.join(", ", channels));

            // EXTRACT MESSAGE FROM CHANNEL
            // - check if schemaFormat contains avro
            // - pull payload if ref is remote
            // - push on Schema Registry the AVRO schema => check compatibility
            for (String channel : channels) {
                AsyncApiChannelItem channelItem = contractDocument.getChannels().getItem(channel);
                AsyncApiMessage message = channelItem.getSubscribe().getMessage();
                if (message.getSchemaFormat().toLowerCase().contains("avro")) {
                    URL payloadUrl = new URL(message.getPayload().get("$ref").asText());
                    LOGGER.info("Download schema from payload URL: '{}'", payloadUrl);
                    String schemaString = IOUtils.toString(payloadUrl, StandardCharsets.UTF_8);
                    AvroSchema parsedSchema = new AvroSchema(schemaString);
                    int schemaId = schemaRegistryClient.register(channel, parsedSchema);
                    LOGGER.info("Schema '{}' registered for subject '{}'", schemaId, channel);
                } else {
                    return ResponseEntity.badRequest().body("The message format shall be 'application/vnd.apache.avro'");
                }
            }

            // ON FULL ASYNCAPI CONTRACT
            // - push on Apicurio Registry the contract => check compatibility
            ArtifactMetaData artifactMetadata = apicurioRegistryClient.createArtifact("com.demo", contractDocument.getInfo().getTitle(), IOUtils.toInputStream(contract, StandardCharsets.UTF_8));
            LOGGER.info("Artifact '{}' with ID: {}", contractDocument.getInfo().getTitle(), artifactMetadata.getContentId());

            return ResponseEntity.ok("OK");
        } catch (Exception e) {
            LOGGER.error("Error while processing contract", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    // PRIVATE METHODS

    private String convertYamlToJson(String yaml) throws JsonProcessingException {
        ObjectMapper yamlReader = new ObjectMapper(new YAMLFactory());
        Object obj = yamlReader.readValue(yaml, Object.class);
        ObjectMapper jsonWriter = new ObjectMapper();
        return jsonWriter.writeValueAsString(obj);
    }
}
