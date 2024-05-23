package com.demo.eventproducer.repository;

import org.springframework.stereotype.Repository;

import java.util.Map;
import java.util.Optional;

@Repository
public class EventSchemaRepository {

    private final Map<String, Integer> eventSchemaTable;

    public EventSchemaRepository() {
        // The schema "com.demo.schema.TestPayload" must be created with the id 1
        // in the Schema Registry so we can link the event named "test" with it
        eventSchemaTable = Map.of("test", 1);
    }

    public Optional<Integer> fetchSchemaIdByEventName(String eventName) {
        return Optional.ofNullable(eventSchemaTable.get(eventName.toLowerCase()));
    }
}
