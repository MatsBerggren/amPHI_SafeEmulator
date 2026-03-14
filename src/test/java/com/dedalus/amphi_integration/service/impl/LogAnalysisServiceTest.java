package com.dedalus.amphi_integration.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.dedalus.amphi_integration.AppConfig;
import com.dedalus.amphi_integration.model.loganalyzer.LogAnalysisResult;
import java.nio.charset.StandardCharsets;
import java.util.List;
import org.junit.jupiter.api.Test;

class LogAnalysisServiceTest {

    private final LogAnalysisService logAnalysisService = new LogAnalysisService(new AppConfig().gson());

    @Test
    void analyze_BuildsReplayableApiSequenceAndSummary() throws Exception {
        String log = String.join("\n",
                "2026-03-06T16:58:53.280+01:00 DEBUG 8252 --- [nio-8443-exec-4] o.s.w.f.CommonsRequestLoggingFilter      : Before request [POST /api/vehiclestate, client=192.168.50.74]",
                "Method EvamController.createNew(..) has returned VehicleState(id=1, timestamp=2026-03-06T15:59:39.241, vehicleStatus=VehicleStatus(id=null, name=Avf Hamtplats, event=EVENT_EXIT_SITE, successorName=Ank Dest, isStartStatus=false, isEndStatus=false, categoryType=STATUS_MISSION, categoryName=mission), activeCaseFullId=18:17869359:2, vehicleLocation=Location(latitude=59.20189240674485, longitude=17.640825396998476, timestamp=1772812779234))",
                "2026-03-06T17:38:31.799+01:00 DEBUG 8252 --- [nio-8443-exec-6] o.s.w.f.CommonsRequestLoggingFilter      : Before request [POST /api/operations, client=192.168.50.74]",
                "2026-03-06T17:38:31.800+01:00 DEBUG 8252 --- [nio-8443-exec-6] c.d.a.controller.EvamController          : POST /operations: {\"operationID\":\"2\",\"callCenterId\":\"18\",\"caseFolderId\":\"17869359\"}");

        LogAnalysisResult result = logAnalysisService.analyze("sample.log", log.getBytes(StandardCharsets.UTF_8));

        assertNotNull(result.getSummary());
        assertEquals(2, result.getSummary().getTotalEvents());
        assertEquals(2, result.getSummary().getReplayableEvents());
        assertEquals(2, result.getSummary().getEndpointsObserved());
        assertTrue(result.getSummary().getOperationKeys().contains("18:17869359:2"));
        assertEquals(2, result.getApiCalls().size());
        assertTrue(result.getApiCalls().stream().allMatch(call -> call.getReplayCommand() != null));
        assertEquals(1, result.getOperationGroups().size());
        assertEquals("18:17869359:2", result.getOperationGroups().get(0).getOperationKey());
        assertEquals(0L, result.getApiCalls().get(0).getRelativeTimeSeconds());
        assertNotNull(result.getScenario());
    }

    @Test
    void analyzeMany_MergesMultipleFilesInSequentialOrder() throws Exception {
        LogAnalysisResult result = logAnalysisService.analyzeMany(
                "Folder upload: crf22 (2 filer)",
                List.of(
                        new LogAnalysisService.NamedLogFile(
                                "crf22/amPHIIntegration.0.out.log",
                                String.join("\n",
                                        "2026-03-06T16:58:53.280+01:00 DEBUG 8252 --- [nio-8443-exec-4] o.s.w.f.CommonsRequestLoggingFilter      : Before request [POST /api/operations, client=192.168.50.74]",
                                        "2026-03-06T16:58:53.281+01:00 DEBUG 8252 --- [nio-8443-exec-4] c.d.a.controller.EvamController          : POST /operations: {\"operationID\":\"2\",\"callCenterId\":\"18\",\"caseFolderId\":\"17869359\"}")
                                        .getBytes(StandardCharsets.UTF_8)),
                        new LogAnalysisService.NamedLogFile(
                                "crf22/amPHIIntegration.1.out.log",
                                String.join("\n",
                                        "2026-03-06T16:58:54.280+01:00 DEBUG 8252 --- [nio-8443-exec-4] o.s.w.f.CommonsRequestLoggingFilter      : Before request [POST /api/vehiclestate, client=192.168.50.74]",
                                        "Method EvamController.createNew(..) has returned VehicleState(id=1, timestamp=2026-03-06T15:59:39.241, vehicleStatus=VehicleStatus(id=null, name=Avf Hamtplats, event=EVENT_EXIT_SITE, successorName=Ank Dest, isStartStatus=false, isEndStatus=false, categoryType=STATUS_MISSION, categoryName=mission), activeCaseFullId=18:17869359:2, vehicleLocation=Location(latitude=59.20189240674485, longitude=17.640825396998476, timestamp=1772812779234))")
                                        .getBytes(StandardCharsets.UTF_8))));

        assertEquals("Folder upload: crf22 (2 filer)", result.getSummary().getSourceLog());
        assertEquals(2, result.getSummary().getTotalEvents());
        assertEquals(2, result.getSummary().getEndpointsObserved());
        assertEquals(2, result.getApiCalls().size());
        assertEquals(1, result.getApiCalls().get(0).getSequence());
        assertEquals(2, result.getApiCalls().get(1).getSequence());
        assertTrue(result.getApiCalls().get(0).getNote().contains("crf22/amPHIIntegration.0.out.log"));
        assertTrue(result.getApiCalls().get(1).getNote().contains("crf22/amPHIIntegration.1.out.log"));
    }
}