package com.demo.eventconsumer.controller;

import com.demo.eventconsumer.model.EventSchemaModel;
import com.demo.eventconsumer.model.EventSubscriptionModel;
import com.demo.eventconsumer.repository.EventSchemaRepository;
import com.demo.eventconsumer.repository.EventSubscriptionRepository;
import com.demo.eventconsumer.service.EventDispatcherService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;

@RestController
@RequestMapping("/subscription")
public class EventSubscriptionController {

    public record EventSubscriptionRequest(String callbackUrl) {}

    private static final Logger LOGGER = LoggerFactory.getLogger(EventSubscriptionController.class);
    private final EventSubscriptionRepository eventSubscriptionRepository;
    private final EventSchemaRepository eventSchemaRepository;
    private final EventDispatcherService eventDispatcherService;

    @Autowired
    public EventSubscriptionController(EventSubscriptionRepository eventSubscriptionRepository,
                                       EventSchemaRepository eventSchemaRepository,
                                       EventDispatcherService eventDispatcherService) {
        this.eventSubscriptionRepository = eventSubscriptionRepository;
        this.eventSchemaRepository = eventSchemaRepository;
        this.eventDispatcherService = eventDispatcherService;
    }

    @PostMapping("/{eventName}")
    public ResponseEntity<String> subscribe(@PathVariable String eventName,
                                            @RequestBody EventSubscriptionRequest subscriptionRequest) {
        try {
            String topicName = fetchTopicByEventName(eventName);
            EventSubscriptionModel eventSubscriptionModel = eventSubscriptionRepository.findById(topicName)
                    .orElseGet(() -> initSubscription(topicName));
            if (!eventSubscriptionModel.callbackUrls().contains(subscriptionRequest.callbackUrl())) {
                eventSubscriptionModel.callbackUrls().add(subscriptionRequest.callbackUrl());
            }
            eventSubscriptionRepository.save(eventSubscriptionModel);
            eventDispatcherService.triggerConsumerUpdate();
            return ResponseEntity.accepted().build();
        } catch (Exception e) {
            LOGGER.error("Error while subscribing to an event stream", e);
            return ResponseEntity.internalServerError().body("Error");
        }
    }

    @DeleteMapping("/{eventName}")
    public ResponseEntity<String> unsubscribe(@PathVariable String eventName) {
        try {
            String topicName = fetchTopicByEventName(eventName);
            eventSubscriptionRepository.deleteById(topicName);
            return ResponseEntity.accepted().build();
        } catch (Exception e) {
            LOGGER.error("Error while unsubscribing to an event stream", e);
            return ResponseEntity.internalServerError().body("Error");
        }
    }

    // PRIVATE METHODS

    private String fetchTopicByEventName(String event) {
        return eventSchemaRepository.findById(event)
                .map(EventSchemaModel::topicName)
                .orElseThrow(() -> new IllegalArgumentException("Event '" + event + "' not known in database"));
    }

    private EventSubscriptionModel initSubscription(String topicName) {
        return new EventSubscriptionModel(topicName, new ArrayList<>());
    }
}
