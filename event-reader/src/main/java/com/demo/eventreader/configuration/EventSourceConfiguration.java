package com.demo.eventreader.configuration;

import com.demo.eventregistry.ApiClient;
import com.demo.eventregistry.api.EventRegistryControllerApi;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class EventSourceConfiguration {

    @Value("${event-source.remote.url}")
    private String remoteUrl;

    @Bean
    public EventRegistryControllerApi remoteEventRegistryApi() {
        ApiClient client = new ApiClient();
        client.setBasePath(remoteUrl);
        return new EventRegistryControllerApi(client);
    }
}
