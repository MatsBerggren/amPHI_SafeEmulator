package com.dedalus.amphi_integration.service.impl;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import org.mockito.junit.jupiter.MockitoExtension;

import com.dedalus.amphi_integration.repository.AmphiAssignmentHistoryRepository;
import com.dedalus.amphi_integration.repository.AmphiDestinationRepository;
import com.dedalus.amphi_integration.repository.OperationDistanceRepository;
import com.dedalus.amphi_integration.service.EvamOperationService;
import com.dedalus.amphi_integration.service.EvamVehicleStateService;

/**
 * Unit tests for AmphiAssignmentServiceImpl.
 * Focuses on testable methods and core business logic.
 */
@ExtendWith(MockitoExtension.class)
class AmphiAssignmentServiceImplTest {

    @Mock
    private EvamOperationService evamOperationService;

    @Mock
    private EvamVehicleStateService evamVehicleStateService;

    @Mock
    private AmphiStateEntryServiceImpl amphiStateEntryService;

    @Mock
    private EvamVehicleStatusServiceImpl evamVehicleStatusService;

    @Mock
    private AmphiAssignmentHistoryRepository amphiAssignmentHistoryRepository;

    @Mock
    private AmphiDestinationRepository amphiDestinationRepository;

    @Mock
    private AmphiAssignmentHistoryServiceImpl amphiAssignmentHistoryService;

    @Mock
    private OperationDistanceRepository operationDistanceRepository;

    @InjectMocks
    private AmphiAssignmentServiceImpl amphiAssignmentService;

    @BeforeEach
    void setUp() {
        // Setup common mocks if needed
    }

    // ========== Tests for getDifferences() static method ==========

    @Test
    void getDifferences_WithIdenticalObjects_ReturnsEmptyString() {
        // Arrange
        TestObject obj1 = new TestObject("value1", 42);
        TestObject obj2 = new TestObject("value1", 42);

        // Act
        String result = AmphiAssignmentServiceImpl.getDifferences(obj1, obj2);

        // Assert
        assertEquals("", result, "Identical objects should produce empty difference string");
    }

    @Test
    void getDifferences_WithDifferentStringField_ReturnsDifference() {
        // Arrange
        TestObject obj1 = new TestObject("value1", 42);
        TestObject obj2 = new TestObject("value2", 42);

        // Act
        String result = AmphiAssignmentServiceImpl.getDifferences(obj1, obj2);

        // Assert
        assertNotNull(result);
        assertTrue(result.contains("stringField"), 
                "Result should mention the changed field");
        assertTrue(result.contains("value1") && result.contains("value2"),
                "Result should show both old and new values");
    }

    @Test
    void getDifferences_WithDifferentIntField_ReturnsDifference() {
        // Arrange
        TestObject obj1 = new TestObject("value", 42);
        TestObject obj2 = new TestObject("value", 100);

        // Act
        String result = AmphiAssignmentServiceImpl.getDifferences(obj1, obj2);

        // Assert
        assertNotNull(result);
        assertTrue(result.contains("intField"),
                "Result should mention the changed field");
        assertTrue(result.contains("42") && result.contains("100"),
                "Result should show both old and new values");
    }

    @Test
    void getDifferences_WithMultipleDifferences_ReturnsAllDifferences() {
        // Arrange
        TestObject obj1 = new TestObject("value1", 42);
        TestObject obj2 = new TestObject("value2", 100);

        // Act
        String result = AmphiAssignmentServiceImpl.getDifferences(obj1, obj2);

        // Assert
        assertNotNull(result);
        assertTrue(result.contains("stringField") && result.contains("intField"),
                "Result should mention all changed fields");
        assertTrue(result.contains(","),
                "Multiple differences should be comma-separated");
    }

    @Test
    void getDifferences_WithNullToValue_ReturnsDifference() {
        // Arrange
        TestObject obj1 = new TestObject(null, 42);
        TestObject obj2 = new TestObject("value", 42);

        // Act
        String result = AmphiAssignmentServiceImpl.getDifferences(obj1, obj2);

        // Assert
        assertNotNull(result);
        assertTrue(result.contains("stringField"),
                "Result should mention field changed from null");
        assertTrue(result.contains("null") && result.contains("value"),
                "Result should show null -> value transition");
    }

    @Test
    void getDifferences_WithValueToNull_ReturnsDifference() {
        // Arrange
        TestObject obj1 = new TestObject("value", 42);
        TestObject obj2 = new TestObject(null, 42);

        // Act
        String result = AmphiAssignmentServiceImpl.getDifferences(obj1, obj2);

        // Assert
        assertNotNull(result);
        assertTrue(result.contains("stringField"),
                "Result should mention field changed to null");
        assertTrue(result.contains("value") && result.contains("null"),
                "Result should show value -> null transition");
    }

    @Test
    void getDifferences_WithBothFieldsNull_ReturnsEmptyDifference() {
        // Arrange
        TestObject obj1 = new TestObject(null, 42);
        TestObject obj2 = new TestObject(null, 42);

        // Act
        String result = AmphiAssignmentServiceImpl.getDifferences(obj1, obj2);

        // Assert
        // Should not include the null field in differences
        assertFalse(result.contains("stringField"),
                "Null fields that remain null should not be in differences");
    }

    @Test
    void getDifferences_WithNullFirstObject_ThrowsException() {
        // Arrange
        TestObject obj2 = new TestObject("value", 42);

        // Act & Assert
        assertThrows(IllegalArgumentException.class,
                () -> AmphiAssignmentServiceImpl.getDifferences(null, obj2),
                "Should throw exception when first object is null");
    }

    @Test
    void getDifferences_WithNullSecondObject_ThrowsException() {
        // Arrange
        TestObject obj1 = new TestObject("value", 42);

        // Act & Assert
        assertThrows(IllegalArgumentException.class,
                () -> AmphiAssignmentServiceImpl.getDifferences(obj1, null),
                "Should throw exception when second object is null");
    }

    @Test
    void getDifferences_WithDifferentTypes_ThrowsException() {
        // Arrange
        TestObject obj1 = new TestObject("value", 42);
        String obj2 = "different type";

        // Act & Assert
        assertThrows(IllegalArgumentException.class,
                () -> AmphiAssignmentServiceImpl.getDifferences(obj1, obj2),
                "Should throw exception when objects are of different types");
    }

    // ========== Tests for deleteOldRecords() ==========

    @Test
    void deleteOldRecords_CallsRepositoryWithCorrectDate() {
        // Arrange
        Integer days = 7;
        LocalDate expectedDate = LocalDate.now().minusDays(days);

        // Act
        amphiAssignmentService.deleteOldRecords(days);

        // Assert
        verify(amphiAssignmentHistoryRepository, times(1))
                .deleteByCreatedBefore(expectedDate);
        verify(operationDistanceRepository, times(1))
                .deleteByTimestampBefore(expectedDate);
    }

    @Test
    void deleteOldRecords_WithZeroDays_DeletesRecordsFromToday() {
        // Arrange
        Integer days = 0;
        LocalDate expectedDate = LocalDate.now();

        // Act
        amphiAssignmentService.deleteOldRecords(days);

        // Assert
        verify(amphiAssignmentHistoryRepository, times(1))
                .deleteByCreatedBefore(expectedDate);
        verify(operationDistanceRepository, times(1))
                .deleteByTimestampBefore(expectedDate);
    }

    @Test
    void deleteOldRecords_With30Days_DeletesOldRecords() {
        // Arrange
        Integer days = 30;
        LocalDate expectedDate = LocalDate.now().minusDays(days);

        // Act
        amphiAssignmentService.deleteOldRecords(days);

        // Assert
        verify(amphiAssignmentHistoryRepository, times(1))
                .deleteByCreatedBefore(expectedDate);
        verify(operationDistanceRepository, times(1))
                .deleteByTimestampBefore(expectedDate);
    }

    // ========== Helper Test Class ==========

    /**
     * Simple test class for testing getDifferences() method.
     * Must be static so it can be used in static method tests.
     */
    private static class TestObject {
        private String stringField;
        private int intField;

        public TestObject(String stringField, int intField) {
            this.stringField = stringField;
            this.intField = intField;
        }
    }
}
