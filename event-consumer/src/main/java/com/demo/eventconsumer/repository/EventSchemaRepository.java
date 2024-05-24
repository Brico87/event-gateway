package com.demo.eventconsumer.repository;

import com.demo.eventconsumer.model.EventSchemaModel;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface EventSchemaRepository extends CrudRepository<EventSchemaModel, String> {}