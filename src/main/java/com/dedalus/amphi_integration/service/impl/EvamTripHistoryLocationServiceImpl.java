package com.dedalus.amphi_integration.service.impl;

import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.dedalus.amphi_integration.model.evam.TripLocationHistory;
import com.dedalus.amphi_integration.repository.EvamTripLocationHistoryRepository;
import com.google.gson.Gson;

@Service
public class EvamTripHistoryLocationServiceImpl {

    @Autowired
    EvamTripLocationHistoryRepository evamTripLocationHistoryRepository;
    @Autowired
    Gson gson;

    public TripLocationHistory updateTripLocationHistory(String json) {
        TripLocationHistory tripLocationHistory = gson.fromJson(json, TripLocationHistory.class);
        evamTripLocationHistoryRepository.deleteById("1");
        tripLocationHistory.setId("1");
        evamTripLocationHistoryRepository.save(tripLocationHistory);
        return tripLocationHistory;
    }

    public TripLocationHistory getById(String id) {
        return evamTripLocationHistoryRepository.findById(id).orElseThrow(
                () -> new RuntimeException("No TripLocationHistory found for id: %s".formatted(id)));
    }

    public List<TripLocationHistory> getAll() {
        return evamTripLocationHistoryRepository.findAll();
    }
}
