package com.dedalus.amphi_integration.repository;

import org.springframework.stereotype.Repository;

import com.dedalus.amphi_integration.model.amphi.MethaneReport;

import jakarta.annotation.PostConstruct;

@Repository
public class EvamMethaneReportRepository extends JsonFileRepository<MethaneReport> {

    @PostConstruct
    public void init() {
        initialize(MethaneReport.class);
    }
}