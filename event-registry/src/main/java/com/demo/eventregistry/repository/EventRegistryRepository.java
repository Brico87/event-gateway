package com.demo.eventregistry.repository;

import com.demo.eventregistry.model.EventSourceModel;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface EventRegistryRepository extends CrudRepository<EventSourceModel, String> {}