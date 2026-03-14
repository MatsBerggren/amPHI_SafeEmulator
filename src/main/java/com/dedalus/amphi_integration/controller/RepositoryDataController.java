package com.dedalus.amphi_integration.controller;

import com.dedalus.amphi_integration.service.impl.RepositoryDataLoadService;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

@RestController
@CrossOrigin
@RequestMapping(value = "/api/admin", produces = "application/json", method = RequestMethod.POST)
@Tag(name = "Repository Data API", description = "Manual loading of persisted repository data")
public class RepositoryDataController {

    private final RepositoryDataLoadService repositoryDataLoadService;

    public RepositoryDataController(RepositoryDataLoadService repositoryDataLoadService) {
        this.repositoryDataLoadService = repositoryDataLoadService;
    }

    @PostMapping("/load-stored-data")
    public Map<String, Object> loadStoredData() {
        Map<String, Integer> loadedEntityCounts = repositoryDataLoadService.loadAllFromDisk();
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("repositories", loadedEntityCounts);
        response.put("totalEntities", loadedEntityCounts.values().stream().mapToInt(Integer::intValue).sum());
        return response;
    }
}