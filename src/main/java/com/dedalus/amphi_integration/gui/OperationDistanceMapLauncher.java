package com.dedalus.amphi_integration.gui;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

import com.dedalus.amphi_integration.model.OperationDistance;
import com.dedalus.amphi_integration.model.evam.VehicleStatus;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

public final class OperationDistanceMapLauncher {

    private static final String GUI_FLAG = "--gui";
    private static final Gson GSON = new GsonBuilder().disableHtmlEscaping().create();
    private static final DateTimeFormatter TIMESTAMP_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private OperationDistanceMapLauncher() {
    }

    public static boolean shouldLaunch(String[] args) {
        for (String arg : args) {
            if (GUI_FLAG.equals(arg)) {
                return true;
            }
        }
        return false;
    }

    static URI buildGuiUri(int port) {
        return URI.create("http://localhost:" + port + "/operation-distance-map.html");
    }

    static byte[] buildPointsJson(List<OperationDistance> entries) {
      return buildPointsJson(entries, List.of());
    }

    static byte[] buildPointsJson(List<OperationDistance> entries, List<VehicleStatus> statuses) {
      return GSON.toJson(toMarkers(entries, statusNameById(statuses))).getBytes(StandardCharsets.UTF_8);
    }

    static List<MapMarker> toMarkers(List<OperationDistance> entries) {
      return toMarkers(entries, Map.of());
    }

    static List<MapMarker> toMarkers(List<OperationDistance> entries, Map<String, String> statusNamesById) {
        List<OperationDistance> sortedEntries = entries.stream()
                .filter(Objects::nonNull)
                .filter(entry -> entry.getLocation() != null)
                .filter(entry -> entry.getLocation().getLatitude() != null && entry.getLocation().getLongitude() != null)
                .sorted(Comparator.comparing(OperationDistance::getTimestamp, Comparator.nullsLast(LocalDateTime::compareTo)))
                .toList();

        return java.util.stream.IntStream.range(0, sortedEntries.size())
        .mapToObj(index -> MapMarker.from(index + 1, sortedEntries.get(index), statusNamesById))
                .toList();
    }

  private static Map<String, String> statusNameById(List<VehicleStatus> statuses) {
    Map<String, String> names = new LinkedHashMap<>();
    for (VehicleStatus status : statuses) {
      if (status == null || status.getId() == null || status.getId().isBlank()) {
        continue;
      }
      names.put(status.getId(), status.getName());
    }
    return names;
  }

    static byte[] buildHtml() {
        return """
                <!DOCTYPE html>
                <html lang="sv">
                <head>
                  <meta charset="UTF-8">
                  <meta name="viewport" content="width=device-width, initial-scale=1.0">
                  <title>Operation Distance Map</title>
                  <link rel="preconnect" href="https://unpkg.com">
                  <link rel="stylesheet" href="https://unpkg.com/leaflet@1.9.4/dist/leaflet.css"
                        integrity="sha256-p4NxAoJBhIIN+hmNHrzRCf9tD/miZyoHS5obTRR9BMY=" crossorigin="">
                  <style>
                    :root {
                      --bg: #f3efe6;
                      --panel: rgba(253, 250, 244, 0.94);
                      --ink: #1f2a2e;
                      --muted: #5f6b6f;
                      --line: rgba(31, 42, 46, 0.14);
                      --accent: #a33a2b;
                      --accent-soft: #d97d61;
                    }
                    * { box-sizing: border-box; }
                    body {
                      margin: 0;
                      min-height: 100vh;
                      font-family: "Segoe UI", "Helvetica Neue", sans-serif;
                      color: var(--ink);
                      background:
                        radial-gradient(circle at top left, rgba(163,58,43,0.18), transparent 26%),
                        radial-gradient(circle at bottom right, rgba(217,125,97,0.16), transparent 30%),
                        var(--bg);
                    }
                    .layout {
                      display: grid;
                      grid-template-columns: minmax(280px, 360px) 1fr;
                      min-height: 100vh;
                      gap: 18px;
                      padding: 18px;
                    }
                    .panel {
                      background: var(--panel);
                      border: 1px solid var(--line);
                      border-radius: 24px;
                      backdrop-filter: blur(10px);
                      box-shadow: 0 18px 50px rgba(31, 42, 46, 0.08);
                    }
                    .sidebar {
                      display: flex;
                      flex-direction: column;
                      overflow: hidden;
                    }
                    .hero {
                      padding: 24px 24px 16px;
                      border-bottom: 1px solid var(--line);
                    }
                    .hero-actions {
                      margin-top: 14px;
                      display: flex;
                      gap: 10px;
                      flex-wrap: wrap;
                    }
                    .hero-link {
                      display: inline-flex;
                      align-items: center;
                      justify-content: center;
                      min-height: 42px;
                      padding: 10px 14px;
                      border-radius: 999px;
                      color: white;
                      background: var(--accent);
                      text-decoration: none;
                      font-weight: 700;
                    }
                    .hero-link.secondary {
                      background: rgba(31, 42, 46, 0.08);
                      color: var(--ink);
                    }
                    .eyebrow {
                      letter-spacing: 0.14em;
                      text-transform: uppercase;
                      color: var(--accent);
                      font-size: 12px;
                      font-weight: 700;
                    }
                    h1 {
                      margin: 10px 0 8px;
                      font-size: clamp(28px, 4vw, 42px);
                      line-height: 0.95;
                    }
                    .meta {
                      margin: 0;
                      color: var(--muted);
                      font-size: 14px;
                      line-height: 1.5;
                    }
                    .status {
                      margin-top: 8px;
                      color: var(--muted);
                      font-size: 13px;
                    }
                    .stats {
                      display: grid;
                      grid-template-columns: repeat(2, minmax(0, 1fr));
                      gap: 12px;
                      padding: 18px 24px;
                    }
                    .replay-panel {
                      margin: 0 24px 18px;
                      padding: 16px;
                      border: 1px solid var(--line);
                      border-radius: 18px;
                      background: rgba(255,255,255,0.65);
                    }
                    .replay-panel label {
                      display: block;
                      margin-bottom: 6px;
                      font-size: 13px;
                      font-weight: 700;
                    }
                    .replay-panel input,
                    .replay-panel select,
                    .replay-panel button {
                      width: 100%;
                      min-height: 42px;
                      border-radius: 12px;
                      border: 1px solid var(--line);
                      font: inherit;
                    }
                    .replay-panel input,
                    .replay-panel select {
                      padding: 10px 12px;
                      background: white;
                      color: var(--ink);
                    }
                    .replay-panel button {
                      border: 0;
                      background: var(--accent);
                      color: white;
                      font-weight: 700;
                      cursor: pointer;
                    }
                    .replay-panel button.secondary {
                      background: rgba(31, 42, 46, 0.74);
                    }
                    .replay-panel button.ghost {
                      background: rgba(255,255,255,0.9);
                      color: var(--ink);
                      border: 1px solid var(--line);
                    }
                    .replay-panel button.danger {
                      background: #7d2418;
                    }
                    .replay-panel button:disabled {
                      cursor: wait;
                      opacity: 0.7;
                    }
                    .replay-grid {
                      display: grid;
                      gap: 12px;
                    }
                    .replay-actions {
                      display: grid;
                      grid-template-columns: repeat(2, minmax(0, 1fr));
                      gap: 10px;
                    }
                    .replay-status {
                      margin-top: 10px;
                      color: var(--muted);
                      font-size: 13px;
                      line-height: 1.45;
                      white-space: pre-line;
                    }
                    .replay-status.error {
                      color: #7d2418;
                    }
                    .stat {
                      padding: 14px;
                      border: 1px solid var(--line);
                      border-radius: 18px;
                      background: rgba(255,255,255,0.55);
                    }
                    .stat-label {
                      color: var(--muted);
                      font-size: 12px;
                      text-transform: uppercase;
                      letter-spacing: 0.08em;
                    }
                    .stat-value {
                      margin-top: 8px;
                      font-size: 24px;
                      font-weight: 700;
                    }
                    .list {
                      padding: 0 16px 16px;
                      overflow: auto;
                    }
                    .entry {
                      padding: 14px 12px;
                      border-top: 1px solid var(--line);
                      border-radius: 18px;
                      transition: background 140ms ease, box-shadow 140ms ease;
                    }
                    .entry:first-child {
                      border-top: 0;
                    }
                    .entry.active {
                      background: rgba(163, 58, 43, 0.08);
                      box-shadow: inset 0 0 0 1px rgba(163, 58, 43, 0.18);
                    }
                    .entry-index {
                      display: inline-flex;
                      align-items: center;
                      justify-content: center;
                      width: 28px;
                      height: 28px;
                      border-radius: 50%;
                      background: var(--accent);
                      color: white;
                      font-size: 13px;
                      font-weight: 700;
                    }
                    .entry-time {
                      margin: 10px 0 4px;
                    }
                    .entry-link {
                      padding: 0;
                      border: 0;
                      background: none;
                      color: var(--accent);
                      font: inherit;
                      font-weight: 700;
                      text-align: left;
                      cursor: pointer;
                      text-decoration: underline;
                      text-underline-offset: 2px;
                    }
                    .entry-link:hover,
                    .entry-link:focus-visible {
                      color: #7d2418;
                      outline: none;
                    }
                    .entry-title {
                      margin: 10px 0 4px;
                      font-weight: 700;
                    }
                    .entry-copy {
                      margin: 0;
                      color: var(--muted);
                      font-size: 14px;
                      line-height: 1.45;
                    }
                    .map-shell {
                      position: relative;
                      overflow: hidden;
                      min-height: 70vh;
                    }
                    #map {
                      position: absolute;
                      inset: 0;
                    }
                    .empty {
                      display: none;
                      place-items: center;
                      height: 100%;
                      padding: 24px;
                      text-align: center;
                      color: var(--muted);
                      font-size: 16px;
                    }
                    .empty.visible {
                      display: grid;
                    }
                    @media (max-width: 900px) {
                      .layout {
                        grid-template-columns: 1fr;
                      }
                      .map-shell {
                        min-height: 55vh;
                      }
                    }
                  </style>
                </head>
                <body>
                  <div class="layout">
                    <aside class="panel sidebar">
                      <div class="hero">
                        <div class="eyebrow">amPHI SafeEmulator</div>
                        <h1>Operation Distance</h1>
                        <p class="meta">Livevy uppdaterad från inkommande API-anrop.</p>
                        <p class="status" id="connectionStatus">Väntar på data...</p>
                        <div class="hero-actions">
                          <a class="hero-link" href="/log-analyzer" target="_blank" rel="noreferrer">Öppna Log Analyzer</a>
                        </div>
                      </div>
                      <div class="stats">
                        <div class="stat">
                          <div class="stat-label">Positioner</div>
                          <div class="stat-value" id="pointCount">0</div>
                        </div>
                        <div class="stat">
                          <div class="stat-label">Sista tillstånd</div>
                          <div class="stat-value" id="lastState">-</div>
                        </div>
                      </div>
                      <section class="replay-panel">
                        <div class="eyebrow">Replay</div>
                        <div class="replay-grid">
                          <div>
                            <label for="scenarioSelect">Sparade operationsfiler</label>
                            <select id="scenarioSelect">
                              <option value="">Laddar lista...</option>
                            </select>
                          </div>
                          <button id="refreshScenarioListButton" class="ghost" type="button">Uppdatera fillista</button>
                          <div>
                            <label for="scenarioFileInput">Eller välj lokal operationsfil</label>
                            <input id="scenarioFileInput" type="file" accept=".json,.scenario.json">
                          </div>
                          <div>
                            <label for="speedSelect">Hastighet</label>
                            <select id="speedSelect">
                              <option value="1">x1</option>
                              <option value="10">x10</option>
                              <option value="100">x100</option>
                              <option value="1000">x1000</option>
                            </select>
                          </div>
                          <div class="replay-actions">
                            <button id="playReplayButton" type="button">Play</button>
                            <button id="pauseReplayButton" class="secondary" type="button">Pausa</button>
                            <button id="stopReplayButton" class="danger" type="button">Stop</button>
                            <button id="clearReplayButton" class="ghost" type="button">Rensa</button>
                          </div>
                        </div>
                        <div id="replayStatus" class="replay-status">Välj en exporterad operationsfil för att starta replay.</div>
                      </section>
                      <div class="list" id="entryList"></div>
                    </aside>
                    <main class="panel map-shell">
                      <div id="map"></div>
                      <div id="emptyState" class="empty">Inga koordinater har kommit in via API ännu.</div>
                    </main>
                  </div>
                  <script src="https://unpkg.com/leaflet@1.9.4/dist/leaflet.js"
                          integrity="sha256-20nQCchB9co0qIjJZRGuk2/Z9VM+kNiyxNV1lvTlZBo=" crossorigin=""></script>
                  <script>
                    const pointCount = document.getElementById('pointCount');
                    const lastState = document.getElementById('lastState');
                    const entryList = document.getElementById('entryList');
                    const emptyState = document.getElementById('emptyState');
                    const connectionStatus = document.getElementById('connectionStatus');
                    const mapElement = document.getElementById('map');
                    const scenarioSelect = document.getElementById('scenarioSelect');
                    const refreshScenarioListButton = document.getElementById('refreshScenarioListButton');
                    const scenarioFileInput = document.getElementById('scenarioFileInput');
                    const speedSelect = document.getElementById('speedSelect');
                    const playReplayButton = document.getElementById('playReplayButton');
                    const pauseReplayButton = document.getElementById('pauseReplayButton');
                    const stopReplayButton = document.getElementById('stopReplayButton');
                    const clearReplayButton = document.getElementById('clearReplayButton');
                    const replayStatus = document.getElementById('replayStatus');

                    const state = {
                      map: null,
                      line: null,
                      stateChangeMarkers: [],
                      stateChangeMarkerMap: new Map(),
                      selectedPointIndex: null,
                      signature: '',
                      replayList: [],
                      replayScenario: null,
                      replayFileName: null,
                      replayInProgress: false,
                      replayPaused: false,
                      replayStopRequested: false
                    };

                    function ensureMap() {
                      if (state.map) {
                        return state.map;
                      }
                      const map = L.map('map', { zoomControl: false });
                      L.control.zoom({ position: 'bottomright' }).addTo(map);
                      L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', {
                        maxZoom: 19,
                        attribution: '&copy; OpenStreetMap contributors'
                      }).addTo(map);
                      state.map = map;
                      return map;
                    }

                    function setConnectionStatus(text) {
                      connectionStatus.textContent = text;
                    }

                    function setReplayStatus(text, isError = false) {
                      replayStatus.textContent = text;
                      replayStatus.classList.toggle('error', isError);
                    }

                    function escapeHtml(value) {
                      return String(value ?? '')
                        .replaceAll('&', '&amp;')
                        .replaceAll('<', '&lt;')
                        .replaceAll('>', '&gt;')
                        .replaceAll('"', '&quot;');
                    }

                    function setReplayButtons() {
                      playReplayButton.disabled = state.replayInProgress;
                      pauseReplayButton.disabled = !state.replayInProgress;
                      stopReplayButton.disabled = !state.replayInProgress;
                      pauseReplayButton.textContent = state.replayPaused ? 'Fortsätt' : 'Pausa';
                    }

                    function normalizeReplayEvents(scenario) {
                      if (!scenario || !Array.isArray(scenario.events)) {
                        return [];
                      }
                      return scenario.events.filter((event) => event && typeof event.endpoint === 'string');
                    }

                    function parseReplayTimestamp(timestamp) {
                      if (!timestamp) {
                        return null;
                      }
                      const millis = Date.parse(timestamp);
                      return Number.isNaN(millis) ? null : millis;
                    }

                    function delay(milliseconds) {
                      if (!milliseconds || milliseconds <= 0) {
                        return Promise.resolve();
                      }
                      return new Promise((resolve) => window.setTimeout(resolve, milliseconds));
                    }

                    async function waitWhilePaused() {
                      while (state.replayPaused && !state.replayStopRequested) {
                        await delay(100);
                      }
                    }

                    async function refreshScenarioList() {
                      try {
                        const response = await fetch('/api/operation-scenarios', { cache: 'no-store' });
                        const items = await response.json();
                        state.replayList = Array.isArray(items) ? items : [];
                        if (!state.replayList.length) {
                          scenarioSelect.innerHTML = '<option value="">Inga exporterade operationsfiler hittades</option>';
                          return;
                        }

                        scenarioSelect.innerHTML = '<option value="">Välj sparad operationsfil...</option>' + state.replayList.map((item, index) => {
                          const label = `${item.fileName} • ${item.sourceLog || item.analysisId}`;
                          return `<option value="${index}">${label}</option>`;
                        }).join('');
                      } catch (error) {
                        scenarioSelect.innerHTML = '<option value="">Kunde inte läsa fillistan</option>';
                        setReplayStatus(`Kunde inte läsa operationsfiler: ${error.message}`, true);
                      }
                    }

                    async function loadReplayScenarioFromArchive(indexValue) {
                      if (indexValue === '') {
                        return;
                      }
                      const item = state.replayList[Number(indexValue)];
                      if (!item) {
                        return;
                      }

                      try {
                        const response = await fetch(`/api/operation-scenarios/file?analysisId=${encodeURIComponent(item.analysisId)}&fileName=${encodeURIComponent(item.fileName)}`);
                        if (!response.ok) {
                          throw new Error(`HTTP ${response.status}`);
                        }
                        const scenario = await response.json();
                        const events = normalizeReplayEvents(scenario);
                        if (!events.length) {
                          throw new Error('Filen innehåller inga replaybara events.');
                        }
                        state.replayScenario = scenario;
                        state.replayFileName = item.fileName;
                        scenarioFileInput.value = '';
                        setReplayStatus(`${item.fileName} laddad från arkivet. ${events.length} events redo för replay.`);
                      } catch (error) {
                        state.replayScenario = null;
                        state.replayFileName = null;
                        setReplayStatus(`Kunde inte läsa sparad operationsfil: ${error.message}`, true);
                      }
                    }

                    async function loadReplayScenario(file) {
                      if (!file) {
                        state.replayScenario = null;
                        state.replayFileName = null;
                        setReplayStatus('Välj en exporterad operationsfil för att starta replay.');
                        return;
                      }

                      try {
                        const text = await file.text();
                        const scenario = JSON.parse(text);
                        const events = normalizeReplayEvents(scenario);
                        if (!events.length) {
                          throw new Error('Filen innehåller inga replaybara events.');
                        }
                        state.replayScenario = scenario;
                        state.replayFileName = file.name;
                        scenarioSelect.value = '';
                        setReplayStatus(`${file.name} laddad. ${events.length} events redo för replay.`);
                      } catch (error) {
                        state.replayScenario = null;
                        state.replayFileName = null;
                        setReplayStatus(`Kunde inte läsa filen: ${error.message}`, true);
                      }
                    }

                    async function replayScenario() {
                      if (state.replayInProgress) {
                        return;
                      }
                      if (!state.replayScenario) {
                        setReplayStatus('Välj en operationsfil först.', true);
                        return;
                      }

                      const speedMultiplier = Number(speedSelect.value || '1');
                      const events = normalizeReplayEvents(state.replayScenario);
                      let processed = 0;
                      let skipped = 0;
                      let failed = 0;
                      let previousTimestamp = null;

                      state.replayInProgress = true;
                      state.replayPaused = false;
                      state.replayStopRequested = false;
                      setReplayButtons();
                      playReplayButton.disabled = true;
                      setReplayStatus(`Replay startad för ${state.replayFileName} i x${speedMultiplier}.`);

                      try {
                        for (let index = 0; index < events.length; index += 1) {
                          if (state.replayStopRequested) {
                            break;
                          }
                          await waitWhilePaused();
                          if (state.replayStopRequested) {
                            break;
                          }

                          const event = events[index];
                          const currentTimestamp = parseReplayTimestamp(event.requestTimestamp);
                          if (previousTimestamp != null && currentTimestamp != null) {
                            const waitMilliseconds = Math.max(0, currentTimestamp - previousTimestamp) / speedMultiplier;
                            await delay(waitMilliseconds);
                            await waitWhilePaused();
                          }
                          previousTimestamp = currentTimestamp == null ? previousTimestamp : currentTimestamp;

                          if (!event.payloadJson || !event.endpoint) {
                            skipped += 1;
                            setReplayStatus(`Replay ${index + 1}/${events.length}. Skippad event utan payload eller endpoint.`);
                            continue;
                          }

                          setReplayStatus(`Replay ${index + 1}/${events.length}: ${event.endpoint}`);
                          try {
                            const response = await fetch('/api/replay-event', {
                              method: 'POST',
                              headers: {
                                'Content-Type': 'application/json; charset=utf-8'
                              },
                              body: JSON.stringify(event)
                            });
                            const payload = await response.json();
                            if (!response.ok) {
                              failed += 1;
                              setReplayStatus(`Replay ${index + 1}/${events.length}: ${event.endpoint} misslyckades med HTTP ${response.status}.`, true);
                              continue;
                            }
                            if (payload.failedCount > 0) {
                              failed += payload.failedCount;
                              setReplayStatus(`Replay ${index + 1}/${events.length}: ${event.endpoint} gav fel.`, true);
                              continue;
                            }
                            if (payload.skippedCount > 0) {
                              skipped += payload.skippedCount;
                              continue;
                            }
                            processed += 1;
                          } catch (error) {
                            failed += 1;
                            setReplayStatus(`Replay ${index + 1}/${events.length} stoppade på ${event.endpoint}: ${error.message}`, true);
                          }
                        }

                        if (state.replayStopRequested) {
                          setReplayStatus(`Replay stoppad för ${state.replayFileName}. Processerade ${processed}, skippade ${skipped}, fel ${failed}.`, true);
                        } else {
                          setReplayStatus(`Replay klar för ${state.replayFileName}. Processerade ${processed}, skippade ${skipped}, fel ${failed}.`, failed > 0);
                        }
                      } finally {
                        state.replayInProgress = false;
                        state.replayPaused = false;
                        state.replayStopRequested = false;
                        setReplayButtons();
                      }
                    }

                    function togglePauseReplay() {
                      if (!state.replayInProgress) {
                        return;
                      }
                      state.replayPaused = !state.replayPaused;
                      setReplayButtons();
                      setReplayStatus(state.replayPaused ? 'Replay pausad.' : 'Replay återupptagen.');
                    }

                    function stopReplay() {
                      if (!state.replayInProgress) {
                        return;
                      }
                      state.replayStopRequested = true;
                      state.replayPaused = false;
                      setReplayButtons();
                    }

                    async function clearReplayState() {
                      try {
                        const response = await fetch('/api/operation-distance/clear', { method: 'POST' });
                        if (!response.ok) {
                          throw new Error(`HTTP ${response.status}`);
                        }
                        const payload = await response.json();
                        state.signature = '';
                        clearMap();
                        entryList.innerHTML = '';
                        pointCount.textContent = '0';
                        lastState.textContent = '-';
                        mapElement.style.display = 'none';
                        emptyState.classList.add('visible');
                        setReplayStatus(`Rensat. ${payload.clearedCount} OperationDistance-händelser togs bort.`);
                        setConnectionStatus('Kartan är rensad.');
                      } catch (error) {
                        setReplayStatus(`Kunde inte rensa kartan: ${error.message}`, true);
                      }
                    }

                    function clearMap() {
                      if (!state.map) {
                        return;
                      }
                      if (state.line) {
                        state.map.removeLayer(state.line);
                        state.line = null;
                      }
                      state.stateChangeMarkers.forEach((marker) => state.map.removeLayer(marker));
                      state.stateChangeMarkers = [];
                      state.stateChangeMarkerMap = new Map();
                    }

                    function buildMarkerIcon(point, isActive) {
                      return L.divIcon({
                        className: 'state-change-flag',
                        html: buildFlagMarkup(point, isActive),
                        iconSize: isActive ? [24, 24] : [20, 20],
                        iconAnchor: isActive ? [8, 20] : [6, 18]
                      });
                    }

                    function updateMarkerSelection() {
                      state.stateChangeMarkers.forEach((marker) => {
                        const point = marker.pointData;
                        const isActive = point?.index === state.selectedPointIndex;
                        marker.setIcon(buildMarkerIcon(point, isActive));
                      });
                    }

                    function updateListSelection() {
                      const entries = entryList.querySelectorAll('.entry');
                      entries.forEach((entry) => {
                        const isActive = Number(entry.dataset.pointIndex) === state.selectedPointIndex;
                        entry.classList.toggle('active', isActive);
                      });
                    }

                    function selectPoint(pointIndex, options = {}) {
                      state.selectedPointIndex = pointIndex;
                      updateListSelection();
                      updateMarkerSelection();

                      const marker = state.stateChangeMarkerMap.get(pointIndex);
                      if (!marker || !state.map) {
                        return;
                      }

                      if (options.scrollList !== false) {
                        const entry = entryList.querySelector(`.entry[data-point-index="${pointIndex}"]`);
                        entry?.scrollIntoView({ block: 'nearest', behavior: options.instant ? 'auto' : 'smooth' });
                      }

                      state.map.panTo(marker.getLatLng(), { animate: !options.instant });
                      marker.openPopup();
                    }

                    function findStateChanges(points) {
                      const changes = [];
                      let previousStateId = null;
                      points.forEach((point, index) => {
                        if (!point.stateId || point.stateId === '-') {
                          return;
                        }
                        if (index === 0 || point.stateId !== previousStateId) {
                          changes.push(point);
                        }
                        previousStateId = point.stateId;
                      });
                      return changes;
                    }

                    function render(points) {
                      pointCount.textContent = String(points.length);
                      lastState.textContent = points.length ? points[points.length - 1].stateId : '-';
                      entryList.innerHTML = '';

                      const stateChanges = findStateChanges(points);

                      stateChanges.forEach((point) => {
                        const item = document.createElement('article');
                        item.className = 'entry';
                        item.dataset.pointIndex = String(point.index);
                        item.innerHTML = `
                          <div class="entry-index">${point.index}</div>
                          <div class="entry-time"><button class="entry-link" type="button" data-point-index="${point.index}">${point.timestampLabel}</button></div>
                          <p class="entry-copy">Lat ${point.latitude.toFixed(6)}, Lon ${point.longitude.toFixed(6)}</p>
                          <p class="entry-copy">${point.stateName || ('State ' + point.stateId)} • Assignment ${point.assignmentDistanceLabel} m</p>
                        `;
                        item.querySelector('.entry-link')?.addEventListener('click', () => selectPoint(point.index));
                        entryList.appendChild(item);
                      });

                      if (!points.length) {
                        mapElement.style.display = 'none';
                        emptyState.classList.add('visible');
                        clearMap();
                        return;
                      }

                      mapElement.style.display = 'block';
                      emptyState.classList.remove('visible');
                      const map = ensureMap();
                      clearMap();
                      const availablePointIndexes = new Set(stateChanges.map((point) => point.index));
                      if (!availablePointIndexes.has(state.selectedPointIndex)) {
                        state.selectedPointIndex = stateChanges.length ? stateChanges[stateChanges.length - 1].index : null;
                      }

                      const latLngs = points.map((point) => [point.latitude, point.longitude]);
                      state.line = L.polyline(latLngs, {
                        color: '#a33a2b',
                        weight: 4,
                        opacity: 0.75
                      }).addTo(map);

                      state.stateChangeMarkers = stateChanges.map((point) => {
                        const marker = L.marker([point.latitude, point.longitude], {
                          icon: buildMarkerIcon(point, point.index === state.selectedPointIndex)
                        }).bindPopup(`
                        <strong>Statusbyte</strong><br>
                        Tid: ${point.timestampLabel}<br>
                        Status: ${point.stateName || ('State ' + point.stateId)}<br>
                        Assignment: ${point.assignmentDistanceLabel} m
                      `).addTo(map);
                        marker.pointData = point;
                        marker.on('click', () => selectPoint(point.index, { scrollList: true }));
                        state.stateChangeMarkerMap.set(point.index, marker);
                        return marker;
                      });

                      map.fitBounds(state.line.getBounds(), { padding: [32, 32] });
                      updateListSelection();
                      updateMarkerSelection();
                    }

                    function statusFlagColor(point) {
                      const stateName = (point.stateName || '').toLowerCase();
                      if (stateName.includes('kvittera')) {
                        return '#2364aa';
                      }
                      if (stateName.includes('hämtplats')) {
                        return '#f4a259';
                      }
                      if (stateName.includes('dest')) {
                        return '#d1495b';
                      }
                      if (stateName.includes('klar')) {
                        return '#2a9d8f';
                      }
                      return '#7d2418';
                    }

                    function buildFlagMarkup(point, isActive) {
                      const color = statusFlagColor(point);
                      const title = escapeHtml(point.stateName || ('State ' + point.stateId));
                      const ring = isActive ? 'box-shadow:0 0 0 3px rgba(163,58,43,0.24);border-radius:999px;' : '';
                      return `
                        <div title="${title}" style="position:relative;width:${isActive ? 22 : 18}px;height:${isActive ? 22 : 18}px;filter:drop-shadow(0 2px 2px rgba(0,0,0,0.18));${ring}">
                          <span style="position:absolute;left:3px;top:1px;width:2px;height:15px;background:#4e4e4e;border-radius:2px;"></span>
                          <span style="position:absolute;left:5px;top:1px;width:11px;height:8px;background:${color};clip-path:polygon(0 0,100% 18%,72% 100%,0 78%);border-radius:1px;"></span>
                        </div>`;
                    }

                    async function refreshPoints() {
                      try {
                        const response = await fetch('/api/operation-distance', { cache: 'no-store' });
                        if (!response.ok) {
                          throw new Error(`HTTP ${response.status}`);
                        }
                        const points = await response.json();
                        const signature = JSON.stringify(points.map((point) => [point.index, point.timestampLabel, point.latitude, point.longitude, point.stateId]));
                        if (signature !== state.signature) {
                          render(points);
                          state.signature = signature;
                        }
                        setConnectionStatus(`Senast uppdaterad ${new Date().toLocaleTimeString('sv-SE')}`);
                      } catch (error) {
                        setConnectionStatus(`Kunde inte hämta live-data: ${error.message}`);
                      }
                    }

                    refreshPoints();
                    window.setInterval(refreshPoints, 2000);
                    setReplayButtons();
                    refreshScenarioList();
                    refreshScenarioListButton.addEventListener('click', refreshScenarioList);
                    scenarioSelect.addEventListener('change', (event) => loadReplayScenarioFromArchive(event.target.value));
                    scenarioFileInput.addEventListener('change', (event) => loadReplayScenario(event.target.files?.[0]));
                    playReplayButton.addEventListener('click', replayScenario);
                    pauseReplayButton.addEventListener('click', togglePauseReplay);
                    stopReplayButton.addEventListener('click', stopReplay);
                    clearReplayButton.addEventListener('click', clearReplayState);
                  </script>
                </body>
                </html>
                """.getBytes(StandardCharsets.UTF_8);
    }

    record MapMarker(
            int index,
            double latitude,
            double longitude,
            String timestampLabel,
            String stateId,
        String stateName,
            String distanceLabel,
            String assignmentDistanceLabel) {

      private static MapMarker from(int index, OperationDistance entry, Map<String, String> statusNamesById) {
            return new MapMarker(
                    index,
                    entry.getLocation().getLatitude(),
                    entry.getLocation().getLongitude(),
                    formatTimestamp(entry.getTimestamp()),
                    entry.getStateID() == null ? "-" : entry.getStateID(),
            formatStateName(entry.getStateID(), statusNamesById),
                    formatMeters(entry.getDistance()),
                    formatMeters(entry.getAssignmentDistance()));
        }

        private static String formatTimestamp(LocalDateTime timestamp) {
            return timestamp == null ? "Okänd tid" : timestamp.format(TIMESTAMP_FORMATTER);
        }

        private static String formatMeters(Double value) {
            return value == null ? "-" : String.format(Locale.ROOT, "%.1f", value);
        }

        private static String formatStateName(String stateId, Map<String, String> statusNamesById) {
          if (stateId == null) {
            return "-";
          }
          return statusNamesById.getOrDefault(stateId, "State " + stateId);
        }
    }
}