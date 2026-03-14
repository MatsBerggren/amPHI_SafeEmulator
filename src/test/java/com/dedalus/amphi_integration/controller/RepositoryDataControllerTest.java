package com.dedalus.amphi_integration.controller;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.dedalus.amphi_integration.service.impl.RepositoryDataLoadService;
import java.util.LinkedHashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(RepositoryDataController.class)
class RepositoryDataControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private RepositoryDataLoadService repositoryDataLoadService;

    @Test
    void loadStoredData_ReturnsRepositoryCountsAndTotal() throws Exception {
        Map<String, Integer> loadedEntityCounts = new LinkedHashMap<>();
        loadedEntityCounts.put("Operation", 2);
        loadedEntityCounts.put("OperationDistance", 3);

        when(repositoryDataLoadService.loadAllFromDisk()).thenReturn(loadedEntityCounts);

        mockMvc.perform(post("/api/admin/load-stored-data"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.repositories.Operation").value(2))
                .andExpect(jsonPath("$.repositories.OperationDistance").value(3))
                .andExpect(jsonPath("$.totalEntities").value(5));
    }
}