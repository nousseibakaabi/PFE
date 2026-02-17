package com.example.back.controller;

import com.example.back.payload.response.HistoryResponse;
import com.example.back.service.HistoryService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/history")
@Slf4j
public class HistoryController {

    @Autowired
    private HistoryService historyService;

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'DECIDEUR', 'COMMERCIAL_METIER', 'CHEF_PROJET')")
    public ResponseEntity<?> getAllHistory() {
        try {
            List<HistoryResponse> history = historyService.getAllHistory();

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", history);
            response.put("count", history.size());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error fetching history: ", e);
            return ResponseEntity.badRequest().body(createErrorResponse(e.getMessage()));
        }
    }

    @GetMapping("/recent")
    @PreAuthorize("hasAnyRole('ADMIN', 'DECIDEUR', 'COMMERCIAL_METIER', 'CHEF_PROJET')")
    public ResponseEntity<?> getRecentHistory(@RequestParam(defaultValue = "50") int limit) {
        try {
            List<HistoryResponse> history = historyService.getRecentHistory(limit);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", history);
            response.put("count", history.size());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error fetching recent history: ", e);
            return ResponseEntity.badRequest().body(createErrorResponse(e.getMessage()));
        }
    }

    @GetMapping("/user/{userId}")
    @PreAuthorize("hasAnyRole('ADMIN')")
    public ResponseEntity<?> getHistoryByUser(@PathVariable Long userId) {
        try {
            List<HistoryResponse> history = historyService.getHistoryByUser(userId);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", history);
            response.put("count", history.size());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error fetching user history: ", e);
            return ResponseEntity.badRequest().body(createErrorResponse(e.getMessage()));
        }
    }

    @GetMapping("/entity/{entityType}/{entityId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'DECIDEUR', 'COMMERCIAL_METIER', 'CHEF_PROJET')")
    public ResponseEntity<?> getHistoryByEntity(
            @PathVariable String entityType,
            @PathVariable Long entityId) {
        try {
            List<HistoryResponse> history = historyService.getHistoryByEntity(entityType, entityId);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", history);
            response.put("count", history.size());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error fetching entity history: ", e);
            return ResponseEntity.badRequest().body(createErrorResponse(e.getMessage()));
        }
    }

    @GetMapping("/date/{date}")
    @PreAuthorize("hasAnyRole('ADMIN', 'DECIDEUR', 'COMMERCIAL_METIER', 'CHEF_PROJET')")
    public ResponseEntity<?> getHistoryByDate(
            @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        try {
            List<HistoryResponse> history = historyService.getHistoryByDate(date);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", history);
            response.put("date", date.toString());
            response.put("count", history.size());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error fetching history by date: ", e);
            return ResponseEntity.badRequest().body(createErrorResponse(e.getMessage()));
        }
    }

    @GetMapping("/grouped-by-day")
    @PreAuthorize("hasAnyRole('ADMIN', 'DECIDEUR', 'COMMERCIAL_METIER', 'CHEF_PROJET')")
    public ResponseEntity<?> getHistoryGroupedByDay() {
        try {
            Map<LocalDate, List<HistoryResponse>> grouped = historyService.getHistoryGroupedByDay();

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", grouped);
            response.put("totalDays", grouped.size());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error fetching grouped history: ", e);
            return ResponseEntity.badRequest().body(createErrorResponse(e.getMessage()));
        }
    }

    @GetMapping("/search")
    @PreAuthorize("hasAnyRole('ADMIN', 'DECIDEUR', 'COMMERCIAL_METIER', 'CHEF_PROJET')")
    public ResponseEntity<?> searchHistory(
            @RequestParam(required = false) String entityType,
            @RequestParam(required = false) String actionType,
            @RequestParam(required = false) Long userId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        try {
            List<HistoryResponse> history = historyService.searchHistory(entityType, actionType, userId, date);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", history);
            response.put("count", history.size());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error searching history: ", e);
            return ResponseEntity.badRequest().body(createErrorResponse(e.getMessage()));
        }
    }

    @GetMapping("/stats")
    @PreAuthorize("hasAnyRole('ADMIN', 'DECIDEUR', 'COMMERCIAL_METIER', 'CHEF_PROJET')")
    public ResponseEntity<?> getHistoryStats() {
        try {
            List<HistoryResponse> allHistory = historyService.getAllHistory();

            Map<String, Long> actionTypeStats = new HashMap<>();
            Map<String, Long> entityTypeStats = new HashMap<>();
            Map<String, Long> userStats = new HashMap<>();

            for (HistoryResponse h : allHistory) {
                // Action type stats
                actionTypeStats.put(h.getActionTypeLabel(),
                        actionTypeStats.getOrDefault(h.getActionTypeLabel(), 0L) + 1);

                // Entity type stats
                entityTypeStats.put(h.getEntityTypeLabel(),
                        entityTypeStats.getOrDefault(h.getEntityTypeLabel(), 0L) + 1);

                // User stats
                if (h.getUserFullName() != null) {
                    userStats.put(h.getUserFullName(),
                            userStats.getOrDefault(h.getUserFullName(), 0L) + 1);
                }
            }

            Map<String, Object> stats = new HashMap<>();
            stats.put("totalEntries", (long) allHistory.size());
            stats.put("byActionType", actionTypeStats);
            stats.put("byEntityType", entityTypeStats);
            stats.put("byUser", userStats);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", stats);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error fetching history stats: ", e);
            return ResponseEntity.badRequest().body(createErrorResponse(e.getMessage()));
        }
    }

    /**
     * TEST ENDPOINT - Remove in production
     */
    @PostMapping("/test/convention/{conventionId}")
    @PreAuthorize("hasAnyRole('COMMERCIAL_METIER')")
    public ResponseEntity<?> testConventionHistory(@PathVariable Long conventionId) {
        try {
            // This is just for testing - you'll need to inject ConventionRepository
            // For now, return a test message
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Test endpoint - History should be working now");
            response.put("note", "Try creating a new convention to see history entries");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Test history failed: {}", e.getMessage(), e);
            return ResponseEntity.badRequest().body(createErrorResponse(e.getMessage()));
        }
    }

    private Map<String, Object> createErrorResponse(String message) {
        Map<String, Object> response = new HashMap<>();
        response.put("success", false);
        response.put("message", message);
        return response;
    }
}