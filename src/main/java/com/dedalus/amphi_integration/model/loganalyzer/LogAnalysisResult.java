package com.dedalus.amphi_integration.model.loganalyzer;

import com.dedalus.amphi_integration.util.EvamLogScenario;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LogAnalysisResult {
    private LogAnalysisSummary summary;
    private List<ReplayApiCall> apiCalls;
    private List<OperationReplayGroup> operationGroups;
    private EvamLogScenario scenario;
}