package com.dedalus.amphi_integration.model;

import java.time.LocalDateTime;

import com.dedalus.amphi_integration.model.amphi.Assignment;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import nonapi.io.github.classgraph.json.Id;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AssignmentHistory {
    @Id
    private LocalDateTime created;
    private String changes;
    private Assignment assignment;
}
