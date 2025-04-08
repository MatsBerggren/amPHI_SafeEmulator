package com.dedalus.amphi_integration.service.impl;

import java.util.Optional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.dedalus.amphi_integration.model.AssignmentHistory;
import com.dedalus.amphi_integration.repository.AmphiAssignmentHistoryRepository;
import com.dedalus.amphi_integration.service.AmphiAssignmentHistoryService;

@Service
public class AmphiAssignmentHistoryServiceImpl implements AmphiAssignmentHistoryService {

    @Autowired
    AmphiAssignmentHistoryRepository amphiAssignmentHistoryRepository;

    @Override
    public Optional<AssignmentHistory> getFirstByOrderByCreatedDesc() {
        return amphiAssignmentHistoryRepository.findFirstByOrderByCreatedDesc();
    }

}
