package com.dedalus.amphi_integration.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.dedalus.amphi_integration.AppConfig;
import com.dedalus.amphi_integration.model.loganalyzer.LogAnalysisResult;
import com.dedalus.amphi_integration.model.loganalyzer.LogAnalysisSummary;
import com.dedalus.amphi_integration.util.EvamLogExtractionQuality;
import com.dedalus.amphi_integration.util.EvamLogScenario;
import com.dedalus.amphi_integration.util.EvamLogScenarioEvent;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class LogAnalysisArchiveServiceTest {

    @TempDir
    Path tempDir;

    @Test
    void saveAndLoad_PersistsAnalysisAndScenarioFiles() throws Exception {
        TestLogAnalysisArchiveService archiveService = new TestLogAnalysisArchiveService(tempDir, new AppConfig().gson());
        LogAnalysisResult result = LogAnalysisResult.builder()
                .summary(LogAnalysisSummary.builder()
                        .sourceLog("traffic.log")
                        .totalEvents(2)
                        .replayableEvents(1)
                        .build())
                .scenario(EvamLogScenario.builder().sourceLog("traffic.log").build())
                .build();

        LogAnalysisResult saved = archiveService.save(result);
        List<LogAnalysisSummary> summaries = archiveService.listSummaries();
        LogAnalysisResult loaded = archiveService.get(saved.getSummary().getAnalysisId());

        assertNotNull(saved.getSummary().getAnalysisId());
        assertNotNull(saved.getSummary().getSavedAt());
        assertEquals(1, summaries.size());
        assertEquals(saved.getSummary().getAnalysisId(), summaries.get(0).getAnalysisId());
        assertEquals("traffic.log", loaded.getSummary().getSourceLog());
        assertTrue(Files.exists(tempDir.resolve(saved.getSummary().getAnalysisId()).resolve("scenario.json")));
    }

        @Test
        void save_SortsScenarioOutputByTimestamp() throws Exception {
        TestLogAnalysisArchiveService archiveService = new TestLogAnalysisArchiveService(tempDir, new AppConfig().gson());
        LogAnalysisResult result = LogAnalysisResult.builder()
            .summary(LogAnalysisSummary.builder()
                .sourceLog("traffic.log")
                .totalEvents(2)
                .replayableEvents(1)
                .build())
            .scenario(EvamLogScenario.builder()
                .sourceLog("traffic.log")
                .events(List.of(
                    event(2, "/api/vehiclestate", null),
                    event(1, "/api/operations", "{\"operationID\":\"1\",\"callCenterId\":\"18\",\"caseFolderId\":\"100\"}")))
                .build())
            .build();

        LogAnalysisResult saved = archiveService.save(result);
        EvamLogScenario loadedScenario = new AppConfig().gson().fromJson(
            Files.readString(tempDir.resolve(saved.getSummary().getAnalysisId()).resolve("scenario.json")),
            EvamLogScenario.class);

        assertEquals("/api/operations", loadedScenario.getEvents().get(0).getEndpoint());
        assertEquals(1, loadedScenario.getEvents().get(0).getSequence());
        assertEquals("/api/vehiclestate", loadedScenario.getEvents().get(1).getEndpoint());
        assertEquals(2, loadedScenario.getEvents().get(1).getSequence());
        }

        @Test
        void save_ExportsOnlyCompleteOperationSlices() throws Exception {
        TestLogAnalysisArchiveService archiveService = new TestLogAnalysisArchiveService(tempDir, new AppConfig().gson());
        LogAnalysisResult result = LogAnalysisResult.builder()
            .summary(LogAnalysisSummary.builder()
                .sourceLog("folder.log")
                .notes(List.of())
                .build())
                .apiCalls(List.of(
                    call(1, "/api/triplocationhistory", null, null),
                    call(2, "/api/operations", "{\"operationID\":\"1\",\"callCenterId\":\"18\",\"caseFolderId\":\"100\"}", "18:100:1"),
                    call(3, "/api/triplocationhistory", null, "18:100:1"),
                    call(4, "/api/operations", "{\"operationID\":\"2\",\"callCenterId\":\"18\",\"caseFolderId\":\"200\"}", "18:200:2"),
                    call(5, "/api/vehiclestate", null, "18:200:2"),
                    call(6, "/api/operations", "{\"operationID\":\"3\",\"callCenterId\":\"18\",\"caseFolderId\":\"300\"}", null),
                    call(7, "/api/vehiclestatus", null, null)))
            .scenario(EvamLogScenario.builder()
                .sourceLog("folder.log")
                .generatedAt("2026-03-14T12:00:00Z")
                .events(List.of(
                    event(1, "/api/triplocationhistory", null),
                    event(2, "/api/operations", "{\"operationID\":\"1\",\"callCenterId\":\"18\",\"caseFolderId\":\"100\"}"),
                    event(3, "/api/triplocationhistory", null),
                    event(4, "/api/operations", "{\"operationID\":\"2\",\"callCenterId\":\"18\",\"caseFolderId\":\"200\"}"),
                    event(5, "/api/vehiclestate", null),
                    event(6, "/api/operations", "{\"operationID\":\"3\",\"callCenterId\":\"18\",\"caseFolderId\":\"300\"}"),
                    event(7, "/api/vehiclestatus", null)))
                .build())
            .build();

        LogAnalysisResult saved = archiveService.save(result);
        Path operationsDirectory = tempDir.resolve(saved.getSummary().getAnalysisId()).resolve("operations");

        assertTrue(Files.exists(operationsDirectory));
        try (var files = Files.list(operationsDirectory)) {
            List<Path> exportedFiles = files.sorted().toList();
            assertEquals(2, exportedFiles.size());
            assertEquals("01-18_100_1.scenario.json", exportedFiles.get(0).getFileName().toString());
            assertEquals("02-18_200_2.scenario.json", exportedFiles.get(1).getFileName().toString());

            EvamLogScenario firstScenario = new AppConfig().gson()
                .fromJson(Files.readString(exportedFiles.get(0)), EvamLogScenario.class);
            EvamLogScenario secondScenario = new AppConfig().gson()
                .fromJson(Files.readString(exportedFiles.get(1)), EvamLogScenario.class);

            assertEquals(2, firstScenario.getEvents().size());
            assertEquals(2, secondScenario.getEvents().size());
            assertEquals(1, firstScenario.getEvents().get(0).getSequence());
            assertEquals(2, firstScenario.getEvents().get(1).getSequence());
            assertTrue(saved.getSummary().getNotes().stream().anyMatch(note -> note.contains("2 kompletta operationssekvenser")));
        }
        }

    @Test
    void save_MergesRepeatedSlicesForSameOperationIntoOneFile() throws Exception {
        TestLogAnalysisArchiveService archiveService = new TestLogAnalysisArchiveService(tempDir, new AppConfig().gson());
        LogAnalysisResult result = LogAnalysisResult.builder()
            .summary(LogAnalysisSummary.builder()
                .sourceLog("folder.log")
                .notes(List.of())
                .build())
            .apiCalls(List.of(
                call(1, "/api/operations", "{\"operationID\":\"1\",\"callCenterId\":\"18\",\"caseFolderId\":\"100\"}", "18:100:1"),
                call(2, "/api/vehiclestate", "{\"activeCaseFullId\":\"18:100:1\"}", "18:100:1"),
                call(3, "/api/operations", "{\"operationID\":\"2\",\"callCenterId\":\"18\",\"caseFolderId\":\"200\"}", "18:200:2"),
                call(4, "/api/vehiclestate", "{\"activeCaseFullId\":\"18:200:2\"}", "18:200:2"),
                call(5, "/api/operations", "{\"operationID\":\"1\",\"callCenterId\":\"18\",\"caseFolderId\":\"100\"}", "18:100:1"),
                call(6, "/api/vehiclestatus", "{\"activeCaseFullId\":\"18:100:1\"}", "18:100:1"),
                call(7, "/api/operations", "{\"operationID\":\"3\",\"callCenterId\":\"18\",\"caseFolderId\":\"300\"}", null)))
            .scenario(EvamLogScenario.builder()
                .sourceLog("folder.log")
                .generatedAt("2026-03-14T12:00:00Z")
                .events(List.of(
                    event(1, "/api/operations", "{\"operationID\":\"1\",\"callCenterId\":\"18\",\"caseFolderId\":\"100\"}"),
                    event(2, "/api/vehiclestate", "{\"activeCaseFullId\":\"18:100:1\"}"),
                    event(3, "/api/operations", "{\"operationID\":\"2\",\"callCenterId\":\"18\",\"caseFolderId\":\"200\"}"),
                    event(4, "/api/vehiclestate", "{\"activeCaseFullId\":\"18:200:2\"}"),
                    event(5, "/api/operations", "{\"operationID\":\"1\",\"callCenterId\":\"18\",\"caseFolderId\":\"100\"}"),
                    event(6, "/api/vehiclestatus", "{\"activeCaseFullId\":\"18:100:1\"}"),
                    event(7, "/api/operations", "{\"operationID\":\"3\",\"callCenterId\":\"18\",\"caseFolderId\":\"300\"}")))
                .build())
            .build();

        LogAnalysisResult saved = archiveService.save(result);
        Path operationsDirectory = tempDir.resolve(saved.getSummary().getAnalysisId()).resolve("operations");

        try (var files = Files.list(operationsDirectory)) {
            List<Path> exportedFiles = files.sorted().toList();
            assertEquals(2, exportedFiles.size());
            assertEquals("01-18_100_1.scenario.json", exportedFiles.get(0).getFileName().toString());
            assertEquals("02-18_200_2.scenario.json", exportedFiles.get(1).getFileName().toString());

            EvamLogScenario firstScenario = new AppConfig().gson()
                .fromJson(Files.readString(exportedFiles.get(0)), EvamLogScenario.class);

            assertEquals(4, firstScenario.getEvents().size());
            assertEquals(1, firstScenario.getEvents().get(0).getSequence());
            assertEquals(4, firstScenario.getEvents().get(3).getSequence());
        }
    }

    @Test
    void save_ExportsFilesUsingVehicleStateGroupingOnly() throws Exception {
        TestLogAnalysisArchiveService archiveService = new TestLogAnalysisArchiveService(tempDir, new AppConfig().gson());
        LogAnalysisResult result = LogAnalysisResult.builder()
            .summary(LogAnalysisSummary.builder()
                .sourceLog("folder.log")
                .notes(List.of())
                .build())
            .apiCalls(List.of(
                call(1, "/api/operations", "{\"operationID\":\"99\",\"callCenterId\":\"18\",\"caseFolderId\":\"999\"}", "18:17869921:1"),
                call(2, "/api/vehiclestate", "{\"activeCaseFullId\":\"18:17869921:1\"}", "18:17869921:1"),
                call(3, "/api/rakelstate", "{\"id\":\"1\"}", "18:17869921:1")))
            .scenario(EvamLogScenario.builder()
                .sourceLog("folder.log")
                .generatedAt("2026-03-14T12:00:00Z")
                .events(List.of())
                .build())
            .build();

        LogAnalysisResult saved = archiveService.save(result);
        Path operationsDirectory = tempDir.resolve(saved.getSummary().getAnalysisId()).resolve("operations");

        try (var files = Files.list(operationsDirectory)) {
            List<Path> exportedFiles = files.sorted().toList();
            assertEquals(1, exportedFiles.size());
            assertEquals("01-18_17869921_1.scenario.json", exportedFiles.get(0).getFileName().toString());
        }
    }

        private EvamLogScenarioEvent event(int sequence, String endpoint, String payloadJson) {
        return EvamLogScenarioEvent.builder()
            .sequence(sequence)
            .requestTimestamp("2026-03-14T12:00:0" + sequence + "Z")
            .method("POST")
            .endpoint(endpoint)
            .payloadType("json")
            .extractionQuality(EvamLogExtractionQuality.RAW_REQUEST)
            .payloadJson(payloadJson)
            .build();
        }

        private com.dedalus.amphi_integration.model.loganalyzer.ReplayApiCall call(
                int sequence,
                String endpoint,
                String payloadJson,
                String operationKey) {
        return com.dedalus.amphi_integration.model.loganalyzer.ReplayApiCall.builder()
            .sequence(sequence)
            .requestTimestamp("2026-03-14T12:00:0" + sequence + "Z")
            .method("POST")
            .endpoint(endpoint)
            .payloadType("json")
            .extractionQuality(EvamLogExtractionQuality.RAW_REQUEST)
            .payloadJson(payloadJson)
            .operationKey(operationKey)
            .replayable(true)
            .build();
        }

    private static final class TestLogAnalysisArchiveService extends LogAnalysisArchiveService {
        private final Path archiveRoot;

        private TestLogAnalysisArchiveService(Path archiveRoot, com.google.gson.Gson gson) {
            super(gson);
            this.archiveRoot = archiveRoot;
        }

        @Override
        protected Path getArchiveRoot() {
            return archiveRoot;
        }
    }
}