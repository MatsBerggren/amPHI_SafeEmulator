package com.dedalus.amphi_integration.model;

import java.time.LocalDateTime;

import com.dedalus.amphi_integration.model.evam.Location;

import lombok.Builder;
import lombok.Data;
import nonapi.io.github.classgraph.json.Id;

@Data
@Builder
public class OperationDistance {
    @Id
    private LocalDateTime timestamp;
    private String operationID;
    private Double distance;
    private Double assignmentDistance;
    private String stateID;
    private Double stateEntryDistance;
    private Location location;
}
