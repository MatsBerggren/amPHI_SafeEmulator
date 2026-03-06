package com.dedalus.amphi_integration.service.impl;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.dedalus.amphi_integration.model.amphi.Destination;
import com.dedalus.amphi_integration.repository.AmphiDestinationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

@ExtendWith(MockitoExtension.class)
class AmphiDestinationServiceImplTest {

    @Mock
    private AmphiDestinationRepository amphiDestinationRepository;

    @InjectMocks
    private AmphiDestinationServiceImpl amphiDestinationService;

    private Destination testDestination1;
    private Destination testDestination2;

    @BeforeEach
    void setUp() {
        testDestination1 = Destination.builder()
                .abbreviation("TH")
                .name("Test Hospital")
                .type("AkutSjukhus")
                .build();

        testDestination2 = Destination.builder()
                .abbreviation("MC")
                .name("Medical Center")
                .type("Vårdcentral")
                .build();
    }

    // ========== updateDestinations Tests ==========

    @Test
    void updateDestinations_WithValidArray_DeletesAllAndSavesDestinations() {
        // Arrange
        Destination[] destinations = {testDestination1, testDestination2};
        when(amphiDestinationRepository.save(any(Destination.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        Destination[] result = amphiDestinationService.updateDestinations(destinations);

        // Assert
        assertNotNull(result);
        assertEquals(2, result.length);
        assertEquals(testDestination1, result[0]);
        assertEquals(testDestination2, result[1]);
        
        verify(amphiDestinationRepository, times(1)).deleteAll();
        verify(amphiDestinationRepository, times(2)).save(any(Destination.class));
        verify(amphiDestinationRepository).save(testDestination1);
        verify(amphiDestinationRepository).save(testDestination2);
    }

    @Test
    void updateDestinations_WithEmptyArray_DeletesAllAndReturnsEmptyArray() {
        // Arrange
        Destination[] emptyDestinations = new Destination[0];

        // Act
        Destination[] result = amphiDestinationService.updateDestinations(emptyDestinations);

        // Assert
        assertNotNull(result);
        assertEquals(0, result.length);
        
        verify(amphiDestinationRepository, times(1)).deleteAll();
        verify(amphiDestinationRepository, never()).save(any(Destination.class));
    }

    @Test
    void updateDestinations_WithSingleDestination_SavesSuccessfully() {
        // Arrange
        Destination[] singleDestination = {testDestination1};
        when(amphiDestinationRepository.save(any(Destination.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        Destination[] result = amphiDestinationService.updateDestinations(singleDestination);

        // Assert
        assertNotNull(result);
        assertEquals(1, result.length);
        assertEquals(testDestination1, result[0]);
        
        verify(amphiDestinationRepository, times(1)).deleteAll();
        verify(amphiDestinationRepository, times(1)).save(testDestination1);
    }

    @Test
    void updateDestinations_VerifiesDeleteAllCalledBeforeSave() {
        // Arrange
        Destination[] destinations = {testDestination1};
        
        // Act
        amphiDestinationService.updateDestinations(destinations);

        // Assert - verify order of operations
        var inOrder = inOrder(amphiDestinationRepository);
        inOrder.verify(amphiDestinationRepository).deleteAll();
        inOrder.verify(amphiDestinationRepository).save(testDestination1);
    }

    // ========== getByNameAndType Tests ==========

    @Test
    void getByNameAndType_WhenDestinationExists_ReturnsDestination() {
        // Arrange
        String name = "Test Hospital";
        String type = "AkutSjukhus";
        when(amphiDestinationRepository.findByNameAndType(name, type))
                .thenReturn(testDestination1);

        // Act
        Destination result = amphiDestinationService.getByNameAndType(name, type);

        // Assert
        assertNotNull(result);
        assertEquals(testDestination1, result);
        assertEquals(name, result.getName());
        assertEquals(type, result.getType());
        
        verify(amphiDestinationRepository, times(1)).findByNameAndType(name, type);
    }

    @Test
    void getByNameAndType_WhenDestinationDoesNotExist_ReturnsNull() {
        // Arrange
        String name = "Nonexistent Hospital";
        String type = "AkutSjukhus";
        when(amphiDestinationRepository.findByNameAndType(name, type))
                .thenReturn(null);

        // Act
        Destination result = amphiDestinationService.getByNameAndType(name, type);

        // Assert
        assertNull(result);
        
        verify(amphiDestinationRepository, times(1)).findByNameAndType(name, type);
    }

    @Test
    void getByNameAndType_WithNullName_DelegatesToRepository() {
        // Arrange
        when(amphiDestinationRepository.findByNameAndType(null, "AkutSjukhus"))
                .thenReturn(null);

        // Act
        Destination result = amphiDestinationService.getByNameAndType(null, "AkutSjukhus");

        // Assert
        assertNull(result);
        
        verify(amphiDestinationRepository, times(1)).findByNameAndType(null, "AkutSjukhus");
    }

    @Test
    void getByNameAndType_WithNullType_DelegatesToRepository() {
        // Arrange
        when(amphiDestinationRepository.findByNameAndType("Test Hospital", null))
                .thenReturn(null);

        // Act
        Destination result = amphiDestinationService.getByNameAndType("Test Hospital", null);

        // Assert
        assertNull(result);
        
        verify(amphiDestinationRepository, times(1)).findByNameAndType("Test Hospital", null);
    }

    // ========== getAllDestinations Tests ==========

    @Test
    void getAllDestinations_WhenDestinationsExist_ReturnsAllDestinations() {
        // Arrange
        List<Destination> expectedDestinations = Arrays.asList(testDestination1, testDestination2);
        when(amphiDestinationRepository.findAll())
                .thenReturn(expectedDestinations);

        // Act
        List<Destination> result = amphiDestinationService.getAllDestinations();

        // Assert
        assertNotNull(result);
        assertEquals(2, result.size());
        assertEquals(testDestination1, result.get(0));
        assertEquals(testDestination2, result.get(1));
        
        verify(amphiDestinationRepository, times(1)).findAll();
    }

    @Test
    void getAllDestinations_WhenNoDestinationsExist_ReturnsEmptyList() {
        // Arrange
        when(amphiDestinationRepository.findAll())
                .thenReturn(Collections.emptyList());

        // Act
        List<Destination> result = amphiDestinationService.getAllDestinations();

        // Assert
        assertNotNull(result);
        assertTrue(result.isEmpty());
        
        verify(amphiDestinationRepository, times(1)).findAll();
    }

    @Test
    void getAllDestinations_WithSingleDestination_ReturnsSingleItemList() {
        // Arrange
        List<Destination> singleDestination = Collections.singletonList(testDestination1);
        when(amphiDestinationRepository.findAll())
                .thenReturn(singleDestination);

        // Act
        List<Destination> result = amphiDestinationService.getAllDestinations();

        // Assert
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(testDestination1, result.get(0));
        
        verify(amphiDestinationRepository, times(1)).findAll();
    }
}
