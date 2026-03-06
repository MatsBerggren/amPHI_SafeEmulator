package com.dedalus.amphi_integration.repository;

import org.springframework.stereotype.Repository;

import com.dedalus.amphi_integration.model.evam.RakelState;

import jakarta.annotation.PostConstruct;

@Repository
public class EvamRakelStateRepository extends JsonFileRepository<RakelState> {

    @PostConstruct
    public void init() {
        initialize(RakelState.class);
    }
}