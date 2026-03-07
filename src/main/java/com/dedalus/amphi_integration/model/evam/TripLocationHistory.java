package com.dedalus.amphi_integration.model.evam;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import nonapi.io.github.classgraph.json.Id;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TripLocationHistory {
    @Id
    private String id;
    private Location[] locationHistory;
    private Integer etaSeconds;
    private Integer distanceToDestinationMeters;
}
