package com.dedalus.amphi_integration.service.impl;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.dedalus.amphi_integration.model.AssignmentHistory;
import com.dedalus.amphi_integration.model.amphi.Assignment;
import com.dedalus.amphi_integration.repository.AmphiAssignmentHistoryRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;

@ExtendWith(MockitoExtension.class)
class AmphiAssignmentHistoryServiceImplTest {

    @Mock
    private AmphiAssignmentHistoryRepository amphiAssignmentHistoryRepository;

    @InjectMocks
    private AmphiAssignmentHistoryServiceImpl amphiAssignmentHistoryService;

    private AssignmentHistory testHistory1;
    private AssignmentHistory testHistory2;

    @BeforeEach
    void setUp() {
        Assignment assignment1 = Assignment.builder()
                .assignment_number("assign-1")
                .rowid("row-1")
                .build();

        Assignment assignment2 = Assignment.builder()
                .assignment_number("assign-2")
                .rowid("row-2")
                .build();

        testHistory1 = AssignmentHistory.builder()
                .created(LocalDateTime.of(2024, 1, 1, 10, 0, 0))
                .changes("Initial creation")
                .assignment(assignment1)
                .build();

        testHistory2 = AssignmentHistory.builder()
                .created(LocalDateTime.of(2024, 1, 2, 10, 0, 0))
                .changes("Status updated")
                .assignment(assignment2)
                .build();
    }

    // ========== getFirstByOrderByCreatedDesc Tests ==========

    @Test
    void getFirstByOrderByCreatedDesc_WhenHistoryExists_ReturnsOptionalWithHistory() {
        // Arrange
        when(amphiAssignmentHistoryRepository.findFirstByOrderByCreatedDesc())
                .thenReturn(Optional.of(testHistory2));

        // Act
        Optional<AssignmentHistory> result = amphiAssignmentHistoryService.getFirstByOrderByCreatedDesc();

        // Assert
        assertTrue(result.isPresent());
        assertEquals(testHistory2, result.get());
        assertEquals(LocalDateTime.of(2024, 1, 2, 10, 0, 0), result.get().getCreated());
        
        verify(amphiAssignmentHistoryRepository, times(1)).findFirstByOrderByCreatedDesc();
    }

    @Test
    void getFirstByOrderByCreatedDesc_WhenNoHistoryExists_ReturnsEmptyOptional() {
        // Arrange
        when(amphiAssignmentHistoryRepository.findFirstByOrderByCreatedDesc())
                .thenReturn(Optional.empty());

        // Act
        Optional<AssignmentHistory> result = amphiAssignmentHistoryService.getFirstByOrderByCreatedDesc();

        // Assert
        assertFalse(result.isPresent());
        assertTrue(result.isEmpty());
        
        verify(amphiAssignmentHistoryRepository, times(1)).findFirstByOrderByCreatedDesc();
    }

    @Test
    void getFirstByOrderByCreatedDesc_DelegatesToRepository() {
        // Arrange
        when(amphiAssignmentHistoryRepository.findFirstByOrderByCreatedDesc())
                .thenReturn(Optional.of(testHistory1));

        // Act
        amphiAssignmentHistoryService.getFirstByOrderByCreatedDesc();

        // Assert
        verify(amphiAssignmentHistoryRepository, times(1)).findFirstByOrderByCreatedDesc();
    }

    @Test
    void getFirstByOrderByCreatedDesc_ReturnsExactRepositoryResult() {
        // Arrange
        Optional<AssignmentHistory> expectedResult = Optional.of(testHistory1);
        when(amphiAssignmentHistoryRepository.findFirstByOrderByCreatedDesc())
                .thenReturn(expectedResult);

        // Act
        Optional<AssignmentHistory> result = amphiAssignmentHistoryService.getFirstByOrderByCreatedDesc();

        // Assert
        assertEquals(expectedResult, result);
        assertSame(expectedResult.get(), result.get());
    }

    @Test
    void getFirstByOrderByCreatedDesc_WithMostRecentHistory_ReturnsNewestEntry() {
        // Arrange - testHistory2 has a more recent date
        when(amphiAssignmentHistoryRepository.findFirstByOrderByCreatedDesc())
                .thenReturn(Optional.of(testHistory2));

        // Act
        Optional<AssignmentHistory> result = amphiAssignmentHistoryService.getFirstByOrderByCreatedDesc();

        // Assert
        assertTrue(result.isPresent());
        assertEquals("Status updated", result.get().getChanges());
        assertEquals(LocalDateTime.of(2024, 1, 2, 10, 0, 0), result.get().getCreated());
        assertTrue(result.get().getCreated().isAfter(testHistory1.getCreated()));
    }
}
