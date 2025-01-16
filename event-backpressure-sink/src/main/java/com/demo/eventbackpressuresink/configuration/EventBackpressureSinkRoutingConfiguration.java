package com.demo.eventbackpressuresink.configuration;

import com.demo.eventbackpressuresink.model.EventRouting;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@ConfigurationProperties(prefix = "application")
public class EventBackpressureSinkRoutingConfiguration {

    private List<EventRouting> routing;

    public List<EventRouting> getRouting() {
        return routing;
    }

    public void setRouting(List<EventRouting> routing) {
        this.routing = routing;
    }
}