package com.demo.eventproducer.service;

import com.demo.eventproducer.model.EventSchemaModel;
import com.demo.eventproducer.repository.EventSchemaRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.avro.Schema;
import org.apache.avro.generic.GenericData;
import org.apache.avro.generic.GenericRecord;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.stream.schema.client.SchemaRegistryClient;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
public class EventProducerService {

    private static final Logger LOGGER = LoggerFactory.getLogger(EventProducerService.class);
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private final KafkaProducer<String, GenericRecord> kafkaProducer;
    private final SchemaRegistryClient schemaRegistryClient;
    private final EventSchemaRepository eventSchemaRepository;

    @Autowired
    public EventProducerService(KafkaProducer<String, GenericRecord> kafkaProducer,
                                SchemaRegistryClient schemaRegistryClient,
                                EventSchemaRepository eventSchemaRepository) {
        this.kafkaProducer = kafkaProducer;
        this.schemaRegistryClient = schemaRegistryClient;
        this.eventSchemaRepository = eventSchemaRepository;
    }

    public void send(String messageKey, String eventName, String eventData) throws JsonProcessingException {
        EventSchemaModel mapping = fetchEventSchemaMapping(eventName);
        String schemaDefinition = fetchSchemaDefinition(mapping.schemaId());
        GenericRecord genericRecord = buildGenericRecord(eventData, schemaDefinition);
        sendEvent(mapping.topicName(), messageKey, genericRecord);
    }

    public void register(String eventName, int schemaId, String topicName) {
        eventSchemaRepository.save(new EventSchemaModel(eventName, schemaId, topicName));
    }

    // PRIVATE METHODS

    private String fetchSchemaDefinition(int schemaId) {
        return schemaRegistryClient.fetch(schemaId);
    }

    private EventSchemaModel fetchEventSchemaMapping(String eventName) {
        return eventSchemaRepository.findById(eventName)
                .orElseThrow(() -> new IllegalArgumentException("Event '" + eventName + "' not known in database"));
    }

    private static GenericRecord buildGenericRecord(String eventData, String schemaDefinition) throws JsonProcessingException {
        Schema.Parser parser = new Schema.Parser();
        Schema schema = parser.parse(schemaDefinition);
        GenericRecord genericRecord = new GenericData.Record(schema);
        Map<String, Object> map = OBJECT_MAPPER.readValue(eventData, new TypeReference<>() {});
        map.forEach(genericRecord::put);
        return genericRecord;
    }

    private void sendEvent(String topicName, String messageKey, GenericRecord genericRecord) {
        ProducerRecord<String, GenericRecord> record = new ProducerRecord<>(topicName, messageKey, genericRecord);
        try {
            kafkaProducer.send(record);
        } catch (Exception e) {
            LOGGER.error("Error while sending event on topic '{}'", topicName, e);
        } finally {
            kafkaProducer.flush();
        }
    }
}
