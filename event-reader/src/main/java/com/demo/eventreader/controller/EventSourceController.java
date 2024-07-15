package com.demo.eventreader.controller;

import com.demo.eventreader.model.EventSourceModel;
import com.demo.eventreader.repository.EventSourceRepository;
import org.apache.commons.collections4.IterableUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
public class EventSourceController {

    public record EventSourceRegisterRequest(String event, String topic) {}

    private static final Logger LOGGER = LoggerFactory.getLogger(EventSourceController.class);

    private final EventSourceRepository eventSourceRepository;

    @Autowired
    public EventSourceController(EventSourceRepository eventSourceRepository) {
        this.eventSourceRepository = eventSourceRepository;
    }

    @GetMapping("/register")
    public ResponseEntity<List<EventSourceModel>> getSources() {
        try {
            List<EventSourceModel> sources = IterableUtils.toList(this.eventSourceRepository.findAll());
            return ResponseEntity.ok(sources);
        } catch (Exception e) {
            LOGGER.error("Error while getting all event sources", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    @PostMapping("/register")
    public ResponseEntity<EventSourceModel> registerSource(@RequestBody EventSourceRegisterRequest registerRequest) {
        try {
            EventSourceModel saved = this.eventSourceRepository.save(new EventSourceModel(registerRequest.event, registerRequest.topic));
            return ResponseEntity.ok(saved);
        } catch (Exception e) {
            LOGGER.error("Error while registering source '{}' for event '{}'", registerRequest.topic, registerRequest.event, e);
            return ResponseEntity.internalServerError().build();
        }
    }
}
