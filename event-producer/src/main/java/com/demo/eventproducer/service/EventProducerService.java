package com.demo.eventproducer.service;

import com.demo.eventproducer.repository.EventSchemaRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.avro.Schema;
import org.apache.avro.generic.GenericData;
import org.apache.avro.generic.GenericRecord;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.stream.function.StreamBridge;
import org.springframework.cloud.stream.schema.client.SchemaRegistryClient;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
public class EventProducerService {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private final StreamBridge streamBridge;
    private final SchemaRegistryClient schemaRegistryClient;
    private final EventSchemaRepository eventSchemaRepository;
    private final String bindingName;

    @Autowired
    public EventProducerService(StreamBridge streamBridge,
                                SchemaRegistryClient schemaRegistryClient,
                                EventSchemaRepository eventSchemaRepository,
                                @Value("${event-producer.binding-name}") String bindingName) {
        // POSSIBILITY TO HAVE ASYNC OPS USING .setAsync(true)
        // Ref: https://docs.spring.io/spring-cloud-stream/reference/spring-cloud-stream/producing-and-consuming-messages.html#_streambridge_with_async_send
        this.streamBridge = streamBridge;
        this.schemaRegistryClient = schemaRegistryClient;
        this.eventSchemaRepository = eventSchemaRepository;
        this.bindingName = bindingName;
    }

    public void send(String messageKey, String eventName, String eventData) throws JsonProcessingException {
        String schemaDefinition = fetchSchemaDefinition(eventName);
        GenericRecord genericRecord = buildGenericRecord(eventData, schemaDefinition);
        Message<GenericRecord> message = buildStreamMessage(messageKey, genericRecord);
        streamBridge.send(bindingName, message);
    }

    // PRIVATE METHODS

    private String fetchSchemaDefinition(String eventName) {
        int schemaId = fetchEventSchemaId(eventName);
        return schemaRegistryClient.fetch(schemaId);
    }

    private int fetchEventSchemaId(String eventName) {
        return eventSchemaRepository.fetchSchemaIdByEventName(eventName)
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

    private static Message<GenericRecord> buildStreamMessage(String messageKey, GenericRecord genericRecord) {
        return MessageBuilder.withPayload(genericRecord)
                .setHeader(KafkaHeaders.KEY, messageKey) // KEY FORMAT TO DEFINE: USE AVRO SCHEMA ?
                .build();
    }
}
