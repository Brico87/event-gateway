package com.demo.eventbackpressuredispatcher.service;

import com.demo.eventbackpressuredispatcher.model.AccessRequestData;
import com.demo.eventbackpressuredispatcher.model.EventData;
import com.demo.eventbackpressuredispatcher.model.UserInfo;
import com.demo.eventbackpressuredispatcher.service.opa.EventPolicyCheckerService;
import com.demo.eventbackpressuredispatcher.service.redis.EventRedisSourceService;
import com.demo.eventbackpressuredispatcher.service.user.EventUserInfoService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Service
public class EventBackpressureSourceService {

    private static final Logger LOGGER = LoggerFactory.getLogger(EventBackpressureSourceService.class);

    private final EventRedisSourceService sourceService;
    private final EventPolicyCheckerService policyCheckerService;
    private final EventUserInfoService userInfoService;

    @Autowired
    public EventBackpressureSourceService(EventRedisSourceService sourceService,
                                          EventPolicyCheckerService policyCheckerService,
                                          EventUserInfoService userInfoService) {
        this.sourceService = sourceService;
        this.policyCheckerService = policyCheckerService;
        this.userInfoService = userInfoService;
    }

    public List<Map<String, Object>> read(String source, int count, String consumerName) {
        LOGGER.info("Reading source {}: {} asks for {} messages", source, consumerName, count);
        List<EventData> read = sourceService.read(source, count, consumerName);
        LOGGER.info("Source {}: {} messages read for {}", source, read.size(), consumerName);
        List<Map<String, Object>> filtered = read.stream()
                .filter(event -> !Objects.isNull(event.payload())) // DLQ for invalid events ?
                .filter(event -> applyPolicy(source, consumerName, event))
                .sorted(Comparator.comparing(EventData::id))
                .map(EventData::payload)
                .toList();
        LOGGER.info("Source {}: {} messages returned for {} after filtering", source, filtered.size(), consumerName);
        return filtered;
    }

    // PRIVATE METHODS

    private boolean applyPolicy(String source, String consumerName, EventData eventData) {
        try {
            AccessRequestData accessRequestData = buildAccessRequestData(source, consumerName, eventData);
            boolean accessAllowed = policyCheckerService.checkEventDataAccess(accessRequestData);
            LOGGER.info("Source {} / event {} access for {}: {}", source, eventData.id(), consumerName, accessAllowed ? "allowed" : "not allowed");
            sourceService.acknowledge(source, consumerName, eventData.id());
            return accessAllowed;
        } catch (Exception e) {
            LOGGER.error("Error while applying policy for event ID {} for {}", eventData.id(), consumerName, e);
            return false;
        }
    }

    private AccessRequestData buildAccessRequestData(String source, String consumerName, EventData eventData) {
        int userId = (int) eventData.payload().get("userId");
        UserInfo userInfo = userInfoService.fetchUserInfo(userId);
        LOGGER.info("Source {} / event {} linked to user: {}", source, eventData.id(), userInfo);
        return new AccessRequestData(consumerName, source, userInfo);
    }
}
