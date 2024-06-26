package com.demo.eventconsumer.repository;

import com.demo.eventconsumer.model.EventSubscriptionModel;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface EventSubscriptionRepository extends CrudRepository<EventSubscriptionModel, String> {}
