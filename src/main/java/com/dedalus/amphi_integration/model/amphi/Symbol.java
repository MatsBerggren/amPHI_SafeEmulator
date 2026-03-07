package com.dedalus.amphi_integration.model.amphi;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Symbol {
    private String id;
    private Integer mapitemtype;
    private String state;
    private Integer heading;
    private String description;
    private String assignmentId;
    private Position position;
    private String unitId;
    private boolean is_deleted;
    private String icon;
    private Integer color;
    private String geometry;
}
