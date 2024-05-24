package com.demo.eventproducer.repository;

import com.demo.eventproducer.model.EventSchemaModel;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface EventSchemaRepository extends CrudRepository<EventSchemaModel, String> {}