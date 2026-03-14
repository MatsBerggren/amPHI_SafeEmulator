package com.dedalus.amphi_integration.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import com.dedalus.amphi_integration.AppConfig;
import com.google.gson.Gson;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;

class EvamLogScenarioExtractorTest {

    private final Gson gson = new AppConfig().gson();

    @Test
    void extract_MixedEvamTraffic_ProducesScenarioEvents() {
        List<String> lines = List.of(
                "2026-03-06T16:58:53.280+01:00 DEBUG 8252 --- [nio-8443-exec-4] o.s.w.f.CommonsRequestLoggingFilter      : Before request [POST /api/vehiclestate, client=192.168.50.74]",
                "Method EvamController.createNew(..) has returned VehicleState(id=1, timestamp=2026-03-06T15:59:39.241, vehicleStatus=VehicleStatus(id=null, name=Avf Hamtplats, event=EVENT_EXIT_SITE, successorName=Ank Dest, isStartStatus=false, isEndStatus=false, categoryType=STATUS_MISSION, categoryName=mission), activeCaseFullId=18:17869359:2, vehicleLocation=Location(latitude=59.20189240674485, longitude=17.640825396998476, timestamp=1772812779234))",
                "2026-03-06T16:58:53.280+01:00 DEBUG 8252 --- [nio-8443-exec-5] o.s.w.f.CommonsRequestLoggingFilter      : Before request [POST /api/triplocationhistory, client=192.168.50.74]",
                "Method EvamController.createNew(..) has returned TripLocationHistory(id=1, locationHistory=[Location(latitude=59.20189240674485, longitude=17.640825396998476, timestamp=2026-03-06T15:59:39.234Z), Location(latitude=59.20189240674485, longitude=17.640825396998476, timestamp=2026-03-06T15:59:38.227Z)], etaSeconds=267)",
                "2026-03-06T17:38:31.794+01:00 DEBUG 8252 --- [nio-8443-exec-5] o.s.w.f.CommonsRequestLoggingFilter      : Before request [POST /api/rakelstate, client=192.168.50.74]",
                "Method EvamController.createNew(..) has returned RakelState(id=1, unitId=null, msisdn=3306540, issi=306540, gssi=09338491)",
                "2026-03-06T17:38:31.799+01:00 DEBUG 8252 --- [nio-8443-exec-1] o.s.w.f.CommonsRequestLoggingFilter      : Before request [POST /api/vehiclestatus, client=192.168.50.74]",
                "Method EvamController.createNew(..) has returned [Lcom.dedalus.amphi_integration.model.evam.VehicleStatus;@55077db1",
                "2026-03-06T17:38:31.799+01:00 DEBUG 8252 --- [nio-8443-exec-6] o.s.w.f.CommonsRequestLoggingFilter      : Before request [POST /api/operations, client=192.168.50.74]",
                "2026-03-06T17:38:31.800+01:00 DEBUG 8252 --- [nio-8443-exec-6] c.d.a.controller.EvamController          : POST /operations: {\"operationID\":\"2\",\"callCenterId\":\"18\"}"
        );

        EvamLogScenarioExtractor extractor = new EvamLogScenarioExtractor(gson);
        EvamLogScenario scenario = extractor.extract(Path.of("scenario.log"), lines);

        assertEquals(5, scenario.getEvents().size());
        assertEquals(1, scenario.getEndpointCounts().get("/api/vehiclestate"));
        assertEquals(1, scenario.getEndpointCounts().get("/api/triplocationhistory"));
        assertEquals(1, scenario.getEndpointCounts().get("/api/rakelstate"));
        assertEquals(1, scenario.getEndpointCounts().get("/api/vehiclestatus"));
        assertEquals(1, scenario.getEndpointCounts().get("/api/operations"));

        EvamLogScenarioEvent vehicleState = scenario.getEvents().get(0);
        assertEquals(EvamLogExtractionQuality.RECONSTRUCTED_RETURN, vehicleState.getExtractionQuality());
        assertNotNull(gson.fromJson(vehicleState.getPayloadJson(), java.util.Map.class));

        EvamLogScenarioEvent tripLocationHistory = scenario.getEvents().get(1);
        assertEquals(EvamLogExtractionQuality.RECONSTRUCTED_RETURN, tripLocationHistory.getExtractionQuality());
        assertNotNull(gson.fromJson(tripLocationHistory.getPayloadJson(), java.util.Map.class));

        EvamLogScenarioEvent rakelState = scenario.getEvents().get(2);
        assertEquals(EvamLogExtractionQuality.RECONSTRUCTED_RETURN, rakelState.getExtractionQuality());
        assertNotNull(gson.fromJson(rakelState.getPayloadJson(), java.util.Map.class));

        EvamLogScenarioEvent vehicleStatus = scenario.getEvents().get(3);
        assertEquals(EvamLogExtractionQuality.OBSERVED_ONLY, vehicleStatus.getExtractionQuality());

        EvamLogScenarioEvent operation = scenario.getEvents().get(4);
        assertEquals(EvamLogExtractionQuality.RAW_REQUEST, operation.getExtractionQuality());
        assertEquals("{\"operationID\":\"2\",\"callCenterId\":\"18\"}", operation.getPayloadJson());
    }

    @Test
    void extract_UsesRawOperationDtoPayloadWhenPresentInLog() {
        List<String> lines = List.of(
                "2026-03-06T17:38:31.799+01:00 DEBUG 8252 --- [nio-8443-exec-6] o.s.w.f.CommonsRequestLoggingFilter      : Before request [POST /api/operations, client=192.168.50.74]",
                "EvamOperationRequestDTO(operation={\"operationID\":\"2\",\"name\":\"Sjuktransporter\",\"callCenterId\":\"18\",\"caseFolderId\":\"17867086\",\"patientName\":\"Sonja Larsson Eriksson\"})",
                "Method EvamController.createNew(..) has returned Operation(id=1, operationID=2, name=Sjuktransporter, callCenterId=18, caseFolderId=17867086)"
        );

        EvamLogScenarioExtractor extractor = new EvamLogScenarioExtractor(gson);
        EvamLogScenario scenario = extractor.extract(Path.of("scenario.log"), lines);

        EvamLogScenarioEvent operation = scenario.getEvents().stream()
            .filter(event -> "/api/operations".equals(event.getEndpoint()))
            .findFirst()
            .orElseThrow();

        assertEquals(EvamLogExtractionQuality.RAW_REQUEST, operation.getExtractionQuality());
        java.util.Map<?, ?> payload = gson.fromJson(operation.getPayloadJson(), java.util.Map.class);
        assertEquals("2", payload.get("operationID"));
        assertEquals("Sjuktransporter", payload.get("name"));
        assertEquals("17867086", payload.get("caseFolderId"));
    }

    @Test
    void extract_InfersOperationPayloadFromNearbyVehicleState() {
        List<String> lines = List.of(
                "2026-03-06T17:38:31.799+01:00 DEBUG 8252 --- [nio-8443-exec-6] o.s.w.f.CommonsRequestLoggingFilter      : Before request [POST /api/operations, client=192.168.50.74]",
                "Method EvamController.createNew(..) has returned null",
                "2026-03-06T17:38:31.810+01:00 DEBUG 8252 --- [nio-8443-exec-9] o.s.w.f.CommonsRequestLoggingFilter      : Before request [POST /api/vehiclestate, client=192.168.50.74]",
                "Method EvamController.createNew(..) has returned VehicleState(id=1, timestamp=2026-03-06T16:38:31.241, vehicleStatus=VehicleStatus(id=null, name=Avf Hamtplats, event=EVENT_EXIT_SITE, successorName=Ank Dest, isStartStatus=false, isEndStatus=false, categoryType=STATUS_MISSION, categoryName=mission), activeCaseFullId=18:17869359:2, vehicleLocation=Location(latitude=59.20189240674485, longitude=17.640825396998476, timestamp=1772812779234))",
                "2026-03-06T17:38:31.820+01:00 DEBUG 8252 --- [nio-8443-exec-8] o.s.w.f.CommonsRequestLoggingFilter      : Before request [POST /api/operationlist, client=192.168.50.74]",
                "Method EvamController.createNew(..) has returned null"
        );

        EvamLogScenarioExtractor extractor = new EvamLogScenarioExtractor(gson);
        EvamLogScenario scenario = extractor.extract(Path.of("scenario.log"), lines);

        EvamLogScenarioEvent operation = scenario.getEvents().stream()
            .filter(event -> "/api/operations".equals(event.getEndpoint()))
            .findFirst()
            .orElseThrow();
        assertEquals(EvamLogExtractionQuality.INFERRED_CONTEXT, operation.getExtractionQuality());
        java.util.Map<?, ?> operationPayload = gson.fromJson(operation.getPayloadJson(), java.util.Map.class);
        assertEquals("18", operationPayload.get("callCenterId"));
        assertEquals("17869359", operationPayload.get("caseFolderId"));
        assertEquals("2", operationPayload.get("operationID"));

        EvamLogScenarioEvent operationList = scenario.getEvents().stream()
            .filter(event -> "/api/operationlist".equals(event.getEndpoint()))
            .findFirst()
            .orElseThrow();
        assertEquals(EvamLogExtractionQuality.INFERRED_CONTEXT, operationList.getExtractionQuality());
        java.util.Map<?, ?> operationListPayload = gson.fromJson(operationList.getPayloadJson(), java.util.Map.class);
        assertNotNull(operationListPayload.get("operationList"));
    }

    @Test
    void extract_InfersPendingOperationPayloadUsingTimestampProximity() {
        List<String> lines = List.of(
                "2026-03-06T17:38:31.799+01:00 DEBUG 8252 --- [nio-8443-exec-6] o.s.w.f.CommonsRequestLoggingFilter      : Before request [POST /api/operations, client=192.168.50.74]",
                "2026-03-06T17:38:31.810+01:00 DEBUG 8252 --- [nio-8443-exec-9] o.s.w.f.CommonsRequestLoggingFilter      : Before request [POST /api/vehiclestate, client=192.168.50.74]",
                "Method EvamController.createNew(..) has returned VehicleState(id=1, timestamp=2026-03-06T16:38:31.241, vehicleStatus=VehicleStatus(id=null, name=Avf Hamtplats, event=EVENT_EXIT_SITE, successorName=Ank Dest, isStartStatus=false, isEndStatus=false, categoryType=STATUS_MISSION, categoryName=mission), activeCaseFullId=18:17869359:2, vehicleLocation=Location(latitude=59.20189240674485, longitude=17.640825396998476, timestamp=1772812779234))",
                "2026-03-06T17:38:31.820+01:00 DEBUG 8252 --- [nio-8443-exec-8] o.s.w.f.CommonsRequestLoggingFilter      : Before request [POST /api/operationlist, client=192.168.50.74]"
        );

        EvamLogScenarioExtractor extractor = new EvamLogScenarioExtractor(gson);
        EvamLogScenario scenario = extractor.extract(Path.of("scenario.log"), lines);

        EvamLogScenarioEvent operation = scenario.getEvents().stream()
            .filter(event -> "/api/operations".equals(event.getEndpoint()))
            .findFirst()
            .orElseThrow();
        assertEquals(EvamLogExtractionQuality.INFERRED_CONTEXT, operation.getExtractionQuality());

        EvamLogScenarioEvent operationList = scenario.getEvents().stream()
            .filter(event -> "/api/operationlist".equals(event.getEndpoint()))
            .findFirst()
            .orElseThrow();
        assertEquals(EvamLogExtractionQuality.INFERRED_CONTEXT, operationList.getExtractionQuality());
    }

    @Test
    void parse_StructuredVehicleState_ReturnsNestedJson() {
        String input = "VehicleState(id=1, timestamp=2026-03-06T15:59:39.241, vehicleStatus=VehicleStatus(id=null, name=Avf Hamtplats, event=EVENT_EXIT_SITE, successorName=Ank Dest, isStartStatus=false, isEndStatus=false, categoryType=STATUS_MISSION, categoryName=mission), activeCaseFullId=18:17869359:2, vehicleLocation=Location(latitude=59.20189240674485, longitude=17.640825396998476, timestamp=1772812779234))";

        java.util.Map<?, ?> json = gson.fromJson(gson.toJson(StructuredLogValueParser.parse(input)), java.util.Map.class);

        assertEquals("18:17869359:2", json.get("activeCaseFullId"));
        assertNotNull(json.get("vehicleStatus"));
        assertNotNull(json.get("vehicleLocation"));
    }
}