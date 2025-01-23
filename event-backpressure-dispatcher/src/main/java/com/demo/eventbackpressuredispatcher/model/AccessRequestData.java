package com.demo.eventbackpressuredispatcher.model;

public record AccessRequestData(String consumer, String resource, UserInfo userInfo) {}
