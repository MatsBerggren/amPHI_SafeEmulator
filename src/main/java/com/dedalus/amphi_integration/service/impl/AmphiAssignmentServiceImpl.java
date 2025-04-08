package com.dedalus.amphi_integration.service.impl;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.StringJoiner;
import java.lang.reflect.Field;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.dedalus.amphi_integration.classes.DateFix;
import com.dedalus.amphi_integration.model.AssignmentHistory;
import com.dedalus.amphi_integration.model.OperationDistance;
import com.dedalus.amphi_integration.model.amphi.AccessRoad;
import com.dedalus.amphi_integration.model.amphi.AllowedState;
import com.dedalus.amphi_integration.model.amphi.Assignment;
import com.dedalus.amphi_integration.model.amphi.CloseReason;
import com.dedalus.amphi_integration.model.amphi.CommandOrganization;
import com.dedalus.amphi_integration.model.amphi.ExtraResources;
import com.dedalus.amphi_integration.model.amphi.IncidentOrganization;
import com.dedalus.amphi_integration.model.amphi.InventoryLevel;
import com.dedalus.amphi_integration.model.amphi.MethaneReport;
import com.dedalus.amphi_integration.model.amphi.Position;
import com.dedalus.amphi_integration.model.amphi.Property;
import com.dedalus.amphi_integration.model.amphi.RekReport;
import com.dedalus.amphi_integration.model.amphi.State;
import com.dedalus.amphi_integration.model.amphi.StateConfiguration;
import com.dedalus.amphi_integration.model.amphi.StateEntry;
import com.dedalus.amphi_integration.model.amphi.ToPosition;
import com.dedalus.amphi_integration.model.amphi.Ward;
import com.dedalus.amphi_integration.model.evam.HospitalLocation;
import com.dedalus.amphi_integration.model.evam.Operation;
import com.dedalus.amphi_integration.model.evam.OperationState;
import com.dedalus.amphi_integration.model.evam.VehicleStatus;
import com.dedalus.amphi_integration.repository.AmphiAssignmentHistoryRepository;
import com.dedalus.amphi_integration.repository.AmphiDestinationRepository;
import com.dedalus.amphi_integration.repository.OperationDistanceRepository;
import com.dedalus.amphi_integration.service.AmphiAssignmentHistoryService;
import com.dedalus.amphi_integration.service.AmphiAssignmentService;
import com.dedalus.amphi_integration.service.AmphiStateEntryService;
import com.dedalus.amphi_integration.service.EvamOperationService;
import com.dedalus.amphi_integration.service.EvamVehicleStateService;
import com.dedalus.amphi_integration.service.EvamVehicleStatusService;

@Service
public class AmphiAssignmentServiceImpl implements AmphiAssignmentService {

    @Autowired
    EvamOperationService evamOperationService;
    @Autowired
    EvamVehicleStateService evamVehicleStateService;
    @Autowired
    AmphiStateEntryService amphiStateEntryService;
    @Autowired
    EvamVehicleStatusService evamVehicleStatusService;
    @Autowired
    AmphiAssignmentHistoryRepository amphiAssignmentHistoryRepository;
    @Autowired
    AmphiDestinationRepository amphiDestinationRepository;
    @Autowired
    AmphiAssignmentHistoryService amphiAssignmentHistoryService;
    @Autowired
    OperationDistanceRepository operationDistanceRepository;

    @Override
    public Assignment[] getAllAssignments() {
        Operation operation;
        try {
            operation = evamOperationService.getById("1");
        } catch (Exception e) {
            return null;
        }
        String OperationID = operation.callCenterId + ":" + operation.caseFolderId + ":" + operation.operationID;
        Optional<OperationDistance> operationDistance = operationDistanceRepository.findFirstByOperationIDOrderByTimestampDesc(OperationID);
        Integer distans = 0;
        if (!operationDistance.isEmpty() && operationDistance.get().getAssignmentDistance() != null) {
            distans = operationDistance.get().getAssignmentDistance().intValue();
        }
        Assignment[] assignments = new Assignment[1];
        Assignment assignment = Assignment.builder()
            .assignment_number(operation.getFullId())
            .close_reason(CloseReason.builder()
                .comment("")
                .reason("").build())
            .created(DateFix.dateFixLong(operation.getCreatedTime()))
            .received(DateFix.dateFixLong(operation.getSendTime()))
            .accepted(DateFix.dateFixLong(operation.getAcceptedTime()))
            .rowid(operation.getAmPHIUniqueId())
            .is_closed(Boolean.toString(operation.operationState == OperationState.AVAILABLE))
            .is_selected(operation.operationState == OperationState.ACTIVE ? "1" : "0")
            .is_destination_alarm_sent("false")
            .selected_destination(getSelectedHospital(operation))
            .eta("2023-10-25T14:33:00Z")
            .is_head_unit("false")
            .is_routed("false")
            .distance(distans)
            .methane_report(MethaneReport.builder().build()) // getMethaneReport(operation)
            .rek_report(RekReport.builder().build()) // getRekReport(operation)
            .position(null)
            .to_position(ToPosition.builder()
                .wgs84_dd_la(operation.destinationSiteLocation.getLatitude())
                .wgs84_dd_lo(operation.destinationSiteLocation.getLongitude()).build())
            .properties(getProperties(operation))
            .state(getState(operation.getVehicleStatus()))
            .state_entries(getStateEntries(operation))
            .state_configuration(getStateConfiguration(operation)).build();

        assignments[0] = assignment;

        // Save the assignment history
        Optional<AssignmentHistory> optionalAssignmentHistory = amphiAssignmentHistoryRepository.findFirstByOrderByCreatedDesc();
        if (optionalAssignmentHistory.isPresent()) {
            Assignment existingAssignment = optionalAssignmentHistory.get().getAssignment();
            if (!existingAssignment.equals(assignment)) {
                String differences = getDifferences(existingAssignment, assignment);
                AssignmentHistory assignmentHistory = AssignmentHistory.builder()
                        .assignment(assignment)
                        .created(LocalDateTime.now())
                        .changes(differences)
                        .build();
                amphiAssignmentHistoryRepository.save(assignmentHistory);
            } 
        } else {
            AssignmentHistory assignmentHistory = AssignmentHistory.builder()
                    .assignment(assignment)
                    .created(LocalDateTime.now())
                    .changes("New assignment")
                    .build();
            amphiAssignmentHistoryRepository.save(assignmentHistory);
        }
        deleteOldRecords(7);

        return assignments;

    }


    private String getSelectedHospital(Operation operation) {
        HospitalLocation selectedHospitalLocation = getSelectedHospitalLocation(operation);
        if (selectedHospitalLocation == null) {
            return "";
        }
    
        return findMatchingWardId(selectedHospitalLocation);
    }
    
    private HospitalLocation getSelectedHospitalLocation(Operation operation) {
        return Arrays.stream(operation.getAvailableHospitalLocations())
            .filter(location -> location.getId().toString().equals(operation.getSelectedHospital().toString()))
            .findFirst()
            .orElse(null);
    }
    
    private String findMatchingWardId(HospitalLocation selectedHospitalLocation) {
        return amphiDestinationRepository.findAll().stream()
            .flatMap(destination -> Arrays.stream(destination.getWards()))
            .filter(ward -> ward.getPosition().getWgs84_dd_la().equals(selectedHospitalLocation.getLatitude()) 
                && ward.getPosition().getWgs84_dd_lo().equals(selectedHospitalLocation.getLongitude()))
            .map(Ward::getId)
            .findFirst()
            .orElse("");
    }

    private StateConfiguration[] getStateConfiguration(Operation operation) {
        List<VehicleStatus> vehicleStatuses = evamVehicleStatusService.getAll();

        return vehicleStatuses.stream().map(vehicleStatus -> StateConfiguration.builder()
            .id(Integer.parseInt(vehicleStatus.getId()))
            .name(vehicleStatus.getName())
            .transition_name(vehicleStatus.getEvent())
            .allowed_transitions(new Integer[] {1, 2, 3, 4, 5, 6, 7}).build()).toArray(StateConfiguration[]::new);
    }

    private StateEntry[] getStateEntries(Operation operation) {
        return amphiStateEntryService.getAll().toArray(new StateEntry[0]);
    }

    private RekReport getRekReport(Operation operation) {
        CommandOrganization commandOrganization =
                CommandOrganization.builder().hq_commander("").hq_commander_medical("").medical_commander("").section_commander_1_incident_site("")
                        .section_commander_2_incident_site("").section_commander_assembly_point("").section_commander_breakpoint("").section_commander_collect_point("").build();

        IncidentOrganization incidentOrganization = IncidentOrganization.builder().assembly_point_injured("").assembly_point_uninjured("").assembly_site("").breakpoint("")
                .collect_point("").command_site("").incident_site("").landing_site("").build();

        Position position =
                Position.builder().wgs84_dd_la(operation.getDestinationSiteLocation().getLatitude()).wgs84_dd_lo(operation.getDestinationSiteLocation().getLongitude()).build();

        return RekReport.builder().affected_count("").command_organization(commandOrganization).comments("").created("2015-03-31T14:14:00.353Z")
                .incident_organization(incidentOrganization).last_updated("2015-03-31T14:44:34.934Z").position(position).resources_on_site("").build();
    }

    private MethaneReport getMethaneReport(Operation operation) {
        AccessRoad accessRoad = AccessRoad.builder().comment("").is_obstructed(false).build();
        ExtraResources extraResources = ExtraResources.builder().ambulances(0).chemical_suit(0).commander_unit(0).doctor_on_duty(0).emergency_wagon(0).helicopter(0).medical_team(0)
                .medical_transport(0).PAM(0).sanitation_wagon(0).transport_ambulance(0).units_total(0).build();
        String[] hazards = new String[] {"Halka", "Rök/Gas"};
        String[] levels = new String[] {"0", "1_3", "2_3", "3_3"};
        InventoryLevel inventoryLevel = InventoryLevel.builder().levels(levels).selected_level_index(1).build();
        Position position = Position.builder().wgs84_dd_la(59.338985).wgs84_dd_lo(18.06327).build();
        String[] types = new String[] {"Trafikolycka"};

        MethaneReport methaneReport = MethaneReport.builder().access_road(accessRoad).created("2023-10-25T14:34:00Z")
                .exact_location(operation.getDestinationSiteLocation().getStreet()).extra_resources(extraResources).hazards(hazards).inventory_level(inventoryLevel)
                .last_updated("2023-10-25T14:35:00Z").major_incident(false).numbers_affected_green(0).numbers_affected_red(0).numbers_affected_yellow(0).position(position)
                .special_injuries("").time_first_departure("2015-03-31T14:45:00Z").types(types).build();

        return methaneReport;
    }

    private ArrayList<Property> getProperties(Operation operation) {
        DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd hh:mm");
        ArrayList<Property> properties = new ArrayList<Property>();

        String[] nameParts = operation.getPatientName().split(" ");
        String firstName = nameParts.length > 0 ? nameParts[0] : "";
        String lastName = nameParts.length > 1 ? String.join(" ", Arrays.copyOfRange(nameParts, 1, nameParts.length)) : "";
        
        properties.add(Property.builder().name("creatorsignature").value("EVAM").build());
        properties.add(Property.builder().name("creatorversion").value("1.1.0").build());
        properties.add(Property.builder().name("created").value(Objects.toString(operation.getCreatedTime().format(dateTimeFormatter), "")).build());
        properties.add(Property.builder().name("sender").value(Objects.toString(operation.getTransmitterCode(), "")).build());
        properties.add(Property.builder().name("central").value(Objects.toString(operation.getCallCenterId(), "")).build());
        properties.add(Property.builder().name("area").value(Objects.toString(null, "")).build());
        properties.add(Property.builder().name("caseindex1").value(Objects.toString(operation.getAlarmCategory(), "")).build());
        properties.add(Property.builder().name("caseindex2").value(Objects.toString(operation.getHeader1(), "")).build());
        properties.add(Property.builder().name("caseindex3").value(Objects.toString(operation.getHeader2(), "")).build());
        properties.add(Property.builder().name("city").value(Objects.toString(operation.getDestinationSiteLocation().getLocality(), "")).build());
        properties.add(Property.builder().name("comment").value(Objects.toString(operation.getCaseInfo(), "")).build());
        properties.add(Property.builder().name("firstname").value(Objects.toString(firstName, "")).build());
        properties.add(Property.builder().name("ib").value(Objects.toString(operation.getOperationID(), "")).build());
        properties.add(Property.builder().name("id").value(Objects.toString(operation.getCaseFolderId(), "")).build());
        properties.add(Property.builder().name("lastname").value(Objects.toString(lastName, "")).build());
        properties.add(Property.builder().name("municipality").value(Objects.toString(operation.getDestinationSiteLocation().getMunicipality(), "")).build());
        properties.add(Property.builder().name("personid").value(Objects.toString(operation.getPatientUID(), "")).build());
        properties.add(Property.builder().name("positionwgs84")
                .value(Objects.toString("La=" + operation.getDestinationSiteLocation().getLatitude() + " Lo=" + operation.getDestinationSiteLocation().getLongitude(), ""))
                .build());
        properties.add(Property.builder().name("priority").value(Objects.toString(operation.getSelectedPriority().toString(), "")).build());
        properties.add(Property.builder().name("priorityin").value(Objects.toString(operation.getSelectedPriority().toString(), "")).build());
        properties.add(Property.builder().name("radiogroup").value(Objects.toString(operation.getRadioGroupMain(), "")).build());
        properties.add(Property.builder().name("radiogroupname").value(Objects.toString("", "")).build());
        properties.add(Property.builder().name("routedirections").value(Objects.toString(operation.getDestinationSiteLocation().getRouteDirections(), "")).build());
        properties.add(Property.builder().name("street").value(Objects.toString(operation.getDestinationSiteLocation().getStreet(), "")).build());
        properties.add(Property.builder().name("toaddress").value(Objects.toString("", "")).build());
        properties.add(Property.builder().name("tocity").value(Objects.toString("", "")).build());
        properties.add(Property.builder().name("toposition").value(Objects.toString("", "")).build());
        properties.add(Property.builder().name("sendtime").value(Objects.toString(operation.getSendTime().format(dateTimeFormatter), "")).build());
        properties.add(Property.builder().name("pickuptime").value(Objects.toString(operation.getDestinationSiteLocation().getPickupTime(), "")).build());
        properties.add(Property.builder().name("toarea").value(Objects.toString("", "")).build());
        properties.add(Property.builder().name("tomunicipality").value(Objects.toString("", "")).build());
        properties.add(Property.builder().name("departuretime").value(Objects.toString("", "")).build());
        properties.add(Property.builder().name("topositionrt90").value(Objects.toString("", "")).build());
        properties.add(Property.builder().name("allradiogroups").value(Objects.toString(operation.getRadioGroupSecondary(), "")).build());
        properties.add(Property.builder().name("additionalcoordinationinformation").value(Objects.toString(operation.getAdditionalCoordinationInformation(), "")).build());
        properties.add(Property.builder().name("additionalinfo").value(Objects.toString(operation.getAdditionalInfo(), "")).build());
        properties.add(Property.builder().name("objectid").value(Objects.toString("", "")).build());
        properties.add(Property.builder().name("alarmcategory").value(Objects.toString("", "")).build());
        properties.add(Property.builder().name("alarmeventcode").value(Objects.toString("", "")).build());
        properties.add(Property.builder().name("areanumberandphonenumber").value(Objects.toString("", "")).build());
        properties.add(Property.builder().name("assignedresourceincasefolder").value(Objects.toString("", "")).build());
        properties.add(Property.builder().name("testassignment").value(Objects.toString("true", "")).build());

        return properties;
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

    public void deleteOldRecords(Integer days) {
        LocalDate today = LocalDate.now();
        amphiAssignmentHistoryRepository.deleteByCreatedBefore(today.minusDays(days));
        operationDistanceRepository.deleteByTimestampBefore(today.minusDays(days));
    }

    public static String getDifferences(Object obj1, Object obj2) {
        if (obj1 == null || obj2 == null) {
            throw new IllegalArgumentException("Both objects must be non-null");
        }

        if (!obj1.getClass().equals(obj2.getClass())) {
            throw new IllegalArgumentException("Both objects must be of the same type");
        }

        StringJoiner differences = new StringJoiner(", ");
        Field[] fields = obj1.getClass().getDeclaredFields();

        for (Field field : fields) {
            field.setAccessible(true);
            try {
                Object value1 = field.get(obj1);
                Object value2 = field.get(obj2);

                if (value1 == null && value2 == null) {
                    continue;
                }

                if (value1 == null || value2 == null || !value1.equals(value2)) {
                    differences.add(field.getName() + ": " + value1 + " -> " + value2);
                }
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            }
        }

        return differences.toString();
    }
}
