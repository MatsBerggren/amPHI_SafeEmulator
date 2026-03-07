package com.dedalus.amphi_integration.model.evam;

import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VehicleState {
    private String id;
    private LocalDateTime timestamp;
    private VehicleStatus vehicleStatus;
    private String activeCaseFullId;
    private Location vehicleLocation;
}
