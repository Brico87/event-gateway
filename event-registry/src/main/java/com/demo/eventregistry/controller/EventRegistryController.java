package com.demo.eventregistry.controller;

import com.demo.eventregistry.model.EventSourceModel;
import com.demo.eventregistry.service.EventRegistryService;
import com.google.common.net.HttpHeaders;
import io.swagger.v3.oas.annotations.Hidden;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
public class EventRegistryController {

    private static final Logger LOGGER = LoggerFactory.getLogger(EventRegistryController.class);

    private final EventRegistryService eventRegistryService;
    private final String landingUrl;

    @Autowired
    public EventRegistryController(EventRegistryService eventRegistryService,
                                   @Value("${springdoc.swagger-ui.path}") String landingUrl) {
        this.eventRegistryService = eventRegistryService;
        this.landingUrl = landingUrl;
    }

    @Hidden
    @GetMapping("/")
    public ResponseEntity<Void> redirectLandingUrl() {
        return ResponseEntity.status(HttpStatus.PERMANENT_REDIRECT)
                .header(HttpHeaders.LOCATION, landingUrl)
                .build();
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

    @GetMapping("/event/{eventName}")
    public ResponseEntity<String> getEventSource(@PathVariable String eventName) {
        try {
            return ResponseEntity.of(eventRegistryService.getEventSource(eventName));
        } catch (Exception e) {
            LOGGER.error("Error while getting event source for '{}'", eventName, e);
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
