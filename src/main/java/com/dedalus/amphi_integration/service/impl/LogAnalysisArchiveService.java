package com.dedalus.amphi_integration.service.impl;

import com.dedalus.amphi_integration.model.loganalyzer.LogAnalysisResult;
import com.dedalus.amphi_integration.model.loganalyzer.LogAnalysisSummary;
import com.dedalus.amphi_integration.model.loganalyzer.ReplayApiCall;
import com.dedalus.amphi_integration.util.EvamLogExtractionQuality;
import com.dedalus.amphi_integration.util.EvamLogScenario;
import com.dedalus.amphi_integration.util.EvamLogScenarioEvent;
import com.google.gson.Gson;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Stream;
import org.springframework.stereotype.Service;

@Service
public class LogAnalysisArchiveService {

    private static final String ANALYSIS_FILE = "analysis.json";
    private static final String SCENARIO_FILE = "scenario.json";
    private static final String OPERATIONS_DIRECTORY = "operations";

    private final Gson gson;
    private final Path archiveRoot = Paths.get("data", "log-analysis");

    public LogAnalysisArchiveService(Gson gson) {
        this.gson = gson;
    }

    public LogAnalysisResult save(LogAnalysisResult result) throws IOException {
        ensureArchiveDirectory();
        sortScenario(result.getScenario());

        LogAnalysisSummary summary = result.getSummary();
        if (summary.getAnalysisId() == null || summary.getAnalysisId().isBlank()) {
            summary.setAnalysisId(UUID.randomUUID().toString());
        }
        summary.setSavedAt(OffsetDateTime.now().toString());

        Path analysisDirectory = getArchiveRoot().resolve(summary.getAnalysisId());
        Files.createDirectories(analysisDirectory);
        Files.writeString(analysisDirectory.resolve(ANALYSIS_FILE), gson.toJson(result));
        Files.writeString(analysisDirectory.resolve(SCENARIO_FILE), gson.toJson(result.getScenario()));
        int exportedOperationFiles = saveOperationScenarios(result, analysisDirectory);
        addOperationExportNote(summary, exportedOperationFiles);
        Files.writeString(analysisDirectory.resolve(ANALYSIS_FILE), gson.toJson(result));
        return result;
    }

    public List<LogAnalysisSummary> listSummaries() throws IOException {
        ensureArchiveDirectory();
        try (Stream<Path> directories = Files.list(getArchiveRoot())) {
            return directories
                    .filter(Files::isDirectory)
                    .map(path -> path.resolve(ANALYSIS_FILE))
                    .filter(Files::exists)
                    .map(this::readSummaryUnchecked)
                    .sorted(Comparator.comparing(
                            LogAnalysisSummary::getSavedAt,
                            Comparator.nullsLast(Comparator.reverseOrder())))
                    .toList();
        }
    }

    public LogAnalysisResult get(String analysisId) throws IOException {
        Path analysisFile = getArchiveRoot().resolve(analysisId).resolve(ANALYSIS_FILE);
        if (!Files.exists(analysisFile)) {
            throw new IllegalArgumentException("Ingen sparad analys hittades för id " + analysisId);
        }
        return gson.fromJson(Files.readString(analysisFile), LogAnalysisResult.class);
    }

    private void ensureArchiveDirectory() throws IOException {
        Files.createDirectories(getArchiveRoot());
    }

    protected Path getArchiveRoot() {
        return archiveRoot;
    }

    private LogAnalysisSummary readSummaryUnchecked(Path analysisFile) {
        try {
            return gson.fromJson(Files.readString(analysisFile), LogAnalysisResult.class).getSummary();
        } catch (IOException exception) {
            throw new IllegalStateException("Kunde inte läsa sparad analys från " + analysisFile, exception);
        }
    }

    private int saveOperationScenarios(LogAnalysisResult result, Path analysisDirectory) throws IOException {
        Path operationsDirectory = analysisDirectory.resolve(OPERATIONS_DIRECTORY);
        Files.createDirectories(operationsDirectory);

        List<OperationScenarioSlice> slices = extractCompleteOperationScenarios(result);
        for (int index = 0; index < slices.size(); index++) {
            OperationScenarioSlice slice = slices.get(index);
            String fileName = String.format(
                    "%02d-%s.scenario.json",
                    index + 1,
                    sanitizeFileName(slice.operationKey()));
            Files.writeString(operationsDirectory.resolve(fileName), gson.toJson(slice.scenario()));
        }
        return slices.size();
    }

    private void addOperationExportNote(LogAnalysisSummary summary, int exportedOperationFiles) {
        List<String> notes = new ArrayList<>(summary.getNotes() == null ? List.of() : summary.getNotes());
        if (exportedOperationFiles > 0) {
            notes.add(exportedOperationFiles
                    + " kompletta operationssekvenser sparades under operations/. Sista ofullständiga operationen ignorerades.");
        } else {
            notes.add("Inga kompletta operationssekvenser hittades för separat export.");
        }
        summary.setNotes(notes);
    }

    private List<OperationScenarioSlice> extractCompleteOperationScenarios(LogAnalysisResult result) {
        if (result == null || result.getApiCalls() == null || result.getApiCalls().isEmpty()) {
            return List.of();
        }

        Map<String, List<ReplayApiCall>> callsByOperationKey = new LinkedHashMap<>();
        for (ReplayApiCall call : result.getApiCalls()) {
            String operationKey = call.getOperationKey();
            if (operationKey == null || operationKey.isBlank()) {
                continue;
            }
            callsByOperationKey.computeIfAbsent(operationKey, ignored -> new ArrayList<>()).add(call);
        }

        return callsByOperationKey.entrySet().stream()
        .map(entry -> new OperationScenarioSlice(
            entry.getKey(),
                        buildScenarioSlice(result.getScenario(), entry.getValue(), entry.getKey())))
        .toList();
    }

    private EvamLogScenario buildScenarioSlice(
            EvamLogScenario parentScenario,
            List<ReplayApiCall> sliceCalls,
            String operationKey) {
        List<EvamLogScenarioEvent> exportedEvents = sliceCalls.stream()
            .sorted(Comparator.comparing(
                ReplayApiCall::getRequestTimestamp,
                Comparator.nullsLast(String::compareTo)))
            .map(call -> EvamLogScenarioEvent.builder()
                .requestTimestamp(call.getRequestTimestamp())
                .method(call.getMethod())
                .endpoint(call.getEndpoint())
                .payloadType(call.getPayloadType())
                .extractionQuality(call.getExtractionQuality())
                .payloadJson(call.getPayloadJson())
                .rawLogValue(call.getRawLogValue())
                .note(call.getNote())
                .build())
            .toList();
        Map<String, Integer> endpointCounts = new LinkedHashMap<>();
        Map<String, Integer> qualityCounts = new LinkedHashMap<>();

        int sequence = 1;
        for (EvamLogScenarioEvent event : exportedEvents) {
            event.setSequence(sequence++);
            mergeCount(endpointCounts, event.getEndpoint());
            mergeCount(qualityCounts, event.getExtractionQuality() == null ? null : event.getExtractionQuality().name());
        }

        return EvamLogScenario.builder()
                .sourceLog(parentScenario.getSourceLog() + " :: operation " + operationKey)
                .generatedAt(parentScenario.getGeneratedAt())
                .events(exportedEvents)
                .endpointCounts(endpointCounts)
                .qualityCounts(qualityCounts)
                .build();
    }

    private void mergeCount(Map<String, Integer> target, String key) {
        if (key == null || key.isBlank()) {
            return;
        }
        target.merge(key, 1, Integer::sum);
    }

    private String sanitizeFileName(String value) {
        return value.replaceAll("[^a-zA-Z0-9._-]+", "_");
    }

    private void sortScenario(EvamLogScenario scenario) {
        if (scenario == null || scenario.getEvents() == null) {
            return;
        }

        List<EvamLogScenarioEvent> sortedEvents = new ArrayList<>(scenario.getEvents());
        sortedEvents.sort(Comparator.comparing(
                EvamLogScenarioEvent::getRequestTimestamp,
                Comparator.nullsLast(String::compareTo)));
        for (int index = 0; index < sortedEvents.size(); index++) {
            sortedEvents.get(index).setSequence(index + 1);
        }
        scenario.setEvents(sortedEvents);
    }

    private record OperationScenarioSlice(String operationKey, EvamLogScenario scenario) {
    }
}