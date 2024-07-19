package com.demo.eventregistry.controller;

import com.demo.eventregistry.model.EventSourceModel;
import com.demo.eventregistry.service.EventRegistryService;
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
public class EventRegistryController {

    private static final Logger LOGGER = LoggerFactory.getLogger(EventRegistryController.class);

    private final EventRegistryService eventRegistryService;

    @Autowired
    public EventRegistryController(EventRegistryService eventRegistryService) {
        this.eventRegistryService = eventRegistryService;
    }

    @PostMapping(value = "/contract", consumes = "application/text")
    public ResponseEntity<String> registerContractFromUrl(@RequestBody String url) {
        try {
            eventRegistryService.registerAsyncApiContractFromUrl(url.trim());
            return ResponseEntity.ok("Contract registered successfully !");
        } catch (Exception e) {
            LOGGER.error("Error while registering contract from URL", e);
            return handleException(e);
        }
    }

    @PostMapping(value = "/contract", consumes = "application/x-yaml")
    public ResponseEntity<String> registerContractFromYaml(@RequestBody String yaml) {
        try {
            eventRegistryService.registerAsyncApiContractFromYaml(yaml);
            return ResponseEntity.ok("Contract registered successfully !");
        } catch (Exception e) {
            LOGGER.error("Error while registering contract from YAML payload", e);
            return handleException(e);
        }
    }

    @PostMapping(value = "/contract", consumes = "application/json")
    public ResponseEntity<String> registerContractFromJson(@RequestBody String json) {
        try {
            eventRegistryService.registerAsyncApiContractFromJson(json);
            return ResponseEntity.ok("Contract registered successfully !");
        } catch (Exception e) {
            LOGGER.error("Error while registering contract frpù JSON payload", e);
            return handleException(e);
        }
    }

    @GetMapping("/event")
    public ResponseEntity<List<EventSourceModel>> getEventSources() {
        try {
            List<EventSourceModel> sources = eventRegistryService.getEventSources();
            return ResponseEntity.ok(sources);
        } catch (Exception e) {
            LOGGER.error("Error while getting all event sources", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    // PRIVATE METHODS

    private ResponseEntity<String> handleException(Exception e) {
        if (e instanceof EventRegistryService.InvalidContractException) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } else {
            return ResponseEntity.internalServerError().body(e.getMessage());
        }
    }
}
