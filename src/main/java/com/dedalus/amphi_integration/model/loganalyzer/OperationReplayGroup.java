package com.dedalus.amphi_integration.model.loganalyzer;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OperationReplayGroup {
    private String operationKey;
    private int callCount;
    private int replayableCount;
    private String firstRequestTimestamp;
    private String lastRequestTimestamp;
    private Long firstRelativeTimeSeconds;
    private Long lastRelativeTimeSeconds;
    private List<String> endpoints;
    private List<Integer> sequences;
}