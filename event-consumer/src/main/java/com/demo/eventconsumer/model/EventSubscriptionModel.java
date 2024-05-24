package com.demo.eventconsumer.model;


import org.springframework.data.redis.core.RedisHash;

import java.util.List;

// "id" is the topic name
@RedisHash("EventSubscription")
public record EventSubscriptionModel(String id, List<String> callbackUrls) {}
