package com.dedalus.amphi_integration.service.impl;

import java.util.Optional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.dedalus.amphi_integration.model.AssignmentHistory;
import com.dedalus.amphi_integration.repository.AmphiAssignmentHistoryRepository;

@Service
public class AmphiAssignmentHistoryServiceImpl {

    @Autowired
    AmphiAssignmentHistoryRepository amphiAssignmentHistoryRepository;

    public Optional<AssignmentHistory> getFirstByOrderByCreatedDesc() {
        return amphiAssignmentHistoryRepository.findFirstByOrderByCreatedDesc();
    }

}

