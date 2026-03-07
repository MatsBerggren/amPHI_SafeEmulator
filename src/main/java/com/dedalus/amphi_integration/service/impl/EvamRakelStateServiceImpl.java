package com.dedalus.amphi_integration.service.impl;

import java.util.Optional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.google.gson.Gson;
import com.dedalus.amphi_integration.model.evam.RakelState;
import com.dedalus.amphi_integration.repository.EvamRakelStateRepository;
import com.dedalus.amphi_integration.util.WrappedPayloadParser;

@Service
public class EvamRakelStateServiceImpl {

    @Autowired
    EvamRakelStateRepository evamRakelStateRepository;
    @Autowired
    Gson gson;

    public RakelState updateRakelState(String json) {
        RakelState rakelState = WrappedPayloadParser.parseObject(json, gson, RakelState.class,
            "rakelState", "rakelstate");

        Optional<RakelState> existingRakelState = evamRakelStateRepository.findById("1");
        if (existingRakelState.isEmpty()) {
            rakelState.setId("1");
            rakelState.setUnitId("1");
            return evamRakelStateRepository.save(rakelState);
        } else {
            RakelState storedRakelState = existingRakelState.get();
            String incomingMsisdn = rakelState.getMsisdn();

            if ("0000567".equals(incomingMsisdn)) {
                storedRakelState.setMsisdn("3393090");
            } else if (incomingMsisdn != null) {
                storedRakelState.setMsisdn(incomingMsisdn);
            }

            storedRakelState.setIssi(rakelState.getIssi());
            storedRakelState.setGssi(rakelState.getGssi());
            if (rakelState.getIsHealthy() != null) {
                storedRakelState.setIsHealthy(rakelState.getIsHealthy());
            }
            return evamRakelStateRepository.save(storedRakelState);
        }
    }

    public RakelState getById(String id) {
        return evamRakelStateRepository.findById(id).orElseThrow(
            () -> new RuntimeException("No RakelState found for id: %s".formatted(id)));
    }
}
