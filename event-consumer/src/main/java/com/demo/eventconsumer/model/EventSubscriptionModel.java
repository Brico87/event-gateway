package com.demo.eventconsumer.model;

public record EventSubscriptionModel(String event, String topic, String callbackUrl) {}
