package com.dedalus.amphi_integration.repository;

import org.springframework.stereotype.Repository;

import com.dedalus.amphi_integration.model.evam.OperationList;

import jakarta.annotation.PostConstruct;

@Repository
public class EvamOperationListRepository extends JsonFileRepository<OperationList> {

    @PostConstruct
    public void init() {
        initialize(OperationList.class);
    }
}
