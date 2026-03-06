package com.dedalus.amphi_integration.repository;

import org.springframework.stereotype.Repository;

import com.dedalus.amphi_integration.model.evam.VehicleStatus;

import jakarta.annotation.PostConstruct;

@Repository
public class EvamVehicleStatusRepository extends JsonFileRepository<VehicleStatus> {

    @PostConstruct
    public void init() {
        initialize(VehicleStatus.class);
    }

    public VehicleStatus findByName(String name) {
        return findAll().stream()
                .filter(v -> name.equals(v.getName()))
                .findFirst()
                .orElse(null);
    }
}