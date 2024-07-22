package com.demo.eventreader.controller;

import com.demo.eventreader.model.EventPayloadModel;
import com.demo.eventreader.service.EventReaderService;
import com.demo.eventreader.service.EventSourceService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Optional;

@RestController
public class EventReaderController {

    public record EventReadRequest(String clientName, String eventName, Long timestamp, int count) {}
    public record EventReadResponse(int count, List<EventPayloadModel> data) {}

    private static final Logger LOGGER = LoggerFactory.getLogger(EventReaderController.class);

    private final EventReaderService eventReaderService;
    private final EventSourceService eventSourceService;

    @Autowired
    public EventReaderController(EventReaderService eventReaderService,
                                 EventSourceService eventSourceService) {
        this.eventReaderService = eventReaderService;
        this.eventSourceService = eventSourceService;
    }

    @GetMapping("/read")
    public ResponseEntity<EventReadResponse> readEvents(@RequestBody EventReadRequest readRequest) {
        try {
            String consumerGroupId = readRequest.clientName + "_" + readRequest.eventName;
            Optional<String> topicNameFound = eventSourceService.getEventSource(readRequest.eventName);
            if (topicNameFound.isPresent()) {
                EventReaderService.EventPaginationResult res = eventReaderService.readEvents(consumerGroupId, topicNameFound.get(), readRequest.timestamp, readRequest.count);
                LOGGER.info("[{}] Read {} events on '{}'", readRequest.clientName, res.data().size(), topicNameFound.get());
                return ResponseEntity.ok(new EventReadResponse(res.data().size(), res.data()));
            } else {
                LOGGER.warn("Event with name '{}' has no associated topic", readRequest.eventName);
                return ResponseEntity.notFound().build();
            }
        } catch (Exception e) {
            LOGGER.error("Error while reading events from stream '{}'", readRequest.eventName, e);
            return ResponseEntity.internalServerError().build();
        }
    }
}
