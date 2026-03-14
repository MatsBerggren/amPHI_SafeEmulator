package com.dedalus.amphi_integration.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import com.dedalus.amphi_integration.service.EvamOperationService;
import com.dedalus.amphi_integration.service.EvamVehicleStateService;
import com.dedalus.amphi_integration.util.EvamLogExtractionQuality;
import com.dedalus.amphi_integration.util.EvamLogScenario;
import com.dedalus.amphi_integration.util.EvamLogScenarioEvent;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class EvamScenarioReplayServiceTest {

    @Mock
    private EvamOperationService evamOperationService;

    @Mock
    private EvamOperationListServiceImpl evamOperationListService;

    @Mock
    private EvamVehicleStateService evamVehicleStateService;

    @Mock
    private EvamRakelStateServiceImpl evamRakelStateService;

    @Mock
    private EvamVehicleStatusServiceImpl evamVehicleStatusService;

    @Mock
    private EvamTripHistoryLocationServiceImpl evamTripHistoryLocationService;

    @Mock
    private EvamMethaneReportServiceImpl evamMethaneReportService;

    @InjectMocks
    private EvamScenarioReplayService replayService;

    @Test
    void replay_DispatchesSupportedEndpointsAndSkipsMissingPayloads() {
        EvamLogScenario scenario = EvamLogScenario.builder()
                .events(List.of(
                        event(1, "/api/operations", "{\"operationID\":\"9\"}"),
                        event(2, "/api/vehiclestate", "{\"activeCaseFullId\":\"18:17869359:2\"}"),
                        event(3, "/api/triplocationhistory", "{\"etaSeconds\":267}"),
                        event(4, "/api/vehiclestatus", null),
                        event(5, "/api/rakelstate", "{\"msisdn\":\"3306540\"}")))
                .build();

        EvamScenarioReplayResult result = replayService.replay(scenario);

        verify(evamOperationService).updateOperation("{\"operationID\":\"9\"}");
        verify(evamVehicleStateService).updateVehicleState("{\"activeCaseFullId\":\"18:17869359:2\"}");
        verify(evamTripHistoryLocationService).updateTripLocationHistory("{\"etaSeconds\":267}");
        verify(evamRakelStateService).updateRakelState("{\"msisdn\":\"3306540\"}");
        verifyNoInteractions(evamVehicleStatusService);

        assertEquals(4, result.getProcessedCount());
        assertEquals(1, result.getSkippedCount());
        assertEquals(0, result.getFailedCount());
        assertEquals(1, result.getProcessedByEndpoint().get("/api/operations"));
        assertEquals(1, result.getProcessedByEndpoint().get("/api/vehiclestate"));
        assertEquals(1, result.getProcessedByEndpoint().get("/api/triplocationhistory"));
        assertEquals(1, result.getProcessedByEndpoint().get("/api/rakelstate"));
        assertEquals(1, result.getSkippedByReason().get("missing-payload"));
    }

    @Test
    void replay_RecordsFailuresAndContinues() {
        org.mockito.Mockito.doThrow(new RuntimeException("broken payload"))
                .when(evamVehicleStateService)
                .updateVehicleState("{\"bad\":true}");

        EvamLogScenario scenario = EvamLogScenario.builder()
                .events(List.of(
                        event(1, "/api/vehiclestate", "{\"bad\":true}"),
                        event(2, "/api/methanereport", "{\"exact_location\":\"Tunnel\"}")))
                .build();

        EvamScenarioReplayResult result = replayService.replay(scenario);

        verify(evamMethaneReportService).updateMethaneReport("{\"exact_location\":\"Tunnel\"}");
        assertEquals(1, result.getProcessedCount());
        assertEquals(0, result.getSkippedCount());
        assertEquals(1, result.getFailedCount());
        assertEquals(1, result.getFailures().size());
    }

    private EvamLogScenarioEvent event(int sequence, String endpoint, String payloadJson) {
        return EvamLogScenarioEvent.builder()
                .sequence(sequence)
                .endpoint(endpoint)
                .method("POST")
                .payloadJson(payloadJson)
                .extractionQuality(payloadJson == null ? EvamLogExtractionQuality.OBSERVED_ONLY : EvamLogExtractionQuality.RECONSTRUCTED_RETURN)
                .build();
    }
}