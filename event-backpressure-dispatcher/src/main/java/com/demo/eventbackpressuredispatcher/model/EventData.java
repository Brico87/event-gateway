package com.demo.eventbackpressuredispatcher.model;

import java.util.Map;

public record EventData(String id, Map<String, String> payload) {}
