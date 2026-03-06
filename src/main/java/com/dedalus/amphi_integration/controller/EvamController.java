package com.dedalus.amphi_integration.controller;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.dedalus.amphi_integration.model.amphi.Destination;
import com.dedalus.amphi_integration.model.amphi.MethaneReport;
import com.dedalus.amphi_integration.model.amphi.Position;
import com.dedalus.amphi_integration.model.amphi.Symbol;
import com.dedalus.amphi_integration.model.amphi.Ward;
import com.dedalus.amphi_integration.model.evam.HospitalLocation;
import com.dedalus.amphi_integration.model.evam.Operation;
import com.dedalus.amphi_integration.model.evam.OperationList;
import com.dedalus.amphi_integration.model.evam.RakelState;
import com.dedalus.amphi_integration.model.evam.TripLocationHistory;
import com.dedalus.amphi_integration.model.evam.VehicleState;
import com.dedalus.amphi_integration.model.evam.VehicleStatus;
import com.dedalus.amphi_integration.service.EvamOperationService;
import com.dedalus.amphi_integration.service.EvamVehicleStateService;
import com.dedalus.amphi_integration.service.impl.AmphiDestinationServiceImpl;
import com.dedalus.amphi_integration.service.impl.AmphiSymbolServiceImpl;
import com.dedalus.amphi_integration.service.impl.EvamMethaneReportServiceImpl;
import com.dedalus.amphi_integration.service.impl.EvamOperationListServiceImpl;
import com.dedalus.amphi_integration.service.impl.EvamRakelStateServiceImpl;
import com.dedalus.amphi_integration.service.impl.EvamTripHistoryLocationServiceImpl;
import com.dedalus.amphi_integration.service.impl.EvamVehicleStatusServiceImpl;
import com.google.gson.Gson;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@CrossOrigin
@RequestMapping(value = "/api", produces = "application/json", method = RequestMethod.GET)
@Tag(name = "Evam API", description = "API collection for CRUD operations on Evam Resource")
public class EvamController {

    private static Instant lastCallTime = Instant.now();
    private final ApplicationEventPublisher eventPublisher;
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
    public EvamController(ApplicationEventPublisher eventPublisher) {
        this.eventPublisher = eventPublisher;
        scheduler.scheduleAtFixedRate(this::checkTimeSinceLastCall, 0, 10, TimeUnit.SECONDS);
    }

    private void checkTimeSinceLastCall() {
        Instant now = Instant.now();
        if (lastCallTime != null && now.getEpochSecond() - lastCallTime.getEpochSecond() > 40) {
            eventPublisher.publishEvent(new TimeExceededEvent(this,true));
        } else {
            eventPublisher.publishEvent(new TimeExceededEvent(this,false));
        } 
    }
    
    @Autowired
    EvamOperationService evamOperationService;
    @Autowired
    EvamOperationListServiceImpl evamOperationListService;
    @Autowired
    EvamVehicleStateService evamVehicleStateService;
    @Autowired
    EvamVehicleStatusServiceImpl evamVehicleStatusService;
    @Autowired
    EvamRakelStateServiceImpl evamRakelStateService;
    @Autowired
    EvamTripHistoryLocationServiceImpl evamTripLocationHistoryService;
    @Autowired
    EvamMethaneReportServiceImpl evamMethaneReportService;
    @Autowired
    AmphiDestinationServiceImpl amphiDestinationService;
    @Autowired
    AmphiSymbolServiceImpl amphiSymbolService;
    @Autowired
    Gson gson;

    @GetMapping
    public Operation getById(@RequestParam String operationId) {
        lastCallTime = Instant.now();
        return evamOperationService.getById(operationId);
    }

    @PostMapping(value = "/operations", produces = "application/json")
    public Operation createNew(@RequestBody String json) {
        lastCallTime = Instant.now();
        log.debug("POST /operations: {}", json);
        return evamOperationService.updateOperation(json);
    }

    @PostMapping(value = "/operationlist", produces = "application/json")
    public OperationList createNewOperationList(@RequestBody String json) {
        lastCallTime = Instant.now();
        log.debug("POST /operationlist: {}", json);
        return evamOperationListService.updateOperationList(json);
    }

    @GetMapping(value = "/hospitallocations", produces = "application/json")
    public String getHospitalLocations() {
        lastCallTime = Instant.now();
        List<Destination> destinations = amphiDestinationService.getAllDestinations();

        ArrayList<HospitalLocation> hospitalLocations = new ArrayList<>();
        for (Destination destination : destinations) {
            for ( Ward ward : destination.getWards()) {
                HospitalLocation hospitalLocation =  HospitalLocation.builder()
                    .id(Integer.parseInt(ward.getId()))
                    .latitude(Optional.ofNullable(ward).map(Ward::getPosition).map(Position::getWgs84_dd_la).orElse(null))
                    .longitude(Optional.ofNullable(ward).map(Ward::getPosition).map(Position::getWgs84_dd_lo).orElse(null))
                    .name(destination.getName() + " " + ward.getName())
                    .build();

                    hospitalLocations.add(hospitalLocation);
            }
        }
        return gson.toJson(hospitalLocations);
    }

    @GetMapping(value = "/symbol", produces = "application/json")
    public String getSymbol() {
        lastCallTime = Instant.now();
        List<Symbol> symbols = amphiSymbolService.getAllSymbols();
        return gson.toJson(symbols);
    }

    @PostMapping(value = "/vehiclestate", produces = "application/json")
    public VehicleState createNewVehicleState(@RequestBody String json) {
        lastCallTime = Instant.now();
        return evamVehicleStateService.updateVehicleState(json);
    }

    @PostMapping(value = "/rakelstate", produces = "application/json")
    public RakelState createNewRakelState(@RequestBody String json) {
        lastCallTime = Instant.now();
        return evamRakelStateService.updateRakelState(json);
    }

    @PostMapping(value = "/vehiclestatus", produces = "application/json")
    public VehicleStatus[] createNewVehicleStatus(@RequestBody String json) {
        lastCallTime = Instant.now();
        return evamVehicleStatusService.updateVehicleStatus(json);
    }

    @PostMapping(value = "/triplocationhistory", produces = "application/json")
    public TripLocationHistory createNewTripLocationHistory(@RequestBody String json) {
        lastCallTime = Instant.now();
        return evamTripLocationHistoryService.updateTripLocationHistory(json);
    }

    @PostMapping(value = "/methanereport", produces = "application/json")
    public MethaneReport createNewMethaneReport(@RequestBody String json) {
        lastCallTime = Instant.now();
        return evamMethaneReportService.updateMethaneReport(json);
    }
}
