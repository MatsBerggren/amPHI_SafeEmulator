package com.dedalus.amphi_integration.model.amphi;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Position {
    private Double rt90_x;
    private Double rt90_y;
    private Double sweref99_e;
    private Double sweref99_n;
    private Double wgs84_dd_la;
    private Double wgs84_dd_lo;
}
