package com.demo.eventconsumer.service;

import com.demo.eventconsumer.model.EventSubscriptionModel;
import com.demo.eventconsumer.repository.EventSchemaRepository;
import com.demo.eventconsumer.repository.EventSubscriptionRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.avro.Schema;
import org.apache.avro.generic.GenericRecord;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.Duration;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.StreamSupport;

@Service
public class EventDispatcherService {

    // Read topic and when message received, call WebClient on webhook
    private static final Logger LOGGER = LoggerFactory.getLogger(EventDispatcherService.class);
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private final EventSubscriptionRepository eventSubscriptionRepository;
    private final EventSchemaRepository eventSchemaRepository;
    private final AtomicBoolean doProcessEvent;

    @Autowired
    public EventDispatcherService(KafkaConsumer<String, GenericRecord> kafkaConsumer,
                                  EventSubscriptionRepository eventSubscriptionRepository,
                                  EventSchemaRepository eventSchemaRepository) {
        this.eventSubscriptionRepository = eventSubscriptionRepository;
        this.eventSchemaRepository = eventSchemaRepository;
        this.doProcessEvent = new AtomicBoolean(false);

        Thread consumerThread = new Thread(() -> consumeEvents(kafkaConsumer), "kafka-consumer-thread");
        consumerThread.setDaemon(true);
        consumerThread.start();
    }

    public void triggerConsumerUpdate() {
        doProcessEvent.set(false);
    }

    // PRIVATE METHODS

    private void consumeEvents(KafkaConsumer<String, GenericRecord> kafkaConsumer) {
        while (true) {
            try {
                List<String> topics = getListenerTopics();
                if (topics.isEmpty()) {
                    LOGGER.warn("No topic to listen / Wait for update");
                    Thread.sleep(5000);
                } else {
                    LOGGER.info("Topics to listen: {}", topics);
                    kafkaConsumer.subscribe(topics);
                    while (doProcessEvent.get()) {
                        ConsumerRecords<String, GenericRecord> records = kafkaConsumer.poll(Duration.ofMillis(100));
                        processRecords(records);
                    }
                    kafkaConsumer.unsubscribe();
                    LOGGER.info("Unsubscribe consumer / Updating topics to listen");
                    doProcessEvent.set(true);
                }
            } catch (Exception e) {
                LOGGER.error("Error occured while consuming events", e);
            }
        }
    }

    private List<String> getListenerTopics() {
        return StreamSupport.stream(eventSubscriptionRepository.findAll().spliterator(), false)
                .map(EventSubscriptionModel::id)
                .distinct()
                .toList();
    }

    private void processRecords(ConsumerRecords<String, GenericRecord> records) {
        for (ConsumerRecord<String, GenericRecord> record : records) {
            LOGGER.info("offset = {}, key = {}, value = {}", record.offset(), record.key(), record.value());
            List<String> callbackUrls = getCallbackUrls(record.topic());
            if (callbackUrls.isEmpty()) {
                LOGGER.info("No subscription linked to events on topic '{}'", record.topic());
            } else {
                callbackUrls.forEach(callbackUrl -> dispatchEvent(record.value(), callbackUrl));
            }
        }
    }

    private List<String> getCallbackUrls(String topicName) {
        return eventSubscriptionRepository.findById(topicName)
                .map(EventSubscriptionModel::callbackUrls)
                .orElseGet(() -> Collections.emptyList());
    }

    private void dispatchEvent(GenericRecord genericRecord, String callbackUrl) {
        // SETUP RETRY POLICY AND MULTI-THREADING USING LINKED QUEUE
        try {
            LOGGER.info("Sending data to '{}'", callbackUrl);
            //NOT NEEDED ? String payload = OBJECT_MAPPER.writeValueAsString(convertToMap(genericRecord));
            WebClient.create(callbackUrl)
                    .post()
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(convertToMap(genericRecord))
                    .retrieve()
                    .bodyToMono(Void.class)
                    .block();
        } catch (Exception e) {
            LOGGER.error("Error occurred when sending data to '{}'", callbackUrl, e);
        }
    }

    private Map<String, Object> convertToMap(GenericRecord genericRecord) {
        Map<String, Object> values = new HashMap<>();
        for (Schema.Field field : genericRecord.getSchema().getFields()) {
            values.put(field.name(), genericRecord.get(field.name()));
        }
        return values;
    }
}
