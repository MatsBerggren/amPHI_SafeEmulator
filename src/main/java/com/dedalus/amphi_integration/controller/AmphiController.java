package com.dedalus.amphi_integration.controller;

import java.util.ArrayList;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.dedalus.amphi_integration.model.amphi.AllowedState;
import com.dedalus.amphi_integration.model.amphi.Destination;
import com.dedalus.amphi_integration.model.amphi.Position;
import com.dedalus.amphi_integration.model.amphi.State;
import com.dedalus.amphi_integration.model.amphi.Symbol;
import com.dedalus.amphi_integration.model.amphi.Unit;
import com.dedalus.amphi_integration.model.amphi.Ward;
import com.dedalus.amphi_integration.model.evam.HospitalLocation;
import com.dedalus.amphi_integration.model.evam.Operation;
import com.dedalus.amphi_integration.model.evam.RakelState;
import com.dedalus.amphi_integration.model.evam.VehicleState;
import com.dedalus.amphi_integration.model.evam.VehicleStatus;
import com.dedalus.amphi_integration.service.AmphiAssignmentService;
import com.dedalus.amphi_integration.service.EvamOperationService;
import com.dedalus.amphi_integration.service.EvamVehicleStateService;
import com.dedalus.amphi_integration.service.impl.AmphiDestinationServiceImpl;
import com.dedalus.amphi_integration.service.impl.EvamRakelStateServiceImpl;
import com.dedalus.amphi_integration.service.impl.EvamVehicleStatusServiceImpl;
import com.google.gson.Gson;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@CrossOrigin
@RequestMapping("/api/rest")
@Tag(name = "amPHI API", description = "API collection for CRUD operations on Amphi Resource")
public class AmphiController {

    private volatile boolean timeExceeded = true;

    @EventListener
    public void handleTimeExceededEvent(TimeExceededEvent event) {
        timeExceeded = event.getTimeExceeded();
    }

    @Autowired
    private EvamOperationService evamOperationService;
    @Autowired
    private EvamVehicleStateService evamVehicleStateService;
    @Autowired
    private EvamRakelStateServiceImpl evamRakelStateService;
    @Autowired
    private AmphiDestinationServiceImpl amphiDestinationService;
    @Autowired
    private AmphiAssignmentService amphiAssignmentService;
    @Autowired
    private EvamVehicleStatusServiceImpl evamVehicleStatusService;
    @Autowired
    private Gson gson;

    @GetMapping(value = "/apiversion/")
    public ResponseEntity<String> getCsamInterfaceVersion() {
        if (timeExceeded) {
            return new ResponseEntity<>("Service unavailable", HttpStatus.SERVICE_UNAVAILABLE);
        }

        return ResponseEntity.ok("2.0.0");
    }

    @GetMapping(value = "/unit/")
    public ResponseEntity<String> getCsamUnit(HttpServletRequest request) {
        
        if (timeExceeded) {
            return new ResponseEntity<>("Service unavailable", HttpStatus.SERVICE_UNAVAILABLE);
        }
        
        String unitId = "1";
        RakelState rakelState = evamRakelStateService.getById(unitId);
        VehicleState vehicleState = evamVehicleStateService.getById(unitId);

        Position position = Position.builder()
                .wgs84_dd_la(vehicleState.getVehicleLocation().getLatitude())
                .wgs84_dd_lo(vehicleState.getVehicleLocation().getLongitude())
                .build();

        Unit unit = Unit.builder()
                .id(rakelState.getUnitId())
                .issi(rakelState.getIssi())
                .msisdn(rakelState.getMsisdn())
                .position(position)
                .state(getState(vehicleState.getVehicleStatus()))
                .build();

        return ResponseEntity.ok(gson.toJson(unit));
    }

    @GetMapping(value = "/destinations/")
    public ResponseEntity<String> getDestinations() {
        if (timeExceeded) {
            return new ResponseEntity<>("Service unavailable", HttpStatus.SERVICE_UNAVAILABLE);
        }

        Operation operation = evamOperationService.getById("1");

        ArrayList<Destination> destinations = new ArrayList<>();
        for (HospitalLocation hospitalLocation : operation.getAvailableHospitalLocations()) {
            Destination destination =  Destination.builder()
                    .abbreviation(hospitalLocation.getId().toString())
                    .name(hospitalLocation.getName())
                    .position(Position.builder()
                            .wgs84_dd_la(hospitalLocation.getLatitude())
                            .wgs84_dd_lo(hospitalLocation.getLongitude())
                            .build())
                    .type("AkutSjukhus")
                    .wards(new Ward[0])
                    .build();

            destinations.add(destination);
        }
        return ResponseEntity.ok(gson.toJson(destinations));
    }

    @PostMapping(value = "/destinations/", consumes = {"application/json"}, produces = "application/json")
    public Destination[] postDestinations(@RequestBody String json) {
        return amphiDestinationService.updateDestinations(gson.fromJson(json, Destination[].class));
    }

    @GetMapping(value = "/symbols/")
    public ResponseEntity<String> getSymbols() {
        if (timeExceeded) {
            return new ResponseEntity<>("Service unavailable", HttpStatus.SERVICE_UNAVAILABLE);
        }
        
        String unitId = "1";
        Operation operation = evamOperationService.getById(unitId);
        RakelState rakelState = evamRakelStateService.getById(unitId);
        VehicleState vehicleState = evamVehicleStateService.getById(unitId);

        Position position = Position.builder()
                .wgs84_dd_la(vehicleState.getVehicleLocation().getLatitude())
                .wgs84_dd_lo(vehicleState.getVehicleLocation().getLongitude())
                .build();

        Symbol symbol = Symbol.builder()
                .assignmentId(operation.getAmPHIUniqueId())
                .description(rakelState.getMsisdn() + " " + operation.getCaseInfo())
                .heading(0)
                .id(operation.getAmPHIUniqueId())
                .is_deleted(false)
                .mapitemtype(0)
                .position(position)
                .state(vehicleState.getVehicleStatus().getName())
                .unitId(rakelState.getMsisdn())
                .build();

        Symbol[] symbols = { symbol };
        
        log.debug("GET /symbols/: {}", gson.toJson(symbols));
        return ResponseEntity.ok(gson.toJson(symbols));
    }

    @PutMapping(value = "/symbols/", consumes = "application/json", produces = "application/json")
    public ResponseEntity<String> postSymbols(@RequestBody Symbol[] symbols) {
        log.debug("PUT /symbols/: {}", gson.toJson(symbols));
        
        if (timeExceeded) {
            return new ResponseEntity<>("Service unavailable", HttpStatus.SERVICE_UNAVAILABLE);
        }

        return ResponseEntity.ok("ok");
    }

    @GetMapping(value = "/assignments/")
    public ResponseEntity<String> getAssignments() {
        if (timeExceeded) {
            return new ResponseEntity<>("Service unavailable", HttpStatus.SERVICE_UNAVAILABLE);
        }

        return ResponseEntity.ok(gson.toJson(amphiAssignmentService.getAllAssignments()));
    }

    private State getState(VehicleStatus selectedVehicleStatus) {
        VehicleStatus vehicleStatus = evamVehicleStatusService.getByName(selectedVehicleStatus.getName());

        AllowedState[] allowedStates = new AllowedState[]{
                AllowedState.builder().action_name("Hämtat").state_id(3).state_name("*HÄMTAT*").build(),
                AllowedState.builder().action_name("Hemåt").state_id(5).state_name("*HEMÅT*").build(),
                AllowedState.builder().action_name("Klar uppdrag").state_id(6).state_name("*KLAR UPPDRAG*").build(),
                AllowedState.builder().action_name("Uppd. disp.").state_id(7).state_name("*UPPD DISP*").build()
        };

        return State.builder()
                .action_name(vehicleStatus.getName())
                .allowed_states(allowedStates)
                .state_id(Integer.parseInt(vehicleStatus.getId()))
                .state_name(vehicleStatus.getEvent())
                .build();
    }
}