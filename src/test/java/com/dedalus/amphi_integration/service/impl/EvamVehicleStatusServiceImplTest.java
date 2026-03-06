package com.dedalus.amphi_integration.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import com.dedalus.amphi_integration.model.evam.VehicleStatus;
import com.dedalus.amphi_integration.repository.EvamVehicleStatusRepository;
import com.google.gson.Gson;

@ExtendWith(MockitoExtension.class)
class EvamVehicleStatusServiceImplTest {

    @Mock
    private EvamVehicleStatusRepository evamVehicleStatusRepository;

    @InjectMocks
    private EvamVehicleStatusServiceImpl evamVehicleStatusService;

    @Spy
    private final Gson gson = new Gson();

    @Test
    void updateVehicleStatus_WithSingleObjectPayload_SavesSingleStatus() {
        VehicleStatus vehicleStatus = VehicleStatus.builder()
                .name("Available")
                .event("READY")
                .build();
        String json = gson.toJson(vehicleStatus);

        when(evamVehicleStatusRepository.saveAll(any())).thenAnswer(invocation -> invocation.getArgument(0));

        VehicleStatus[] result = evamVehicleStatusService.updateVehicleStatus(json);

        assertNotNull(result);
        assertEquals(1, result.length);
        assertEquals("1", result[0].getId());
        assertEquals("Available", result[0].getName());

        verify(evamVehicleStatusRepository).deleteAll();
        verify(evamVehicleStatusRepository).saveAll(any());
    }

    @Test
    void updateVehicleStatus_WithArrayPayload_AssignsIdsInOrder() {
        String json = gson.toJson(List.of(
                VehicleStatus.builder().name("Available").build(),
                VehicleStatus.builder().name("Busy").build()));

        when(evamVehicleStatusRepository.saveAll(any())).thenAnswer(invocation -> invocation.getArgument(0));

        VehicleStatus[] result = evamVehicleStatusService.updateVehicleStatus(json);

        assertEquals(2, result.length);
        assertEquals("1", result[0].getId());
        assertEquals("2", result[1].getId());
        assertEquals("Busy", result[1].getName());

        verify(evamVehicleStatusRepository, times(1)).deleteAll();
        verify(evamVehicleStatusRepository, times(1)).saveAll(any());
    }
}