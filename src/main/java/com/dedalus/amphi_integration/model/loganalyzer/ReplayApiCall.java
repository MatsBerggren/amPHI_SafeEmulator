package com.dedalus.amphi_integration.model.loganalyzer;

import com.dedalus.amphi_integration.util.EvamLogExtractionQuality;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReplayApiCall {
    private Integer sequence;
    private String requestTimestamp;
    private Long relativeTimeSeconds;
    private String method;
    private String endpoint;
    private String payloadType;
    private EvamLogExtractionQuality extractionQuality;
    private boolean replayable;
    private String operationKey;
    private String payloadJson;
    private String rawLogValue;
    private String note;
    private String replayCommand;
}