package com.demo.eventreader.service;

import com.demo.eventreader.model.EventSourceModel;
import com.demo.eventreader.repository.EventSourceRepository;
import com.demo.eventregistry.api.EventRegistryControllerApi;
import org.apache.commons.collections4.IterableUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;

import java.util.List;
import java.util.Optional;

@Service
public class EventSourceService {

    private static final Logger LOGGER = LoggerFactory.getLogger(EventSourceService.class);

    private final EventSourceRepository eventSourceRepository;
    private final EventRegistryControllerApi remoteEventRegistryApi;
    private final boolean remoteEventSourceEnabled;

    public EventSourceService(EventSourceRepository eventSourceRepository,
                              EventRegistryControllerApi remoteEventRegistryApi,
                              @Value("${event-source.remote.enabled}") boolean remoteEventSourceEnabled) {
        this.eventSourceRepository = eventSourceRepository;
        this.remoteEventRegistryApi = remoteEventRegistryApi;
        this.remoteEventSourceEnabled = remoteEventSourceEnabled;
    }

    public List<EventSourceModel> getEventSources() {
        if (remoteEventSourceEnabled) {
            return remoteEventRegistryApi.getEventSources().stream()
                    .map(s -> new EventSourceModel(s.getEvent(), s.getTopic()))
                    .toList();
        } else {
            return IterableUtils.toList(eventSourceRepository.findAll());
        }
    }

    public Optional<String> getEventSource(String eventName) {
        if (remoteEventSourceEnabled) {
            try {
                String topicName = remoteEventRegistryApi.getEventSource(eventName);
                LOGGER.info("Topic name '{}' fetched on remote event registry for '{}'", topicName, eventName);
                return Optional.of(topicName);
            } catch (HttpClientErrorException e) {
                if (HttpStatus.NOT_FOUND.equals(e.getStatusCode())) {
                    return Optional.empty();
                } else {
                    throw e;
                }
            }
        } else {
            return eventSourceRepository.findById(eventName)
                    .map(EventSourceModel::topic);
        }
    }

    public EventSourceModel saveEventSource(String eventName, String topicName) {
        if (remoteEventSourceEnabled) {
            throw new UnsupportedOperationException("Remote source is enabled / Access to event source is read-only");
        } else {
            return eventSourceRepository.save(new EventSourceModel(eventName, topicName));
        }
    }
}
