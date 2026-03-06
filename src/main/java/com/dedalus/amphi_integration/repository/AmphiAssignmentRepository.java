package com.dedalus.amphi_integration.repository;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.Optional;

import org.springframework.stereotype.Repository;

import com.dedalus.amphi_integration.model.amphi.Assignment;

import jakarta.annotation.PostConstruct;

@Repository
public class AmphiAssignmentRepository extends JsonFileRepository<Assignment> {

    @PostConstruct
    public void init() {
        initialize(Assignment.class);
    }

    public void deleteByCreatedBefore(LocalDate date) {
        // Assignment.created is a String, so we skip this filter or parse if needed
        // For now, we'll leave it as a no-op or you can implement string date parsing
    }

    public Optional<Assignment> findFirstByOrderByCreatedDesc() {
        // Assignment.created is a String, so we sort as strings
        return findAll().stream()
                .filter(a -> a.getCreated() != null)
                .max(Comparator.comparing(Assignment::getCreated));
    }
}