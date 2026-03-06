package com.dedalus.amphi_integration.service;

import com.dedalus.amphi_integration.model.evam.VehicleState;

public interface EvamVehicleStateService {
    VehicleState updateVehicleState(String json);
    VehicleState getById(String id);
}
