package com.dedalus.amphi_integration.service.impl;

import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.dedalus.amphi_integration.model.amphi.StateEntry;
import com.dedalus.amphi_integration.repository.AmphiStateEntryRepository;
import com.dedalus.amphi_integration.service.AmphiStateEntryService;

@Service
public class AmphiStateEntryServiceImpl implements AmphiStateEntryService {

    @Autowired
    AmphiStateEntryRepository amphiStateEntryRepository;


    @Override
    public StateEntry updateStateEntry(StateEntry stateEntry) {
        Optional<StateEntry> existingStateEntry = amphiStateEntryRepository.findById(stateEntry.getId());

        if (existingStateEntry.isEmpty()) {
            return saveNewStateEntry(stateEntry);
        } else {
            return updateExistingStateEntry(existingStateEntry.get(), stateEntry);
        }
    }

    private StateEntry saveNewStateEntry(StateEntry stateEntry) {
        return amphiStateEntryRepository.save(stateEntry);
    }

    private StateEntry updateExistingStateEntry(StateEntry existingStateEntry, StateEntry newStateEntry) {
        existingStateEntry.setDistance(newStateEntry.getDistance());
        existingStateEntry.setFromId(newStateEntry.getFromId());
        existingStateEntry.setTime(newStateEntry.getTime());
        existingStateEntry.setId(newStateEntry.getId());

        return amphiStateEntryRepository.save(existingStateEntry);
    }

    @Override
    public List<StateEntry> getAll() {
        return amphiStateEntryRepository.findAll();
    }
}
