package com.dedalus.amphi_integration.model.evam;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class DestinationControlPointLocation {

        private Double latitude;

        private Double longitude;

        private String name;

        private String additionalInfo;

}
