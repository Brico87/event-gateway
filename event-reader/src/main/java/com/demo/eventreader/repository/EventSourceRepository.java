package com.demo.eventreader.repository;

import com.demo.eventreader.model.EventSourceModel;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface EventSourceRepository extends CrudRepository<EventSourceModel, String> {}