package com.dedalus.amphi_integration.repository;

import java.util.Optional;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import com.dedalus.amphi_integration.model.amphi.StateEntry;

@Repository
public interface AmphiStateEntryRepository extends MongoRepository<StateEntry, String> {

    Optional<StateEntry> findFirstByOrderByTimeDesc();
}