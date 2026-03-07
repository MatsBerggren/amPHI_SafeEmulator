package com.dedalus.amphi_integration.model.amphi;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Unit {
    private String id;
    private String issi;
    private String msisdn;
    private Position position;
    private State state;
}
