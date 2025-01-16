package com.demo.eventbackpressuresink.service.redis;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.connection.stream.ObjectRecord;
import org.springframework.data.redis.connection.stream.RecordId;
import org.springframework.data.redis.connection.stream.StreamRecords;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.Objects;

@Service
public class EventRedisSinkService {

    private final RedisTemplate<String, String> redisTemplate;

    @Autowired
    public EventRedisSinkService(RedisTemplate<String, String> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    public String send(String sink, String payload) throws IOException {
        ObjectRecord<String, String> record = StreamRecords.newRecord()
                .ofObject(payload)
                .withStreamKey(sink);

        RecordId recordId = this.redisTemplate.opsForStream().add(record);

        if (Objects.isNull(recordId)) {
            throw new IOException("Error while sending event to Redis Streams sink");
        }

        return recordId.toString();
    }

}
