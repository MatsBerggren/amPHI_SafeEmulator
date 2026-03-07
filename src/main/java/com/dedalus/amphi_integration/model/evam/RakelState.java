package com.dedalus.amphi_integration.model.evam;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RakelState {
    private String id;
    private String unitId;
    private String msisdn;
    private String issi;
    private String gssi;
    private Boolean isHealthy;
}
