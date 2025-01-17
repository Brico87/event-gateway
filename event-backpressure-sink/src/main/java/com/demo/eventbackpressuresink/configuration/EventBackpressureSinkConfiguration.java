package com.demo.eventbackpressuresink.configuration;

import org.apache.avro.generic.GenericRecord;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.util.Properties;

@Configuration
public class EventBackpressureSinkConfiguration {

    // TODO: redis connection => https://redis.io/blog/multiplexing-explained/
    // Open one connection and share it between threads
    // https://redis.io/docs/latest/develop/clients/pools-and-muxing/
    // To be read: https://redis.io/blog/youre-probably-thinking-about-redis-streams-wrong/

    @Value("${kafka.bootstrap-servers}")
    private String bootstrapServers;

    @Value("${kafka.schema-registry-url}")
    private String schemaRegistryUrl;

    // BEANS

    @Bean
    public RedisTemplate<String, String> redisTemplate(RedisConnectionFactory connectionFactory) {
        RedisTemplate<String, String> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);
        template.setHashValueSerializer(new StringRedisSerializer());
        template.setKeySerializer(new StringRedisSerializer());
        template.setValueSerializer(new StringRedisSerializer());
        template.setHashKeySerializer(new StringRedisSerializer());
        template.setStringSerializer(new StringRedisSerializer());
        template.afterPropertiesSet();
        return template;
    }

    // BUILDERS

    public KafkaConsumer<String, GenericRecord> buildKafkaConsumer() {
        Properties properties = new Properties();
        properties.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        properties.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, "org.apache.kafka.common.serialization.StringDeserializer");
        properties.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, "io.confluent.kafka.serializers.KafkaAvroDeserializer");
        properties.put(ConsumerConfig.GROUP_ID_CONFIG, "event-backpressure-sink");
        properties.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "latest"); // TO BE DEFINED
        properties.put(ConsumerConfig.ALLOW_AUTO_CREATE_TOPICS_CONFIG, false);
        properties.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, false); // TO BE DEFINED
        properties.put("schema.registry.url", schemaRegistryUrl);
        properties.put("value.subject.name.strategy", "io.confluent.kafka.serializers.subject.RecordNameStrategy");
        properties.put("specific.avro.reader", "false"); // TO PULL GENERICRECORD
        properties.put("auto.register.schemas", "false");
        return new KafkaConsumer<>(properties);
    }
}
