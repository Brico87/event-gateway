package com.demo.eventreader.service;

import com.demo.eventreader.model.EventPayloadModel;
import com.demo.eventreader.service.factory.KafkaConsumerFactory;
import org.apache.avro.Schema;
import org.apache.avro.generic.GenericRecord;
import org.apache.commons.lang3.time.StopWatch;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.consumer.OffsetAndMetadata;
import org.apache.kafka.common.TopicPartition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Service
public class EventReaderService {

    private static final Logger LOGGER = LoggerFactory.getLogger(EventReaderService.class);
    private static final long READ_TIMEOUT_MS = 10000; // 10 seconds to fetch all reads
    private static final long KAFKA_POLL_TIMEOUT_MS = 100;

    private final KafkaConsumerFactory consumerFactory;

    @Autowired
    public EventReaderService(KafkaConsumerFactory consumerFactory) {
        this.consumerFactory = consumerFactory;
    }

    // TODO: topic name information must come from a DB
    public List<EventPayloadModel> readEvents(String consumerGroupId, String topic, int numberOfEventsToRead) {
        List<EventPayloadModel> res = new ArrayList<>();
        try (KafkaConsumer<String, GenericRecord> kafkaConsumer = consumerFactory.buildKafkaListener(consumerGroupId)) {
            kafkaConsumer.subscribe(List.of(topic));
            res = readEvents(kafkaConsumer, numberOfEventsToRead);
            kafkaConsumer.unsubscribe();
        } catch (Exception e) {
            LOGGER.error("Error while reading events for '{}' on topic '{}'", consumerGroupId, topic);
        }
        return res;
    }

    // PRIVATE METHODS

    private List<EventPayloadModel> readEvents(KafkaConsumer<String, GenericRecord> kafkaConsumer, int numberOfEventsToRead) {
        List<EventPayloadModel> res = new ArrayList<>();
        int numberOfEventsReadSoFar = 0;
        StopWatch stopWatch = StopWatch.createStarted();
        Map<TopicPartition, OffsetAndMetadata> currentOffsets = new HashMap<>();

        // poll for new data
        while (numberOfEventsReadSoFar < numberOfEventsToRead && stopWatch.getTime(TimeUnit.MILLISECONDS) < READ_TIMEOUT_MS) {
            ConsumerRecords<String, GenericRecord> records = kafkaConsumer.poll(Duration.ofMillis(KAFKA_POLL_TIMEOUT_MS));
            for (ConsumerRecord<String, GenericRecord> record : records) {
                LOGGER.info("Key: {}, partition: {}, offset: {}", record.key(), record.partition(), record.offset());
                res.add(new EventPayloadModel(convertToMap(record.value())));
                currentOffsets.put(new TopicPartition(record.topic(), record.partition()), new OffsetAndMetadata(record.offset() + 1));
                numberOfEventsReadSoFar++;
                if (numberOfEventsReadSoFar >= numberOfEventsToRead) {
                    break;
                }
            }
            kafkaConsumer.commitSync(currentOffsets);
        }

        stopWatch.stop();

        LOGGER.info("Events read: {} in {} sec", numberOfEventsReadSoFar, stopWatch.getTime(TimeUnit.SECONDS));


        return res;
    }

    private Map<String, Object> convertToMap(GenericRecord genericRecord) {
        Map<String, Object> values = new HashMap<>();
        for (Schema.Field field : genericRecord.getSchema().getFields()) {
            values.put(field.name(), genericRecord.get(field.name()));
        }
        return values;
    }
}
