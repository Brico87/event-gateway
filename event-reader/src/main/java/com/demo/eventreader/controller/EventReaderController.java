package com.demo.eventreader.controller;

import com.demo.eventreader.model.EventPayloadModel;
import com.demo.eventreader.repository.EventSourceRepository;
import com.demo.eventreader.service.EventReaderService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
public class EventReaderController {

    public record EventReadRequest(String clientName, String eventName, Long timestamp, int count) {}
    public record EventReadResponse(int count, List<EventPayloadModel> data) {}

    private static final Logger LOGGER = LoggerFactory.getLogger(EventReaderController.class);

    private final EventReaderService eventReaderService;
    private final EventSourceRepository eventSourceRepository;

    @Autowired
    public EventReaderController(EventReaderService eventReaderService,
                                 EventSourceRepository eventSourceRepository) {
        this.eventReaderService = eventReaderService;
        this.eventSourceRepository = eventSourceRepository;
    }

    @GetMapping("/read")
    public ResponseEntity<EventReadResponse> readEvents(@RequestBody EventReadRequest readRequest) {
        try {
            String consumerGroupId = readRequest.clientName + "_" + readRequest.eventName;
            String topicName = eventSourceRepository.findById(readRequest.eventName)
                    .orElseThrow(() -> new IllegalArgumentException("Event with name '" + readRequest.eventName + "' has no associated topic"))
                    .topic();
            EventReaderService.EventPaginationResult res = eventReaderService.readEvents(consumerGroupId, topicName, readRequest.timestamp, readRequest.count);
            LOGGER.info("[{}] Read {} events on '{}'", readRequest.clientName, res.data().size(), topicName);
            return ResponseEntity.ok(new EventReadResponse(res.data().size(), res.data()));
        } catch (Exception e) {
            LOGGER.error("Error while reading events from stream '{}'", readRequest.eventName, e);
            return ResponseEntity.internalServerError().build();
        }
    }
}
