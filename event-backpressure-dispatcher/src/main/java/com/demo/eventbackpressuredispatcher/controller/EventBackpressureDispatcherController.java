package com.demo.eventbackpressuredispatcher.controller;

import com.demo.eventbackpressuredispatcher.model.EventData;
import com.demo.eventbackpressuredispatcher.service.EventBackpressureSourceService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
public class EventBackpressureDispatcherController {

    private final EventBackpressureSourceService sourceService;

    @Autowired
    public EventBackpressureDispatcherController(EventBackpressureSourceService sourceService) {
        this.sourceService = sourceService;
    }

    @GetMapping("/event")
    @ResponseBody
    public List<EventData> readEvents(@RequestParam String consumer, @RequestParam int count) {
        return sourceService.read("my-stream", count, consumer);
    }
}
