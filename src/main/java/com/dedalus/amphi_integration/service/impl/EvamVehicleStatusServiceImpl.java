package com.dedalus.amphi_integration.service.impl;

import java.util.ArrayList;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.dedalus.amphi_integration.model.evam.VehicleStatus;
import com.dedalus.amphi_integration.repository.EvamVehicleStatusRepository;
import com.google.gson.Gson;
import com.google.gson.JsonElement;

@Service
public class EvamVehicleStatusServiceImpl {

    @Autowired
    private EvamVehicleStatusRepository evamVehicleStatusRepository;
    @Autowired
    private Gson gson;

    public VehicleStatus[] updateVehicleStatus(String json) {
        JsonElement payload = gson.fromJson(json, JsonElement.class);
        List<VehicleStatus> vehicleStatuses = new ArrayList<>();

        if (payload != null && payload.isJsonArray()) {
            vehicleStatuses.addAll(List.of(gson.fromJson(payload, VehicleStatus[].class)));
        } else if (payload != null && payload.isJsonObject()) {
            vehicleStatuses.add(gson.fromJson(payload, VehicleStatus.class));
        }

        evamVehicleStatusRepository.deleteAll();

        for (int i = 0; i < vehicleStatuses.size(); i++) {
            vehicleStatuses.get(i).setId(String.valueOf(i + 1));
        }

        evamVehicleStatusRepository.saveAll(vehicleStatuses);
        return vehicleStatuses.toArray(new VehicleStatus[0]);
    }

    public VehicleStatus getById(String id) {
        return evamVehicleStatusRepository.findById(id).orElseThrow(
                () -> new RuntimeException("No VehicleStatus found for id: %s".formatted(id)));
    }

    public List<VehicleStatus> getAll() {
        return evamVehicleStatusRepository.findAll();
    }

    public VehicleStatus getByName(String name) {
        return evamVehicleStatusRepository.findByName(name);
    }
}
