package com.demo.eventbackpressuredispatcher.service.user;

import com.demo.eventbackpressuredispatcher.model.UserInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.Map;


@Service
public class EventUserInfoService {

    private static final Logger LOGGER = LoggerFactory.getLogger(EventUserInfoService.class);
    private static final Map<Integer, UserInfo> USERS = Map.of(
            1, new UserInfo("FR-OCC", "FR-31"),
            2, new UserInfo("FR-OCC", "FR-12"),
            3, new UserInfo("FR-NAQ", "FR-64")
    );

    public UserInfo fetchUserInfo(int userId) {
        return USERS.get(userId);
    }
}
