package com.demo.eventregistry.service;

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
import io.apicurio.registry.rest.v2.beans.ArtifactMetaData;
import io.apicurio.registry.rest.v2.beans.IfExists;
import io.confluent.kafka.schemaregistry.avro.AvroSchema;
import io.confluent.kafka.schemaregistry.client.SchemaRegistryClient;
import io.confluent.kafka.schemaregistry.client.rest.exceptions.RestClientException;
import org.apache.commons.io.IOUtils;
import org.apache.kafka.clients.admin.Admin;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.common.errors.UnknownTopicOrPartitionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.web.util.UriUtils;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.regex.Pattern;

@Service
public class EventRegistryService {

    private static final Logger LOGGER = LoggerFactory.getLogger(EventRegistryService.class);
    private static final ObjectMapper YAML_READER = new ObjectMapper(new YAMLFactory());
    private static final ObjectMapper JSON_MAPPER = new ObjectMapper();
    private static final Pattern TOPIC_NAME_PATTERN = Pattern.compile("[A-Za-z]+\\.event.[A-Za-z]+\\.[A-Za-z]+");
    private static final String SUPPORTED_CONTRACT_FORMAT = "asyncapi";
    private static final String ASYNCAPI_REMOTE_REFERENCE_TAG = "$ref";

    private final Admin kafkaAdminClient;
    private final SchemaRegistryClient schemaRegistryClient;
    private final RegistryClient apicurioRegistryClient;
    private final String apicurioArtefactGroupId;

    @Autowired
    public EventRegistryService(Admin kafkaAdminClient,
                                SchemaRegistryClient schemaRegistryClient,
                                RegistryClient apicurioRegistryClient,
                                @Value("${apicurio.artefact-group-id}") String apicurioArtefactGroupId) {
        this.kafkaAdminClient = kafkaAdminClient;
        this.schemaRegistryClient = schemaRegistryClient;
        this.apicurioRegistryClient = apicurioRegistryClient;
        this.apicurioArtefactGroupId = apicurioArtefactGroupId;
    }

    public static class InvalidContractException extends Exception {
        public InvalidContractException(String errorMessage, Exception exception) {
            super(errorMessage, exception);
        }

        public InvalidContractException(String errorMessage) {
            super(errorMessage);
        }
    }

    public void registerAsyncApiContractFromUrl(String contractUrl) throws InvalidContractException, RestClientException, IOException, ExecutionException, InterruptedException {
        Optional<String> extensionOpt = Optional.ofNullable(UriUtils.extractFileExtension(contractUrl));

        if (extensionOpt.isPresent()) {
            String extension = extensionOpt.get().toLowerCase();
            // SUPPORT JSON AND YAML ONLY
            if (List.of("json", "yaml").contains(extension)) {
                // FETCH THE CONTRACT
                String contract = fetchRemoteContract(contractUrl);

                // PROCESS CONTRACT
                if ("json".equals(extension)) {
                    registerAsyncApiContractFromJson(contract);
                } else {
                    registerAsyncApiContractFromYaml(contract);
                }
            } else  {
                throw new InvalidContractException("Impossible to fetch contract at URL '" + contractUrl + "': file extension not supported");
            }
        } else {
            throw new InvalidContractException("Impossible to fetch contract at URL '" + contractUrl + "': URL not valid");
        }
    }

    public void registerAsyncApiContractFromYaml(String contractYaml) throws IOException, InvalidContractException, ExecutionException, InterruptedException, RestClientException {
        // CONVERT THE ASYNCAPI CONTRACT FROM YAML TO JSON
        String contractJson = convertYamlToJson(contractYaml);

        // REGISTER THE CONTRACT
        registerAsyncApiContractFromJson(contractJson);
    }

    public void registerAsyncApiContractFromJson(String contractJson) throws IOException, InvalidContractException, ExecutionException, InterruptedException, RestClientException {
        // READ THE ASYNCAPI CONTRACT FROM JSON
        Document document = Library.readDocumentFromJSONString(contractJson);
        String documentModelType = document.root().modelType().name();
        validateContractType(documentModelType);

        // VALIDATE THE ASYNCAPI CONTRACT
        AsyncApiDocument asyncApiDocument = (AsyncApiDocument) document;
        validateContract(asyncApiDocument);

        // EXTRACT CHANNEL NAMES AND CREATE TOPICS
        List<String> channels = asyncApiDocument.getChannels().getItemNames();
        createKafkaTopics(channels);

        // EXTRACT MESSAGE FROM CHANNEL
        publishChannelsAvroSchema(channels, asyncApiDocument);

        // PUBLISH ASYNCAPI CONTRACT
        publishAsyncApiContract(asyncApiDocument.getInfo().getTitle(), contractJson);
    }

    // PRIVATE METHODS

    private static String fetchRemoteContract(String url) throws InvalidContractException {
        try {
            URL payloadUrl = new URL(url);
            LOGGER.info("Download contract from URL: '{}'", payloadUrl);
            return IOUtils.toString(payloadUrl, StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new InvalidContractException("Error while fetching contract from: '" + url + "'", e);
        }
    }

    private static String convertYamlToJson(String yaml) throws JsonProcessingException {
        Object obj = YAML_READER.readValue(yaml, Object.class);
        return JSON_MAPPER.writeValueAsString(obj);
    }

    private static void validateContractType(String documentModelType) throws InvalidContractException {
        if (documentModelType.toLowerCase().contains(SUPPORTED_CONTRACT_FORMAT)) {
            LOGGER.info("Received a contract of type: '{}'", documentModelType);
        } else {
            throw new InvalidContractException("Only AsyncAPI contracts are supported");
        }
    }

    private static void validateContract(AsyncApiDocument contractDocument) throws InvalidContractException, JsonProcessingException {
        List<ValidationProblem> validationProblems = Library.validate(contractDocument, new DefaultSeverityRegistry());
        if (!CollectionUtils.isEmpty(validationProblems)) {
            StringBuilder validationProblemsStr = new StringBuilder();
            for (ValidationProblem problem : validationProblems) {
                validationProblemsStr.append(JSON_MAPPER.writeValueAsString(problem));
            }
            String validationProblemsSummary = validationProblemsStr.toString();
            LOGGER.error("Validation issues detected: {}", validationProblemsSummary);
            throw new InvalidContractException(validationProblemsSummary);
        }
    }

    private void createKafkaTopics(List<String> channels) throws InvalidContractException, InterruptedException, ExecutionException {
        // - for each, validate name format using regex
        // - then create not existing topics using Kafka AdminClient
        List<NewTopic> topicsToCreate = new ArrayList<>();
        for (String channel : channels) {
            validateKafkaTopicName(channel);
            checkIfKafkaTopicNeedsCreation(channel).ifPresent(topicsToCreate::add);
        }

        if (!topicsToCreate.isEmpty()) {
            kafkaAdminClient.createTopics(topicsToCreate).all().get();
            String topicsCreated = String.join(", ", topicsToCreate.stream().map(NewTopic::name).toList());
            LOGGER.info("The following topics have been created successfully: {}", topicsCreated);
        }
    }

    private Optional<NewTopic> checkIfKafkaTopicNeedsCreation(String channel) throws ExecutionException, InterruptedException {
        try {
            kafkaAdminClient.describeTopics(List.of(channel)).topicNameValues().get(channel).get();
            LOGGER.info("Topic '{}' is already existing", channel);
            return Optional.empty();
        } catch (Exception e) {
            if (e.getCause() instanceof UnknownTopicOrPartitionException) {
                LOGGER.info("Topic '{}' will be created", channel);
                // numPartitions and replication factor will be assigned by the broker
                return Optional.of(new NewTopic(channel, Optional.empty(), Optional.empty()));
            } else {
                throw e;
            }
        }
    }

    private static void validateKafkaTopicName(String channel) throws InvalidContractException {
        boolean matches = TOPIC_NAME_PATTERN.matcher(channel).matches();
        if (!matches) {
            LOGGER.warn("Channel/topic name is not matching the expected format: '{}'", channel);
            throw new InvalidContractException("Channel name is not valid: " + channel);
        }
    }

    private void publishChannelsAvroSchema(List<String> channels, AsyncApiDocument asyncApiDocument) throws IOException, RestClientException, InvalidContractException {
        // - check if schemaFormat contains avro
        // - pull payload if ref is remote
        // - push on Schema Registry the AVRO schema => check compatibility
        for (String channel : channels) {
            AsyncApiChannelItem channelItem = asyncApiDocument.getChannels().getItem(channel);
            AsyncApiMessage message = channelItem.getSubscribe().getMessage();
            if (message.getSchemaFormat().toLowerCase().contains("avro") &&
                    message.getPayload().has(ASYNCAPI_REMOTE_REFERENCE_TAG)) {
                String remoteReference = message.getPayload().get(ASYNCAPI_REMOTE_REFERENCE_TAG).asText();
                String schemaString = fetchRemoteSchemaReference(remoteReference);
                registerAvroSchema(channel, schemaString);
            } else {
                throw new InvalidContractException("The message schema format for '" + channel + "' shall be 'application/vnd.apache.avro' and the payload shall be a remote reference");
            }
        }
    }

    private void registerAvroSchema(String channel, String schemaString) throws IOException, RestClientException {
        AvroSchema parsedSchema = new AvroSchema(schemaString);
        int schemaId = schemaRegistryClient.register(channel, parsedSchema);
        LOGGER.info("Schema '{}' registered for subject '{}'", schemaId, channel);
    }

    private static String fetchRemoteSchemaReference(String remoteReference) throws InvalidContractException {
        try {
            URL payloadUrl = new URL(remoteReference);
            LOGGER.info("Download schema from payload URL: '{}'", payloadUrl);
            return IOUtils.toString(payloadUrl, StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new InvalidContractException("Error while fetching remote reference: '" + remoteReference + "'", e);
        }
    }

    private void publishAsyncApiContract(String identifier, String json) {
        // - push on Apicurio Registry the contract => check compatibility
        InputStream inputStream = IOUtils.toInputStream(json, StandardCharsets.UTF_8);
        ArtifactMetaData artifactMetadata = apicurioRegistryClient.createArtifact(apicurioArtefactGroupId, identifier, null, "ASYNCAPI", IfExists.UPDATE, false, inputStream);
        LOGGER.info("Artifact '{}' published with content id: {}", identifier, artifactMetadata.getContentId());
    }
}
