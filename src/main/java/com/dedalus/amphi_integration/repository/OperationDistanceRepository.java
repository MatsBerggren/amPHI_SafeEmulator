package com.dedalus.amphi_integration.repository;

import java.time.LocalDate;
import java.util.Optional;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import com.dedalus.amphi_integration.model.OperationDistance;

@Repository
public interface OperationDistanceRepository extends MongoRepository<OperationDistance, String> {
    void deleteByTimestampBefore(LocalDate date);

    Optional<OperationDistance> findFirstByOrderByTimestampDesc();
    Optional<OperationDistance> findFirstByOperationIDOrderByTimestampDesc(String operationID);
    Optional<OperationDistance> findFirstByOperationIDAndStateIDOrderByTimestampDesc(String operationID, String stateID);
}