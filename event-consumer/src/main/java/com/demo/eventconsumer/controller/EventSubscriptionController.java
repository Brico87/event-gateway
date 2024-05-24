package com.demo.eventconsumer.controller;

import com.demo.eventconsumer.model.EventSubscriptionModel;
import com.demo.eventconsumer.repository.EventSubscriptionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/subscription")
public class EventSubscriptionController {

    public record EventSubscriptionRequest(String callbackUrl) {}

    private final EventSubscriptionRepository repository;

    @Autowired
    public EventSubscriptionController(EventSubscriptionRepository repository) {
        this.repository = repository;
    }

    @PostMapping("/{eventName}/{topicName}")
    public ResponseEntity<Void> subscribe(@PathVariable String eventName,
                                          @PathVariable String topicName,
                                          @RequestBody EventSubscriptionRequest subscriptionRequest) {
        repository.setSubscription(new EventSubscriptionModel(eventName, topicName, subscriptionRequest.callbackUrl()));
        return ResponseEntity.accepted().build();
    }

    @DeleteMapping("/{eventName}")
    public ResponseEntity<Void> unsubscribe(@PathVariable String eventName) {
        repository.deleteSubscription(eventName);
        return ResponseEntity.accepted().build();
    }
}
