package com.dedalus.amphi_integration.model.amphi;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class State {
    private String action_name;
    private AllowedState[] allowed_states;
    private Integer state_id;
    private String state_name;
}
