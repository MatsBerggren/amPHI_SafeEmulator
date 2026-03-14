package com.dedalus.amphi_integration.service.impl;

import com.dedalus.amphi_integration.model.loganalyzer.LogAnalysisResult;
import com.dedalus.amphi_integration.model.loganalyzer.LogAnalysisSummary;
import com.dedalus.amphi_integration.model.loganalyzer.OperationReplayGroup;
import com.dedalus.amphi_integration.model.loganalyzer.ReplayApiCall;
import com.dedalus.amphi_integration.util.EvamLogScenario;
import com.dedalus.amphi_integration.util.EvamLogScenarioEvent;
import com.dedalus.amphi_integration.util.EvamLogScenarioExtractor;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.io.IOException;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.stereotype.Service;

@Service
public class LogAnalysisService {

    private static final DateTimeFormatter GENERATED_AT_FORMAT = DateTimeFormatter.ISO_OFFSET_DATE_TIME;

    private static final Set<String> REPLAYABLE_ENDPOINTS = Set.of(
            "/api/operations",
            "/api/operationlist",
            "/api/vehiclestate",
            "/api/rakelstate",
            "/api/vehiclestatus",
            "/api/triplocationhistory",
            "/api/methanereport");
        private static final Set<String> IGNORED_ENDPOINTS = Set.of(
            "/api/assignments",
            "/api/assignments/");
    private static final Pattern TRAILING_NUMBER = Pattern.compile("(\\d+)(?=\\D*$)");
    private static final String VEHICLE_STATE_ENDPOINT = "/api/vehiclestate";

    private final Gson gson;

    public LogAnalysisService(Gson gson) {
        this.gson = gson;
    }

    public LogAnalysisResult analyze(String sourceLog, byte[] rawBytes) throws IOException {
        return analyze(sourceLog, rawBytes, ProgressListener.noOp());
    }

    public LogAnalysisResult analyze(String sourceLog, byte[] rawBytes, ProgressListener progressListener) throws IOException {
        progressListener.onProgress(new ProgressUpdate(
                "extracting",
            "Tolkar loggfil och letar EVAM-anrop i " + sourceLog + "...",
                0,
                1,
                sourceLog));
        EvamLogScenario scenario = extractScenario(sourceLog, rawBytes);
        progressListener.onProgress(new ProgressUpdate(
                "analyzing",
            "Tolkning klar: " + scenarioEventCount(scenario) + " händelser från "
                + endpointCount(scenario) + " endpointar hittades. Bygger API-sekvens...",
                1,
                1,
                sourceLog));
        return analyzeScenario(scenario, progressListener);
    }

    public LogAnalysisResult analyzeMany(String sourceLog, List<NamedLogFile> logFiles) throws IOException {
        return analyzeMany(sourceLog, logFiles, ProgressListener.noOp());
    }

    public LogAnalysisResult analyzeMany(String sourceLog, List<NamedLogFile> logFiles, ProgressListener progressListener)
            throws IOException {
        return analyzeScenario(mergeScenarios(sourceLog, logFiles, progressListener), progressListener);
    }

    public EvamLogScenario extractScenario(String sourceLog, byte[] rawBytes) throws IOException {
        return new EvamLogScenarioExtractor(gson).extract(sourceLog, rawBytes);
    }

    public EvamLogScenario createScenarioShell(String sourceLog) {
        return EvamLogScenario.builder()
                .sourceLog(sourceLog)
                .generatedAt(OffsetDateTime.now(ZoneOffset.UTC).format(GENERATED_AT_FORMAT))
                .events(new ArrayList<>())
                .endpointCounts(new LinkedHashMap<>())
                .qualityCounts(new LinkedHashMap<>())
                .build();
    }

    public EvamLogScenario appendToScenario(EvamLogScenario baseScenario, NamedLogFile logFile) throws IOException {
        EvamLogScenario existingScenario = baseScenario == null
                ? createScenarioShell(logFile.path())
                : baseScenario;
        EvamLogScenario extractedScenario = extractScenario(logFile.path(), logFile.content());
        return appendExtractedScenario(existingScenario, logFile.path(), extractedScenario);
    }

    public LogAnalysisResult analyzeScenario(EvamLogScenario scenario) {
        return analyzeScenario(scenario, ProgressListener.noOp());
        }

        public LogAnalysisResult analyzeScenario(EvamLogScenario scenario, ProgressListener progressListener) {
        scenario = sortScenarioChronologically(scenario);
        progressListener.onProgress(new ProgressUpdate(
            "normalizing",
            "Normaliserar " + scenarioEventCount(scenario) + " identifierade händelser till replaybara anrop...",
            null,
            null,
            null));
        List<ReplayApiCall> apiCalls = scenario.getEvents().stream()
            .filter(event -> !shouldIgnoreEndpoint(event.getEndpoint()))
                .map(this::toReplayApiCall)
                .toList();
        List<ReplayApiCall> normalizedCalls = normalizeCalls(apiCalls);
        long replayableCount = normalizedCalls.stream().filter(ReplayApiCall::isReplayable).count();
        progressListener.onProgress(new ProgressUpdate(
            "grouping",
            replayableCount + " av " + normalizedCalls.size()
                    + " anrop kan replayas direkt. Grupperar per operation...",
            null,
            null,
            null));
        List<OperationReplayGroup> operationGroups = buildOperationGroups(normalizedCalls);
        progressListener.onProgress(new ProgressUpdate(
            "summarizing",
            "Bygger sammanfattning för " + operationGroups.size() + " operationsgrupper...",
            null,
            null,
            null));

        return LogAnalysisResult.builder()
            .summary(buildSummary(scenario, normalizedCalls))
            .apiCalls(normalizedCalls)
            .operationGroups(operationGroups)
                .scenario(scenario)
                .build();
    }

    public EvamLogScenario mergeScenarios(String sourceLog, List<NamedLogFile> logFiles) throws IOException {
        return mergeScenarios(sourceLog, logFiles, ProgressListener.noOp());
    }

    public EvamLogScenario mergeScenarios(String sourceLog, List<NamedLogFile> logFiles, ProgressListener progressListener)
            throws IOException {
        EvamLogScenario mergedScenario = createScenarioShell(sourceLog);
        List<NamedLogFile> orderedLogFiles = sortLogFiles(logFiles);
        for (int index = 0; index < orderedLogFiles.size(); index++) {
            NamedLogFile logFile = orderedLogFiles.get(index);
            progressListener.onProgress(new ProgressUpdate(
                    "extracting",
                    "Tolkar fil " + (index + 1) + " av " + orderedLogFiles.size() + ": " + logFile.path(),
                    index,
                    orderedLogFiles.size(),
                    logFile.path()));
                EvamLogScenario extractedScenario = extractScenario(logFile.path(), logFile.content());
                mergedScenario = appendExtractedScenario(mergedScenario, logFile.path(), extractedScenario);
            progressListener.onProgress(new ProgressUpdate(
                    "extracting",
                    "Fil klar: " + logFile.path() + ". "
                        + scenarioEventCount(extractedScenario) + " händelser hittade i filen, "
                        + scenarioEventCount(mergedScenario) + " totalt.",
                    index + 1,
                    orderedLogFiles.size(),
                    logFile.path()));
        }

        return mergedScenario;
    }

    private List<NamedLogFile> sortLogFiles(List<NamedLogFile> logFiles) {
        return logFiles.stream()
                .sorted(Comparator
                        .comparingInt((NamedLogFile file) -> trailingNumber(file.path()))
                        .thenComparing(NamedLogFile::path, String.CASE_INSENSITIVE_ORDER))
                .toList();
    }

    private int trailingNumber(String path) {
        if (path == null || path.isBlank()) {
            return Integer.MAX_VALUE;
        }

        Matcher matcher = TRAILING_NUMBER.matcher(path);
        if (matcher.find()) {
            try {
                return Integer.parseInt(matcher.group(1));
            } catch (NumberFormatException exception) {
                return Integer.MAX_VALUE;
            }
        }
        return Integer.MAX_VALUE;
    }

    private EvamLogScenario appendExtractedScenario(
            EvamLogScenario baseScenario,
            String filePath,
            EvamLogScenario extractedScenario) {
        List<EvamLogScenarioEvent> mergedEvents = new ArrayList<>(
                baseScenario.getEvents() == null ? List.of() : baseScenario.getEvents());
        Map<String, Integer> endpointCounts = new LinkedHashMap<>(
                baseScenario.getEndpointCounts() == null ? Map.of() : baseScenario.getEndpointCounts());
        Map<String, Integer> qualityCounts = new LinkedHashMap<>(
                baseScenario.getQualityCounts() == null ? Map.of() : baseScenario.getQualityCounts());

        int sequence = mergedEvents.size() + 1;
        for (EvamLogScenarioEvent event : extractedScenario.getEvents()) {
            mergedEvents.add(EvamLogScenarioEvent.builder()
                    .sequence(sequence++)
                    .requestTimestamp(event.getRequestTimestamp())
                    .method(event.getMethod())
                    .endpoint(event.getEndpoint())
                    .payloadType(event.getPayloadType())
                    .extractionQuality(event.getExtractionQuality())
                    .payloadJson(event.getPayloadJson())
                    .rawLogValue(event.getRawLogValue())
                    .note(prefixNote(filePath, event.getNote()))
                    .build());
        }

        mergeCounts(endpointCounts, extractedScenario.getEndpointCounts());
        mergeCounts(qualityCounts, extractedScenario.getQualityCounts());

        return EvamLogScenario.builder()
                .sourceLog(baseScenario.getSourceLog())
                .generatedAt(OffsetDateTime.now(ZoneOffset.UTC).format(GENERATED_AT_FORMAT))
                .events(mergedEvents)
                .endpointCounts(endpointCounts)
                .qualityCounts(qualityCounts)
                .build();
    }

    private EvamLogScenario sortScenarioChronologically(EvamLogScenario scenario) {
        if (scenario == null || scenario.getEvents() == null || scenario.getEvents().isEmpty()) {
            return scenario;
        }

        List<EvamLogScenarioEvent> sortedEvents = new ArrayList<>(scenario.getEvents());
        sortedEvents.sort(Comparator
                .comparing((EvamLogScenarioEvent event) -> parseTimestamp(event.getRequestTimestamp()),
                        Comparator.nullsLast(Comparator.naturalOrder()))
                .thenComparing(event -> event.getSequence() == null ? Integer.MAX_VALUE : event.getSequence()));

        for (int index = 0; index < sortedEvents.size(); index++) {
            sortedEvents.get(index).setSequence(index + 1);
        }

        return EvamLogScenario.builder()
                .sourceLog(scenario.getSourceLog())
                .generatedAt(scenario.getGeneratedAt())
                .events(sortedEvents)
                .endpointCounts(scenario.getEndpointCounts())
                .qualityCounts(scenario.getQualityCounts())
                .build();
    }

    private String prefixNote(String filePath, String note) {
        String prefix = "Source file: " + filePath;
        if (note == null || note.isBlank()) {
            return prefix;
        }
        return prefix + ". " + note;
    }

    private void mergeCounts(Map<String, Integer> target, Map<String, Integer> source) {
        if (source == null) {
            return;
        }
        source.forEach((key, value) -> target.merge(key, value, Integer::sum));
    }

    private ReplayApiCall toReplayApiCall(EvamLogScenarioEvent event) {
        boolean hasPayload = event.getPayloadJson() != null && !event.getPayloadJson().isBlank();
        boolean replayable = hasPayload && REPLAYABLE_ENDPOINTS.contains(event.getEndpoint());

        return ReplayApiCall.builder()
                .sequence(event.getSequence())
                .requestTimestamp(event.getRequestTimestamp())
            .relativeTimeSeconds(null)
                .method(event.getMethod())
                .endpoint(event.getEndpoint())
                .payloadType(event.getPayloadType())
                .extractionQuality(event.getExtractionQuality())
                .replayable(replayable)
                .operationKey(extractVehicleStateOperationKey(event))
                .payloadJson(event.getPayloadJson())
                .rawLogValue(event.getRawLogValue())
                .note(event.getNote())
                .replayCommand(replayable ? buildReplayCommand(event) : null)
                .build();
    }

    private boolean shouldIgnoreEndpoint(String endpoint) {
        return endpoint != null && IGNORED_ENDPOINTS.contains(endpoint);
    }

    private List<ReplayApiCall> normalizeCalls(List<ReplayApiCall> apiCalls) {
        List<ReplayApiCall> inferredCalls = inferMissingOperationKeys(apiCalls);
        OffsetDateTime baseline = inferredCalls.stream()
                .map(ReplayApiCall::getRequestTimestamp)
                .map(this::parseTimestamp)
                .filter(java.util.Objects::nonNull)
                .min(Comparator.naturalOrder())
                .orElse(null);

        return inferredCalls.stream()
                .map(call -> ReplayApiCall.builder()
                        .sequence(call.getSequence())
                        .requestTimestamp(call.getRequestTimestamp())
                        .relativeTimeSeconds(calculateRelativeSeconds(call.getRequestTimestamp(), baseline))
                        .method(call.getMethod())
                        .endpoint(call.getEndpoint())
                        .payloadType(call.getPayloadType())
                        .extractionQuality(call.getExtractionQuality())
                        .replayable(call.isReplayable())
                        .operationKey(call.getOperationKey())
                        .payloadJson(call.getPayloadJson())
                        .rawLogValue(call.getRawLogValue())
                        .note(call.getNote())
                        .replayCommand(call.getReplayCommand())
                        .build())
                .toList();
    }

    private List<ReplayApiCall> inferMissingOperationKeys(List<ReplayApiCall> apiCalls) {
        return java.util.stream.IntStream.range(0, apiCalls.size())
                .mapToObj(index -> {
                    ReplayApiCall call = apiCalls.get(index);
                    if (call.getOperationKey() != null && !call.getOperationKey().isBlank()) {
                        return call;
                    }

                    String inferredOperationKey = inferOperationKey(index, apiCalls);
                    if (inferredOperationKey == null) {
                        return call;
                    }

                    String updatedNote = call.getNote() == null || call.getNote().isBlank()
                            ? "Operation inferred from nearby event context."
                            : call.getNote() + " Operation inferred from nearby event context.";

                    return ReplayApiCall.builder()
                            .sequence(call.getSequence())
                            .requestTimestamp(call.getRequestTimestamp())
                            .relativeTimeSeconds(call.getRelativeTimeSeconds())
                            .method(call.getMethod())
                            .endpoint(call.getEndpoint())
                            .payloadType(call.getPayloadType())
                            .extractionQuality(call.getExtractionQuality())
                            .replayable(call.isReplayable())
                            .operationKey(inferredOperationKey)
                            .payloadJson(call.getPayloadJson())
                            .rawLogValue(call.getRawLogValue())
                            .note(updatedNote)
                            .replayCommand(call.getReplayCommand())
                            .build();
                })
                .toList();
    }

    private String inferOperationKey(int targetIndex, List<ReplayApiCall> apiCalls) {
        ReplayApiCall target = apiCalls.get(targetIndex);
        OffsetDateTime targetTime = parseTimestamp(target.getRequestTimestamp());

        String bestOperationKey = null;
        Duration bestDistance = null;
        for (int index = 0; index < apiCalls.size(); index++) {
            if (index == targetIndex) {
                continue;
            }

            ReplayApiCall candidate = apiCalls.get(index);
            if (!VEHICLE_STATE_ENDPOINT.equals(candidate.getEndpoint())
                    || candidate.getOperationKey() == null
                    || candidate.getOperationKey().isBlank()) {
                continue;
            }

            OffsetDateTime candidateTime = parseTimestamp(candidate.getRequestTimestamp());
            Duration distance = calculateDistance(targetTime, candidateTime, Math.abs(targetIndex - index));
            if (bestDistance == null || distance.compareTo(bestDistance) < 0) {
                bestDistance = distance;
                bestOperationKey = candidate.getOperationKey();
            }
        }

        if (bestDistance != null && bestDistance.compareTo(Duration.ofMinutes(2)) <= 0) {
            return bestOperationKey;
        }
        return bestDistance == null ? null : bestOperationKey;
    }

    private Duration calculateDistance(OffsetDateTime targetTime, OffsetDateTime candidateTime, int sequenceDistance) {
        if (targetTime != null && candidateTime != null) {
            return Duration.between(targetTime, candidateTime).abs();
        }
        return Duration.ofSeconds(sequenceDistance * 5L);
    }

    private Long calculateRelativeSeconds(String timestamp, OffsetDateTime baseline) {
        OffsetDateTime parsed = parseTimestamp(timestamp);
        if (baseline == null || parsed == null) {
            return null;
        }
        return Duration.between(baseline, parsed).toSeconds();
    }

    private OffsetDateTime parseTimestamp(String timestamp) {
        try {
            return timestamp == null || timestamp.isBlank() ? null : OffsetDateTime.parse(timestamp);
        } catch (RuntimeException exception) {
            return null;
        }
    }

    private List<OperationReplayGroup> buildOperationGroups(List<ReplayApiCall> apiCalls) {
        Map<String, List<ReplayApiCall>> grouped = new LinkedHashMap<>();
        for (ReplayApiCall call : apiCalls) {
            String key = call.getOperationKey() == null || call.getOperationKey().isBlank()
                    ? "unassigned"
                    : call.getOperationKey();
            grouped.computeIfAbsent(key, ignored -> new java.util.ArrayList<>()).add(call);
        }

        return grouped.entrySet().stream()
                .map(entry -> {
                    List<ReplayApiCall> calls = entry.getValue();
                    ReplayApiCall first = calls.get(0);
                    ReplayApiCall last = calls.get(calls.size() - 1);
                    return OperationReplayGroup.builder()
                            .operationKey(entry.getKey())
                            .callCount(calls.size())
                            .replayableCount((int) calls.stream().filter(ReplayApiCall::isReplayable).count())
                            .firstRequestTimestamp(first.getRequestTimestamp())
                            .lastRequestTimestamp(last.getRequestTimestamp())
                            .firstRelativeTimeSeconds(first.getRelativeTimeSeconds())
                            .lastRelativeTimeSeconds(last.getRelativeTimeSeconds())
                            .endpoints(calls.stream().map(ReplayApiCall::getEndpoint).distinct().toList())
                            .sequences(calls.stream().map(ReplayApiCall::getSequence).toList())
                            .build();
                })
                .toList();
    }

    private LogAnalysisSummary buildSummary(EvamLogScenario scenario, List<ReplayApiCall> apiCalls) {
        List<EvamLogScenarioEvent> events = scenario.getEvents();
        List<String> operationKeys = apiCalls.stream()
                .map(ReplayApiCall::getOperationKey)
                .filter(key -> key != null && !key.isBlank())
                .distinct()
                .toList();
        List<String> notes = apiCalls.stream()
                .map(ReplayApiCall::getNote)
                .filter(note -> note != null && !note.isBlank())
                .distinct()
                .toList();

        return LogAnalysisSummary.builder()
            .analysisId(null)
                .sourceLog(scenario.getSourceLog())
                .generatedAt(scenario.getGeneratedAt())
            .savedAt(null)
                .totalEvents(events.size())
                .replayableEvents((int) apiCalls.stream().filter(ReplayApiCall::isReplayable).count())
                .observedOnlyEvents((int) apiCalls.stream().filter(call -> !call.isReplayable()).count())
                .payloadlessEvents((int) apiCalls.stream().filter(call -> call.getPayloadJson() == null || call.getPayloadJson().isBlank()).count())
                .endpointsObserved(scenario.getEndpointCounts() == null ? 0 : scenario.getEndpointCounts().size())
                .firstRequestTimestamp(events.isEmpty() ? null : events.get(0).getRequestTimestamp())
                .lastRequestTimestamp(events.isEmpty() ? null : events.get(events.size() - 1).getRequestTimestamp())
                .endpointCounts(scenario.getEndpointCounts())
                .qualityCounts(scenario.getQualityCounts())
                .operationKeys(operationKeys)
                .notes(notes)
                .build();
    }

    private String extractVehicleStateOperationKey(EvamLogScenarioEvent event) {
        if (event == null || !VEHICLE_STATE_ENDPOINT.equals(event.getEndpoint())) {
            return null;
        }
        return extractVehicleStateOperationKey(event.getPayloadJson());
    }

    private String extractVehicleStateOperationKey(String payloadJson) {
        if (payloadJson == null || payloadJson.isBlank()) {
            return null;
        }

        try {
            JsonElement payload = gson.fromJson(payloadJson, JsonElement.class);
            return extractVehicleStateOperationKey(payload);
        } catch (RuntimeException exception) {
            return null;
        }
    }

    private String extractVehicleStateOperationKey(JsonElement payload) {
        if (payload == null || payload.isJsonNull()) {
            return null;
        }
        if (payload.isJsonObject()) {
            JsonObject object = payload.getAsJsonObject();
            if (object.has("activeCaseFullId") && !object.get("activeCaseFullId").isJsonNull()) {
                return object.get("activeCaseFullId").getAsString();
            }
        }
        return null;
    }

    private String buildReplayCommand(EvamLogScenarioEvent event) {
        String method = event.getMethod() == null || event.getMethod().isBlank() ? "POST" : event.getMethod();
        String payload = event.getPayloadJson() == null ? "" : event.getPayloadJson();
        return "$baseUrl = 'https://localhost:8443'\n"
                + "$body = @'\n"
                + payload
                + "\n'@\n"
                + "Invoke-RestMethod -Method " + method
                + " -Uri \"$baseUrl" + event.getEndpoint() + "\" -ContentType 'application/json; charset=utf-8' -Body $body";
    }

    public record NamedLogFile(String path, byte[] content) {
    }

    private int scenarioEventCount(EvamLogScenario scenario) {
        return scenario == null || scenario.getEvents() == null ? 0 : scenario.getEvents().size();
    }

    private int endpointCount(EvamLogScenario scenario) {
        return scenario == null || scenario.getEndpointCounts() == null ? 0 : scenario.getEndpointCounts().size();
    }

    @FunctionalInterface
    public interface ProgressListener {
        void onProgress(ProgressUpdate progressUpdate);

        static ProgressListener noOp() {
            return ignored -> {
            };
        }
    }

    public record ProgressUpdate(
            String phase,
            String message,
            Integer processedFiles,
            Integer totalFiles,
            String currentFile) {
    }
}