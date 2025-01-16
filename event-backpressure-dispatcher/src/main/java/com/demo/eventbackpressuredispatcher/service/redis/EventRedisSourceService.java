package com.demo.eventbackpressuredispatcher.service.redis;

import com.demo.eventbackpressuredispatcher.model.EventData;
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
import java.util.Objects;

@Service
public class EventRedisSourceService {

    private static final Logger LOGGER = LoggerFactory.getLogger(EventRedisSourceService.class);

    private final RedisTemplate<String, String> redisTemplate;

    @Autowired
    public EventRedisSourceService(RedisTemplate<String, String> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    @PostConstruct
    private void createConsumerGroups() {
        createConsumerGroup("my-stream", "toto");
        createConsumerGroup("my-stream", "titi");
    }

    public List<EventData> read(String source, int count, String consumerName) {
        StreamOperations<String, String, String> streamOperations = redisTemplate.opsForStream();
        StreamOffset<String> stringStreamOffset = StreamOffset.create(source, ReadOffset.lastConsumed());
        List<MapRecord<String, String, String>> messages = streamOperations.read(
                Consumer.from(consumerName, consumerName + "-0"),
                StreamReadOptions.empty().count(count),
                stringStreamOffset
        );

        if (!Objects.isNull(messages)) {
            return messages.stream()
                    .map(m -> {
                        streamOperations.acknowledge(source, consumerName, m.getId());
                        return new EventData(m.getId().toString(), m.getValue());
                    })
                    .toList();
        }

        return Collections.emptyList();
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
}
