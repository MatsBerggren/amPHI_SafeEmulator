package com.dedalus.amphi_integration.model.evam;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class OperationList {
    private String id;
    private Operation[] operationList;
}
