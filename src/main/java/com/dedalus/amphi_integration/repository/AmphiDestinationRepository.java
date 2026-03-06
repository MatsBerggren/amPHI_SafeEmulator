package com.dedalus.amphi_integration.repository;

import org.springframework.stereotype.Repository;

import com.dedalus.amphi_integration.model.amphi.Destination;

import jakarta.annotation.PostConstruct;

@Repository
public class AmphiDestinationRepository extends JsonFileRepository<Destination> {

    @PostConstruct
    public void init() {
        initialize(Destination.class);
    }

    public Destination findByNameAndType(String name, String type) {
        return findAll().stream()
                .filter(d -> name.equals(d.getName()) && type.equals(d.getType()))
                .findFirst()
                .orElse(null);
    }
}