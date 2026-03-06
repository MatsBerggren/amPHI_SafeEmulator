package com.dedalus.amphi_integration.model.evam; 

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class LeavePatientLocation {
        private Double latitude;
        private Double longitude;
        private String street;
        private String locality;
        private String municipality;
        private String routeDirections;
        private String leaveTime;
}
