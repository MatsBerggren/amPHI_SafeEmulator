package com.dedalus.amphi_integration.util;

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
public class EvamLogScenario {
    private String sourceLog;
    private String generatedAt;
    private List<EvamLogScenarioEvent> events;
    private Map<String, Integer> endpointCounts;
    private Map<String, Integer> qualityCounts;
}