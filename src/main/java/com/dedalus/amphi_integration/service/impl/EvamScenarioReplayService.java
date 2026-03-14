package com.dedalus.amphi_integration.service.impl;

import com.dedalus.amphi_integration.service.EvamOperationService;
import com.dedalus.amphi_integration.service.EvamVehicleStateService;
import com.dedalus.amphi_integration.util.EvamLogScenario;
import com.dedalus.amphi_integration.util.EvamLogScenarioEvent;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class EvamScenarioReplayService {

    @Autowired
    private EvamOperationService evamOperationService;

    @Autowired
    private EvamOperationListServiceImpl evamOperationListService;

    @Autowired
    private EvamVehicleStateService evamVehicleStateService;

    @Autowired
    private EvamRakelStateServiceImpl evamRakelStateService;

    @Autowired
    private EvamVehicleStatusServiceImpl evamVehicleStatusService;

    @Autowired
    private EvamTripHistoryLocationServiceImpl evamTripHistoryLocationService;

    @Autowired
    private EvamMethaneReportServiceImpl evamMethaneReportService;

    public EvamScenarioReplayResult replay(EvamLogScenario scenario) {
        EvamScenarioReplayResult result = EvamScenarioReplayResult.empty();

        for (EvamLogScenarioEvent event : scenario.getEvents()) {
            if (event.getPayloadJson() == null || event.getPayloadJson().isBlank()) {
                skip(result, "missing-payload");
                continue;
            }

            try {
                if (dispatch(event)) {
                    result.setProcessedCount(result.getProcessedCount() + 1);
                    result.getProcessedByEndpoint().merge(event.getEndpoint(), 1, Integer::sum);
                } else {
                    skip(result, "unsupported-endpoint");
                }
            } catch (RuntimeException exception) {
                result.setFailedCount(result.getFailedCount() + 1);
                result.getFailures().add("sequence=" + event.getSequence() + " endpoint=" + event.getEndpoint()
                        + " message=" + exception.getMessage());
            }
        }

        return result;
    }

    private boolean dispatch(EvamLogScenarioEvent event) {
        return switch (event.getEndpoint()) {
            case "/api/operations" -> {
                evamOperationService.updateOperation(event.getPayloadJson());
                yield true;
            }
            case "/api/operationlist" -> {
                evamOperationListService.updateOperationList(event.getPayloadJson());
                yield true;
            }
            case "/api/vehiclestate" -> {
                evamVehicleStateService.updateVehicleState(event.getPayloadJson());
                yield true;
            }
            case "/api/rakelstate" -> {
                evamRakelStateService.updateRakelState(event.getPayloadJson());
                yield true;
            }
            case "/api/vehiclestatus" -> {
                evamVehicleStatusService.updateVehicleStatus(event.getPayloadJson());
                yield true;
            }
            case "/api/triplocationhistory" -> {
                evamTripHistoryLocationService.updateTripLocationHistory(event.getPayloadJson());
                yield true;
            }
            case "/api/methanereport" -> {
                evamMethaneReportService.updateMethaneReport(event.getPayloadJson());
                yield true;
            }
            default -> false;
        };
    }

    private void skip(EvamScenarioReplayResult result, String reason) {
        result.setSkippedCount(result.getSkippedCount() + 1);
        result.getSkippedByReason().merge(reason, 1, Integer::sum);
    }
}