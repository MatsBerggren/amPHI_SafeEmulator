package com.dedalus.amphi_integration.model.amphi;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ExtraResources {
    private Integer ambulances;
    private Integer chemical_suit;
    private Integer commander_unit;
    private Integer doctor_on_duty;
    private Integer emergency_wagon;
    private Integer helicopter;
    private Integer medical_team;
    private Integer medical_transport;
    private Integer PAM;
    private Integer sanitation_wagon;
    private Integer transport_ambulance;
    private Integer units_total;
}
