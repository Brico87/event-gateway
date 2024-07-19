package com.demo.eventregistry.configuration;

import org.apache.kafka.clients.admin.AdminClientConfig;
import org.springframework.beans.factory.annotation.Value;
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

    public Properties getKafkaProperties() {
        Properties properties = new Properties();
        properties.put(AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        return properties;
    }

    public String getSchemaRegistryUrl() {
        return this.schemaRegistryUrl;
    }

    public String getApicurioRegistryUrl() {
        return this.apicurioRegistryUrl;
    }
}
