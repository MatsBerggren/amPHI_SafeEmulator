package com.dedalus.amphi_integration.model.evam;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class RakelState {
    private String id;
    private String unitId;
    private String msisdn;
    private String issi;
    private String gssi;
}
