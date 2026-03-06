package com.dedalus.amphi_integration.controller;

import com.dedalus.amphi_integration.model.amphi.Assignment;
import com.dedalus.amphi_integration.model.amphi.Destination;
import com.dedalus.amphi_integration.model.amphi.Symbol;
import com.dedalus.amphi_integration.service.AmphiAssignmentService;
import com.dedalus.amphi_integration.service.EvamOperationService;
import com.dedalus.amphi_integration.service.EvamVehicleStateService;
import com.dedalus.amphi_integration.service.impl.AmphiDestinationServiceImpl;
import com.dedalus.amphi_integration.service.impl.EvamRakelStateServiceImpl;
import com.dedalus.amphi_integration.service.impl.EvamVehicleStatusServiceImpl;
import com.google.gson.Gson;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Unit tests for AmphiController.
 * Uses MockMvc for testing REST endpoints without starting a full server.
 * 
 * These tests focus on testing the controller endpoints and their basic behavior,
 * avoiding complex setup of domain model objects.
 */
@WebMvcTest(AmphiController.class)
class AmphiControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private EvamOperationService evamOperationService;

    @MockBean
    private EvamVehicleStateService evamVehicleStateService;

    @MockBean
    private EvamRakelStateServiceImpl evamRakelStateService;

    @MockBean
    private AmphiDestinationServiceImpl amphiDestinationService;

    @MockBean
    private AmphiAssignmentService amphiAssignmentService;

    @MockBean
    private EvamVehicleStatusServiceImpl evamVehicleStatusService;

    @MockBean
    private Gson gson;

    // ========== API Version Endpoint Tests ==========

    @Test
    void getCsamInterfaceVersion_WhenTimeExceeded_ReturnsServiceUnavailable() throws Exception {
        // Arrange - timeExceeded starts as true in controller
        
        // Act & Assert - expect 503 due to timeExceeded flag
        mockMvc.perform(get("/api/rest/apiversion/"))
                .andExpect(status().isServiceUnavailable())
                .andExpect(content().string("Service unavailable"));
    }

    // ========== Destinations Endpoint Tests ==========

    @Test
    void postDestinations_WithValidData_ReturnsOk() throws Exception {
        // Arrange - POST destinations doesn't check timeExceeded
        Destination[] destinations = new Destination[] {
            Destination.builder()
                .abbreviation("TH")
                .name("Test Hospital")
                .type("AkutSjukhus")
                .build()
        };
        
        when(gson.fromJson(any(String.class), eq(Destination[].class))).thenReturn(destinations);
        when(amphiDestinationService.updateDestinations(any(Destination[].class)))
                .thenReturn(destinations);
        when(gson.toJson(any(Object.class))).thenReturn("[{\"name\":\"Test Hospital\"}]");

        String json = new Gson().toJson(destinations);

        // Act & Assert
        mockMvc.perform(post("/api/rest/destinations/")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json))
                .andExpect(status().isOk());
    }

    @Test
    void postDestinations_WithEmptyArray_ReturnsOk() throws Exception {
        // Arrange
        Destination[] emptyDestinations = new Destination[0];
        when(amphiDestinationService.updateDestinations(any(Destination[].class)))
                .thenReturn(emptyDestinations);

        String json = new Gson().toJson(emptyDestinations);

        // Act & Assert
        mockMvc.perform(post("/api/rest/destinations/")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json))
                .andExpect(status().isOk());
    }

    // ========== Symbols Endpoint Tests ========== 

    @Test
    void postSymbols_WhenTimeExceeded_ReturnsServiceUnavailable() throws Exception {
        // Arrange - timeExceeded starts as true in controller
        Symbol[] symbols = new Symbol[] {
            Symbol.builder()
                .id("symbol-1")
                .unitId("unit-1")
                .description("Test symbol")
                .mapitemtype(0)
                .heading(0)
                .is_deleted(false)
                .build()
        };

        String json = new Gson().toJson(symbols);

        // Act & Assert - expect 503 due to timeExceeded flag
        mockMvc.perform(put("/api/rest/symbols/")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json))
                .andExpect(status().isServiceUnavailable());
    }

    @Test
    void postSymbols_WithEmptyArray_ReturnsServiceUnavailable() throws Exception {
        // Arrange - timeExceeded starts as true
        Symbol[] emptySymbols = new Symbol[0];
        String json = new Gson().toJson(emptySymbols);

        // Act & Assert - expect 503 due to timeExceeded flag
        mockMvc.perform(put("/api/rest/symbols/")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json))
                .andExpect(status().isServiceUnavailable());
    }

    // ========== Assignments Endpoint Tests ==========

    @Test
    void getAssignments_WhenTimeExceeded_ReturnsServiceUnavailable() throws Exception {
        // Arrange - timeExceeded starts as true in controller
        Assignment[] assignments = new Assignment[] {
            Assignment.builder()
                .assignment_number("test-assignment-1")
                .rowid("row-1")
                .is_closed("false")
                .is_selected("1")
                .distance(100)
                .build()
        };
        
        when(amphiAssignmentService.getAllAssignments()).thenReturn(assignments);

        // Act & Assert - expect 503 due to timeExceeded flag
        mockMvc.perform(get("/api/rest/assignments/"))
                .andExpect(status().isServiceUnavailable());
    }

    // ========== Endpoint Path and Method Tests ==========

    @Test
    void getCsamInterfaceVersion_WithWrongMethod_ReturnsMethodNotAllowed() throws Exception {
        // Act & Assert - POST when GET is expected
        mockMvc.perform(post("/api/rest/apiversion/"))
                .andExpect(status().isMethodNotAllowed());
    }

    @Test
    void postDestinations_WithGetMethod_ReturnsMethodNotAllowed() throws Exception {
        // Act & Assert - GET when POST is expected for posting
        // Note: GET /api/rest/destinations/ is valid, but we're testing POST endpoint behavior
        String json = new Gson().toJson(new Destination[0]);
        
        // PUT when POST is expected
        mockMvc.perform(put("/api/rest/destinations/")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json))
                .andExpect(status().isMethodNotAllowed());
    }
}
