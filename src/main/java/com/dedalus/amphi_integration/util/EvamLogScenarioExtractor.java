package com.dedalus.amphi_integration.util;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.MalformedInputException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Deque;
import java.util.Set;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class EvamLogScenarioExtractor {

    private static final Set<String> IGNORED_ENDPOINTS = Set.of(
        "/api/assignments",
        "/api/assignments/");

    private static final Pattern BEFORE_REQUEST = Pattern.compile(
            "^(\\S+)\\s+DEBUG.*Before request \\[(\\w+) (/api/[a-z]+), client=.*$"
    );
    private static final Pattern RAW_REQUEST = Pattern.compile("^.*POST /(operations|operationlist): (.*)$");
        private static final Pattern RAW_OPERATION_DTO = Pattern.compile("^.*EvamOperationRequestDTO\\(operation=(\\{.*\\})\\)\\s*$");
    private static final Pattern RETURN_LINE = Pattern.compile(
            "^Method EvamController\\.[^(]+\\(\\.\\.\\) (?:has )?returned(?::)? (.*)$"
    );

    private final Gson gson;

    public EvamLogScenarioExtractor(Gson gson) {
        this.gson = gson;
    }

    public EvamLogScenario extract(Path logPath) throws IOException {
        return extract(logPath, readAllLinesWithFallback(logPath));
    }

    public EvamLogScenario extract(String sourceLog, byte[] rawBytes) throws IOException {
        return extract(Path.of(sourceLog), readAllLinesWithFallback(rawBytes));
    }

    EvamLogScenario extract(Path sourceLog, List<String> lines) {
        Map<String, Deque<PendingEvent>> pendingByEndpoint = new LinkedHashMap<>();
        List<EvamLogScenarioEvent> events = new ArrayList<>();

        for (String line : lines) {
            Matcher beforeMatcher = BEFORE_REQUEST.matcher(line);
            if (beforeMatcher.matches()) {
                String endpoint = beforeMatcher.group(3);
                if (shouldIgnoreEndpoint(endpoint)) {
                    continue;
                }
                PendingEvent pending = new PendingEvent();
                pending.requestTimestamp = beforeMatcher.group(1);
                pending.method = beforeMatcher.group(2);
                pending.endpoint = endpoint;
                pendingByEndpoint.computeIfAbsent(pending.endpoint, key -> new ArrayDeque<>()).addLast(pending);
                continue;
            }

            Matcher rawMatcher = RAW_REQUEST.matcher(line);
            if (rawMatcher.matches()) {
                String endpoint = "/api/" + rawMatcher.group(1);
                PendingEvent pending = pollPending(pendingByEndpoint, endpoint);
                if (pending != null) {
                    pending.payloadType = endpoint.endsWith("operationlist") ? "OperationList" : "Operation";
                    pending.extractionQuality = EvamLogExtractionQuality.RAW_REQUEST;
                    pending.payloadJson = rawMatcher.group(2).trim();
                    pending.rawLogValue = rawMatcher.group(2).trim();
                    events.add(pending.toEvent(events.size() + 1));
                }
                continue;
            }

            Matcher rawOperationDtoMatcher = RAW_OPERATION_DTO.matcher(line);
            if (rawOperationDtoMatcher.matches()) {
                PendingEvent pending = pollPending(pendingByEndpoint, "/api/operations");
                if (pending != null) {
                    pending.payloadType = "Operation";
                    pending.extractionQuality = EvamLogExtractionQuality.RAW_REQUEST;
                    pending.payloadJson = rawOperationDtoMatcher.group(1).trim();
                    pending.rawLogValue = rawOperationDtoMatcher.group(1).trim();
                    events.add(pending.toEvent(events.size() + 1));
                }
                continue;
            }

            Matcher returnMatcher = RETURN_LINE.matcher(line);
            if (!returnMatcher.matches()) {
                continue;
            }

            String returnValue = returnMatcher.group(1).trim();
            String endpoint = resolveEndpointFromReturnValue(returnValue);
            if (endpoint == null) {
                continue;
            }

            PendingEvent pending = pollPending(pendingByEndpoint, endpoint);
            if (pending == null) {
                continue;
            }

            pending.rawLogValue = returnValue;
            pending.payloadType = resolvePayloadType(returnValue, endpoint);

            if (returnValue.startsWith("[Lcom.dedalus.amphi_integration.model.evam.VehicleStatus;")) {
                pending.extractionQuality = EvamLogExtractionQuality.OBSERVED_ONLY;
                pending.note = "VehicleStatus array was logged as a Java array reference and could not be reconstructed.";
            } else if (!"null".equals(returnValue)) {
                try {
                    JsonElement jsonElement = StructuredLogValueParser.parse(returnValue);
                    pending.payloadJson = gson.toJson(jsonElement);
                    pending.extractionQuality = EvamLogExtractionQuality.RECONSTRUCTED_RETURN;
                } catch (IllegalArgumentException exception) {
                    pending.extractionQuality = EvamLogExtractionQuality.OBSERVED_ONLY;
                    pending.note = "Return value could not be parsed into structured JSON.";
                }
            } else {
                pending.extractionQuality = EvamLogExtractionQuality.OBSERVED_ONLY;
                pending.note = "Endpoint returned null and no payload was available in the log.";
            }

            events.add(pending.toEvent(events.size() + 1));
        }

        flushPending(pendingByEndpoint, events);
        inferOperationPayloads(events);
        interpolateMissingTimestamps(events);
        sortEventsByTimestamp(events);
        resequence(events);

        return EvamLogScenario.builder()
                .sourceLog(sourceLog.toString())
                .generatedAt(OffsetDateTime.now().toString())
                .events(events)
                .endpointCounts(countByEndpoint(events))
                .qualityCounts(countByQuality(events))
                .build();
    }

    private boolean shouldIgnoreEndpoint(String endpoint) {
        return endpoint != null && IGNORED_ENDPOINTS.contains(endpoint);
    }

    public void writeScenario(Path logPath, Path outputPath) throws IOException {
        EvamLogScenario scenario = extract(logPath);
        Files.writeString(outputPath, gson.toJson(scenario));
    }

    private List<String> readAllLinesWithFallback(Path logPath) throws IOException {
        List<Charset> candidateCharsets = List.of(
                StandardCharsets.UTF_8,
                Charset.forName("windows-1252"),
                StandardCharsets.ISO_8859_1);

        IOException lastException = null;
        for (Charset charset : candidateCharsets) {
            try {
                return Files.readAllLines(logPath, charset);
            } catch (MalformedInputException exception) {
                lastException = exception;
            }
        }

        throw lastException == null ? new IOException("Failed to read log file: " + logPath) : lastException;
    }

    private List<String> readAllLinesWithFallback(byte[] rawBytes) throws IOException {
        List<Charset> candidateCharsets = List.of(
                StandardCharsets.UTF_8,
                Charset.forName("windows-1252"),
                StandardCharsets.ISO_8859_1);

        IOException lastException = null;
        for (Charset charset : candidateCharsets) {
            try {
                CharsetDecoder decoder = charset.newDecoder()
                        .onMalformedInput(CodingErrorAction.REPORT)
                        .onUnmappableCharacter(CodingErrorAction.REPORT);
                CharBuffer decoded = decoder.decode(ByteBuffer.wrap(rawBytes));
                return decoded.toString().lines().toList();
            } catch (MalformedInputException exception) {
                lastException = exception;
            }
        }

        throw lastException == null ? new IOException("Failed to decode uploaded log content") : lastException;
    }

    private PendingEvent pollPending(Map<String, Deque<PendingEvent>> pendingByEndpoint, String endpoint) {
        return Optional.ofNullable(pendingByEndpoint.get(endpoint))
                .filter(queue -> !queue.isEmpty())
                .map(Deque::pollFirst)
                .orElse(null);
    }

    private void flushPending(Map<String, Deque<PendingEvent>> pendingByEndpoint, List<EvamLogScenarioEvent> events) {
        for (Map.Entry<String, Deque<PendingEvent>> entry : pendingByEndpoint.entrySet()) {
            while (!entry.getValue().isEmpty()) {
                PendingEvent pending = entry.getValue().pollFirst();
                pending.extractionQuality = EvamLogExtractionQuality.OBSERVED_ONLY;
                pending.note = "Only the request envelope was logged for this event.";
                events.add(pending.toEvent(events.size() + 1));
            }
        }
    }

    private Map<String, Integer> countByEndpoint(List<EvamLogScenarioEvent> events) {
        Map<String, Integer> counts = new LinkedHashMap<>();
        for (EvamLogScenarioEvent event : events) {
            counts.merge(event.getEndpoint(), 1, Integer::sum);
        }
        return counts;
    }

    private Map<String, Integer> countByQuality(List<EvamLogScenarioEvent> events) {
        Map<String, Integer> counts = new LinkedHashMap<>();
        for (EvamLogScenarioEvent event : events) {
            counts.merge(event.getExtractionQuality().name(), 1, Integer::sum);
        }
        return counts;
    }

    private void inferOperationPayloads(List<EvamLogScenarioEvent> events) {
        for (int index = 0; index < events.size(); index++) {
            EvamLogScenarioEvent event = events.get(index);
            if (event.getPayloadJson() != null && !event.getPayloadJson().isBlank()) {
                continue;
            }
            if (!"/api/operations".equals(event.getEndpoint()) && !"/api/operationlist".equals(event.getEndpoint())) {
                continue;
            }

            String activeCaseFullId = findNearestActiveCaseFullId(events, event);
            if (activeCaseFullId == null) {
                continue;
            }

            String[] parts = activeCaseFullId.split(":");
            if (parts.length < 3) {
                continue;
            }

            JsonObject operation = new JsonObject();
            operation.addProperty("callCenterId", parts[0]);
            operation.addProperty("caseFolderId", parts[1]);
            operation.addProperty("operationID", parts[2]);
            operation.addProperty("name", "Inferred from log context");
            operation.addProperty("caseInfo", activeCaseFullId);

            if ("/api/operations".equals(event.getEndpoint())) {
                event.setPayloadType("Operation");
                event.setPayloadJson(gson.toJson(operation));
            } else {
                JsonObject operationList = new JsonObject();
                operationList.add("operationList", gson.toJsonTree(List.of(operation)));
                event.setPayloadType("OperationList");
                event.setPayloadJson(gson.toJson(operationList));
            }

            event.setExtractionQuality(EvamLogExtractionQuality.INFERRED_CONTEXT);
            event.setNote("Payload inferred from nearby VehicleState activeCaseFullId.");
        }
    }

    private String findNearestActiveCaseFullId(List<EvamLogScenarioEvent> events, EvamLogScenarioEvent targetEvent) {
        OffsetDateTime pivotTime = parseTimestamp(targetEvent.getRequestTimestamp());
        String bestActiveCaseFullId = null;
        Duration bestDistance = null;

        for (EvamLogScenarioEvent candidate : events) {
            if (candidate == targetEvent || candidate.getPayloadJson() == null || candidate.getPayloadJson().isBlank()) {
                continue;
            }

            OffsetDateTime candidateTime = parseTimestamp(candidate.getRequestTimestamp());
            if (pivotTime != null && candidateTime != null) {
                Duration distance = Duration.between(pivotTime, candidateTime).abs();
                if (distance.toMinutes() > 2) {
                    continue;
                }

                String activeCaseFullId = extractActiveCaseFullId(candidate);
                if (activeCaseFullId != null && (bestDistance == null || distance.compareTo(bestDistance) < 0)) {
                    bestActiveCaseFullId = activeCaseFullId;
                    bestDistance = distance;
                }
                continue;
            }

            if (bestActiveCaseFullId == null) {
                bestActiveCaseFullId = extractActiveCaseFullId(candidate);
            }
        }

        return bestActiveCaseFullId;
    }

    private String extractActiveCaseFullId(EvamLogScenarioEvent candidate) {
        try {
            JsonObject payload = gson.fromJson(candidate.getPayloadJson(), JsonObject.class);
            if (payload != null && payload.has("activeCaseFullId") && !payload.get("activeCaseFullId").isJsonNull()) {
                return payload.get("activeCaseFullId").getAsString();
            }
            if (payload != null && payload.has("operationList") && payload.get("operationList").isJsonArray()
                    && !payload.getAsJsonArray("operationList").isEmpty()) {
                JsonObject firstOperation = payload.getAsJsonArray("operationList").get(0).getAsJsonObject();
                if (firstOperation.has("callCenterId") && firstOperation.has("caseFolderId") && firstOperation.has("operationID")) {
                    return firstOperation.get("callCenterId").getAsString() + ":"
                            + firstOperation.get("caseFolderId").getAsString() + ":"
                            + firstOperation.get("operationID").getAsString();
                }
            }
            if (payload != null && payload.has("callCenterId") && payload.has("caseFolderId") && payload.has("operationID")) {
                return payload.get("callCenterId").getAsString() + ":"
                        + payload.get("caseFolderId").getAsString() + ":"
                        + payload.get("operationID").getAsString();
            }
        } catch (RuntimeException exception) {
            return null;
        }

        return null;
    }

    private OffsetDateTime parseTimestamp(String timestamp) {
        try {
            return timestamp == null ? null : OffsetDateTime.parse(timestamp);
        } catch (RuntimeException exception) {
            return null;
        }
    }

    private void interpolateMissingTimestamps(List<EvamLogScenarioEvent> events) {
        int index = 0;
        while (index < events.size()) {
            if (parseTimestamp(events.get(index).getRequestTimestamp()) != null) {
                index += 1;
                continue;
            }

            int missingStart = index;
            while (index < events.size() && parseTimestamp(events.get(index).getRequestTimestamp()) == null) {
                index += 1;
            }

            int missingEnd = index - 1;
            OffsetDateTime previousKnown = missingStart > 0
                    ? parseTimestamp(events.get(missingStart - 1).getRequestTimestamp())
                    : null;
            OffsetDateTime nextKnown = index < events.size()
                    ? parseTimestamp(events.get(index).getRequestTimestamp())
                    : null;

            assignInterpolatedTimestamps(events, missingStart, missingEnd, previousKnown, nextKnown);
        }
    }

    private void assignInterpolatedTimestamps(
            List<EvamLogScenarioEvent> events,
            int missingStart,
            int missingEnd,
            OffsetDateTime previousKnown,
            OffsetDateTime nextKnown) {
        int missingCount = missingEnd - missingStart + 1;
        if (missingCount < 1) {
            return;
        }

        if (previousKnown != null && nextKnown != null && previousKnown.isBefore(nextKnown)) {
            long startMillis = previousKnown.toInstant().toEpochMilli();
            long endMillis = nextKnown.toInstant().toEpochMilli();
            long gapMillis = Math.max(endMillis - startMillis, missingCount + 1L);

            for (int offset = 0; offset < missingCount; offset++) {
                long assignedMillis = startMillis + ((offset + 1L) * gapMillis) / (missingCount + 1L);
                events.get(missingStart + offset).setRequestTimestamp(formatEpochMillis(assignedMillis));
            }
            return;
        }

        if (previousKnown != null) {
            long baseMillis = previousKnown.toInstant().toEpochMilli();
            for (int offset = 0; offset < missingCount; offset++) {
                events.get(missingStart + offset).setRequestTimestamp(formatEpochMillis(baseMillis + offset + 1L));
            }
            return;
        }

        if (nextKnown != null) {
            long baseMillis = nextKnown.toInstant().toEpochMilli();
            for (int offset = missingCount - 1; offset >= 0; offset--) {
                int distanceFromNext = missingCount - offset;
                events.get(missingStart + offset).setRequestTimestamp(formatEpochMillis(baseMillis - distanceFromNext));
            }
        }
    }

    private String formatEpochMillis(long epochMillis) {
        return Instant.ofEpochMilli(epochMillis).atOffset(ZoneOffset.UTC).toString();
    }

    private void sortEventsByTimestamp(List<EvamLogScenarioEvent> events) {
        events.sort(Comparator
                .comparing((EvamLogScenarioEvent event) -> parseTimestamp(event.getRequestTimestamp()),
                        Comparator.nullsLast(Comparator.naturalOrder()))
                .thenComparing(event -> event.getSequence() == null ? Integer.MAX_VALUE : event.getSequence()));
    }

    private void resequence(List<EvamLogScenarioEvent> events) {
        for (int index = 0; index < events.size(); index++) {
            events.get(index).setSequence(index + 1);
        }
    }

    private String resolveEndpointFromReturnValue(String returnValue) {
        if (returnValue.startsWith("VehicleState(")) {
            return "/api/vehiclestate";
        }
        if (returnValue.startsWith("TripLocationHistory(")) {
            return "/api/triplocationhistory";
        }
        if (returnValue.startsWith("RakelState(")) {
            return "/api/rakelstate";
        }
        if (returnValue.startsWith("[Lcom.dedalus.amphi_integration.model.evam.VehicleStatus;")) {
            return "/api/vehiclestatus";
        }
        return null;
    }

    private String resolvePayloadType(String returnValue, String endpoint) {
        if (returnValue.startsWith("[Lcom.dedalus.amphi_integration.model.evam.VehicleStatus;")) {
            return "VehicleStatus[]";
        }
        int objectStart = returnValue.indexOf('(');
        if (objectStart > 0) {
            return returnValue.substring(0, objectStart);
        }
        return endpoint.endsWith("operationlist") ? "OperationList" : null;
    }

    private static final class PendingEvent {
        private String requestTimestamp;
        private String method;
        private String endpoint;
        private String payloadType;
        private EvamLogExtractionQuality extractionQuality;
        private String payloadJson;
        private String rawLogValue;
        private String note;

        private EvamLogScenarioEvent toEvent(int sequence) {
            return EvamLogScenarioEvent.builder()
                    .sequence(sequence)
                    .requestTimestamp(requestTimestamp)
                    .method(method)
                    .endpoint(endpoint)
                    .payloadType(payloadType)
                    .extractionQuality(extractionQuality == null ? EvamLogExtractionQuality.OBSERVED_ONLY : extractionQuality)
                    .payloadJson(payloadJson)
                    .rawLogValue(rawLogValue)
                    .note(note)
                    .build();
        }
    }
}