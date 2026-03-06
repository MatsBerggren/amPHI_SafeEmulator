package com.dedalus.amphi_integration.service;

import com.dedalus.amphi_integration.model.evam.Operation;

public interface EvamOperationService {
    Operation updateOperation(String json);
    Operation getById(String id);
}
