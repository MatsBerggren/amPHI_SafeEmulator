package com.dedalus.amphi_integration.service.impl;

import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.dedalus.amphi_integration.model.evam.OperationList;
import com.dedalus.amphi_integration.repository.EvamOperationListRepository;
import com.google.gson.Gson;

@Slf4j
@Service
public class EvamOperationListServiceImpl {

    @Autowired
    Gson gson;
    @Autowired
    EvamOperationListRepository evamOperationListRepository;

    public OperationList updateOperationList(String json) {
        OperationList operationList = gson.fromJson(json, OperationList.class);
        log.info("Updating OperationList: {}", operationList);

        Optional<OperationList> existingOperationList = evamOperationListRepository.findById("1");

        if (existingOperationList.isEmpty()) {
            operationList.setId("1");
            evamOperationListRepository.save(operationList);
        } else {
            existingOperationList.get().setOperationList(operationList.getOperationList());
            evamOperationListRepository.save(existingOperationList.get());
            log.info("OperationList Updated");
        }

        return operationList;
    }

    public OperationList getById(String id) {
        return evamOperationListRepository.findById(id).orElseThrow(() -> new RuntimeException("No OperationList found for id: %s".formatted(id)));
    }
}