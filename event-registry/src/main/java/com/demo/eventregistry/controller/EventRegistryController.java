package com.demo.eventregistry.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import io.apicurio.datamodels.Library;
import io.apicurio.datamodels.models.Document;
import io.apicurio.datamodels.models.asyncapi.AsyncApiChannelItem;
import io.apicurio.datamodels.models.asyncapi.AsyncApiDocument;
import io.apicurio.datamodels.validation.DefaultSeverityRegistry;
import io.apicurio.datamodels.validation.ValidationProblem;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.util.CollectionUtils;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
public class EventRegistryController {

    private static final Logger LOGGER = LoggerFactory.getLogger(EventRegistryController.class);


    @Autowired
    public EventRegistryController() {

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
                LOGGER.info("Validation issues detected: {}", validationProblemsStr);
                return ResponseEntity.badRequest().body(validationProblemsStr.toString());
            }

            // EXTRACT CHANNEL NAME = TOPIC
            // - create topic using Kafka AdminClient
            List<String> channels = contractDocument.getChannels().getItemNames();
            for (String channelName : channels) {
                AsyncApiChannelItem channelItem = contractDocument.getChannels().getItem(channelName);
                String payload = channelItem.getSubscribe().getMessage().getPayload().get("$ref").asText();
                LOGGER.info("Payload: {}", payload);
            }


            // EXTRACT MESSAGE FROM CHANNEL
            // - check if schemaFormat contains avro
            // - pull payload if ref is remote

            // EXTRACT AVRO ARTIFACT
            // - push on Schema Registry the AVRO schema => check compatibility

            // ON FULL ASYNCAPI CONTRACT
            // - push on Apicurio Registry the contract => check compatibility

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
