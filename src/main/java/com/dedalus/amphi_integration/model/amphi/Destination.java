package com.dedalus.amphi_integration.model.amphi;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Destination {
    private String abbreviation;
    private String name;
    private Position position;
    private String type;
    private Ward[] wards;
}
