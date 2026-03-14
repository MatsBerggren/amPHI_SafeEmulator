package com.dedalus.amphi_integration.util;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EvamLogScenarioEvent {
    private Integer sequence;
    private String requestTimestamp;
    private String method;
    private String endpoint;
    private String payloadType;
    private EvamLogExtractionQuality extractionQuality;
    private String payloadJson;
    private String rawLogValue;
    private String note;
}