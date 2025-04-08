package com.dedalus.amphi_integration.model.amphi;

import com.google.gson.annotations.SerializedName;

import lombok.Builder;
import lombok.Data;
import nonapi.io.github.classgraph.json.Id;

@Data
@Builder
public class StateEntry {
    @Id
    @SerializedName("to_id")
    private String id;
    @SerializedName("from_id")
    private String fromId;
    private Integer distance;
    private String time;
}
