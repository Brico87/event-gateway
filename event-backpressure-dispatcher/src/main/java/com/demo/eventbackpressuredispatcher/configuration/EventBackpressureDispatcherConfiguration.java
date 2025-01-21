package com.demo.eventbackpressuredispatcher.configuration;

import com.demo.eventbackpressuredispatcher.opa.client.ApiClient;
import org.apache.commons.pool2.impl.GenericObjectPoolConfig;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConfiguration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceClientConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.connection.lettuce.LettucePoolingClientConfiguration;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.time.Duration;

@Configuration
public class EventBackpressureDispatcherConfiguration {

    // TODO: redis connection pool compatible with ACLs
    // https://redis.io/docs/latest/develop/clients/pools-and-muxing/
    // https://redis.io/blog/connection-pools-for-serverless-functions-and-backend-services/
    // https://redis.io/blog/getting-started-redis-6-access-control-lists-acls/
    // To be read: https://redis.io/blog/youre-probably-thinking-about-redis-streams-wrong/

    @Value("${redis.database}")
    private int database;

    @Value("${redis.host}")
    private String hostName;

    @Value("${redis.port}")
    private int port;

    @Value("${redis.password}")
    private String password;

    @Value("${redis.timeout}")
    private long timeout;

    @Value("${redis.pooling.max-total}")
    private int poolingMaxTotal;

    @Value("${redis.pooling.min-idle}")
    private int poolingMinIdle;

    @Value("${redis.pooling.max-idle}")
    private int poolingMaxIdle;

    @Value("${policy-server.base-url}")
    private String policyServerBaseUrl;

    @Bean
    public RedisConfiguration redisConfiguration() {
        RedisStandaloneConfiguration redisConfig = new RedisStandaloneConfiguration(hostName, port);
        redisConfig.setDatabase(database);
        redisConfig.setPassword(password);
        return redisConfig;
    }

    @Bean
    public GenericObjectPoolConfig poolConfiguration() {
        GenericObjectPoolConfig poolConfig = new GenericObjectPoolConfig();
        poolConfig.setMinIdle(poolingMinIdle);
        poolConfig.setMaxIdle(poolingMaxIdle);
        poolConfig.setMaxTotal(poolingMaxTotal);
        return poolConfig;
    }

    @Bean
    public LettuceClientConfiguration clientConfiguration(GenericObjectPoolConfig poolConfiguration){
        return LettucePoolingClientConfiguration.builder()
                .poolConfig(poolConfiguration)
                .commandTimeout(Duration.ofMillis(timeout))
                .build();
    }

    @Bean
    public RedisConnectionFactory lettuceConnectionFactory(RedisConfiguration redisConfiguration, LettuceClientConfiguration clientConfiguration) {
        LettuceConnectionFactory connectionFactory = new LettuceConnectionFactory(redisConfiguration, clientConfiguration);
        connectionFactory.setShareNativeConnection(false);
        return connectionFactory;
    }

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

    @Bean
    public ApiClient policyServerApiClient() {
        ApiClient apiClient = new ApiClient();
        apiClient.setBasePath(policyServerBaseUrl);
        return apiClient;
    }
}
