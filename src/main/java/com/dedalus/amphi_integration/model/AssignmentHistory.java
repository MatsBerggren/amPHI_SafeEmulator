package com.dedalus.amphi_integration.model;

import java.time.LocalDateTime;

import com.dedalus.amphi_integration.model.amphi.Assignment;

import lombok.Builder;
import lombok.Data;
import nonapi.io.github.classgraph.json.Id;

@Data
@Builder
public class AssignmentHistory {
    @Id
    private LocalDateTime created;
    private String changes;
    private Assignment assignment;
}
