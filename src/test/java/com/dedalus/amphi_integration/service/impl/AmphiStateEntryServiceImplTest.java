package com.dedalus.amphi_integration.service.impl;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.dedalus.amphi_integration.model.amphi.StateEntry;
import com.dedalus.amphi_integration.repository.AmphiStateEntryRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

@ExtendWith(MockitoExtension.class)
class AmphiStateEntryServiceImplTest {

    @Mock
    private AmphiStateEntryRepository amphiStateEntryRepository;

    @InjectMocks
    private AmphiStateEntryServiceImpl amphiStateEntryService;

    private StateEntry testStateEntry1;
    private StateEntry testStateEntry2;

    @BeforeEach
    void setUp() {
        testStateEntry1 = StateEntry.builder()
                .id("state-1")
                .fromId("from-1")
                .distance(100)
                .time("10:00:00")
                .build();

        testStateEntry2 = StateEntry.builder()
                .id("state-2")
                .fromId("from-2")
                .distance(200)
                .time("11:00:00")
                .build();
    }

    // ========== updateStateEntry Tests - New Entry ==========

    @Test
    void updateStateEntry_WhenEntryDoesNotExist_SavesNewEntry() {
        // Arrange
        when(amphiStateEntryRepository.findById("state-1"))
                .thenReturn(Optional.empty());
        when(amphiStateEntryRepository.save(testStateEntry1))
                .thenReturn(testStateEntry1);

        // Act
        StateEntry result = amphiStateEntryService.updateStateEntry(testStateEntry1);

        // Assert
        assertNotNull(result);
        assertEquals(testStateEntry1, result);
        
        verify(amphiStateEntryRepository, times(1)).findById("state-1");
        verify(amphiStateEntryRepository, times(1)).save(testStateEntry1);
    }

    @Test
    void updateStateEntry_WhenNewEntry_CallsSaveOnce() {
        // Arrange
        when(amphiStateEntryRepository.findById(any()))
                .thenReturn(Optional.empty());
        when(amphiStateEntryRepository.save(any(StateEntry.class)))
                .thenReturn(testStateEntry1);

        // Act
        amphiStateEntryService.updateStateEntry(testStateEntry1);

        // Assert
        verify(amphiStateEntryRepository, times(1)).save(testStateEntry1);
    }

    // ========== updateStateEntry Tests - Existing Entry ==========

    @Test
    void updateStateEntry_WhenEntryExists_UpdatesExistingEntry() {
        // Arrange
        StateEntry existingEntry = StateEntry.builder()
                .id("state-1")
                .fromId("from-old")
                .distance(50)
                .time("09:00:00")
                .build();

        StateEntry updatedEntry = StateEntry.builder()
                .id("state-1")
                .fromId("from-new")
                .distance(150)
                .time("12:00:00")
                .build();

        when(amphiStateEntryRepository.findById("state-1"))
                .thenReturn(Optional.of(existingEntry));
        when(amphiStateEntryRepository.save(any(StateEntry.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        StateEntry result = amphiStateEntryService.updateStateEntry(updatedEntry);

        // Assert
        assertNotNull(result);
        assertEquals("state-1", result.getId());
        assertEquals("from-new", result.getFromId());
        assertEquals(150, result.getDistance());
        assertEquals("12:00:00", result.getTime());
        
        verify(amphiStateEntryRepository, times(1)).findById("state-1");
        verify(amphiStateEntryRepository, times(1)).save(existingEntry);
    }

    @Test
    void updateStateEntry_WhenEntryExists_UpdatesAllFields() {
        // Arrange
        StateEntry existingEntry = StateEntry.builder()
                .id("state-1")
                .fromId("old-from")
                .distance(10)
                .time("08:00:00")
                .build();

        StateEntry newData = StateEntry.builder()
                .id("state-1")
                .fromId("new-from")
                .distance(999)
                .time("23:59:59")
                .build();

        when(amphiStateEntryRepository.findById("state-1"))
                .thenReturn(Optional.of(existingEntry));
        when(amphiStateEntryRepository.save(existingEntry))
                .thenReturn(existingEntry);

        // Act
        amphiStateEntryService.updateStateEntry(newData);

        // Assert
        assertEquals("state-1", existingEntry.getId());
        assertEquals("new-from", existingEntry.getFromId());
        assertEquals(999, existingEntry.getDistance());
        assertEquals("23:59:59", existingEntry.getTime());
        
        verify(amphiStateEntryRepository).save(existingEntry);
    }

    @Test
    void updateStateEntry_WhenEntryExists_DoesNotCreateNewObject() {
        // Arrange
        StateEntry existingEntry = StateEntry.builder()
                .id("state-1")
                .fromId("from-1")
                .distance(100)
                .time("10:00:00")
                .build();

        when(amphiStateEntryRepository.findById("state-1"))
                .thenReturn(Optional.of(existingEntry));
        when(amphiStateEntryRepository.save(any(StateEntry.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        StateEntry result = amphiStateEntryService.updateStateEntry(testStateEntry1);

        // Assert
        assertSame(existingEntry, result);
        verify(amphiStateEntryRepository, times(1)).save(existingEntry);
    }

    @Test
    void updateStateEntry_WhenUpdating_PreservesObjectIdentity() {
        // Arrange
        StateEntry existingEntry = StateEntry.builder()
                .id("state-1")
                .fromId("from-old")
                .distance(50)
                .time("09:00:00")
                .build();

        StateEntry updateData = StateEntry.builder()
                .id("state-1")
                .fromId("from-new")
                .distance(150)
                .time("12:00:00")
                .build();

        when(amphiStateEntryRepository.findById("state-1"))
                .thenReturn(Optional.of(existingEntry));
        when(amphiStateEntryRepository.save(existingEntry))
                .thenReturn(existingEntry);

        // Act
        StateEntry result = amphiStateEntryService.updateStateEntry(updateData);

        // Assert - should return the same object that was retrieved
        assertSame(existingEntry, result);
    }

    // ========== getAll Tests ==========

    @Test
    void getAll_WhenEntriesExist_ReturnsAllEntries() {
        // Arrange
        List<StateEntry> expectedEntries = Arrays.asList(testStateEntry1, testStateEntry2);
        when(amphiStateEntryRepository.findAll())
                .thenReturn(expectedEntries);

        // Act
        List<StateEntry> result = amphiStateEntryService.getAll();

        // Assert
        assertNotNull(result);
        assertEquals(2, result.size());
        assertEquals(testStateEntry1, result.get(0));
        assertEquals(testStateEntry2, result.get(1));
        
        verify(amphiStateEntryRepository, times(1)).findAll();
    }

    @Test
    void getAll_WhenNoEntriesExist_ReturnsEmptyList() {
        // Arrange
        when(amphiStateEntryRepository.findAll())
                .thenReturn(Collections.emptyList());

        // Act
        List<StateEntry> result = amphiStateEntryService.getAll();

        // Assert
        assertNotNull(result);
        assertTrue(result.isEmpty());
        
        verify(amphiStateEntryRepository, times(1)).findAll();
    }

    @Test
    void getAll_WithSingleEntry_ReturnsSingleItemList() {
        // Arrange
        List<StateEntry> singleEntry = Collections.singletonList(testStateEntry1);
        when(amphiStateEntryRepository.findAll())
                .thenReturn(singleEntry);

        // Act
        List<StateEntry> result = amphiStateEntryService.getAll();

        // Assert
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(testStateEntry1, result.get(0));
        
        verify(amphiStateEntryRepository, times(1)).findAll();
    }
}
