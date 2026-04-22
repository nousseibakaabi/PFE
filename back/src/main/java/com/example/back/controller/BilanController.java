package com.example.back.controller;

import com.example.back.payload.response.BilanDTO;
import com.example.back.service.BilanService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/bilan")
@RequiredArgsConstructor
@Slf4j
@PreAuthorize("hasAnyRole('ADMIN', 'COMMERCIAL_METIER', 'DECIDEUR')")
public class BilanController {

    private final BilanService bilanService;

    /**
     * Get bilan for all factures only
     */
    @GetMapping("/factures")
    public ResponseEntity<?> getFacturesBilan(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        try {
            BilanDTO bilan = bilanService.getFacturesBilan(startDate, endDate);
            return ResponseEntity.ok(createSuccessResponse(bilan));
        } catch (Exception e) {
            log.error("Error generating factures bilan: {}", e.getMessage(), e);
            return ResponseEntity.badRequest().body(createErrorResponse(e.getMessage()));
        }
    }

    /**
     * Get bilan for all conventions only
     */
    @GetMapping("/conventions")
    public ResponseEntity<?> getConventionsBilan(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(defaultValue = "false") boolean includeOldVersions) {
        try {
            BilanDTO bilan = bilanService.getConventionsBilan(startDate, endDate, includeOldVersions);
            return ResponseEntity.ok(createSuccessResponse(bilan));
        } catch (Exception e) {
            log.error("Error generating conventions bilan: {}", e.getMessage(), e);
            return ResponseEntity.badRequest().body(createErrorResponse(e.getMessage()));
        }
    }

    /**
     * Get combined bilan (conventions + factures)
     */
    @GetMapping("/combined")
    public ResponseEntity<?> getCombinedBilan(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(defaultValue = "false") boolean includeOldVersions) {
        try {
            BilanDTO bilan = bilanService.getCombinedBilan(startDate, endDate, includeOldVersions);
            return ResponseEntity.ok(createSuccessResponse(bilan));
        } catch (Exception e) {
            log.error("Error generating combined bilan: {}", e.getMessage(), e);
            return ResponseEntity.badRequest().body(createErrorResponse(e.getMessage()));
        }
    }

    /**
     * Get bilan for a specific convention (includes its factures)
     */
    @GetMapping("/convention/{conventionId}")
    public ResponseEntity<?> getConventionBilan(@PathVariable Long conventionId) {
        try {
            BilanDTO bilan = bilanService.getConventionBilan(conventionId);
            return ResponseEntity.ok(createSuccessResponse(bilan));
        } catch (Exception e) {
            log.error("Error generating convention bilan: {}", e.getMessage(), e);
            return ResponseEntity.badRequest().body(createErrorResponse(e.getMessage()));
        }
    }

    /**
     * Get bilan by month
     */
    @GetMapping("/month")
    public ResponseEntity<?> getBilanByMonth(
            @RequestParam int year,
            @RequestParam int month,
            @RequestParam(defaultValue = "combined") String type,
            @RequestParam(defaultValue = "false") boolean includeOldVersions) {
        try {
            BilanDTO bilan = bilanService.getBilanByMonth(year, month, type, includeOldVersions);
            return ResponseEntity.ok(createSuccessResponse(bilan));
        } catch (Exception e) {
            log.error("Error generating monthly bilan: {}", e.getMessage(), e);
            return ResponseEntity.badRequest().body(createErrorResponse(e.getMessage()));
        }
    }

    /**
     * Get bilan by year
     */
    @GetMapping("/year")
    public ResponseEntity<?> getBilanByYear(
            @RequestParam int year,
            @RequestParam(defaultValue = "combined") String type,
            @RequestParam(defaultValue = "false") boolean includeOldVersions) {
        try {
            BilanDTO bilan = bilanService.getBilanByYear(year, type, includeOldVersions);
            return ResponseEntity.ok(createSuccessResponse(bilan));
        } catch (Exception e) {
            log.error("Error generating yearly bilan: {}", e.getMessage(), e);
            return ResponseEntity.badRequest().body(createErrorResponse(e.getMessage()));
        }
    }

    private Map<String, Object> createSuccessResponse(Object data) {
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("data", data);
        return response;
    }

    private Map<String, Object> createErrorResponse(String message) {
        Map<String, Object> response = new HashMap<>();
        response.put("success", false);
        response.put("message", message);
        return response;
    }
}