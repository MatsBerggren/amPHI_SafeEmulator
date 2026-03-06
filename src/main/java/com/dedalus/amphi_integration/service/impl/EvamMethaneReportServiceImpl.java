package com.dedalus.amphi_integration.service.impl;

import java.util.List;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.google.gson.Gson;
import com.dedalus.amphi_integration.model.amphi.MethaneReport;
import com.dedalus.amphi_integration.repository.EvamMethaneReportRepository;

@Slf4j
@Service
public class EvamMethaneReportServiceImpl {

    @Autowired
    Gson gson;
    @Autowired
    EvamMethaneReportRepository evamMethaneReportRepository;

    public MethaneReport updateMethaneReport(String json) {
        MethaneReport methaneReport = gson.fromJson(json, MethaneReport.class);
        log.info("Updating MethaneReport: {}", methaneReport);

        Optional<MethaneReport> existingMethaneReport = evamMethaneReportRepository.findById("1");

        if (existingMethaneReport.isEmpty()) {
            methaneReport.setId("1");
            evamMethaneReportRepository.save(methaneReport);
        } else {
            existingMethaneReport.get().setAccess_road(methaneReport.getAccess_road());
            existingMethaneReport.get().setCreated(methaneReport.getCreated());
            existingMethaneReport.get().setExact_location(methaneReport.getExact_location());
            existingMethaneReport.get().setExtra_resources(methaneReport.getExtra_resources());
            existingMethaneReport.get().setHazards(methaneReport.getHazards());
            existingMethaneReport.get().setInventory_level(methaneReport.getInventory_level());
            existingMethaneReport.get().setLast_updated(methaneReport.getLast_updated());
            existingMethaneReport.get().setMajor_incident(methaneReport.getMajor_incident());
            existingMethaneReport.get().setNumbers_affected_green(methaneReport.getNumbers_affected_green());
            existingMethaneReport.get().setNumbers_affected_red(methaneReport.getNumbers_affected_red());
            existingMethaneReport.get().setNumbers_affected_yellow(methaneReport.getNumbers_affected_yellow());
            existingMethaneReport.get().setPosition(methaneReport.getPosition());
            existingMethaneReport.get().setSpecial_injuries(methaneReport.getSpecial_injuries());
            existingMethaneReport.get().setTime_first_departure(methaneReport.getTime_first_departure());
            existingMethaneReport.get().setTypes(methaneReport.getTypes());
            evamMethaneReportRepository.save(existingMethaneReport.get());
            log.info("Methane Report Updated");
        }

        return methaneReport;
    }

    public MethaneReport getById(String id) {
        return evamMethaneReportRepository.findById(id).orElseThrow(() -> new RuntimeException("No MethaneReport found for id: %s".formatted(id)));
    }

    public List<MethaneReport> getAll() {
        return evamMethaneReportRepository.findAll();
    }
}
