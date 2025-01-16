package com.demo.eventbackpressuresink.service;

import com.demo.eventbackpressuresink.configuration.EventBackpressureSinkConfiguration;
import com.demo.eventbackpressuresink.configuration.EventBackpressureSinkRoutingConfiguration;
import com.demo.eventbackpressuresink.model.EventRouting;
import com.demo.eventbackpressuresink.service.redis.EventRedisSinkService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.avro.generic.GenericRecord;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class EventBackpressureSinkService {

    private static final Logger LOGGER = LoggerFactory.getLogger(EventBackpressureSinkService.class);
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    @Autowired
    public EventBackpressureSinkService(EventBackpressureSinkConfiguration configuration,
                                        EventBackpressureSinkRoutingConfiguration routingConfiguration,
                                        EventRedisSinkService sinkService) {
        Thread consumerThread = new Thread(
                () -> consumeEvents(configuration.buildKafkaConsumer(), routingConfiguration.getRouting(), sinkService),
                "kafka-consumer-thread"
        );
        consumerThread.setDaemon(true);
        consumerThread.start();
    }

    // PRIVATE METHODS

    private void consumeEvents(KafkaConsumer<String, GenericRecord> kafkaConsumer,
                               List<EventRouting> routingList,
                               EventRedisSinkService sinkService) {
        while (true) {
            try {
                Map<String, String> routingMap = routingList.stream().collect(Collectors.toMap(EventRouting::source, EventRouting::sink));
                List<String> topics = routingMap.keySet().stream().toList();
                LOGGER.info("Topics to listen: {}", topics);
                kafkaConsumer.subscribe(topics);
                while (true) {
                    ConsumerRecords<String, GenericRecord> records = kafkaConsumer.poll(Duration.ofMillis(100));
                    processRecords(records, routingMap, sinkService);
                    kafkaConsumer.commitSync();
                }
            } catch (Exception e) {
                LOGGER.error("Error occurred while consuming events", e);
                try {
                    Thread.sleep(5000);
                } catch (Exception ignored) {
                }
            }
        }
    }

    private void processRecords(ConsumerRecords<String, GenericRecord> records, Map<String, String> routingMap, EventRedisSinkService sinkService) throws IOException {
        for (ConsumerRecord<String, GenericRecord> record : records) {
            String sinkName = routingMap.get(record.topic());
            LOGGER.info("Dispatch event offset = {}, key = {}, value = {} to sink '{}'", record.offset(), record.key(), record.value(), sinkName);
            String payload = record.value().toString();
            String id = sinkService.send(sinkName, payload);
            LOGGER.info("Event dispatched to sink '{}': {}", sinkName, id);
        }
    }
}
