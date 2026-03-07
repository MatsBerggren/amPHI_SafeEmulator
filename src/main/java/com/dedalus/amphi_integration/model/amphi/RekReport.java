package com.dedalus.amphi_integration.model.amphi;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RekReport {
    private String affected_count;
    private CommandOrganization command_organization;
    private String comments;
    private String created;
    private IncidentOrganization incident_organization;
    private String last_updated;
    private Position position;
    private String resources_on_site;
}
