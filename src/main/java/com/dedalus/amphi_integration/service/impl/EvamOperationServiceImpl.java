package com.dedalus.amphi_integration.service.impl;

import java.util.Optional;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.dedalus.amphi_integration.model.evam.Operation;
import com.dedalus.amphi_integration.model.evam.VehicleState;
import com.dedalus.amphi_integration.repository.AmphiStateEntryRepository;
import com.dedalus.amphi_integration.repository.EvamMethaneReportRepository;
import com.dedalus.amphi_integration.repository.EvamOperationRepository;
import com.dedalus.amphi_integration.repository.EvamTripLocationHistoryRepository;
import com.dedalus.amphi_integration.repository.EvamVehicleStateRepository;
import com.dedalus.amphi_integration.service.EvamOperationService;
import com.dedalus.amphi_integration.util.WrappedPayloadParser;
import com.google.gson.Gson;

@Slf4j
@Service
public class EvamOperationServiceImpl implements EvamOperationService {

    @Autowired
    private EvamOperationRepository evamOperationRepository;
    @Autowired
    private EvamVehicleStateRepository evamVehicleStateRepository;
    @Autowired
    private AmphiStateEntryRepository amphiStateEntryRepository;
    @Autowired
    private EvamMethaneReportRepository evamMethaneRepository;
    @Autowired
    private EvamTripLocationHistoryRepository evamTripLocationHistoryRepository;
    @Autowired
    private Gson gson;

    @Override
    public Operation updateOperation(String json) {
        Operation operation = parseOperation(json);
        Optional<Operation> existingOperation = evamOperationRepository.findById("1");

        if (existingOperation.isEmpty() || !operation.getFullId().equals(existingOperation.get().getFullId())) {
            cleanDB();
            operation = saveNewOperation(operation);
            return operation;
        }

        return updateExistingOperation(existingOperation.get(), operation);
    }

    private Operation parseOperation(String json) {
        return WrappedPayloadParser.parseObject(json, gson, Operation.class, "operation");
    }

    private void cleanDB() {
        evamMethaneRepository.deleteAll();
        evamOperationRepository.deleteAll();
        amphiStateEntryRepository.deleteAll();
        evamTripLocationHistoryRepository.deleteAll();
        evamVehicleStateRepository.save(VehicleState.builder().id("1").build());
    }

    private Operation saveNewOperation(Operation operation) {
        operation.setId("1");
        operation.setAmPHIUniqueId(UUID.randomUUID().toString());
        return evamOperationRepository.save(operation);
    }

    private Operation updateExistingOperation(Operation existingOperation, Operation operation) {
        existingOperation.updateFrom(operation);
        return evamOperationRepository.save(existingOperation);
    }

    @Override
    public Operation getById(String id) {
        return evamOperationRepository.findById(id).orElseThrow(() -> new RuntimeException("No Operation found for id: %s".formatted(id)));
    }
}
