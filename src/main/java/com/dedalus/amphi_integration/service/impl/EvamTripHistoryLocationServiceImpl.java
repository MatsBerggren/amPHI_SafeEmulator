package com.dedalus.amphi_integration.service.impl;

import java.time.LocalDateTime;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.dedalus.amphi_integration.classes.LocalDateTimeDeserializer;
import com.dedalus.amphi_integration.dto.EvamTripLocationHistoryRequestDTO;
import com.dedalus.amphi_integration.model.evam.TripLocationHistory;
import com.dedalus.amphi_integration.repository.EvamTripLocationHistoryRepository;
import com.dedalus.amphi_integration.service.EvamTripLocationHistoryService;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

@Service
/**
 * This class implements the EvamTripLocationHistoryService interface for managing trip location
 * history.
 */
public class EvamTripHistoryLocationServiceImpl implements EvamTripLocationHistoryService {

    @Autowired
    EvamTripLocationHistoryRepository evamTripLocationHistoryRepository;

    private Gson gson;

// This code snippet is a constructor for the `EvamTripHistoryLocationServiceImpl` class in Java. It
// initializes a Gson object (`gson`) with specific configurations for JSON serialization and
// deserialization.
    public EvamTripHistoryLocationServiceImpl() {
        GsonBuilder gsonBuilder = new GsonBuilder();
        gsonBuilder.registerTypeAdapter(LocalDateTime.class, new LocalDateTimeDeserializer());
        this.gson = gsonBuilder.setPrettyPrinting().disableHtmlEscaping().create();
    }

/**
 * The function updates a trip location history record in a database based on the provided DTO.
 * 
 * @param evamTripLocationHistoryRequestDTO EvamTripLocationHistoryRequestDTO is a data transfer object
 * that contains information related to trip location history. It is used as a parameter in the
 * updateTripLocationHistory method to update the trip location history record in the database.
 * @return The method `updateTripLocationHistory` is returning an object of type `TripLocationHistory`.
 */
    @Override
    public TripLocationHistory updateTripLocationHistory(
            EvamTripLocationHistoryRequestDTO evamTripLocationHistoryRequestDTO) {

        TripLocationHistory tripLocationHistory = gson
                .fromJson(evamTripLocationHistoryRequestDTO.getTripLocationHistory(), TripLocationHistory.class);
        evamTripLocationHistoryRepository.deleteById("1");

        tripLocationHistory.setId("1");
        evamTripLocationHistoryRepository.save(tripLocationHistory);

        return tripLocationHistory;
    }

/**
 * This function retrieves a TripLocationHistory object by its ID from a repository and throws a
 * RuntimeException if no object is found.
 * 
 * @param id The `id` parameter is a unique identifier used to retrieve a specific
 * `TripLocationHistory` object from the repository.
 * @return The method is returning a TripLocationHistory object with the specified id. If no
 * TripLocationHistory is found for the given id, it will throw a RuntimeException with a message
 * indicating that no TripLocationHistory was found for that id.
 */
    @Override
    public TripLocationHistory getById(String id) {
        return evamTripLocationHistoryRepository.findById(id).orElseThrow(
                () -> new RuntimeException("No TripLocationHistory found for id: %s".formatted(id)));
    }

/**
 * The function returns all trip location history records from the repository.
 * 
 * @return A List of TripLocationHistory objects is being returned.
 */
    @Override
    public List<TripLocationHistory> getAll() {
        return evamTripLocationHistoryRepository.findAll();
    }

}

