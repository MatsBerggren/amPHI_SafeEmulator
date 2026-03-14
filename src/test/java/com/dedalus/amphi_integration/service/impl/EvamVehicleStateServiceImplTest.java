package com.dedalus.amphi_integration.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import com.dedalus.amphi_integration.AppConfig;
import com.dedalus.amphi_integration.model.OperationDistance;
import com.dedalus.amphi_integration.model.amphi.StateEntry;
import com.dedalus.amphi_integration.model.evam.Location;
import com.dedalus.amphi_integration.model.evam.VehicleState;
import com.dedalus.amphi_integration.model.evam.VehicleStatus;
import com.dedalus.amphi_integration.repository.AmphiStateEntryRepository;
import com.dedalus.amphi_integration.repository.EvamOperationRepository;
import com.dedalus.amphi_integration.repository.EvamVehicleStateRepository;
import com.dedalus.amphi_integration.repository.EvamVehicleStatusRepository;
import com.dedalus.amphi_integration.repository.OperationDistanceRepository;
import com.google.gson.Gson;

@ExtendWith(MockitoExtension.class)
class EvamVehicleStateServiceImplTest {

    @Mock
    private AmphiStateEntryRepository amphiStateEntryRepository;

    @Mock
    private EvamVehicleStateRepository evamVehicleStateRepository;

    @Mock
    private EvamVehicleStatusRepository evamVehicleStatusRepository;

    @Mock
    private OperationDistanceRepository operationDistanceRepository;

    @Mock
    private AmphiStateEntryServiceImpl amphiStateEntryService;

    @Mock
    private EvamOperationRepository evamOperationRepository;

    @InjectMocks
    private EvamVehicleStateServiceImpl evamVehicleStateService;

    @Spy
    private final Gson gson = new AppConfig().gson();

    @Test
    void updateVehicleState_WithSameStatus_KeepsPublishedAssignmentDistanceUnchanged() {
        VehicleStatus status = VehicleStatus.builder().id("2").name("På väg").build();
        VehicleState previousState = VehicleState.builder()
                .id("1")
                .vehicleLocation(Location.builder().latitude(57.7000).longitude(14.1000).build())
                .build();
        VehicleState incomingState = VehicleState.builder()
                .timestamp(LocalDateTime.of(2026, 3, 7, 10, 0, 0))
                .activeCaseFullId("1:1234567891:9")
                .vehicleStatus(VehicleStatus.builder().name("På väg").build())
                .vehicleLocation(Location.builder().latitude(57.7005).longitude(14.1005).build())
                .build();
        OperationDistance previousDistance = OperationDistance.builder()
                .timestamp(LocalDateTime.of(2026, 3, 7, 9, 59, 0))
                .operationID("1:1234567891:9")
                .assignmentDistance(1200.0)
                .publishedAssignmentDistance(800.0)
                .stateEntryDistance(300.0)
                .stateID("2")
                .location(previousState.getVehicleLocation())
                .build();

        when(amphiStateEntryRepository.findFirstByOrderByTimeDesc()).thenReturn(Optional.of(StateEntry.builder().id("2").time("2026-03-07T10:00:00+01:00").build()));
        when(evamVehicleStatusRepository.findByName("På väg")).thenReturn(status);
        when(evamVehicleStateRepository.findFirstByOrderByTimestampDesc()).thenReturn(Optional.of(previousState));
        when(operationDistanceRepository.findFirstByOrderByTimestampDesc()).thenReturn(Optional.of(previousDistance));
        when(evamVehicleStateRepository.save(any(VehicleState.class))).thenAnswer(invocation -> invocation.getArgument(0));

        evamVehicleStateService.updateVehicleState(gson.toJson(incomingState));

        ArgumentCaptor<OperationDistance> captor = ArgumentCaptor.forClass(OperationDistance.class);
        verify(operationDistanceRepository).save(captor.capture());
        OperationDistance savedDistance = captor.getValue();

        assertNotNull(savedDistance);
        assertEquals(LocalDateTime.of(2026, 3, 7, 10, 0, 0), savedDistance.getTimestamp());
        assertEquals(800.0, savedDistance.getPublishedAssignmentDistance());
        assertEquals("2", savedDistance.getStateID());
    }

    @Test
    void updateVehicleState_WithStatusChange_PublishesNewAssignmentDistanceSnapshot() {
        VehicleStatus newStatus = VehicleStatus.builder().id("3").name("Framme").build();
        VehicleState previousState = VehicleState.builder()
                .id("1")
                .vehicleLocation(Location.builder().latitude(57.7000).longitude(14.1000).build())
                .build();
        VehicleState incomingState = VehicleState.builder()
                .timestamp(LocalDateTime.of(2026, 3, 7, 10, 1, 0))
                .activeCaseFullId("1:1234567891:9")
                .vehicleStatus(VehicleStatus.builder().name("Framme").build())
                .vehicleLocation(Location.builder().latitude(57.7005).longitude(14.1005).build())
                .build();
        OperationDistance previousDistance = OperationDistance.builder()
                .timestamp(LocalDateTime.of(2026, 3, 7, 10, 0, 0))
                .operationID("1:1234567891:9")
                .assignmentDistance(1200.0)
                .publishedAssignmentDistance(800.0)
                .stateEntryDistance(300.0)
                .stateID("2")
                .location(previousState.getVehicleLocation())
                .build();

        when(amphiStateEntryRepository.findFirstByOrderByTimeDesc()).thenReturn(Optional.of(StateEntry.builder().id("2").time("2026-03-07T10:00:00+01:00").build()));
        when(evamVehicleStatusRepository.findByName("Framme")).thenReturn(newStatus);
        when(evamVehicleStateRepository.findFirstByOrderByTimestampDesc()).thenReturn(Optional.of(previousState));
        when(operationDistanceRepository.findFirstByOrderByTimestampDesc()).thenReturn(Optional.of(previousDistance));
        when(operationDistanceRepository.findFirstByOperationIDAndStateIDOrderByTimestampDesc("1:1234567891:9", "2")).thenReturn(Optional.of(previousDistance));
        when(evamVehicleStateRepository.save(any(VehicleState.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(amphiStateEntryService.updateStateEntry(any(StateEntry.class))).thenAnswer(invocation -> invocation.getArgument(0));

        evamVehicleStateService.updateVehicleState(gson.toJson(incomingState));

        ArgumentCaptor<OperationDistance> captor = ArgumentCaptor.forClass(OperationDistance.class);
        verify(operationDistanceRepository).save(captor.capture());
        OperationDistance savedDistance = captor.getValue();

        assertNotNull(savedDistance);
        assertEquals(LocalDateTime.of(2026, 3, 7, 10, 1, 0), savedDistance.getTimestamp());
        assertEquals(savedDistance.getAssignmentDistance(), savedDistance.getPublishedAssignmentDistance());
        assertEquals("3", savedDistance.getStateID());
    }

    @Test
    void updateVehicleState_WhenVehicleStatusIsMissing_RegistersStatusAndUsesGeneratedId() {
        VehicleState incomingState = VehicleState.builder()
                .timestamp(LocalDateTime.of(2026, 3, 7, 10, 0, 0))
                .activeCaseFullId("1:1234567891:9")
                .vehicleStatus(VehicleStatus.builder()
                        .name("Ank Hämtplats")
                        .event("EVENT_AT_SITE")
                        .successorName("Avf Hämtplats")
                        .categoryType("STATUS_MISSION")
                        .categoryName("mission")
                        .build())
                .vehicleLocation(Location.builder().latitude(57.7005).longitude(14.1005).build())
                .build();

        when(amphiStateEntryRepository.findFirstByOrderByTimeDesc()).thenReturn(Optional.empty());
        when(evamVehicleStatusRepository.findByName("Ank Hämtplats")).thenReturn(null);
        when(evamVehicleStatusRepository.findAll()).thenReturn(java.util.List.of());
        when(evamVehicleStatusRepository.save(any(VehicleStatus.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(evamVehicleStateRepository.findFirstByOrderByTimestampDesc()).thenReturn(Optional.of(
                VehicleState.builder()
                        .id("1")
                        .vehicleLocation(Location.builder().latitude(57.7000).longitude(14.1000).build())
                        .build()));
        when(operationDistanceRepository.findFirstByOrderByTimestampDesc()).thenReturn(Optional.empty());
        when(evamVehicleStateRepository.save(any(VehicleState.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(amphiStateEntryService.updateStateEntry(any(StateEntry.class))).thenAnswer(invocation -> invocation.getArgument(0));

        evamVehicleStateService.updateVehicleState(gson.toJson(incomingState));

        verify(evamVehicleStatusRepository).save(argThat(status ->
                "1".equals(status.getId())
                        && "Ank Hämtplats".equals(status.getName())
                        && "EVENT_AT_SITE".equals(status.getEvent())));

        ArgumentCaptor<OperationDistance> captor = ArgumentCaptor.forClass(OperationDistance.class);
        verify(operationDistanceRepository).save(captor.capture());
        OperationDistance savedDistance = captor.getValue();

        assertNotNull(savedDistance);
        assertEquals(LocalDateTime.of(2026, 3, 7, 10, 0, 0), savedDistance.getTimestamp());
        assertEquals("1", savedDistance.getStateID());
        assertEquals(savedDistance.getAssignmentDistance(), savedDistance.getPublishedAssignmentDistance());
    }

    @Test
    void updateVehicleState_WhenTopLevelTimestampIsMissing_UsesLocationTimestamp() {
        VehicleState incomingState = VehicleState.builder()
                .activeCaseFullId("1:1234567891:9")
                .vehicleStatus(VehicleStatus.builder().name("På väg").build())
                .vehicleLocation(Location.builder()
                        .latitude(57.7005)
                        .longitude(14.1005)
                        .timestamp("1772866800000")
                        .build())
                .build();

        when(amphiStateEntryRepository.findFirstByOrderByTimeDesc()).thenReturn(Optional.empty());
        when(evamVehicleStatusRepository.findByName("På väg")).thenReturn(VehicleStatus.builder().id("2").name("På väg").build());
        when(evamVehicleStateRepository.findFirstByOrderByTimestampDesc()).thenReturn(Optional.of(
                VehicleState.builder()
                        .id("1")
                        .vehicleLocation(Location.builder().latitude(57.7000).longitude(14.1000).build())
                        .build()));
        when(operationDistanceRepository.findFirstByOrderByTimestampDesc()).thenReturn(Optional.empty());
        when(evamVehicleStateRepository.save(any(VehicleState.class))).thenAnswer(invocation -> invocation.getArgument(0));

        evamVehicleStateService.updateVehicleState(gson.toJson(incomingState));

        ArgumentCaptor<OperationDistance> captor = ArgumentCaptor.forClass(OperationDistance.class);
        verify(operationDistanceRepository).save(captor.capture());

        assertEquals(
                LocalDateTime.ofInstant(Instant.ofEpochMilli(1772866800000L), ZoneId.systemDefault()),
                captor.getValue().getTimestamp());
    }
}