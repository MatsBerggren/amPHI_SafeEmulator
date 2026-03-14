package com.dedalus.amphi_integration.gui;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.dedalus.amphi_integration.model.OperationDistance;
import com.dedalus.amphi_integration.model.evam.Location;

class OperationDistanceMapLauncherTest {

    @Test
    void toMarkersSortsPointsByTimestamp() {
        OperationDistance second = OperationDistance.builder()
                .timestamp(LocalDateTime.parse("2026-03-07T17:58:03"))
                .stateID("2")
                .distance(20.0)
                .assignmentDistance(20.0)
                .location(Location.builder().latitude(57.2).longitude(14.2).build())
                .build();
        OperationDistance first = OperationDistance.builder()
                .timestamp(LocalDateTime.parse("2026-03-07T17:57:17"))
                .stateID("1")
                .distance(0.0)
                .assignmentDistance(0.0)
                .location(Location.builder().latitude(57.1).longitude(14.1).build())
                .build();

        List<OperationDistanceMapLauncher.MapMarker> markers = OperationDistanceMapLauncher.toMarkers(List.of(second, first));

        assertThat(markers)
                .extracting(OperationDistanceMapLauncher.MapMarker::timestampLabel)
                .containsExactly(
                        "2026-03-07 17:57:17",
                        "2026-03-07 17:58:03");
    }

    @Test
    void buildHtmlIncludesPollingEndpoint() {
        String html = new String(OperationDistanceMapLauncher.buildHtml(), java.nio.charset.StandardCharsets.UTF_8);

        assertThat(html).contains("fetch('/api/operation-distance'");
        assertThat(html).contains("window.setInterval(refreshPoints, 2000)");
        assertThat(html).contains("Livevy uppdaterad från inkommande API-anrop");
                assertThat(html).contains("Positioner");
                assertThat(html).doesNotContain("L.circleMarker");
                assertThat(html).contains("state.stateChangeMarkers = stateChanges.map");
                assertThat(html).contains("href=\"/log-analyzer\"");
                assertThat(html).contains("id=\"scenarioSelect\"");
                assertThat(html).contains("Uppdatera fillista");
                assertThat(html).contains("id=\"scenarioFileInput\"");
                assertThat(html).contains("id=\"speedSelect\"");
                assertThat(html).contains("Play");
                assertThat(html).contains("Pausa");
                assertThat(html).contains("Stop");
                assertThat(html).contains("Rensa");
                assertThat(html).contains("/api/replay-event");
                assertThat(html).contains("/api/operation-scenarios");
                assertThat(html).contains("/api/operation-distance/clear");
    }

    @Test
    void buildGuiUriUsesLocalhostHttp() {
        assertThat(OperationDistanceMapLauncher.buildGuiUri(8765).toString())
                .isEqualTo("http://localhost:8765/operation-distance-map.html");
    }

    @Test
    void buildPointsJsonIncludesStateIdAndCoordinates() {
        OperationDistance entry = OperationDistance.builder()
                .timestamp(LocalDateTime.parse("2026-03-07T17:57:17"))
                .stateID("1")
                .distance(12.5)
                .assignmentDistance(42.0)
                .location(Location.builder()
                        .latitude(57.123456)
                        .longitude(14.654321)
                        .build())
                .build();

        String json = new String(OperationDistanceMapLauncher.buildPointsJson(List.of(entry)), java.nio.charset.StandardCharsets.UTF_8);

        assertThat(json).contains("\"stateId\":\"1\"");
        assertThat(json).contains("57.123456");
        assertThat(json).contains("14.654321");
    }
}