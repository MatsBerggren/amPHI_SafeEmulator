package com.dedalus.amphi_integration.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import com.dedalus.amphi_integration.AppConfig;
import com.dedalus.amphi_integration.model.evam.Operation;
import com.dedalus.amphi_integration.model.evam.OperationState;
import com.dedalus.amphi_integration.repository.AmphiAssignmentRepository;
import com.dedalus.amphi_integration.repository.AmphiStateEntryRepository;
import com.dedalus.amphi_integration.repository.EvamMethaneReportRepository;
import com.dedalus.amphi_integration.repository.EvamOperationRepository;
import com.dedalus.amphi_integration.repository.EvamTripLocationHistoryRepository;
import com.dedalus.amphi_integration.repository.EvamVehicleStateRepository;
import com.google.gson.Gson;

@ExtendWith(MockitoExtension.class)
class EvamOperationServiceImplTest {

    @Mock
    private AmphiAssignmentRepository amphiAssignmentRepository;

    @Mock
    private EvamOperationRepository evamOperationRepository;

    @Mock
    private EvamVehicleStateRepository evamVehicleStateRepository;

    @Mock
    private AmphiStateEntryRepository amphiStateEntryRepository;

    @Mock
    private EvamMethaneReportRepository evamMethaneRepository;

    @Mock
    private EvamTripLocationHistoryRepository evamTripLocationHistoryRepository;

    @InjectMocks
    private EvamOperationServiceImpl evamOperationService;

    @Spy
        private final Gson gson = new AppConfig().gson();

    @Test
    void updateOperation_WithWrappedStringPayload_ParsesAndSavesOperation() {
        String operationJson = """
                {
                  \"operationID\": \"9\",
                  \"name\": \"Test operation\",
                  \"callCenterId\": \"1\",
                  \"caseFolderId\": \"1234567891\",
                  \"selectedHospital\": \"42\",
                  \"selectedPriority\": 1,
                  \"operationState\": \"ACTIVE\",
                  \"assignedResourceMissionNo\": \"339-3090\\u00161\"
                }
                """;
        String wrappedPayload = gson.toJson(java.util.Map.of("operation", operationJson));

        when(evamOperationRepository.findById("1")).thenReturn(Optional.empty());
        when(evamOperationRepository.save(org.mockito.ArgumentMatchers.any(Operation.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        Operation result = evamOperationService.updateOperation(wrappedPayload);

        assertNotNull(result);
        assertEquals("1", result.getId());
        assertEquals("9", result.getOperationID());
        assertEquals("Test operation", result.getName());
        assertEquals("1", result.getCallCenterId());
        assertEquals("1234567891", result.getCaseFolderId());
        assertEquals(42, result.getSelectedHospital());
        assertEquals(1, result.getSelectedPriority());
        assertEquals(OperationState.ACTIVE, result.getOperationState());
        assertEquals("1:1234567891:9:1", result.getFullId());

        verify(evamOperationRepository).deleteAll();
        verify(amphiStateEntryRepository).deleteAll();
        verify(evamTripLocationHistoryRepository).deleteAll();
        verify(evamMethaneRepository).deleteAll();
    }

    @Test
    void updateOperation_WithRawPayload_UpdatesExistingOperation() {
        Operation existing = Operation.builder()
                .id("1")
                .callCenterId("1")
                .caseFolderId("1234567891")
                .operationID("9")
                .name("Old name")
                .build();
        String rawPayload = """
                {
                  \"operationID\": \"9\",
                  \"name\": \"New name\",
                  \"callCenterId\": \"1\",
                  \"caseFolderId\": \"1234567891\"
                }
                """;

        when(evamOperationRepository.findById("1")).thenReturn(Optional.of(existing));
        when(evamOperationRepository.save(existing)).thenReturn(existing);

        Operation result = evamOperationService.updateOperation(rawPayload);

        assertEquals("New name", result.getName());
        assertEquals("1:1234567891:9:0", result.getFullId());

        verify(evamOperationRepository, never()).deleteAll();
        verify(amphiStateEntryRepository, never()).deleteAll();
    }
}