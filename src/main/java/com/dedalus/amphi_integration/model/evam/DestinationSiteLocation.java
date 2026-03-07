package com.dedalus.amphi_integration.model.evam;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DestinationSiteLocation {
        private Double latitude;
        private Double longitude;
        private String street;
        private String locality;
        private String municipality;
        private String routeDirections;
        private String pickupTime;
}
