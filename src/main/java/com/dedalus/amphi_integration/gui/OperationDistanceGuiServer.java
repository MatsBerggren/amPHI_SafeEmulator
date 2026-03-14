package com.dedalus.amphi_integration.gui;

import java.awt.Desktop;
import java.io.IOException;
import java.net.URLDecoder;
import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.springframework.beans.factory.DisposableBean;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import com.dedalus.amphi_integration.model.OperationDistance;
import com.dedalus.amphi_integration.model.evam.VehicleState;
import com.dedalus.amphi_integration.model.evam.VehicleStatus;
import com.dedalus.amphi_integration.repository.EvamVehicleStateRepository;
import com.dedalus.amphi_integration.repository.EvamVehicleStatusRepository;
import com.dedalus.amphi_integration.service.impl.EvamScenarioReplayResult;
import com.dedalus.amphi_integration.service.impl.EvamScenarioReplayService;
import com.dedalus.amphi_integration.repository.OperationDistanceRepository;
import com.dedalus.amphi_integration.util.EvamLogScenario;
import com.dedalus.amphi_integration.util.EvamLogScenarioEvent;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class OperationDistanceGuiServer implements DisposableBean {

    private static final int GUI_PORT = 8765;
    private static final Gson GSON = new GsonBuilder().disableHtmlEscaping().create();

    private final ApplicationArguments applicationArguments;
    private final OperationDistanceRepository operationDistanceRepository;
    private final EvamVehicleStateRepository evamVehicleStateRepository;
    private final EvamVehicleStatusRepository evamVehicleStatusRepository;
    private final EvamScenarioReplayService evamScenarioReplayService;

    private HttpServer server;

    public OperationDistanceGuiServer(ApplicationArguments applicationArguments,
            OperationDistanceRepository operationDistanceRepository,
            EvamVehicleStateRepository evamVehicleStateRepository,
            EvamVehicleStatusRepository evamVehicleStatusRepository,
            EvamScenarioReplayService evamScenarioReplayService) {
        this.applicationArguments = applicationArguments;
        this.operationDistanceRepository = operationDistanceRepository;
        this.evamVehicleStateRepository = evamVehicleStateRepository;
        this.evamVehicleStatusRepository = evamVehicleStatusRepository;
        this.evamScenarioReplayService = evamScenarioReplayService;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void onApplicationReady() {
        if (!applicationArguments.containsOption("gui")) {
            return;
        }

        try {
            startServer();
            URI guiUri = OperationDistanceMapLauncher.buildGuiUri(server.getAddress().getPort());
            log.info("Operation Distance GUI available at {}", guiUri);
            openBrowser(guiUri);
        } catch (IOException e) {
            log.error("Failed to start Operation Distance GUI server", e);
        }
    }

    private synchronized void startServer() throws IOException {
        if (server != null) {
            return;
        }

        server = HttpServer.create(new InetSocketAddress("127.0.0.1", GUI_PORT), 0);
        byte[] html = OperationDistanceMapLauncher.buildHtml();

        server.createContext("/", exchange -> redirect(exchange, "/operation-distance-map.html"));
        server.createContext("/log-analyzer", exchange -> redirect(exchange, "https://127.0.0.1:8443/log-analyzer"));
        server.createContext("/operation-distance-map.html", exchange -> respond(exchange, "text/html; charset=UTF-8", html));
        server.createContext("/api/operation-distance", exchange -> respond(
                exchange,
                "application/json; charset=UTF-8",
            OperationDistanceMapLauncher.buildPointsJson(
                filterOperationDistancesForCurrentVehicleState(
                    operationDistanceRepository.findAll(),
                    evamVehicleStateRepository.findById("1").orElse(null)),
                evamVehicleStatusRepository.findAll())));
        server.createContext("/api/operation-distance/clear", this::clearOperationDistance);
        server.createContext("/api/operation-scenarios", this::listOperationScenarios);
        server.createContext("/api/operation-scenarios/file", this::getOperationScenarioFile);
        server.createContext("/api/replay-event", this::replayEvent);
        server.createContext("/favicon.ico", exchange -> {
            exchange.sendResponseHeaders(204, -1);
            exchange.close();
        });
        server.setExecutor(null);
        server.start();
    }

    private void openBrowser(URI uri) {
        if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
            try {
                Desktop.getDesktop().browse(uri);
                return;
            } catch (IOException e) {
                log.warn("Unable to open browser automatically; open {} manually", uri, e);
            }
        }

        log.info("Open Operation Distance GUI manually at {}", uri);
    }

    private void redirect(HttpExchange exchange, String location) throws IOException {
        exchange.getResponseHeaders().set("Location", location);
        exchange.sendResponseHeaders(302, -1);
        exchange.close();
    }

    private void clearOperationDistance(HttpExchange exchange) throws IOException {
        if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) {
            respond(exchange, 405, "application/json; charset=UTF-8", GSON.toJson(Map.of("message", "Method not allowed")).getBytes(StandardCharsets.UTF_8));
            return;
        }

        int clearedCount = (int) operationDistanceRepository.count();
        operationDistanceRepository.deleteAll();
        respond(exchange,
                "application/json; charset=UTF-8",
                GSON.toJson(Map.of("clearedCount", clearedCount)).getBytes(StandardCharsets.UTF_8));
    }

    private void listOperationScenarios(HttpExchange exchange) throws IOException {
        if (!"GET".equalsIgnoreCase(exchange.getRequestMethod())) {
            respond(exchange, 405, "application/json; charset=UTF-8", GSON.toJson(Map.of("message", "Method not allowed")).getBytes(StandardCharsets.UTF_8));
            return;
        }

        List<OperationScenarioItem> items = new ArrayList<>();
        Path archiveRoot = Paths.get("data", "log-analysis");
        if (Files.exists(archiveRoot)) {
            try (var analysisDirectories = Files.list(archiveRoot)) {
                for (Path analysisDirectory : analysisDirectories.filter(Files::isDirectory).toList()) {
                    Path analysisFile = analysisDirectory.resolve("analysis.json");
                    Path operationsDirectory = analysisDirectory.resolve("operations");
                    if (!Files.exists(operationsDirectory)) {
                        continue;
                    }

                    String sourceLog = analysisDirectory.getFileName().toString();
                    String savedAt = null;
                    if (Files.exists(analysisFile)) {
                        try {
                            JsonObject analysisJson = GSON.fromJson(Files.readString(analysisFile), JsonObject.class);
                            if (analysisJson != null && analysisJson.has("summary") && analysisJson.get("summary").isJsonObject()) {
                                JsonObject summary = analysisJson.getAsJsonObject("summary");
                                if (summary.has("sourceLog") && !summary.get("sourceLog").isJsonNull()) {
                                    sourceLog = summary.get("sourceLog").getAsString();
                                }
                                if (summary.has("savedAt") && !summary.get("savedAt").isJsonNull()) {
                                    savedAt = summary.get("savedAt").getAsString();
                                }
                            }
                        } catch (RuntimeException exception) {
                            log.warn("Could not parse analysis metadata from {}", analysisFile, exception);
                        }
                    }

                    try (var operationFiles = Files.list(operationsDirectory)) {
                        for (Path operationFile : operationFiles
                                .filter(Files::isRegularFile)
                                .filter(path -> path.getFileName().toString().endsWith(".scenario.json"))
                                .toList()) {
                            items.add(new OperationScenarioItem(
                                    analysisDirectory.getFileName().toString(),
                                    operationFile.getFileName().toString(),
                                    sourceLog,
                                    savedAt,
                                    Files.size(operationFile)));
                        }
                    }
                }
            }
        }

        items.sort(Comparator
                .comparing(OperationScenarioItem::savedAt, Comparator.nullsLast(Comparator.reverseOrder()))
                .thenComparing(OperationScenarioItem::fileName));

        respond(exchange, "application/json; charset=UTF-8", GSON.toJson(items).getBytes(StandardCharsets.UTF_8));
    }

    private void getOperationScenarioFile(HttpExchange exchange) throws IOException {
        if (!"GET".equalsIgnoreCase(exchange.getRequestMethod())) {
            respond(exchange, 405, "application/json; charset=UTF-8", GSON.toJson(Map.of("message", "Method not allowed")).getBytes(StandardCharsets.UTF_8));
            return;
        }

        Map<String, String> query = parseQuery(exchange.getRequestURI().getRawQuery());
        String analysisId = query.get("analysisId");
        String fileName = query.get("fileName");
        if (analysisId == null || fileName == null || fileName.contains("/") || fileName.contains("\\")) {
            respond(exchange, 400, "application/json; charset=UTF-8", GSON.toJson(Map.of("message", "analysisId och fileName krävs.")).getBytes(StandardCharsets.UTF_8));
            return;
        }

        Path filePath = Paths.get("data", "log-analysis", analysisId, "operations", fileName);
        if (!Files.exists(filePath)) {
            respond(exchange, 404, "application/json; charset=UTF-8", GSON.toJson(Map.of("message", "Operationsfilen hittades inte.")).getBytes(StandardCharsets.UTF_8));
            return;
        }

        respond(exchange, "application/json; charset=UTF-8", Files.readAllBytes(filePath));
    }

    private void replayEvent(HttpExchange exchange) throws IOException {
        if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) {
            respond(exchange, 405, "application/json; charset=UTF-8", GSON.toJson(Map.of("message", "Method not allowed")).getBytes(StandardCharsets.UTF_8));
            return;
        }

        try {
            String body = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
            EvamLogScenarioEvent event = GSON.fromJson(body, EvamLogScenarioEvent.class);
            EvamScenarioReplayResult result = evamScenarioReplayService.replay(
                    EvamLogScenario.builder().events(List.of(event)).build());
            respond(exchange, "application/json; charset=UTF-8", GSON.toJson(result).getBytes(StandardCharsets.UTF_8));
        } catch (RuntimeException exception) {
            respond(exchange, 400, "application/json; charset=UTF-8", GSON.toJson(Map.of("message", exception.getMessage())).getBytes(StandardCharsets.UTF_8));
        }
    }

    private Map<String, String> parseQuery(String rawQuery) {
        Map<String, String> values = new LinkedHashMap<>();
        if (rawQuery == null || rawQuery.isBlank()) {
            return values;
        }
        for (String pair : rawQuery.split("&")) {
            String[] parts = pair.split("=", 2);
            String key = URLDecoder.decode(parts[0], StandardCharsets.UTF_8);
            String value = parts.length > 1 ? URLDecoder.decode(parts[1], StandardCharsets.UTF_8) : "";
            values.put(key, value);
        }
        return values;
    }

    private void respond(HttpExchange exchange, String contentType, byte[] body) throws IOException {
        respond(exchange, 200, contentType, body);
    }

    private void respond(HttpExchange exchange, int statusCode, String contentType, byte[] body) throws IOException {
        exchange.getResponseHeaders().set("Content-Type", contentType);
        exchange.getResponseHeaders().set("Cache-Control", "no-store, max-age=0");
        exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
        exchange.sendResponseHeaders(statusCode, body.length);
        exchange.getResponseBody().write(body);
        exchange.getResponseBody().flush();
        exchange.close();
    }

    static List<OperationDistance> filterOperationDistancesForCurrentVehicleState(
            List<OperationDistance> entries,
            VehicleState currentVehicleState) {
        if (entries == null || entries.isEmpty() || currentVehicleState == null) {
            return List.of();
        }

        String activeCaseFullId = currentVehicleState.getActiveCaseFullId();
        if (activeCaseFullId == null || activeCaseFullId.isBlank()) {
            return List.of();
        }

        return entries.stream()
                .filter(Objects::nonNull)
                .filter(entry -> activeCaseFullId.equals(entry.getOperationID()))
                .toList();
    }

    private record OperationScenarioItem(
            String analysisId,
            String fileName,
            String sourceLog,
            String savedAt,
            long sizeBytes) {
    }

    @Override
    public synchronized void destroy() {
        if (server != null) {
            server.stop(0);
            server = null;
        }
    }
}