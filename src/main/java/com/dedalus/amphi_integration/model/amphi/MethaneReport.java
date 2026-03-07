package com.dedalus.amphi_integration.model.amphi;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MethaneReport {
    private String id;
    private AccessRoad access_road;
    private String created;
    private String exact_location;
    private ExtraResources extra_resources;
    private String[] hazards;
    private InventoryLevel inventory_level;
    private String last_updated;
    private Boolean major_incident;
    private Integer numbers_affected_green;
    private Integer numbers_affected_red;
    private Integer numbers_affected_yellow;
    private Position position;
    private String special_injuries;
    private String time_first_departure;
    private String[] types;
}
