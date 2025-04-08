package com.dedalus.amphi_integration.repository;

import java.util.Optional;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import com.dedalus.amphi_integration.model.evam.VehicleState;

@Repository
public interface EvamVehicleStateRepository extends MongoRepository<VehicleState, String> {

    Optional<VehicleState> findFirstByOrderByTimestampDesc();
}