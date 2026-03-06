package com.dedalus.amphi_integration.service.impl;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.dedalus.amphi_integration.model.amphi.Symbol;
import com.dedalus.amphi_integration.repository.AmphiSymbolRepository;
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
class AmphiSymbolServiceImplTest {

    @Mock
    private AmphiSymbolRepository amphiSymbolRepository;

    @InjectMocks
    private AmphiSymbolServiceImpl amphiSymbolService;

    private Symbol testSymbol1;
    private Symbol testSymbol2;

    @BeforeEach
    void setUp() {
        testSymbol1 = Symbol.builder()
                .id("symbol-1")
                .unitId("unit-1")
                .description("Test symbol 1")
                .mapitemtype(0)
                .heading(90)
                .is_deleted(false)
                .state("active")
                .build();

        testSymbol2 = Symbol.builder()
                .id("symbol-2")
                .unitId("unit-2")
                .description("Test symbol 2")
                .mapitemtype(1)
                .heading(180)
                .is_deleted(false)
                .state("inactive")
                .build();
    }

    // ========== updateSymbols Tests ==========

    @Test
    void updateSymbols_WithValidArray_DeletesAllAndSavesSymbols() {
        // Arrange
        Symbol[] symbols = {testSymbol1, testSymbol2};
        when(amphiSymbolRepository.save(any(Symbol.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        Symbol[] result = amphiSymbolService.updateSymbols(symbols);

        // Assert
        assertNotNull(result);
        assertEquals(2, result.length);
        assertEquals(testSymbol1, result[0]);
        assertEquals(testSymbol2, result[1]);
        
        verify(amphiSymbolRepository, times(1)).deleteAll();
        verify(amphiSymbolRepository, times(2)).save(any(Symbol.class));
        verify(amphiSymbolRepository).save(testSymbol1);
        verify(amphiSymbolRepository).save(testSymbol2);
    }

    @Test
    void updateSymbols_WithEmptyArray_DeletesAllAndReturnsEmptyArray() {
        // Arrange
        Symbol[] emptySymbols = new Symbol[0];

        // Act
        Symbol[] result = amphiSymbolService.updateSymbols(emptySymbols);

        // Assert
        assertNotNull(result);
        assertEquals(0, result.length);
        
        verify(amphiSymbolRepository, times(1)).deleteAll();
        verify(amphiSymbolRepository, never()).save(any(Symbol.class));
    }

    @Test
    void updateSymbols_WithSingleSymbol_SavesSuccessfully() {
        // Arrange
        Symbol[] singleSymbol = {testSymbol1};
        when(amphiSymbolRepository.save(any(Symbol.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        Symbol[] result = amphiSymbolService.updateSymbols(singleSymbol);

        // Assert
        assertNotNull(result);
        assertEquals(1, result.length);
        assertEquals(testSymbol1, result[0]);
        
        verify(amphiSymbolRepository, times(1)).deleteAll();
        verify(amphiSymbolRepository, times(1)).save(testSymbol1);
    }

    @Test
    void updateSymbols_VerifiesDeleteAllCalledBeforeSave() {
        // Arrange
        Symbol[] symbols = {testSymbol1};
        
        // Act
        amphiSymbolService.updateSymbols(symbols);

        // Assert - verify order of operations
        var inOrder = inOrder(amphiSymbolRepository);
        inOrder.verify(amphiSymbolRepository).deleteAll();
        inOrder.verify(amphiSymbolRepository).save(testSymbol1);
    }

    @Test
    void updateSymbols_WithDeletedSymbol_StillSavesSymbol() {
        // Arrange
        Symbol deletedSymbol = Symbol.builder()
                .id("symbol-deleted")
                .unitId("unit-1")
                .description("Deleted symbol")
                .mapitemtype(0)
                .heading(0)
                .is_deleted(true)
                .build();
        
        Symbol[] symbols = {deletedSymbol};
        when(amphiSymbolRepository.save(any(Symbol.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        Symbol[] result = amphiSymbolService.updateSymbols(symbols);

        // Assert
        assertNotNull(result);
        assertEquals(1, result.length);
        assertTrue(result[0].is_deleted());
        
        verify(amphiSymbolRepository, times(1)).save(deletedSymbol);
    }

    // ========== getAllSymbols Tests ==========

    @Test
    void getAllSymbols_WhenSymbolsExist_ReturnsAllSymbols() {
        // Arrange
        List<Symbol> expectedSymbols = Arrays.asList(testSymbol1, testSymbol2);
        when(amphiSymbolRepository.findAll())
                .thenReturn(expectedSymbols);

        // Act
        List<Symbol> result = amphiSymbolService.getAllSymbols();

        // Assert
        assertNotNull(result);
        assertEquals(2, result.size());
        assertEquals(testSymbol1, result.get(0));
        assertEquals(testSymbol2, result.get(1));
        
        verify(amphiSymbolRepository, times(1)).findAll();
    }

    @Test
    void getAllSymbols_WhenNoSymbolsExist_ReturnsEmptyList() {
        // Arrange
        when(amphiSymbolRepository.findAll())
                .thenReturn(Collections.emptyList());

        // Act
        List<Symbol> result = amphiSymbolService.getAllSymbols();

        // Assert
        assertNotNull(result);
        assertTrue(result.isEmpty());
        
        verify(amphiSymbolRepository, times(1)).findAll();
    }

    @Test
    void getAllSymbols_WithSingleSymbol_ReturnsSingleItemList() {
        // Arrange
        List<Symbol> singleSymbol = Collections.singletonList(testSymbol1);
        when(amphiSymbolRepository.findAll())
                .thenReturn(singleSymbol);

        // Act
        List<Symbol> result = amphiSymbolService.getAllSymbols();

        // Assert
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(testSymbol1, result.get(0));
        
        verify(amphiSymbolRepository, times(1)).findAll();
    }

    @Test
    void getAllSymbols_IncludesDeletedSymbols() {
        // Arrange
        Symbol deletedSymbol = Symbol.builder()
                .id("symbol-deleted")
                .is_deleted(true)
                .build();
        
        List<Symbol> symbols = Arrays.asList(testSymbol1, deletedSymbol);
        when(amphiSymbolRepository.findAll())
                .thenReturn(symbols);

        // Act
        List<Symbol> result = amphiSymbolService.getAllSymbols();

        // Assert
        assertNotNull(result);
        assertEquals(2, result.size());
        assertTrue(result.stream().anyMatch(s -> s.is_deleted()));
        
        verify(amphiSymbolRepository, times(1)).findAll();
    }
}
