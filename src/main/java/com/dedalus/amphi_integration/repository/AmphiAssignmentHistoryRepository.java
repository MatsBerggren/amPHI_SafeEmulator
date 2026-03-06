package com.dedalus.amphi_integration.repository;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.Optional;

import org.springframework.stereotype.Repository;

import com.dedalus.amphi_integration.model.AssignmentHistory;

import jakarta.annotation.PostConstruct;

@Repository
public class AmphiAssignmentHistoryRepository extends JsonFileRepository<AssignmentHistory> {

    @PostConstruct
    public void init() {
        initialize(AssignmentHistory.class);
    }

    public void deleteByCreatedBefore(LocalDate date) {
        findAll().stream()
                .filter(a -> a.getCreated() != null && a.getCreated().toLocalDate().isBefore(date))
                .forEach(this::delete);
    }

    public Optional<AssignmentHistory> findFirstByOrderByCreatedDesc() {
        return findAll().stream()
                .filter(a -> a.getCreated() != null)
                .max(Comparator.comparing(AssignmentHistory::getCreated));
    }
}