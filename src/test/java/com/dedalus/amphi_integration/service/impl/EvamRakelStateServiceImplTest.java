package com.dedalus.amphi_integration.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import com.dedalus.amphi_integration.model.evam.RakelState;
import com.dedalus.amphi_integration.repository.EvamRakelStateRepository;
import com.google.gson.Gson;

@ExtendWith(MockitoExtension.class)
class EvamRakelStateServiceImplTest {

    @Mock
    private EvamRakelStateRepository evamRakelStateRepository;

    @InjectMocks
    private EvamRakelStateServiceImpl evamRakelStateService;

        @Spy
    private final Gson gson = new Gson();

    @Test
    void updateRakelState_WithNullMsisdn_PreservesStoredMsisdn() {
        RakelState existing = RakelState.builder()
                .id("1")
                .unitId("1")
                .msisdn("3393090")
                .issi("old-issi")
                .gssi("old-gssi")
                .build();
        RakelState incoming = RakelState.builder()
                .issi("new-issi")
                .gssi("new-gssi")
                .build();

        when(evamRakelStateRepository.findById("1")).thenReturn(Optional.of(existing));
        when(evamRakelStateRepository.save(existing)).thenReturn(existing);

        RakelState result = evamRakelStateService.updateRakelState(gson.toJson(incoming));

        assertNotNull(result);
        assertEquals("3393090", result.getMsisdn());
        assertEquals("new-issi", result.getIssi());
        assertEquals("new-gssi", result.getGssi());

        verify(evamRakelStateRepository).save(existing);
    }

    @Test
    void updateRakelState_WithLegacyMsisdn_RewritesMappedValue() {
        RakelState existing = RakelState.builder()
                .id("1")
                .unitId("1")
                .msisdn("old")
                .build();
        RakelState incoming = RakelState.builder()
                .msisdn("0000567")
                .issi("issi")
                .gssi("gssi")
                .build();

        when(evamRakelStateRepository.findById("1")).thenReturn(Optional.of(existing));
        when(evamRakelStateRepository.save(existing)).thenReturn(existing);

        RakelState result = evamRakelStateService.updateRakelState(gson.toJson(incoming));

        assertEquals("3393090", result.getMsisdn());
        assertEquals("issi", result.getIssi());
        assertEquals("gssi", result.getGssi());

        verify(evamRakelStateRepository).save(existing);
    }
}