package com.dedalus.amphi_integration.model.evam;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OperationUnit {
    private String unitId;
    private String status;
    private String role;
    private String source;
    private String eta;
    private String reportedInArea;
}
