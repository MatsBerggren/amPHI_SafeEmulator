package com.dedalus.amphi_integration.service.impl;

import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.dedalus.amphi_integration.model.amphi.Destination;
import com.dedalus.amphi_integration.repository.AmphiDestinationRepository;

@Slf4j
@Service
public class AmphiDestinationServiceImpl {

    @Autowired
    AmphiDestinationRepository amphiDestinationRepository;

    public Destination[] updateDestinations(Destination[] destinations) {
        log.info("Updating {} destinations", destinations.length);
        amphiDestinationRepository.deleteAll();
        for (Destination destination : destinations) {
            amphiDestinationRepository.save(destination);
        }
        return destinations;
    }

    public Destination getByNameAndType(String name, String type) {
        return amphiDestinationRepository.findByNameAndType(name, type);
    }

    public List<Destination> getAllDestinations() {
        return amphiDestinationRepository.findAll();
    }
}
