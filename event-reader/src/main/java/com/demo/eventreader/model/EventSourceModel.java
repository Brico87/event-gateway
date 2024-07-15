package com.demo.eventreader.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.redis.core.RedisHash;

// "id" is the event name
@RedisHash("EventSource")
public record EventSourceModel(@Id String event, String topic) {}
