package com.demo.eventreader.service.factory;

import org.apache.avro.generic.GenericRecord;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Properties;

@Service
public class KafkaConsumerFactory {

    private final String bootstrapServers;
    private final String schemaRegistryUrl;

    @Autowired
    public KafkaConsumerFactory(@Value("${kafka.bootstrap-servers}") String bootstrapServers,
                                @Value("${kafka.schema-registry-url}") String schemaRegistryUrl) {
        this.bootstrapServers = bootstrapServers;
        this.schemaRegistryUrl = schemaRegistryUrl;
    }

    public KafkaConsumer<String, GenericRecord> buildKafkaListener(String consumerGroupId) {
        Properties properties = new Properties();
        properties.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        properties.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, "org.apache.kafka.common.serialization.StringDeserializer");
        properties.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, "io.confluent.kafka.serializers.KafkaAvroDeserializer");
        properties.put(ConsumerConfig.GROUP_ID_CONFIG, consumerGroupId);
        properties.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest"); // Means that we read the topic from the beginning at consumer group creation
        properties.put(ConsumerConfig.ALLOW_AUTO_CREATE_TOPICS_CONFIG, false); // Topics shall exist on brokers
        properties.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, false);
        properties.put("schema.registry.url", schemaRegistryUrl);
        properties.put("value.subject.name.strategy", "io.confluent.kafka.serializers.subject.RecordNameStrategy");
        properties.put("specific.avro.reader", "false"); // To pull and get GenericRecord and not SpecificRecord with typing (TO BE DEFINED if we use CloudEvents)
        properties.put("auto.register.schemas", "false"); // Schemas shall exist on registry
        return new KafkaConsumer<>(properties);
    }
}
