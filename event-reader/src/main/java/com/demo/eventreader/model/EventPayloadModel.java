package com.demo.eventreader.model;

import java.util.Map;

// Use CloudEvents model
public record EventPayloadModel(Map<String, Object> fields) {}
