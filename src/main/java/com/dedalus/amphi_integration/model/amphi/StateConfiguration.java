package com.dedalus.amphi_integration.model.amphi;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StateConfiguration {
    private Integer[] allowed_transitions;
    private Integer id;
    private String name;
    private String transition_name;
}
