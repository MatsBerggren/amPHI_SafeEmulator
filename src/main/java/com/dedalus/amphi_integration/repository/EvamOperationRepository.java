package com.dedalus.amphi_integration.repository;

import org.springframework.stereotype.Repository;

import com.dedalus.amphi_integration.model.evam.Operation;

import jakarta.annotation.PostConstruct;

@Repository
public class EvamOperationRepository extends JsonFileRepository<Operation> {

    @PostConstruct
    public void init() {
        initialize(Operation.class);
    }
}
