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
        void save_ExportsOnlyCompleteOperationSlices() throws Exception {
        TestLogAnalysisArchiveService archiveService = new TestLogAnalysisArchiveService(tempDir, new AppConfig().gson());
        LogAnalysisResult result = LogAnalysisResult.builder()
            .summary(LogAnalysisSummary.builder()
                .sourceLog("folder.log")
                .notes(List.of())
                .build())
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