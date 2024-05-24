package com.demo.eventconsumer.repository;

import com.demo.eventconsumer.model.EventSubscriptionModel;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Repository
public class EventSubscriptionRepository {

    // Add list to store event name, topic and webhook
    List<EventSubscriptionModel> subscriptions;

    public EventSubscriptionRepository() {
        this.subscriptions = new ArrayList<>();
    }

    public void setSubscription(EventSubscriptionModel subscription) {
        subscriptions.add(subscription);
    }

    public List<EventSubscriptionModel> getSubscriptionsForEvent(String eventName) {
        return subscriptions.stream()
                .filter(subscription -> eventName.equalsIgnoreCase(subscription.event()))
                .toList();
    }

    public List<EventSubscriptionModel> getSubscriptionsForTopic(String topicName) {
        return subscriptions.stream()
                .filter(subscription -> topicName.equalsIgnoreCase(subscription.topic()))
                .toList();
    }

    public void deleteSubscription(String eventName) {
        subscriptions.removeIf(subscription -> eventName.equalsIgnoreCase(subscription.event()));
    }
}
