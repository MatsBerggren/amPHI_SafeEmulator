package com.dedalus.amphi_integration.service.impl;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EvamScenarioReplayResult {
    private int processedCount;
    private int skippedCount;
    private int failedCount;
    private Map<String, Integer> processedByEndpoint;
    private Map<String, Integer> skippedByReason;
    private List<String> failures;

    public static EvamScenarioReplayResult empty() {
        return EvamScenarioReplayResult.builder()
                .processedByEndpoint(new LinkedHashMap<>())
                .skippedByReason(new LinkedHashMap<>())
                .failures(new ArrayList<>())
                .build();
    }
}