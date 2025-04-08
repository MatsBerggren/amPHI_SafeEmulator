package com.dedalus.amphi_integration.service;

import java.util.Optional;

import com.dedalus.amphi_integration.model.AssignmentHistory;

public interface AmphiAssignmentHistoryService {
    Optional<AssignmentHistory> getFirstByOrderByCreatedDesc();
}
