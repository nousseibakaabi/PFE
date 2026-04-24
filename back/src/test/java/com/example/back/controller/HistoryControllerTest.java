package com.example.back.controller;

import com.example.back.payload.response.HistoryResponse;
import com.example.back.service.HistoryService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.time.LocalDateTime;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class HistoryControllerTest {

    @Mock
    private HistoryService historyService;

    @InjectMocks
    private HistoryController controller;

    @Test
    void getAllHistory_returnsEntriesAndCount() {
        HistoryResponse entry = new HistoryResponse();
        entry.setId(1L);
        entry.setActionTypeLabel("CREATE");

        when(historyService.getAllHistory()).thenReturn(List.of(entry));

        ResponseEntity<?> response = controller.getAllHistory();

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat((Map<String, Object>) response.getBody())
                .containsEntry("success", true)
                .containsEntry("count", 1);
    }

    @Test
    void getHistoryStats_aggregatesActionEntityAndUsers() {
        HistoryResponse entry = new HistoryResponse();
        entry.setActionTypeLabel("CREATE");
        entry.setEntityTypeLabel("CONVENTION");
        entry.setUserFullName("Alice Doe");
        entry.setTimestamp(LocalDateTime.now());

        when(historyService.getAllHistory()).thenReturn(List.of(entry));

        ResponseEntity<?> response = controller.getHistoryStats();

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        Map<String, Object> body = (Map<String, Object>) response.getBody();
        Map<String, Object> stats = (Map<String, Object>) body.get("data");
        assertThat(body).containsEntry("success", true);
        assertThat(stats).containsEntry("totalEntries", 1L);
    }

    @Test
    void getRecentHistory_returnsLimitedEntries() {
        when(historyService.getRecentHistory(5)).thenReturn(List.of(new HistoryResponse()));

        ResponseEntity<?> response = controller.getRecentHistory(5);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat((Map<String, Object>) response.getBody()).containsEntry("count", 1);
    }

    @Test
    void getHistoryByUser_returnsEntries() {
        when(historyService.getHistoryByUser(4L)).thenReturn(List.of(new HistoryResponse()));

        ResponseEntity<?> response = controller.getHistoryByUser(4L);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat((Map<String, Object>) response.getBody()).containsEntry("count", 1);
    }

    @Test
    void getHistoryByEntity_returnsEntries() {
        when(historyService.getHistoryByEntity("CONVENTION", 9L)).thenReturn(List.of(new HistoryResponse()));

        ResponseEntity<?> response = controller.getHistoryByEntity("CONVENTION", 9L);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat((Map<String, Object>) response.getBody()).containsEntry("count", 1);
    }

    @Test
    void getHistoryByDate_returnsDateEcho() {
        LocalDate date = LocalDate.of(2026, 4, 1);
        when(historyService.getHistoryByDate(date)).thenReturn(List.of(new HistoryResponse()));

        ResponseEntity<?> response = controller.getHistoryByDate(date);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat((Map<String, Object>) response.getBody()).containsEntry("date", "2026-04-01");
    }

    @Test
    void getHistoryGroupedByDay_returnsTotalDays() {
        when(historyService.getHistoryGroupedByDay()).thenReturn(Map.of(LocalDate.of(2026, 4, 1), List.of(new HistoryResponse())));

        ResponseEntity<?> response = controller.getHistoryGroupedByDay();

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat((Map<String, Object>) response.getBody()).containsEntry("totalDays", 1);
    }

    @Test
    void searchHistory_returnsEntries() {
        when(historyService.searchHistory("APP", "CREATE", 2L, LocalDate.of(2026, 4, 1)))
                .thenReturn(List.of(new HistoryResponse()));

        ResponseEntity<?> response = controller.searchHistory("APP", "CREATE", 2L, LocalDate.of(2026, 4, 1));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat((Map<String, Object>) response.getBody()).containsEntry("count", 1);
    }
}
