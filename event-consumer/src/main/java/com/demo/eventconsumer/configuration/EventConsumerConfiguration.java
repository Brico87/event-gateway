package com.demo.eventconsumer.configuration;

import org.apache.avro.generic.GenericRecord;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Properties;

@Configuration
public class EventConsumerConfiguration {

    @Value("${kafka.bootstrap-servers}")
    private String bootstrapServers;

    @Value("${kafka.schema-registry-url}")
    private String schemaRegistryUrl;

    @Bean
    public KafkaConsumer<String, GenericRecord> kafkaConsumer() {
        Properties properties = new Properties();
        properties.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        properties.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, "org.apache.kafka.common.serialization.StringDeserializer");
        properties.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, "io.confluent.kafka.serializers.KafkaAvroDeserializer");
        properties.put(ConsumerConfig.GROUP_ID_CONFIG, "event-gateway");
        properties.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "latest"); // TO BE DEFINED
        properties.put(ConsumerConfig.ALLOW_AUTO_CREATE_TOPICS_CONFIG, false);
        properties.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, true); // TO BE DEFINED
        properties.put("schema.registry.url", schemaRegistryUrl);
        properties.put("value.subject.name.strategy", "io.confluent.kafka.serializers.subject.RecordNameStrategy");
        properties.put("specific.avro.reader", "false"); // TO PULL GENERICRECORD
        properties.put("auto.register.schemas", "false");
        return new KafkaConsumer<>(properties);
    }
}
