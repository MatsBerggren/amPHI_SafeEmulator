package com.dedalus.amphi_integration.service.impl;

import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.dedalus.amphi_integration.model.amphi.StateEntry;
import com.dedalus.amphi_integration.repository.AmphiStateEntryRepository;

@Service
public class AmphiStateEntryServiceImpl {

    @Autowired
    AmphiStateEntryRepository amphiStateEntryRepository;

    public StateEntry updateStateEntry(StateEntry stateEntry) {
        Optional<StateEntry> existingStateEntry = amphiStateEntryRepository.findById(stateEntry.getId());

        if (existingStateEntry.isEmpty()) {
            return amphiStateEntryRepository.save(stateEntry);
        } else {
            StateEntry existing = existingStateEntry.get();
            existing.setDistance(stateEntry.getDistance());
            existing.setFromId(stateEntry.getFromId());
            existing.setTime(stateEntry.getTime());
            existing.setId(stateEntry.getId());
            return amphiStateEntryRepository.save(existing);
        }
    }

    public List<StateEntry> getAll() {
        return amphiStateEntryRepository.findAll();
    }
}

