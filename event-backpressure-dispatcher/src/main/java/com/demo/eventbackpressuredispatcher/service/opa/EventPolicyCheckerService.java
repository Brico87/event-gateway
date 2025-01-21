package com.demo.eventbackpressuredispatcher.service.opa;

import com.demo.eventbackpressuredispatcher.opa.api.DefaultApi;
import com.demo.eventbackpressuredispatcher.opa.client.ApiClient;
import com.demo.eventbackpressuredispatcher.opa.model.ResultResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@Service
public class EventPolicyCheckerService {

    private static final Logger LOGGER = LoggerFactory.getLogger(EventPolicyCheckerService.class);

    private final DefaultApi policyServerApi;

    @Autowired
    public EventPolicyCheckerService(ApiClient policyServerApiClient) {
        this.policyServerApi = new DefaultApi(policyServerApiClient);
    }

    public boolean checkEventDataAccess(String consumerName) {
        Map<String, Object> inputParams = new HashMap<>();
        Map<String, Object> accessParams = new HashMap<>();
        accessParams.put("user", consumerName);
        inputParams.put("input", accessParams);
        ResultResponse response = policyServerApi.getAccessForInput(inputParams);
        return Optional.ofNullable(response.getResult()).orElse(false);
    }

}
