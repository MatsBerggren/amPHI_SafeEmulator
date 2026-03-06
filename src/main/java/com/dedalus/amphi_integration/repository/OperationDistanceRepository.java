package com.dedalus.amphi_integration.repository;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.Optional;

import org.springframework.stereotype.Repository;

import com.dedalus.amphi_integration.model.OperationDistance;

import jakarta.annotation.PostConstruct;

@Repository
public class OperationDistanceRepository extends JsonFileRepository<OperationDistance> {

    @PostConstruct
    public void init() {
        initialize(OperationDistance.class);
    }

    public void deleteByTimestampBefore(LocalDate date) {
        findAll().stream()
                .filter(o -> o.getTimestamp() != null && o.getTimestamp().toLocalDate().isBefore(date))
                .forEach(this::delete);
    }

    public Optional<OperationDistance> findFirstByOrderByTimestampDesc() {
        return findAll().stream()
                .filter(o -> o.getTimestamp() != null)
                .max(Comparator.comparing(OperationDistance::getTimestamp));
    }

    public Optional<OperationDistance> findFirstByOperationIDOrderByTimestampDesc(String operationID) {
        return findAll().stream()
                .filter(o -> operationID.equals(o.getOperationID()) && o.getTimestamp() != null)
                .max(Comparator.comparing(OperationDistance::getTimestamp));
    }

    public Optional<OperationDistance> findFirstByOperationIDAndStateIDOrderByTimestampDesc(String operationID, String stateID) {
        return findAll().stream()
                .filter(o -> operationID.equals(o.getOperationID()) && stateID.equals(o.getStateID()) && o.getTimestamp() != null)
                .max(Comparator.comparing(OperationDistance::getTimestamp));
    }
}