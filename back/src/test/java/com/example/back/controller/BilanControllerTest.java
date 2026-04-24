package com.example.back.controller;

import com.example.back.payload.response.BilanDTO;
import com.example.back.service.BilanService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.time.LocalDate;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BilanControllerTest {

    @Mock
    private BilanService bilanService;

    @InjectMocks
    private BilanController controller;

    @Test
    void getFacturesBilan_returnsSuccessPayload() {
        BilanDTO bilanDTO = new BilanDTO();
        bilanDTO.setTitle("Factures");
        when(bilanService.getFacturesBilan(LocalDate.of(2026, 1, 1), LocalDate.of(2026, 1, 31)))
                .thenReturn(bilanDTO);

        ResponseEntity<?> response = controller.getFacturesBilan(LocalDate.of(2026, 1, 1), LocalDate.of(2026, 1, 31));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat((Map<String, Object>) response.getBody())
                .containsEntry("success", true)
                .containsEntry("data", bilanDTO);
    }

    @Test
    void getCombinedBilan_whenServiceFails_returnsBadRequest() {
        when(bilanService.getCombinedBilan(LocalDate.of(2026, 1, 1), LocalDate.of(2026, 1, 31), true))
                .thenThrow(new RuntimeException("boom"));

        ResponseEntity<?> response = controller.getCombinedBilan(
                LocalDate.of(2026, 1, 1),
                LocalDate.of(2026, 1, 31),
                true
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat((Map<String, Object>) response.getBody())
                .containsEntry("success", false)
                .containsEntry("message", "boom");
    }

    @Test
    void getConventionsBilan_returnsSuccessPayload() {
        BilanDTO bilanDTO = new BilanDTO();
        bilanDTO.setTitle("Conventions");
        when(bilanService.getConventionsBilan(LocalDate.of(2026, 2, 1), LocalDate.of(2026, 2, 28), false))
                .thenReturn(bilanDTO);

        ResponseEntity<?> response = controller.getConventionsBilan(
                LocalDate.of(2026, 2, 1),
                LocalDate.of(2026, 2, 28),
                false
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat((Map<String, Object>) response.getBody()).containsEntry("data", bilanDTO);
    }

    @Test
    void getConventionBilan_returnsSuccessPayload() {
        BilanDTO bilanDTO = new BilanDTO();
        when(bilanService.getConventionBilan(9L)).thenReturn(bilanDTO);

        ResponseEntity<?> response = controller.getConventionBilan(9L);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat((Map<String, Object>) response.getBody()).containsEntry("data", bilanDTO);
    }

    @Test
    void getBilanByMonth_returnsSuccessPayload() {
        BilanDTO bilanDTO = new BilanDTO();
        when(bilanService.getBilanByMonth(2026, 4, "combined", true)).thenReturn(bilanDTO);

        ResponseEntity<?> response = controller.getBilanByMonth(2026, 4, "combined", true);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat((Map<String, Object>) response.getBody()).containsEntry("data", bilanDTO);
    }

    @Test
    void getBilanByYear_returnsSuccessPayload() {
        BilanDTO bilanDTO = new BilanDTO();
        when(bilanService.getBilanByYear(2026, "factures", false)).thenReturn(bilanDTO);

        ResponseEntity<?> response = controller.getBilanByYear(2026, "factures", false);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat((Map<String, Object>) response.getBody()).containsEntry("data", bilanDTO);
    }
}
