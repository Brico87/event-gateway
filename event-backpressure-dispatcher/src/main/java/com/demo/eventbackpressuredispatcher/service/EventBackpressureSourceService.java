package com.demo.eventbackpressuredispatcher.service;

import com.demo.eventbackpressuredispatcher.model.EventData;
import com.demo.eventbackpressuredispatcher.service.redis.EventRedisSourceService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class EventBackpressureSourceService {

    private static final Logger LOGGER = LoggerFactory.getLogger(EventBackpressureSourceService.class);

    private final EventRedisSourceService sourceService;

    @Autowired
    public EventBackpressureSourceService(EventRedisSourceService sourceService) {
        this.sourceService = sourceService;
    }

    public List<EventData> read(String source, int count, String consumerName) {
        LOGGER.info("Reading source {}: {} asks for {} messages", source, consumerName, count);
        return sourceService.read(source, count, consumerName);
    }
}
