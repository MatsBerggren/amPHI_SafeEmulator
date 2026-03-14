package com.dedalus.amphi_integration.model.loganalyzer;

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
public class LogAnalysisSummary {
    private String analysisId;
    private String sourceLog;
    private String generatedAt;
    private String savedAt;
    private int totalEvents;
    private int replayableEvents;
    private int observedOnlyEvents;
    private int payloadlessEvents;
    private int endpointsObserved;
    private String firstRequestTimestamp;
    private String lastRequestTimestamp;
    private Map<String, Integer> endpointCounts;
    private Map<String, Integer> qualityCounts;
    private List<String> operationKeys;
    private List<String> notes;
}