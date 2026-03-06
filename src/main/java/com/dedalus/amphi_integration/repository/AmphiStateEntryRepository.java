package com.dedalus.amphi_integration.repository;

import java.util.Comparator;
import java.util.Optional;

import org.springframework.stereotype.Repository;

import com.dedalus.amphi_integration.model.amphi.StateEntry;

import jakarta.annotation.PostConstruct;

@Repository
public class AmphiStateEntryRepository extends JsonFileRepository<StateEntry> {

    @PostConstruct
    public void init() {
        initialize(StateEntry.class);
    }

    public Optional<StateEntry> findFirstByOrderByTimeDesc() {
        return findAll().stream()
                .filter(entry -> entry.getTime() != null)
                .max(Comparator.comparing(StateEntry::getTime));
    }
}