package com.dedalus.amphi_integration.service.impl;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
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
import com.dedalus.amphi_integration.util.DateFix;
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
import com.dedalus.amphi_integration.model.evam.Location;
import com.dedalus.amphi_integration.model.evam.Operation;
import com.dedalus.amphi_integration.model.evam.OperationState;
import com.dedalus.amphi_integration.model.evam.TripLocationHistory;
import com.dedalus.amphi_integration.model.evam.VehicleState;
import com.dedalus.amphi_integration.model.evam.VehicleStatus;
import com.dedalus.amphi_integration.repository.AmphiAssignmentHistoryRepository;
import com.dedalus.amphi_integration.repository.AmphiDestinationRepository;
import com.dedalus.amphi_integration.repository.OperationDistanceRepository;
import com.dedalus.amphi_integration.service.AmphiAssignmentService;
import com.dedalus.amphi_integration.service.EvamOperationService;
import com.dedalus.amphi_integration.service.EvamVehicleStateService;

@Service
public class AmphiAssignmentServiceImpl implements AmphiAssignmentService {

    @Autowired
    EvamOperationService evamOperationService;
    @Autowired
    EvamVehicleStateService evamVehicleStateService;
    @Autowired
    AmphiStateEntryServiceImpl amphiStateEntryService;
    @Autowired
    EvamVehicleStatusServiceImpl evamVehicleStatusService;
    @Autowired
    AmphiAssignmentHistoryRepository amphiAssignmentHistoryRepository;
    @Autowired
    AmphiDestinationRepository amphiDestinationRepository;
    @Autowired
    AmphiAssignmentHistoryServiceImpl amphiAssignmentHistoryService;
    @Autowired
    OperationDistanceRepository operationDistanceRepository;
    @Autowired
    EvamTripHistoryLocationServiceImpl evamTripHistoryLocationService;
    @Autowired
    EvamMethaneReportServiceImpl evamMethaneReportService;

    @Override
    public Assignment[] getAllAssignments() {
        Operation operation;
        try {
            operation = evamOperationService.getById("1");
        } catch (Exception e) {
            return null;
        }
        String OperationID = operation.getCallCenterId() + ":" + operation.getCaseFolderId() + ":" + operation.getOperationID();
        Optional<OperationDistance> operationDistance = operationDistanceRepository.findFirstByOperationIDOrderByTimestampDesc(OperationID);
        Integer distans = 0;
        if (!operationDistance.isEmpty() && operationDistance.get().getPublishedAssignmentDistance() != null) {
            distans = operationDistance.get().getPublishedAssignmentDistance().intValue();
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
            .is_closed(Boolean.toString(operation.getOperationState() == OperationState.AVAILABLE))
            .is_selected(operation.getOperationState() == OperationState.ACTIVE ? "1" : "0")
            .is_destination_alarm_sent("false")
            .selected_destination(getSelectedHospital(operation))
            .eta(getEta(operation))
            .is_head_unit("false")
            .is_routed("false")
            .distance(distans)
            .methane_report(getMethaneReport(operation))
            .rek_report(getRekReport(operation))
            .position(getAssignmentPosition(operation))
            .to_position(ToPosition.builder()
                .wgs84_dd_la(operation.getDestinationSiteLocation().getLatitude())
                .wgs84_dd_lo(operation.getDestinationSiteLocation().getLongitude()).build())
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
        Integer selectedHospitalId = operation.getSelectedHospital();
        if (selectedHospitalId == null || operation.getAvailableHospitalLocations() == null) {
            return null;
        }

        return Arrays.stream(operation.getAvailableHospitalLocations())
            .filter(location -> selectedHospitalId.equals(location.getId()))
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
                buildPosition(operation);

        return RekReport.builder()
                .affected_count("")
                .command_organization(commandOrganization)
                .comments(Objects.toString(operation.getCaseInfo(), ""))
                .created(DateFix.dateFixLong(operation.getCreatedTime()))
                .incident_organization(incidentOrganization)
                .last_updated(DateFix.dateFixLong(firstNonNull(operation.getAcceptedTime(), operation.getSendTime(), operation.getCreatedTime())))
                .position(position)
                .resources_on_site(Objects.toString(operation.getAssignedResourceMissionNo(), ""))
                .build();
    }

    private MethaneReport getMethaneReport(Operation operation) {
        AccessRoad defaultAccessRoad = AccessRoad.builder().comment("").is_obstructed(false).build();
        ExtraResources defaultResources = ExtraResources.builder().ambulances(0).chemical_suit(0).commander_unit(0).doctor_on_duty(0).emergency_wagon(0).helicopter(0).medical_team(0)
                .medical_transport(0).PAM(0).sanitation_wagon(0).transport_ambulance(0).units_total(0).build();
        InventoryLevel defaultInventoryLevel = InventoryLevel.builder().levels(new String[] {"0", "1_3", "2_3", "3_3"}).selected_level_index(0).build();

        MethaneReport methaneReport;
        try {
            methaneReport = evamMethaneReportService.getById("1");
        } catch (Exception e) {
            methaneReport = MethaneReport.builder().build();
        }

        return MethaneReport.builder()
                .id(methaneReport.getId())
                .access_road(methaneReport.getAccess_road() != null ? methaneReport.getAccess_road() : defaultAccessRoad)
                .created(nonBlankOrElse(methaneReport.getCreated(), DateFix.dateFixLong(operation.getCreatedTime())))
                .exact_location(nonBlankOrElse(methaneReport.getExact_location(), Optional.ofNullable(operation.getDestinationSiteLocation()).map(destination -> destination.getStreet()).orElse("")))
                .extra_resources(methaneReport.getExtra_resources() != null ? methaneReport.getExtra_resources() : defaultResources)
                .hazards(methaneReport.getHazards() != null ? methaneReport.getHazards() : new String[0])
                .inventory_level(methaneReport.getInventory_level() != null ? methaneReport.getInventory_level() : defaultInventoryLevel)
                .last_updated(nonBlankOrElse(methaneReport.getLast_updated(), DateFix.dateFixLong(firstNonNull(operation.getAcceptedTime(), operation.getSendTime(), operation.getCreatedTime()))))
                .major_incident(methaneReport.getMajor_incident() != null ? methaneReport.getMajor_incident() : false)
                .numbers_affected_green(methaneReport.getNumbers_affected_green() != null ? methaneReport.getNumbers_affected_green() : 0)
                .numbers_affected_red(methaneReport.getNumbers_affected_red() != null ? methaneReport.getNumbers_affected_red() : 0)
                .numbers_affected_yellow(methaneReport.getNumbers_affected_yellow() != null ? methaneReport.getNumbers_affected_yellow() : 0)
                .position(methaneReport.getPosition() != null ? methaneReport.getPosition() : buildPosition(operation))
                .special_injuries(nonBlankOrElse(methaneReport.getSpecial_injuries(), ""))
                .time_first_departure(nonBlankOrElse(methaneReport.getTime_first_departure(), getFirstDepartureTime(operation)))
                .types(methaneReport.getTypes() != null ? methaneReport.getTypes() : new String[0])
                .build();
    }

    private String getEta(Operation operation) {
        String pickupTime = Optional.ofNullable(operation.getDestinationSiteLocation())
                .map(destination -> destination.getPickupTime())
                .filter(time -> !time.isBlank())
                .orElse(null);
        if (pickupTime != null) {
            return pickupTime;
        }

        try {
            TripLocationHistory tripLocationHistory = evamTripHistoryLocationService.getById("1");
            if (tripLocationHistory.getEtaSeconds() != null) {
                return DateFix.dateFixLong(LocalDateTime.now(ZoneOffset.UTC).plusSeconds(tripLocationHistory.getEtaSeconds()));
            }
        } catch (Exception e) {
            // No trip history stored for the current operation.
        }

        return null;
    }

    private Position getAssignmentPosition(Operation operation) {
        try {
            VehicleState vehicleState = evamVehicleStateService.getById("1");
            if (vehicleState == null || vehicleState.getVehicleLocation() == null) {
                return null;
            }

            String operationId = operation.getCallCenterId() + ":" + operation.getCaseFolderId() + ":" + operation.getOperationID();
            if (vehicleState.getActiveCaseFullId() != null && !vehicleState.getActiveCaseFullId().equals(operationId)) {
                return null;
            }

            return buildPosition(vehicleState.getVehicleLocation());
        } catch (Exception e) {
            return null;
        }
    }

    private ArrayList<Property> getProperties(Operation operation) {
        DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
        ArrayList<Property> properties = new ArrayList<Property>();

        String[] nameParts = operation.getPatientName().split(" ");
        String firstName = nameParts.length > 0 ? nameParts[0] : "";
        String lastName = nameParts.length > 1 ? String.join(" ", Arrays.copyOfRange(nameParts, 1, nameParts.length)) : "";
        
        properties.add(Property.builder().name("creatorsignature").value("EVAM").build());
        properties.add(Property.builder().name("creatorversion").value("1.1.0").build());
        properties.add(Property.builder().name("created").value(formatStockholm(operation.getCreatedTime(), dateTimeFormatter)).build());
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
        properties.add(Property.builder().name("priority").value(Objects.toString(operation.getSelectedPriority(), "")).build());
        properties.add(Property.builder().name("priorityin").value(Objects.toString(operation.getSelectedPriority(), "")).build());
        properties.add(Property.builder().name("radiogroup").value(Objects.toString(operation.getRadioGroupMain(), "")).build());
        properties.add(Property.builder().name("radiogroupname").value(Objects.toString("", "")).build());
        properties.add(Property.builder().name("routedirections").value(Objects.toString(operation.getDestinationSiteLocation().getRouteDirections(), "")).build());
        properties.add(Property.builder().name("street").value(Objects.toString(operation.getDestinationSiteLocation().getStreet(), "")).build());
        properties.add(Property.builder().name("toaddress").value(Objects.toString("", "")).build());
        properties.add(Property.builder().name("tocity").value(Objects.toString("", "")).build());
        properties.add(Property.builder().name("toposition").value(Objects.toString("", "")).build());
        properties.add(Property.builder().name("sendtime").value(formatStockholm(operation.getSendTime(), dateTimeFormatter)).build());
        properties.add(Property.builder().name("pickuptime").value(Objects.toString(operation.getDestinationSiteLocation().getPickupTime(), "")).build());
        properties.add(Property.builder().name("toarea").value(Objects.toString("", "")).build());
        properties.add(Property.builder().name("tomunicipality").value(Objects.toString("", "")).build());
        properties.add(Property.builder().name("departuretime").value(Objects.toString("", "")).build());
        properties.add(Property.builder().name("topositionrt90").value(Objects.toString("", "")).build());
        properties.add(Property.builder().name("allradiogroups").value(Objects.toString(operation.getRadioGroupSecondary(), "")).build());
        properties.add(Property.builder().name("additionalcoordinationinformation").value(Objects.toString(operation.getAdditionalCoordinationInformation(), "")).build());
        properties.add(Property.builder().name("additionalinfo").value(Objects.toString(operation.getAdditionalInfo(), "")).build());
        properties.add(Property.builder().name("objectid").value(Objects.toString("", "")).build());
        properties.add(Property.builder().name("alarmcategory").value(Objects.toString(operation.getAlarmCategory(), "")).build());
        properties.add(Property.builder().name("alarmeventcode").value(Objects.toString(operation.getAlarmEventCode(), "")).build());
        properties.add(Property.builder().name("areanumberandphonenumber").value(Objects.toString("", "")).build());
        properties.add(Property.builder().name("assignedresourceincasefolder").value(Objects.toString("", "")).build());
        properties.add(Property.builder().name("testassignment").value(Objects.toString("true", "")).build());

        return properties;
    }

    private String formatStockholm(LocalDateTime dateTime, DateTimeFormatter formatter) {
        LocalDateTime stockholmTime = DateFix.toStockholmLocalTime(dateTime);
        if (stockholmTime == null) {
            return "";
        }
        return stockholmTime.format(formatter);
    }

    private Position buildPosition(Operation operation) {
        if (operation.getDestinationSiteLocation() != null) {
            return Position.builder()
                    .wgs84_dd_la(operation.getDestinationSiteLocation().getLatitude())
                    .wgs84_dd_lo(operation.getDestinationSiteLocation().getLongitude())
                    .build();
        }

        if (operation.getLeavePatientLocation() != null) {
            return Position.builder()
                    .wgs84_dd_la(operation.getLeavePatientLocation().getLatitude())
                    .wgs84_dd_lo(operation.getLeavePatientLocation().getLongitude())
                    .build();
        }

        return Optional.ofNullable(getLatestTripLocation())
                .map(location -> Position.builder().wgs84_dd_la(location.getLatitude()).wgs84_dd_lo(location.getLongitude()).build())
                .orElse(null);
    }

    private Position buildPosition(Location location) {
        if (location == null) {
            return null;
        }

        return Position.builder()
                .wgs84_dd_la(location.getLatitude())
                .wgs84_dd_lo(location.getLongitude())
                .build();
    }

    private Location getLatestTripLocation() {
        try {
            TripLocationHistory tripLocationHistory = evamTripHistoryLocationService.getById("1");
            if (tripLocationHistory.getLocationHistory() != null && tripLocationHistory.getLocationHistory().length > 0) {
                return tripLocationHistory.getLocationHistory()[tripLocationHistory.getLocationHistory().length - 1];
            }
        } catch (Exception e) {
            // No trip history stored for the current operation.
        }

        return null;
    }

    private String getFirstDepartureTime(Operation operation) {
        String leaveTime = Optional.ofNullable(operation.getLeavePatientLocation())
                .map(location -> location.getLeaveTime())
                .filter(time -> !time.isBlank())
                .orElse(null);
        if (leaveTime != null) {
            return leaveTime;
        }

        return DateFix.dateFixLong(firstNonNull(operation.getAcceptedTime(), operation.getSendTime(), operation.getCreatedTime()));
    }

    @SafeVarargs
    private final <T> T firstNonNull(T... values) {
        for (T value : values) {
            if (value != null) {
                return value;
            }
        }
        return null;
    }

    private String nonBlankOrElse(String value, String fallback) {
        if (value == null || value.isBlank()) {
            return fallback;
        }
        return value;
    }

    private State getState(VehicleStatus selectedVehicleStatus) {
        if (selectedVehicleStatus == null) {
            return buildState(null, null, 0, null);
        }

        VehicleStatus vehicleStatus = evamVehicleStatusService.getByName(selectedVehicleStatus.getName());

        AllowedState[] allowedStates = new AllowedState[]{
                AllowedState.builder().action_name("Hämtat").state_id(3).state_name("*HÄMTAT*").build(),
                AllowedState.builder().action_name("Hemåt").state_id(5).state_name("*HEMÅT*").build(),
                AllowedState.builder().action_name("Klar uppdrag").state_id(6).state_name("*KLAR UPPDRAG*").build(),
                AllowedState.builder().action_name("Uppd. disp.").state_id(7).state_name("*UPPD DISP*").build()
        };

        String actionName = nonBlankOrElse(
            Optional.ofNullable(vehicleStatus).map(VehicleStatus::getName).orElse(null),
            nonBlankOrElse(selectedVehicleStatus.getName(), ""));
        String stateName = nonBlankOrElse(
            Optional.ofNullable(vehicleStatus).map(VehicleStatus::getEvent).orElse(null),
            nonBlankOrElse(selectedVehicleStatus.getEvent(), ""));
        Integer stateId = Optional.ofNullable(vehicleStatus)
            .map(VehicleStatus::getId)
            .filter(id -> !id.isBlank())
            .map(Integer::parseInt)
            .orElse(0);

        return buildState(actionName, allowedStates, stateId, stateName);
        }

        private State buildState(String actionName, AllowedState[] allowedStates, Integer stateId, String stateName) {
        return State.builder()
            .action_name(nonBlankOrElse(actionName, ""))
                .allowed_states(allowedStates)
            .state_id(stateId)
            .state_name(nonBlankOrElse(stateName, ""))
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
