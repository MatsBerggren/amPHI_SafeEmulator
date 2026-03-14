package com.dedalus.amphi_integration.service.impl;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Collections;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;

import com.dedalus.amphi_integration.model.amphi.AccessRoad;
import com.dedalus.amphi_integration.model.amphi.Assignment;
import com.dedalus.amphi_integration.model.amphi.Destination;
import com.dedalus.amphi_integration.model.amphi.ExtraResources;
import com.dedalus.amphi_integration.model.amphi.InventoryLevel;
import com.dedalus.amphi_integration.model.amphi.MethaneReport;
import com.dedalus.amphi_integration.model.amphi.Position;
import com.dedalus.amphi_integration.model.amphi.Property;
import com.dedalus.amphi_integration.model.amphi.Ward;
import com.dedalus.amphi_integration.model.OperationDistance;
import com.dedalus.amphi_integration.model.evam.DestinationSiteLocation;
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
import com.dedalus.amphi_integration.service.EvamOperationService;
import com.dedalus.amphi_integration.service.EvamVehicleStateService;
import com.dedalus.amphi_integration.util.DateFix;

/**
 * Unit tests for AmphiAssignmentServiceImpl.
 * Focuses on testable methods and core business logic.
 */
@ExtendWith(MockitoExtension.class)
class AmphiAssignmentServiceImplTest {

    @Mock
    private EvamOperationService evamOperationService;

    @Mock
    private EvamVehicleStateService evamVehicleStateService;

    @Mock
    private AmphiStateEntryServiceImpl amphiStateEntryService;

    @Mock
    private EvamVehicleStatusServiceImpl evamVehicleStatusService;

    @Mock
    private AmphiAssignmentHistoryRepository amphiAssignmentHistoryRepository;

    @Mock
    private AmphiDestinationRepository amphiDestinationRepository;

    @Mock
    private AmphiAssignmentHistoryServiceImpl amphiAssignmentHistoryService;

    @Mock
    private OperationDistanceRepository operationDistanceRepository;

        @Mock
        private EvamTripHistoryLocationServiceImpl evamTripHistoryLocationService;

        @Mock
        private EvamMethaneReportServiceImpl evamMethaneReportService;

    @InjectMocks
    private AmphiAssignmentServiceImpl amphiAssignmentService;

    // ========== Tests for getDifferences() static method ==========

    @Test
    void getDifferences_WithIdenticalObjects_ReturnsEmptyString() {
        // Arrange
        TestObject obj1 = new TestObject("value1", 42);
        TestObject obj2 = new TestObject("value1", 42);

        // Act
        String result = AmphiAssignmentServiceImpl.getDifferences(obj1, obj2);

        // Assert
        assertEquals("", result, "Identical objects should produce empty difference string");
    }

    @Test
    void getDifferences_WithDifferentStringField_ReturnsDifference() {
        // Arrange
        TestObject obj1 = new TestObject("value1", 42);
        TestObject obj2 = new TestObject("value2", 42);

        // Act
        String result = AmphiAssignmentServiceImpl.getDifferences(obj1, obj2);

        // Assert
        assertNotNull(result);
        assertTrue(result.contains("stringField"), 
                "Result should mention the changed field");
        assertTrue(result.contains("value1") && result.contains("value2"),
                "Result should show both old and new values");
    }

    @Test
    void getDifferences_WithDifferentIntField_ReturnsDifference() {
        // Arrange
        TestObject obj1 = new TestObject("value", 42);
        TestObject obj2 = new TestObject("value", 100);

        // Act
        String result = AmphiAssignmentServiceImpl.getDifferences(obj1, obj2);

        // Assert
        assertNotNull(result);
        assertTrue(result.contains("intField"),
                "Result should mention the changed field");
        assertTrue(result.contains("42") && result.contains("100"),
                "Result should show both old and new values");
    }

    @Test
    void getDifferences_WithMultipleDifferences_ReturnsAllDifferences() {
        // Arrange
        TestObject obj1 = new TestObject("value1", 42);
        TestObject obj2 = new TestObject("value2", 100);

        // Act
        String result = AmphiAssignmentServiceImpl.getDifferences(obj1, obj2);

        // Assert
        assertNotNull(result);
        assertTrue(result.contains("stringField") && result.contains("intField"),
                "Result should mention all changed fields");
        assertTrue(result.contains(","),
                "Multiple differences should be comma-separated");
    }

    @Test
    void getDifferences_WithNullToValue_ReturnsDifference() {
        // Arrange
        TestObject obj1 = new TestObject(null, 42);
        TestObject obj2 = new TestObject("value", 42);

        // Act
        String result = AmphiAssignmentServiceImpl.getDifferences(obj1, obj2);

        // Assert
        assertNotNull(result);
        assertTrue(result.contains("stringField"),
                "Result should mention field changed from null");
        assertTrue(result.contains("null") && result.contains("value"),
                "Result should show null -> value transition");
    }

    @Test
    void getDifferences_WithValueToNull_ReturnsDifference() {
        // Arrange
        TestObject obj1 = new TestObject("value", 42);
        TestObject obj2 = new TestObject(null, 42);

        // Act
        String result = AmphiAssignmentServiceImpl.getDifferences(obj1, obj2);

        // Assert
        assertNotNull(result);
        assertTrue(result.contains("stringField"),
                "Result should mention field changed to null");
        assertTrue(result.contains("value") && result.contains("null"),
                "Result should show value -> null transition");
    }

    @Test
    void getDifferences_WithBothFieldsNull_ReturnsEmptyDifference() {
        // Arrange
        TestObject obj1 = new TestObject(null, 42);
        TestObject obj2 = new TestObject(null, 42);

        // Act
        String result = AmphiAssignmentServiceImpl.getDifferences(obj1, obj2);

        // Assert
        // Should not include the null field in differences
        assertFalse(result.contains("stringField"),
                "Null fields that remain null should not be in differences");
    }

    @Test
    void getDifferences_WithNullFirstObject_ThrowsException() {
        // Arrange
        TestObject obj2 = new TestObject("value", 42);

        // Act & Assert
        assertThrows(IllegalArgumentException.class,
                () -> AmphiAssignmentServiceImpl.getDifferences(null, obj2),
                "Should throw exception when first object is null");
    }

    @Test
    void getDifferences_WithNullSecondObject_ThrowsException() {
        // Arrange
        TestObject obj1 = new TestObject("value", 42);

        // Act & Assert
        assertThrows(IllegalArgumentException.class,
                () -> AmphiAssignmentServiceImpl.getDifferences(obj1, null),
                "Should throw exception when second object is null");
    }

    @Test
    void getDifferences_WithDifferentTypes_ThrowsException() {
        // Arrange
        TestObject obj1 = new TestObject("value", 42);
        String obj2 = "different type";

        // Act & Assert
        assertThrows(IllegalArgumentException.class,
                () -> AmphiAssignmentServiceImpl.getDifferences(obj1, obj2),
                "Should throw exception when objects are of different types");
    }

    // ========== Tests for deleteOldRecords() ==========

    @Test
    void deleteOldRecords_CallsRepositoryWithCorrectDate() {
        // Arrange
        Integer days = 7;
        LocalDate expectedDate = LocalDate.now().minusDays(days);

        // Act
        amphiAssignmentService.deleteOldRecords(days);

        // Assert
        verify(amphiAssignmentHistoryRepository, times(1))
                .deleteByCreatedBefore(expectedDate);
        verify(operationDistanceRepository, times(1))
                .deleteByTimestampBefore(expectedDate);
    }

    @Test
    void deleteOldRecords_WithZeroDays_DeletesRecordsFromToday() {
        // Arrange
        Integer days = 0;
        LocalDate expectedDate = LocalDate.now();

        // Act
        amphiAssignmentService.deleteOldRecords(days);

        // Assert
        verify(amphiAssignmentHistoryRepository, times(1))
                .deleteByCreatedBefore(expectedDate);
        verify(operationDistanceRepository, times(1))
                .deleteByTimestampBefore(expectedDate);
    }

    @Test
    void deleteOldRecords_With30Days_DeletesOldRecords() {
        // Arrange
        Integer days = 30;
        LocalDate expectedDate = LocalDate.now().minusDays(days);

        // Act
        amphiAssignmentService.deleteOldRecords(days);

        // Assert
        verify(amphiAssignmentHistoryRepository, times(1))
                .deleteByCreatedBefore(expectedDate);
        verify(operationDistanceRepository, times(1))
                .deleteByTimestampBefore(expectedDate);
    }

    @Test
    void getAllAssignments_UsesOperationAndFallbackDataInsteadOfHardcodedValues() {
        DestinationSiteLocation destination = DestinationSiteLocation.builder()
                .latitude(57.781)
                .longitude(14.161)
                .street("Sjukhusgatan 1")
                .locality("Jonkoping")
                .municipality("Jonkoping")
                .routeDirections("Infart A")
                .pickupTime("2026-03-06T21:40:00+01:00")
                .build();
        VehicleStatus vehicleStatus = VehicleStatus.builder()
                .id("2")
                .name("På väg")
                .event("PA_VAG")
                .build();
        Operation operation = Operation.builder()
                .id("1")
                .amPHIUniqueId("row-1")
                .callCenterId("1")
                .caseFolderId("1234567891")
                .operationID("9")
                .createdTime(LocalDateTime.of(2026, 3, 6, 19, 16, 22, 523000000))
                .sendTime(LocalDateTime.of(2026, 3, 6, 19, 17, 22, 523000000))
                .acceptedTime(LocalDateTime.of(2026, 3, 6, 19, 18, 52, 523000000))
                .destinationSiteLocation(destination)
                .patientName("Anna Andersson")
                .caseInfo("Fallskada")
                .vehicleStatus(vehicleStatus)
                .operationState(OperationState.ACTIVE)
                .assignedResourceMissionNo("339-3090\u00161")
                .build();

        when(evamOperationService.getById("1")).thenReturn(operation);
        when(operationDistanceRepository.findFirstByOperationIDOrderByTimestampDesc("1:1234567891:9")).thenReturn(Optional.empty());
        when(amphiAssignmentHistoryRepository.findFirstByOrderByCreatedDesc()).thenReturn(Optional.empty());
        when(amphiAssignmentHistoryRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(amphiStateEntryService.getAll()).thenReturn(Collections.emptyList());
        when(evamVehicleStatusService.getAll()).thenReturn(Collections.singletonList(vehicleStatus));
        when(evamVehicleStatusService.getByName("På väg")).thenReturn(vehicleStatus);
        when(evamMethaneReportService.getById("1")).thenThrow(new RuntimeException("missing"));

        Assignment[] result = amphiAssignmentService.getAllAssignments();

        assertNotNull(result);
        assertEquals(1, result.length);
        assertEquals("2026-03-06T21:40:00+01:00", result[0].getEta());
        assertEquals(DateFix.dateFixLong(operation.getCreatedTime()), result[0].getRek_report().getCreated());
        assertEquals(DateFix.dateFixLong(operation.getAcceptedTime()), result[0].getRek_report().getLast_updated());
        assertEquals("Fallskada", result[0].getRek_report().getComments());
        assertEquals("339-3090\u00161", result[0].getRek_report().getResources_on_site());
        assertEquals(DateFix.dateFixLong(operation.getCreatedTime()), result[0].getMethane_report().getCreated());
        assertEquals(DateFix.dateFixLong(operation.getAcceptedTime()), result[0].getMethane_report().getLast_updated());
        assertEquals("Sjukhusgatan 1", result[0].getMethane_report().getExact_location());
        assertEquals(DateFix.dateFixLong(operation.getAcceptedTime()), result[0].getMethane_report().getTime_first_departure());
        assertEquals(57.781, result[0].getMethane_report().getPosition().getWgs84_dd_la());
        assertEquals(14.161, result[0].getMethane_report().getPosition().getWgs84_dd_lo());
    }

    @Test
    void getAllAssignments_PrefersStoredMethaneAndTripEtaWhenPickupTimeMissing() {
        DestinationSiteLocation destination = DestinationSiteLocation.builder()
                .latitude(57.7)
                .longitude(14.2)
                .street("Infartsvagen 2")
                .build();
        VehicleStatus vehicleStatus = VehicleStatus.builder()
                .id("2")
                .name("På väg")
                .event("PA_VAG")
                .build();
        Operation operation = Operation.builder()
                .id("1")
                .amPHIUniqueId("row-2")
                .callCenterId("1")
                .caseFolderId("1234567891")
                .operationID("9")
                .createdTime(LocalDateTime.of(2026, 3, 6, 19, 16, 22, 523000000))
                .sendTime(LocalDateTime.of(2026, 3, 6, 19, 17, 22, 523000000))
                .acceptedTime(LocalDateTime.of(2026, 3, 6, 19, 18, 52, 523000000))
                .destinationSiteLocation(destination)
                .patientName("Anna Andersson")
                .caseInfo("Brand")
                .vehicleStatus(vehicleStatus)
                .operationState(OperationState.ACTIVE)
                .build();
        MethaneReport storedMethane = MethaneReport.builder()
                .created("2026-03-06T21:35:00+01:00")
                .last_updated("2026-03-06T21:36:00+01:00")
                .time_first_departure("2026-03-06T21:31:00+01:00")
                .exact_location("Tunnelmynning")
                .position(Position.builder().wgs84_dd_la(58.0).wgs84_dd_lo(15.0).build())
                .access_road(AccessRoad.builder().comment("Norr") .is_obstructed(false).build())
                .extra_resources(ExtraResources.builder().ambulances(2).units_total(2).build())
                .inventory_level(InventoryLevel.builder().levels(new String[] {"0"}).selected_level_index(0).build())
                .hazards(new String[] {"Rök"})
                .types(new String[] {"Brand"})
                .build();
        TripLocationHistory tripLocationHistory = TripLocationHistory.builder()
                .id("1")
                .etaSeconds(0)
                .build();

        when(evamOperationService.getById("1")).thenReturn(operation);
        when(operationDistanceRepository.findFirstByOperationIDOrderByTimestampDesc("1:1234567891:9")).thenReturn(Optional.empty());
        when(amphiAssignmentHistoryRepository.findFirstByOrderByCreatedDesc()).thenReturn(Optional.empty());
        when(amphiAssignmentHistoryRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(amphiStateEntryService.getAll()).thenReturn(Collections.emptyList());
        when(evamVehicleStatusService.getAll()).thenReturn(Collections.singletonList(vehicleStatus));
        when(evamVehicleStatusService.getByName("På väg")).thenReturn(vehicleStatus);
        when(evamMethaneReportService.getById("1")).thenReturn(storedMethane);
        when(evamTripHistoryLocationService.getById("1")).thenReturn(tripLocationHistory);

        Assignment[] result = amphiAssignmentService.getAllAssignments();

        assertNotNull(result[0].getEta());
        assertEquals("Tunnelmynning", result[0].getMethane_report().getExact_location());
        assertEquals("2026-03-06T21:35:00+01:00", result[0].getMethane_report().getCreated());
        assertEquals("2026-03-06T21:36:00+01:00", result[0].getMethane_report().getLast_updated());
        assertEquals("2026-03-06T21:31:00+01:00", result[0].getMethane_report().getTime_first_departure());
        assertEquals(58.0, result[0].getMethane_report().getPosition().getWgs84_dd_la());
        assertEquals(15.0, result[0].getMethane_report().getPosition().getWgs84_dd_lo());
    }

    @Test
    void getAllAssignments_ResolvesSelectedDestinationWhenHospitalIsProvidedByName() {
        DestinationSiteLocation destination = DestinationSiteLocation.builder()
                .latitude(57.65294)
                .longitude(14.06855)
                .street("Barnarpsgatan 43")
                .locality("Jonkoping")
                .municipality("Jonkoping")
                .routeDirections("Infart A")
                .pickupTime("2026-03-06T21:40:00+01:00")
                .build();
        VehicleStatus vehicleStatus = VehicleStatus.builder()
                .id("2")
                .name("På väg")
                .event("PA_VAG")
                .build();
        HospitalLocation selectedHospital = HospitalLocation.builder()
                .id(1)
                .name("Länssjukhuset Ryhov")
                .latitude(57.7664914)
                .longitude(14.1918686)
                .build();
        Operation operation = Operation.builder()
                .id("1")
                .amPHIUniqueId("row-selected-hospital-name")
                .callCenterId("1")
                .caseFolderId("1234567891")
                .operationID("9")
                .createdTime(LocalDateTime.of(2026, 3, 6, 19, 16, 22, 523000000))
                .sendTime(LocalDateTime.of(2026, 3, 6, 19, 17, 22, 523000000))
                .acceptedTime(LocalDateTime.of(2026, 3, 6, 19, 18, 52, 523000000))
                .destinationSiteLocation(destination)
                .patientName("Anna Andersson")
                .caseInfo("Fallskada")
                .vehicleStatus(vehicleStatus)
                .operationState(OperationState.ACTIVE)
                .assignedResourceMissionNo("339-3090\u00161")
                .selectedHospital("Länssjukhuset Ryhov")
                .availableHospitalLocations(new HospitalLocation[] { selectedHospital })
                .build();
        Destination amphiDestination = Destination.builder()
                .name("Ryhov")
                .wards(new Ward[] {
                        Ward.builder()
                                .id("ward-ryhov")
                                .position(Position.builder()
                                        .wgs84_dd_la(57.7664914)
                                        .wgs84_dd_lo(14.1918686)
                                        .build())
                                .build()
                })
                .build();

        when(evamOperationService.getById("1")).thenReturn(operation);
        when(operationDistanceRepository.findFirstByOperationIDOrderByTimestampDesc("1:1234567891:9")).thenReturn(Optional.empty());
        when(amphiAssignmentHistoryRepository.findFirstByOrderByCreatedDesc()).thenReturn(Optional.empty());
        when(amphiAssignmentHistoryRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(amphiStateEntryService.getAll()).thenReturn(Collections.emptyList());
        when(evamVehicleStatusService.getAll()).thenReturn(Collections.singletonList(vehicleStatus));
        when(evamVehicleStatusService.getByName("På väg")).thenReturn(vehicleStatus);
        when(evamMethaneReportService.getById("1")).thenThrow(new RuntimeException("missing"));
        when(amphiDestinationRepository.findAll()).thenReturn(List.of(amphiDestination));

        Assignment[] result = amphiAssignmentService.getAllAssignments();

        assertNotNull(result);
        assertEquals(1, result.length);
        assertEquals("ward-ryhov", result[0].getSelected_destination());
    }

    @Test
    void getAllAssignments_UsesPublishedAssignmentDistanceForAmphiDistance() {
        DestinationSiteLocation destination = DestinationSiteLocation.builder()
                .latitude(57.7)
                .longitude(14.2)
                .street("Infartsvagen 2")
                .pickupTime("2026-03-07T10:10:00+01:00")
                .build();
        VehicleStatus vehicleStatus = VehicleStatus.builder()
                .id("2")
                .name("På väg")
                .event("PA_VAG")
                .build();
        Operation operation = Operation.builder()
                .id("1")
                .amPHIUniqueId("row-3")
                .callCenterId("1")
                .caseFolderId("1234567891")
                .operationID("9")
                .createdTime(LocalDateTime.of(2026, 3, 7, 8, 0, 0))
                .sendTime(LocalDateTime.of(2026, 3, 7, 8, 1, 0))
                .acceptedTime(LocalDateTime.of(2026, 3, 7, 8, 2, 0))
                .destinationSiteLocation(destination)
                .patientName("Anna Andersson")
                .vehicleStatus(vehicleStatus)
                .operationState(OperationState.ACTIVE)
                .build();
        OperationDistance operationDistance = OperationDistance.builder()
                .timestamp(LocalDateTime.of(2026, 3, 7, 8, 5, 0))
                .operationID("1:1234567891:9")
                .assignmentDistance(1500.0)
                .publishedAssignmentDistance(900.0)
                .build();

        when(evamOperationService.getById("1")).thenReturn(operation);
        when(operationDistanceRepository.findFirstByOperationIDOrderByTimestampDesc("1:1234567891:9")).thenReturn(Optional.of(operationDistance));
        when(amphiAssignmentHistoryRepository.findFirstByOrderByCreatedDesc()).thenReturn(Optional.empty());
        when(amphiAssignmentHistoryRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(amphiStateEntryService.getAll()).thenReturn(Collections.emptyList());
        when(evamVehicleStatusService.getAll()).thenReturn(Collections.singletonList(vehicleStatus));
        when(evamVehicleStatusService.getByName("På väg")).thenReturn(vehicleStatus);
        when(evamMethaneReportService.getById("1")).thenThrow(new RuntimeException("missing"));

        Assignment[] result = amphiAssignmentService.getAllAssignments();

        assertEquals(900, result[0].getDistance());
    }

    @Test
    void getAllAssignments_MapsAlarmCategoryAndAlarmEventCodePropertiesFromOperation() {
        DestinationSiteLocation destination = DestinationSiteLocation.builder()
                .latitude(57.7)
                .longitude(14.2)
                .street("Infartsvagen 2")
                .pickupTime("2026-03-07T10:10:00+01:00")
                .build();
        VehicleStatus vehicleStatus = VehicleStatus.builder()
                .id("2")
                .name("På väg")
                .event("PA_VAG")
                .build();
        Operation operation = Operation.builder()
                .id("1")
                .amPHIUniqueId("row-alarm-fields")
                .callCenterId("1")
                .caseFolderId("1234567891")
                .operationID("9")
                .createdTime(LocalDateTime.of(2026, 3, 7, 8, 0, 0))
                .sendTime(LocalDateTime.of(2026, 3, 7, 8, 1, 0))
                .acceptedTime(LocalDateTime.of(2026, 3, 7, 8, 2, 0))
                .destinationSiteLocation(destination)
                .patientName("Anna Andersson")
                .vehicleStatus(vehicleStatus)
                .operationState(OperationState.ACTIVE)
                .alarmCategory("Trafikolycka")
                .alarmEventCode("314B")
                .build();

        when(evamOperationService.getById("1")).thenReturn(operation);
        when(evamVehicleStateService.getById("1")).thenThrow(new RuntimeException("missing"));
        when(operationDistanceRepository.findFirstByOperationIDOrderByTimestampDesc("1:1234567891:9")).thenReturn(Optional.empty());
        when(amphiAssignmentHistoryRepository.findFirstByOrderByCreatedDesc()).thenReturn(Optional.empty());
        when(amphiAssignmentHistoryRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(amphiStateEntryService.getAll()).thenReturn(Collections.emptyList());
        when(evamVehicleStatusService.getAll()).thenReturn(Collections.singletonList(vehicleStatus));
        when(evamVehicleStatusService.getByName("På väg")).thenReturn(vehicleStatus);
        when(evamMethaneReportService.getById("1")).thenThrow(new RuntimeException("missing"));

        Assignment[] result = amphiAssignmentService.getAllAssignments();

        assertEquals("Trafikolycka", getPropertyValue(result[0].getProperties(), "alarmcategory"));
        assertEquals("314B", getPropertyValue(result[0].getProperties(), "alarmeventcode"));
    }

    @Test
    void getAllAssignments_UsesLatestVehicleStatePositionForAssignmentPosition() {
        DestinationSiteLocation destination = DestinationSiteLocation.builder()
                .latitude(57.7)
                .longitude(14.2)
                .street("Infartsvagen 2")
                .pickupTime("2026-03-07T10:10:00+01:00")
                .build();
        VehicleStatus vehicleStatus = VehicleStatus.builder()
                .id("2")
                .name("På väg")
                .event("PA_VAG")
                .build();
        Operation operation = Operation.builder()
                .id("1")
                .amPHIUniqueId("row-vehicle-position")
                .callCenterId("1")
                .caseFolderId("1234567891")
                .operationID("9")
                .createdTime(LocalDateTime.of(2026, 3, 7, 8, 0, 0))
                .sendTime(LocalDateTime.of(2026, 3, 7, 8, 1, 0))
                .acceptedTime(LocalDateTime.of(2026, 3, 7, 8, 2, 0))
                .destinationSiteLocation(destination)
                .patientName("Anna Andersson")
                .vehicleStatus(vehicleStatus)
                .operationState(OperationState.ACTIVE)
                .build();
        VehicleState vehicleState = VehicleState.builder()
                .id("1")
                .activeCaseFullId("1:1234567891:9")
                .vehicleLocation(Location.builder().latitude(57.6123).longitude(14.1234).build())
                .build();

        when(evamOperationService.getById("1")).thenReturn(operation);
        when(evamVehicleStateService.getById("1")).thenReturn(vehicleState);
        when(operationDistanceRepository.findFirstByOperationIDOrderByTimestampDesc("1:1234567891:9")).thenReturn(Optional.empty());
        when(amphiAssignmentHistoryRepository.findFirstByOrderByCreatedDesc()).thenReturn(Optional.empty());
        when(amphiAssignmentHistoryRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(amphiStateEntryService.getAll()).thenReturn(Collections.emptyList());
        when(evamVehicleStatusService.getAll()).thenReturn(Collections.singletonList(vehicleStatus));
        when(evamVehicleStatusService.getByName("På väg")).thenReturn(vehicleStatus);
        when(evamMethaneReportService.getById("1")).thenThrow(new RuntimeException("missing"));

        Assignment[] result = amphiAssignmentService.getAllAssignments();

        assertNotNull(result[0].getPosition());
        assertEquals(57.6123, result[0].getPosition().getWgs84_dd_la());
        assertEquals(14.1234, result[0].getPosition().getWgs84_dd_lo());
    }

    @Test
    void getAllAssignments_LeavesAssignmentPositionNullWhenVehicleStatePositionMissing() {
        DestinationSiteLocation destination = DestinationSiteLocation.builder()
                .latitude(57.7)
                .longitude(14.2)
                .street("Infartsvagen 2")
                .pickupTime("2026-03-07T10:10:00+01:00")
                .build();
        VehicleStatus vehicleStatus = VehicleStatus.builder()
                .id("2")
                .name("På väg")
                .event("PA_VAG")
                .build();
        Operation operation = Operation.builder()
                .id("1")
                .amPHIUniqueId("row-vehicle-position-null")
                .callCenterId("1")
                .caseFolderId("1234567891")
                .operationID("9")
                .createdTime(LocalDateTime.of(2026, 3, 7, 8, 0, 0))
                .sendTime(LocalDateTime.of(2026, 3, 7, 8, 1, 0))
                .acceptedTime(LocalDateTime.of(2026, 3, 7, 8, 2, 0))
                .destinationSiteLocation(destination)
                .patientName("Anna Andersson")
                .vehicleStatus(vehicleStatus)
                .operationState(OperationState.ACTIVE)
                .build();
        VehicleState vehicleState = VehicleState.builder()
                .id("1")
                .activeCaseFullId("1:1234567891:9")
                .vehicleLocation(null)
                .build();

        when(evamOperationService.getById("1")).thenReturn(operation);
        when(evamVehicleStateService.getById("1")).thenReturn(vehicleState);
        when(operationDistanceRepository.findFirstByOperationIDOrderByTimestampDesc("1:1234567891:9")).thenReturn(Optional.empty());
        when(amphiAssignmentHistoryRepository.findFirstByOrderByCreatedDesc()).thenReturn(Optional.empty());
        when(amphiAssignmentHistoryRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(amphiStateEntryService.getAll()).thenReturn(Collections.emptyList());
        when(evamVehicleStatusService.getAll()).thenReturn(Collections.singletonList(vehicleStatus));
        when(evamVehicleStatusService.getByName("På väg")).thenReturn(vehicleStatus);
        when(evamMethaneReportService.getById("1")).thenThrow(new RuntimeException("missing"));

        Assignment[] result = amphiAssignmentService.getAllAssignments();

        assertNull(result[0].getPosition());
    }

    @Test
    void getAllAssignments_WithUnknownVehicleStatus_FallsBackToOperationStatus() {
        DestinationSiteLocation destination = DestinationSiteLocation.builder()
                .latitude(57.7)
                .longitude(14.2)
                .street("Infartsvagen 2")
                .pickupTime("2026-03-07T10:10:00+01:00")
                .build();
        VehicleStatus operationVehicleStatus = VehicleStatus.builder()
                .id("2")
                .name("På väg")
                .event("PA_VAG")
                .build();
        Operation operation = Operation.builder()
                .id("1")
                .amPHIUniqueId("row-4")
                .callCenterId("1")
                .caseFolderId("1234567891")
                .operationID("9")
                .createdTime(LocalDateTime.of(2026, 3, 7, 8, 0, 0))
                .sendTime(LocalDateTime.of(2026, 3, 7, 8, 1, 0))
                .acceptedTime(LocalDateTime.of(2026, 3, 7, 8, 2, 0))
                .destinationSiteLocation(destination)
                .patientName("Anna Andersson")
                .vehicleStatus(operationVehicleStatus)
                .operationState(OperationState.ACTIVE)
                .build();

        when(evamOperationService.getById("1")).thenReturn(operation);
        when(operationDistanceRepository.findFirstByOperationIDOrderByTimestampDesc("1:1234567891:9")).thenReturn(Optional.empty());
        when(amphiAssignmentHistoryRepository.findFirstByOrderByCreatedDesc()).thenReturn(Optional.empty());
        when(amphiAssignmentHistoryRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(amphiStateEntryService.getAll()).thenReturn(Collections.emptyList());
        when(evamVehicleStatusService.getAll()).thenReturn(Collections.singletonList(operationVehicleStatus));
        when(evamVehicleStatusService.getByName("På väg")).thenReturn(null);
        when(evamMethaneReportService.getById("1")).thenThrow(new RuntimeException("missing"));

        Assignment[] result = amphiAssignmentService.getAllAssignments();

        assertEquals("På väg", result[0].getState().getAction_name());
        assertEquals("PA_VAG", result[0].getState().getState_name());
        assertEquals(0, result[0].getState().getState_id());
    }

    // ========== Helper Test Class ==========

    /**
     * Simple test class for testing getDifferences() method.
     * Must be static so it can be used in static method tests.
     */
        @SuppressWarnings("unused")
        private static class TestObject {
        private String stringField;
        private int intField;

        public TestObject(String stringField, int intField) {
            this.stringField = stringField;
            this.intField = intField;
        }
    }

        private static String getPropertyValue(List<Property> properties, String name) {
                for (Property property : properties) {
                        if (property.getName().equals(name)) {
                                return property.getValue();
                        }
                }
                return null;
        }
}
