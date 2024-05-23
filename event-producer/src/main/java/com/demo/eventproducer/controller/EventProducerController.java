package com.demo.eventproducer.controller;

import com.demo.eventproducer.service.EventProducerService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
public class EventProducerController {

    private static final Logger LOGGER = LoggerFactory.getLogger(EventProducerController.class);
    private final EventProducerService service;

    @Autowired
    public EventProducerController(EventProducerService service) {
        this.service = service;
    }
    
    @PostMapping("/event/{eventName}")
    public ResponseEntity<String> sendEventTestPayload(@PathVariable String eventName,
                                                       @RequestBody String eventData) {
        try {
            String clientId = "1111"; // GET CLIENT ID FROM HEADER/COOKIE ?

            // We could store the event in a internal LinkedBlockingQueue as a String (or in a "technical" Kafka topic)
            // and then answer right away to the customer with an OK status saying "will be sent"
            service.send(buildUniqueKey(clientId), eventName, eventData);

            return ResponseEntity.ok("Sent");
        } catch (Exception e) {
            LOGGER.error("Error while sending event", e);
            return ResponseEntity.internalServerError().body("Error");
        }
    }

    // PRIVATE METHODS

    private String buildUniqueKey(String clientId) {
        return clientId + "_" + UUID.randomUUID().toString();
    }
}
