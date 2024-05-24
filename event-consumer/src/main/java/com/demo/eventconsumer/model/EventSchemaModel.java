package com.demo.eventconsumer.model;

import org.springframework.data.redis.core.RedisHash;

// "id" is the event name
@RedisHash("EventMapping")
public record EventSchemaModel(String id, int schemaId, String topicName) {}
