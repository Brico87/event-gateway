package com.demo.eventbackpressuredispatcher.service.redis;

import com.demo.eventbackpressuredispatcher.model.EventData;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.lettuce.core.RedisBusyException;
import io.lettuce.core.RedisCommandExecutionException;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.RedisSystemException;
import org.springframework.data.redis.connection.stream.*;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StreamOperations;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Service
public class EventRedisSourceService {

    private static final Logger LOGGER = LoggerFactory.getLogger(EventRedisSourceService.class);

    private final RedisTemplate<String, String> redisTemplate;
    private final ObjectMapper objectMapper;

    @Autowired
    public EventRedisSourceService(RedisTemplate<String, String> redisTemplate) {
        this.redisTemplate = redisTemplate;
        this.objectMapper = new ObjectMapper();
    }

    @PostConstruct
    private void createConsumerGroups() {
        createConsumerGroup("my-stream", "toto");
        createConsumerGroup("my-stream", "titi");
    }

    public List<EventData> read(String source, int count, String consumerName) {
        StreamOperations<String, String, String> streamOperations = redisTemplate.opsForStream();
        StreamOffset<String> stringStreamOffset = StreamOffset.create(source, ReadOffset.lastConsumed());

        // RECLAIM PENDING MESSAGES: https://redis.io/docs/latest/commands/xpending/

        List<MapRecord<String, String, String>> messages = streamOperations.read(
                Consumer.from(consumerName, consumerName + "-0"),
                StreamReadOptions.empty().count(count),
                stringStreamOffset
        );

        if (!Objects.isNull(messages)) {
            return messages.stream()
                    .map(this::processRecord)
                    .toList();
        }

        return Collections.emptyList();
    }

    public long acknowledge(String source, String consumerName, String recordId) {
        StreamOperations<String, String, String> streamOperations = redisTemplate.opsForStream();
        return streamOperations.acknowledge(source, consumerName, recordId);
    }

    // PRIVATE METHODS

    private void createConsumerGroup(String stream, String group) {
        try {
            redisTemplate.opsForStream().createGroup(stream, group);
        } catch (RedisSystemException e) {
            if (e.getRootCause().getClass().equals(RedisBusyException.class)) {
                LOGGER.info("STREAM - Redis group already exists, skipping Redis group creation: {} for stream {}", group, stream);
            } else if (e.getRootCause().getClass().equals(RedisCommandExecutionException.class)) {
                LOGGER.info("STREAM - Stream does not yet exist, creating empty stream: {}", stream);
                redisTemplate.opsForStream().add(stream, Collections.singletonMap("", ""));
                redisTemplate.opsForStream().createGroup(stream, group);
            } else {
                throw new RuntimeException("Error while creating consumer group", e);
            }
        }
    }

    private EventData processRecord(MapRecord<String, String, String> record) {
        String recordId = record.getId().toString();
        Map<String, String> recordValue = record.getValue();
        try {
            Map<String, Object> extracted = extractRecordPayload(recordId, recordValue);
            return new EventData(recordId, extracted);
        } catch (JsonProcessingException e) {
            LOGGER.error("Error while extracting record {} data", recordId, e);
            return new EventData(recordId, null);
        }
    }

    private Map<String, Object> extractRecordPayload(String recordId, Map<String, String> recordValue) throws JsonProcessingException {
        TypeReference<Map<String,Object>> typeReference = new TypeReference<>() {};
        return objectMapper.readValue(recordValue.get("payload"), typeReference);
    }
}
