package com.demo.eventconsumer.service;

import com.demo.eventconsumer.configuration.EventConsumerConfiguration;
import com.demo.eventconsumer.model.EventSubscriptionModel;
import com.demo.eventconsumer.repository.EventSubscriptionRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.avro.Schema;
import org.apache.avro.generic.GenericRecord;
import org.apache.kafka.clients.consumer.Consumer;
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
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Service
public class EventDispatcherService {

    // Read topic and when message received, call WebClient on webhook
    private static final Logger LOGGER = LoggerFactory.getLogger(EventDispatcherService.class);
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private final EventConsumerConfiguration eventConsumerConfiguration;
    private final EventSubscriptionRepository eventSubscriptionRepository;

    private Thread dispatcherThread;

    @Autowired
    public EventDispatcherService(EventConsumerConfiguration eventConsumerConfiguration,
                                  EventSubscriptionRepository eventSubscriptionRepository) {
        this.eventConsumerConfiguration = eventConsumerConfiguration;
        this.eventSubscriptionRepository = eventSubscriptionRepository;

        this.dispatcherThread = new Thread(() -> consumeEvents("test-topic"));
        this.dispatcherThread.setDaemon(true);
        this.dispatcherThread.start();
    }

    // PRIVATE METHODS

    private void consumeEvents(String topicName) {
        try (Consumer<String, GenericRecord> consumer = new KafkaConsumer<>(eventConsumerConfiguration.getConsumerProperties())) {
            consumer.subscribe(List.of(topicName));
            while (true) {
                ConsumerRecords<String, GenericRecord> records = consumer.poll(Duration.ofMillis(100));
                processRecords(topicName, records);
            }
        } catch (Exception e) {
            LOGGER.error("Error occured while consuming events", e);
        }
    }

    private void processRecords(String topicName, ConsumerRecords<String, GenericRecord> records) {
        for (ConsumerRecord<String, GenericRecord> record : records) {
            LOGGER.info("offset = {}, key = {}, value = {}", record.offset(), record.key(), record.value());
            List<EventSubscriptionModel> subscriptions = eventSubscriptionRepository.getSubscriptionsForTopic(topicName);
            if (subscriptions.isEmpty()) {
                LOGGER.info("No subscription linked to events on topic '{}'", topicName);
            } else {
                subscriptions.forEach(subscription -> dispatchEvent(record.value(), subscription.callbackUrl()));
            }
        }
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
