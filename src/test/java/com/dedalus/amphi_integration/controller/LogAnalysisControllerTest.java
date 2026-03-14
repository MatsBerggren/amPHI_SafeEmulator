package com.dedalus.amphi_integration.controller;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.dedalus.amphi_integration.model.loganalyzer.LogAnalysisResult;
import com.dedalus.amphi_integration.model.loganalyzer.LogAnalysisSummary;
import com.dedalus.amphi_integration.model.loganalyzer.LogAnalysisJobStatus;
import com.dedalus.amphi_integration.model.loganalyzer.OperationReplayGroup;
import com.dedalus.amphi_integration.model.loganalyzer.ReplayApiCall;
import com.dedalus.amphi_integration.service.impl.EvamScenarioReplayResult;
import com.dedalus.amphi_integration.service.impl.EvamScenarioReplayService;
import com.dedalus.amphi_integration.service.impl.LogAnalysisArchiveService;
import com.dedalus.amphi_integration.service.impl.LogAnalysisImportSessionService;
import com.dedalus.amphi_integration.service.impl.LogAnalysisJobService;
import com.dedalus.amphi_integration.service.impl.LogAnalysisService;
import com.dedalus.amphi_integration.util.EvamLogExtractionQuality;
import com.dedalus.amphi_integration.util.EvamLogScenario;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

@WebMvcTest({ LogAnalysisController.class, LogAnalysisPageController.class })
class LogAnalysisControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private LogAnalysisService logAnalysisService;

        @MockBean
        private LogAnalysisArchiveService logAnalysisArchiveService;

        @MockBean
        private LogAnalysisImportSessionService logAnalysisImportSessionService;

        @MockBean
        private LogAnalysisJobService logAnalysisJobService;

        @MockBean
        private EvamScenarioReplayService evamScenarioReplayService;

    @Test
    void analyzeLog_ReturnsStructuredAnalysis() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "traffic.log",
                "text/plain",
                "Before request".getBytes());

        LogAnalysisResult result = LogAnalysisResult.builder()
                .summary(LogAnalysisSummary.builder()
                        .analysisId("analysis-1")
                        .sourceLog("traffic.log")
                        .totalEvents(1)
                        .replayableEvents(1)
                        .observedOnlyEvents(0)
                        .payloadlessEvents(0)
                        .endpointsObserved(1)
                        .savedAt("2026-03-14T10:05:00+01:00")
                        .endpointCounts(Map.of("/api/operations", 1))
                        .qualityCounts(Map.of("RAW_REQUEST", 1))
                        .operationKeys(List.of("18:17869359:2"))
                        .notes(List.of())
                        .build())
                .apiCalls(List.of(ReplayApiCall.builder()
                        .sequence(1)
                        .endpoint("/api/operations")
                        .method("POST")
                        .extractionQuality(EvamLogExtractionQuality.RAW_REQUEST)
                        .replayable(true)
                        .payloadJson("{\"operationID\":\"2\"}")
                        .build()))
                .operationGroups(List.of(OperationReplayGroup.builder().operationKey("18:17869359:2").callCount(1).build()))
                .scenario(EvamLogScenario.builder().sourceLog("traffic.log").build())
                .build();

        when(logAnalysisService.analyze(eq("traffic.log"), eq("Before request".getBytes()))).thenReturn(result);
        when(logAnalysisArchiveService.save(result)).thenReturn(result);

        mockMvc.perform(multipart("/api/log-analysis").file(file))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.summary.totalEvents").value(1))
                .andExpect(jsonPath("$.summary.analysisId").value("analysis-1"))
                .andExpect(jsonPath("$.summary.endpointCounts['/api/operations']").value(1))
                .andExpect(jsonPath("$.apiCalls[0].endpoint").value("/api/operations"))
                .andExpect(jsonPath("$.apiCalls[0].replayable").value(true));
    }

    @Test
    void analyzeLog_WithMultipleFiles_UsesCombinedAnalysis() throws Exception {
        MockMultipartFile firstFile = new MockMultipartFile(
                "file",
                "crf22/amPHIIntegration.0.out.log",
                "text/plain",
                "Before request 0".getBytes());
        MockMultipartFile secondFile = new MockMultipartFile(
                "file",
                "crf22/amPHIIntegration.1.out.log",
                "text/plain",
                "Before request 1".getBytes());

        LogAnalysisResult result = LogAnalysisResult.builder()
                .summary(LogAnalysisSummary.builder()
                        .analysisId("analysis-folder")
                        .sourceLog("Folder upload: crf22 (2 filer)")
                        .totalEvents(2)
                        .replayableEvents(2)
                        .build())
                .apiCalls(List.of())
                .operationGroups(List.of())
                .scenario(EvamLogScenario.builder().sourceLog("Folder upload: crf22 (2 filer)").build())
                .build();

        when(logAnalysisService.analyzeMany(
                eq("Folder upload: crf22 (2 filer)"),
                argThat(files -> files.size() == 2
                        && "crf22/amPHIIntegration.0.out.log".equals(files.get(0).path())
                        && "Before request 0".equals(new String(files.get(0).content()))
                        && "crf22/amPHIIntegration.1.out.log".equals(files.get(1).path())
                        && "Before request 1".equals(new String(files.get(1).content())))))
                .thenReturn(result);
        when(logAnalysisArchiveService.save(result)).thenReturn(result);

        mockMvc.perform(multipart("/api/log-analysis").file(firstFile).file(secondFile))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.summary.analysisId").value("analysis-folder"))
                .andExpect(jsonPath("$.summary.sourceLog").value("Folder upload: crf22 (2 filer)"));
    }

    @Test
    void analyzeLog_WhenFileMissing_ReturnsBadRequest() throws Exception {
        MockMultipartFile file = new MockMultipartFile("file", "empty.log", "text/plain", new byte[0]);

        mockMvc.perform(multipart("/api/log-analysis").file(file))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Ingen loggfil valdes."));
    }

    @Test
    void startImportSession_ReturnsSessionMetadata() throws Exception {
        when(logAnalysisImportSessionService.startSession("Folder upload: crf22 (4 filer)", 4))
                .thenReturn(new LogAnalysisImportSessionService.ImportSessionProgress(
                        "session-1",
                        "Folder upload: crf22 (4 filer)",
                        0,
                        4,
                        null));

        mockMvc.perform(post("/api/log-analysis/import-sessions")
                        .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                        .content("{\"sourceLog\":\"Folder upload: crf22 (4 filer)\",\"totalFiles\":4}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sessionId").value("session-1"))
                .andExpect(jsonPath("$.totalFiles").value(4));
    }

    @Test
    void appendImportSessionFile_ProcessesSingleStep() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "crf22/amPHIIntegration.0.out.log",
                "text/plain",
                "Before request 0".getBytes());

        when(logAnalysisImportSessionService.appendFile(
                "session-1",
                "crf22/amPHIIntegration.0.out.log",
                "Before request 0".getBytes()))
                .thenReturn(new LogAnalysisImportSessionService.ImportSessionProgress(
                        "session-1",
                        "Folder upload: crf22 (2 filer)",
                        1,
                        2,
                        "crf22/amPHIIntegration.0.out.log"));

        mockMvc.perform(multipart("/api/log-analysis/import-sessions/session-1/files").file(file))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.processedFiles").value(1))
                .andExpect(jsonPath("$.lastFile").value("crf22/amPHIIntegration.0.out.log"));
    }

    @Test
    void startAnalyzeLogJob_ReturnsAcceptedJobStatus() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "traffic.log",
                "text/plain",
                "Before request".getBytes());

        when(logAnalysisJobService.startFileJob(
                eq("traffic.log"),
                argThat(files -> files.size() == 1
                        && "traffic.log".equals(files.get(0).path())
                        && "Before request".equals(new String(files.get(0).content())))))
                .thenReturn(new LogAnalysisJobStatus(
                        "job-1",
                        LogAnalysisJobStatus.JobState.QUEUED,
                        "queued",
                        "Analysen väntar på att starta.",
                        0,
                        1,
                        null,
                        null,
                        null));

        mockMvc.perform(multipart("/api/log-analysis/jobs").file(file))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.jobId").value("job-1"))
                .andExpect(jsonPath("$.state").value("QUEUED"));
    }

    @Test
    void getAnalyzeLogJob_ReturnsCurrentStatus() throws Exception {
        when(logAnalysisJobService.getJob("job-1"))
                .thenReturn(new LogAnalysisJobStatus(
                        "job-1",
                        LogAnalysisJobStatus.JobState.RUNNING,
                        "extracting",
                        "Läser loggfil traffic.log...",
                        0,
                        1,
                        "traffic.log",
                        null,
                        null));

        mockMvc.perform(get("/api/log-analysis/jobs/job-1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.phase").value("extracting"))
                .andExpect(jsonPath("$.currentFile").value("traffic.log"));
    }

    @Test
    void completeImportSession_ReturnsSavedAnalysis() throws Exception {
        LogAnalysisResult result = LogAnalysisResult.builder()
                .summary(LogAnalysisSummary.builder()
                        .analysisId("analysis-folder")
                        .sourceLog("Folder upload: crf22 (2 filer)")
                        .totalEvents(2)
                        .build())
                .scenario(EvamLogScenario.builder().sourceLog("Folder upload: crf22 (2 filer)").build())
                .build();

        when(logAnalysisImportSessionService.completeSession("session-1")).thenReturn(result);

        mockMvc.perform(post("/api/log-analysis/import-sessions/session-1/complete"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.summary.analysisId").value("analysis-folder"));
    }

    @Test
    void completeImportSessionAsJob_ReturnsAcceptedJobStatus() throws Exception {
        EvamLogScenario scenario = EvamLogScenario.builder()
                .sourceLog("Folder upload: crf22 (2 filer)")
                .events(List.of())
                .build();

        when(logAnalysisImportSessionService.prepareCompletion("session-1"))
                .thenReturn(new LogAnalysisImportSessionService.CompletionRequest(
                        "Folder upload: crf22 (2 filer)",
                        2,
                        scenario));
        when(logAnalysisJobService.startScenarioJob("Folder upload: crf22 (2 filer)", scenario, 2))
                .thenReturn(new LogAnalysisJobStatus(
                        "job-folder-1",
                        LogAnalysisJobStatus.JobState.QUEUED,
                        "queued",
                        "Analysen väntar på att starta.",
                        0,
                        2,
                        null,
                        null,
                        null));

        mockMvc.perform(post("/api/log-analysis/import-sessions/session-1/complete-job"))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.jobId").value("job-folder-1"))
                .andExpect(jsonPath("$.totalFiles").value(2));
    }

    @Test
    void listSavedAnalyses_ReturnsArchiveSummaries() throws Exception {
        when(logAnalysisArchiveService.listSummaries()).thenReturn(List.of(
                LogAnalysisSummary.builder()
                        .analysisId("analysis-1")
                        .sourceLog("traffic.log")
                        .savedAt("2026-03-14T10:05:00+01:00")
                        .totalEvents(2)
                        .replayableEvents(1)
                        .operationKeys(List.of("18:17869359:2"))
                        .build()));

        mockMvc.perform(get("/api/log-analysis/archive"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].analysisId").value("analysis-1"))
                .andExpect(jsonPath("$[0].sourceLog").value("traffic.log"));
    }

    @Test
    void replaySavedAnalysis_ReturnsReplayResult() throws Exception {
        LogAnalysisResult archived = LogAnalysisResult.builder()
                .summary(LogAnalysisSummary.builder().analysisId("analysis-1").build())
                .scenario(EvamLogScenario.builder().events(List.of()).build())
                .build();
        when(logAnalysisArchiveService.get("analysis-1")).thenReturn(archived);
        when(evamScenarioReplayService.replay(archived.getScenario())).thenReturn(EvamScenarioReplayResult.builder()
                .processedCount(3)
                .skippedCount(1)
                .failedCount(0)
                .processedByEndpoint(Map.of("/api/operations", 1))
                .skippedByReason(Map.of("missing-payload", 1))
                .failures(List.of())
                .build());

        mockMvc.perform(post("/api/log-analysis/archive/analysis-1/replay"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.processedCount").value(3))
                .andExpect(jsonPath("$.skippedCount").value(1));
    }

    @Test
    void handleMaxUploadSizeExceeded_ReturnsJsonPayloadTooLarge() throws Exception {
        when(logAnalysisService.analyze(eq("traffic.log"), eq("Before request".getBytes())))
                .thenThrow(new MaxUploadSizeExceededException(64L * 1024 * 1024));

        MockMultipartFile file = new MockMultipartFile(
                "file",
                "traffic.log",
                "text/plain",
                "Before request".getBytes());

        mockMvc.perform(multipart("/api/log-analysis").file(file))
                .andExpect(status().isPayloadTooLarge())
                .andExpect(jsonPath("$.message").value("Loggfilen är för stor. Nuvarande gräns är 64 MB."));
    }
}