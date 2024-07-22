package com.demo.eventreader.service;

import com.demo.eventreader.model.EventPayloadModel;
import com.demo.eventreader.service.factory.KafkaConsumerFactory;
import org.apache.avro.Schema;
import org.apache.avro.generic.GenericRecord;
import org.apache.commons.lang3.time.StopWatch;
import org.apache.kafka.clients.consumer.*;
import org.apache.kafka.common.PartitionInfo;
import org.apache.kafka.common.TopicPartition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Service
public class EventReaderService {

    public record EventPaginationResult(List<EventPayloadModel> data) {}

    private static final Logger LOGGER = LoggerFactory.getLogger(EventReaderService.class);
    private static final long READ_TIMEOUT_MS = 10000; // 10 seconds to fetch all reads
    private static final long KAFKA_POLL_TIMEOUT_MS = 100;

    private final KafkaConsumerFactory consumerFactory;

    @Autowired
    public EventReaderService(KafkaConsumerFactory consumerFactory) {
        this.consumerFactory = consumerFactory;
    }

    // TODO: topic name information must come from a DB
    public EventPaginationResult readEvents(String consumerGroupId, String topic, Long timestamp, int numberOfEventsToRead) {
        EventPaginationResult res = new EventPaginationResult(new ArrayList<>());
        try (KafkaConsumer<String, GenericRecord> kafkaConsumer = consumerFactory.buildKafkaListener(consumerGroupId)) {
            configureKafkaConsumer(kafkaConsumer, topic, timestamp);
            res = readEvents(kafkaConsumer, numberOfEventsToRead);
            kafkaConsumer.unsubscribe();
        } catch (Exception e) {
            LOGGER.error("Error while reading events for '{}' on topic '{}'", consumerGroupId, topic, e);
        }
        return res;
    }

    // PRIVATE METHODS

    private static void configureKafkaConsumer(KafkaConsumer<String, GenericRecord> kafkaConsumer, String topic, Long timestamp) {
        if (timestamp != null && timestamp < System.currentTimeMillis()) {
            try {
                configureKafkaConsumerUsingTimestamp(kafkaConsumer, topic, timestamp);
            } catch (Exception e) {
                kafkaConsumer.unsubscribe();
                LOGGER.warn("Error while configuring kafka consumer using timestamp / Switching to subscribe mode", e);
                kafkaConsumer.subscribe(List.of(topic));
            }
        } else {
            kafkaConsumer.subscribe(List.of(topic));
        }
    }

    private static void configureKafkaConsumerUsingTimestamp(KafkaConsumer<String, GenericRecord> kafkaConsumer, String topic, Long timestamp) {
        // Get the list of partitions
        List<PartitionInfo> partitionInfos = kafkaConsumer.partitionsFor(topic);
        // Transform PartitionInfo into TopicPartition
        List<TopicPartition> topicPartitionList = partitionInfos
                .stream()
                .map(info -> new TopicPartition(topic, info.partition()))
                .toList();
        // Assign the consumer to these partitions
        kafkaConsumer.assign(topicPartitionList);
        // Look for offsets based on timestamp
        Map<TopicPartition, Long> partitionTimestampMap = topicPartitionList.stream()
                .collect(Collectors.toMap(tp -> tp, tp -> timestamp));
        Map<TopicPartition, OffsetAndTimestamp> partitionOffsetMap = kafkaConsumer.offsetsForTimes(partitionTimestampMap);
        for (OffsetAndTimestamp offset : partitionOffsetMap.values()) {
            if (Objects.isNull(offset)) {
                throw new IllegalStateException("Fetched OffsetAndTimestamp object is null for timestamp '" + timestamp + "'");
            }
        }
        // Force the consumer to seek for those offsets
        partitionOffsetMap.forEach((tp, offsetAndTimestamp) -> kafkaConsumer.seek(tp, offsetAndTimestamp.offset()));
    }

    private static EventPaginationResult readEvents(KafkaConsumer<String, GenericRecord> kafkaConsumer, int numberOfEventsToRead) {
        List<EventPayloadModel> res = new ArrayList<>();
        int numberOfEventsReadSoFar = 0;
        StopWatch stopWatch = StopWatch.createStarted();
        Map<TopicPartition, OffsetAndMetadata> currentOffsets = new HashMap<>();

        // poll for new data
        while (numberOfEventsReadSoFar < numberOfEventsToRead && stopWatch.getTime(TimeUnit.MILLISECONDS) < READ_TIMEOUT_MS) {
            ConsumerRecords<String, GenericRecord> records = kafkaConsumer.poll(Duration.ofMillis(KAFKA_POLL_TIMEOUT_MS));
            for (ConsumerRecord<String, GenericRecord> record : records) {
                LOGGER.debug("Key: {}, partition: {}, offset: {}", record.key(), record.partition(), record.offset());
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

        LOGGER.debug("Events read: {} in {} sec", numberOfEventsReadSoFar, stopWatch.getTime(TimeUnit.SECONDS));

        return new EventPaginationResult(res);
    }

    private static Map<String, String> convertToMap(GenericRecord genericRecord) {
        Map<String, String> values = new HashMap<>();
        for (Schema.Field field : genericRecord.getSchema().getFields()) {
            String value = genericRecord.get(field.name()).toString();
            values.put(field.name(), value);
        }
        return values;
    }
}
