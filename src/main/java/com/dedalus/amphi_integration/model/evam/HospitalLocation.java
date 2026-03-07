package com.dedalus.amphi_integration.model.evam;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HospitalLocation {
    private Integer id;
    private Double latitude;
    private Double longitude;
    private String name;
    private String street1;
    private String city;
    private String region;
    private String postalCode;
}
