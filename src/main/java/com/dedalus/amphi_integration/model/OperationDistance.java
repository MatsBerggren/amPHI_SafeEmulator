package com.dedalus.amphi_integration.model;

import java.time.LocalDateTime;

import com.dedalus.amphi_integration.model.evam.Location;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import nonapi.io.github.classgraph.json.Id;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OperationDistance {
    @Id
    private LocalDateTime timestamp;
    private String operationID;
    private Double distance;
    private Double assignmentDistance;
    private Double publishedAssignmentDistance;
    private String stateID;
    private Double stateEntryDistance;
    private Location location;
}
