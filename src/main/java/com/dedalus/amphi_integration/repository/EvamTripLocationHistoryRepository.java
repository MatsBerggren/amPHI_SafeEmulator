package com.dedalus.amphi_integration.repository;

import org.springframework.stereotype.Repository;

import com.dedalus.amphi_integration.model.evam.TripLocationHistory;

import jakarta.annotation.PostConstruct;

@Repository
public class EvamTripLocationHistoryRepository extends JsonFileRepository<TripLocationHistory> {

    @PostConstruct
    public void init() {
        initialize(TripLocationHistory.class);
    }
}