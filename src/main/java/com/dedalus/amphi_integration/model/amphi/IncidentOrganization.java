package com.dedalus.amphi_integration.model.amphi;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class IncidentOrganization {
    private String assembly_point_injured;
    private String assembly_point_uninjured;
    private String assembly_site;
    private String breakpoint;
    private String collect_point;
    private String command_site;
    private String incident_site;
    private String landing_site;
}
