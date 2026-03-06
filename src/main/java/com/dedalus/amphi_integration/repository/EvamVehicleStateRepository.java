package com.dedalus.amphi_integration.repository;

import java.util.Comparator;
import java.util.Optional;

import org.springframework.stereotype.Repository;

import com.dedalus.amphi_integration.model.evam.VehicleState;

import jakarta.annotation.PostConstruct;

@Repository
public class EvamVehicleStateRepository extends JsonFileRepository<VehicleState> {

    @PostConstruct
    public void init() {
        initialize(VehicleState.class);
    }

    public Optional<VehicleState> findFirstByOrderByTimestampDesc() {
        return findAll().stream()
                .filter(v -> v.getTimestamp() != null)
                .max(Comparator.comparing(VehicleState::getTimestamp));
    }
}