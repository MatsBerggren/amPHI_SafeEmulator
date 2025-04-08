package com.dedalus.amphi_integration.repository;

import java.time.LocalDate;

import java.util.Optional;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import com.dedalus.amphi_integration.model.AssignmentHistory;

@Repository
public interface AmphiAssignmentHistoryRepository extends MongoRepository<AssignmentHistory, String> {
    void deleteByCreatedBefore(LocalDate date);

    Optional<AssignmentHistory> findFirstByOrderByCreatedDesc();
}