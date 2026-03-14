package com.dedalus.amphi_integration.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.Test;
import com.dedalus.amphi_integration.AppConfig;
import com.dedalus.amphi_integration.model.amphi.MethaneReport;
import com.dedalus.amphi_integration.model.evam.Operation;
import com.dedalus.amphi_integration.model.evam.VehicleState;
import com.google.gson.Gson;

class WrappedPayloadParserTest {

    private final Gson gson = new AppConfig().gson();

    @Test
    void parseObject_WithRawPayload_ReturnsTargetObject() {
        String json = """
                {
                  \"activeCaseFullId\": \"1:1234567891:9:1\"
                }
                """;

        VehicleState result = WrappedPayloadParser.parseObject(json, gson, VehicleState.class,
                "vehicleState", "vehiclestate");

        assertNotNull(result);
        assertEquals("1:1234567891:9:1", result.getActiveCaseFullId());
    }

    @Test
    void parseObject_WithWrappedStringPayload_ReturnsTargetObject() {
        String payload = """
                {
                  \"exact_location\": \"Tunnelmynning\",
                  \"major_incident\": true
                }
                """;
        String wrapped = gson.toJson(java.util.Map.of("methaneReport", payload));

        MethaneReport result = WrappedPayloadParser.parseObject(wrapped, gson, MethaneReport.class,
                "methaneReport", "methanereport");

        assertNotNull(result);
        assertEquals("Tunnelmynning", result.getExact_location());
        assertEquals(true, result.getMajor_incident());
    }

    @Test
    void parseObject_WithWrappedObjectPayload_ReturnsTargetObject() {
        String wrapped = """
                {
                  \"vehicleState\": {
                    \"activeCaseFullId\": \"1:1234567891:9:1\"
                  }
                }
                """;

        VehicleState result = WrappedPayloadParser.parseObject(wrapped, gson, VehicleState.class,
                "vehicleState", "vehiclestate");

        assertNotNull(result);
        assertEquals("1:1234567891:9:1", result.getActiveCaseFullId());
    }

                @Test
                void parseObject_WithHospitalNameAndNumericPriority_ReturnsParsedValues() {
                                String wrapped = """
                                                                {
                                                                        "operation": {
                                                                                "selectedHospital": "Länssjukhuset Ryhov",
                                                                                "selectedPriority": 1
                                                                        }
                                                                }
                                                                """;

                                Operation result = WrappedPayloadParser.parseObject(wrapped, gson, Operation.class, "operation");

                                assertNotNull(result);
                                assertEquals("Länssjukhuset Ryhov", result.getSelectedHospital());
                                assertEquals(1, result.getSelectedPriority());
                }

                @Test
                void parseObject_WithNumericHospitalValue_ReturnsStringValue() {
                                String wrapped = """
                                                                {
                                                                        "operation": {
                                                                                "selectedHospital": 42
                                                                        }
                                                                }
                                                                """;

                                Operation result = WrappedPayloadParser.parseObject(wrapped, gson, Operation.class, "operation");

                                assertNotNull(result);
                                assertEquals("42", result.getSelectedHospital());
                }
}