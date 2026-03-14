package com.dedalus.amphi_integration.gui;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.dedalus.amphi_integration.model.OperationDistance;
import com.dedalus.amphi_integration.model.evam.Location;
import com.dedalus.amphi_integration.model.evam.VehicleState;

class OperationDistanceGuiServerTest {

    @Test
    void filterOperationDistancesForCurrentVehicleState_ReturnsOnlyActiveCaseEntries() {
        OperationDistance matchingFirst = OperationDistance.builder()
                .operationID("18:17869921:1")
                .timestamp(LocalDateTime.parse("2026-03-06T18:00:00"))
                .location(Location.builder().latitude(59.31).longitude(18.08).build())
                .build();
        OperationDistance otherCase = OperationDistance.builder()
                .operationID("18:99999999:2")
                .timestamp(LocalDateTime.parse("2026-03-06T18:01:00"))
                .location(Location.builder().latitude(59.32).longitude(18.09).build())
                .build();
        OperationDistance matchingSecond = OperationDistance.builder()
                .operationID("18:17869921:1")
                .timestamp(LocalDateTime.parse("2026-03-06T18:02:00"))
                .location(Location.builder().latitude(59.33).longitude(18.10).build())
                .build();

        List<OperationDistance> filtered = OperationDistanceGuiServer.filterOperationDistancesForCurrentVehicleState(
                List.of(matchingFirst, otherCase, matchingSecond),
                VehicleState.builder().activeCaseFullId("18:17869921:1").build());

        assertThat(filtered)
                .extracting(OperationDistance::getOperationID)
                .containsExactly("18:17869921:1", "18:17869921:1");
    }

    @Test
    void filterOperationDistancesForCurrentVehicleState_ReturnsEmptyWithoutActiveCase() {
        List<OperationDistance> filtered = OperationDistanceGuiServer.filterOperationDistancesForCurrentVehicleState(
                List.of(OperationDistance.builder().operationID("18:17869921:1").build()),
                VehicleState.builder().build());

        assertThat(filtered).isEmpty();
    }
}