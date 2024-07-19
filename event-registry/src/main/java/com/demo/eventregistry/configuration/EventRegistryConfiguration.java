package com.demo.eventregistry.configuration;

import io.apicurio.registry.rest.client.RegistryClient;
import io.apicurio.registry.rest.client.RegistryClientFactory;
import io.confluent.kafka.schemaregistry.client.CachedSchemaRegistryClient;
import io.confluent.kafka.schemaregistry.client.SchemaRegistryClient;
import org.apache.kafka.clients.admin.Admin;
import org.apache.kafka.clients.admin.AdminClientConfig;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Properties;

@Configuration
public class EventRegistryConfiguration {

    @Value("${kafka.bootstrap-servers}")
    private String bootstrapServers;

    @Value("${kafka.schema-registry-url}")
    private String schemaRegistryUrl;

    @Value("${apicurio.registry-url}")
    private String apicurioRegistryUrl;

    @Bean
    public Admin kafkaAdminClient() {
        Properties properties = new Properties();
        properties.put(AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        return Admin.create(properties);
    }

    @Bean
    public SchemaRegistryClient schemaRegistryClient() {
        return new CachedSchemaRegistryClient(schemaRegistryUrl, 10);
    }

    @Bean
    public RegistryClient apicurioRegistryClient() {
        return RegistryClientFactory.create(apicurioRegistryUrl);
    }
}
