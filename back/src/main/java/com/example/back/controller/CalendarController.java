package com.example.back.controller;

import com.example.back.entity.Facture;
import com.example.back.entity.Project;
import com.example.back.payload.response.CalendarEventDTO;
import com.example.back.repository.FactureRepository;
import com.example.back.repository.ProjectRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/calendar")
@CrossOrigin(origins = "*")
@Slf4j
public class CalendarController {

    @Autowired
    private FactureRepository factureRepository;

    @Autowired
    private ProjectRepository projectRepository;

    @GetMapping("/invoices")
    @PreAuthorize("hasAnyRole('COMMERCIAL_METIER')")
    public ResponseEntity<?> getInvoiceEvents(
            @RequestParam(required = false) LocalDate start,
            @RequestParam(required = false) LocalDate end) {
        try {
            List<Facture> invoices;

            if (start != null && end != null) {
                // Get invoices within date range
                invoices = factureRepository.findByDateEcheanceBetween(start, end);
            } else {
                // Get all invoices
                invoices = factureRepository.findAll();
            }

            List<CalendarEventDTO> events = invoices.stream()
                    .map(this::convertFactureToEvent)
                    .collect(Collectors.toList());

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", events);
            response.put("count", events.size());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error fetching invoice calendar events: ", e);
            return ResponseEntity.badRequest().body(createErrorResponse("Failed to fetch invoice events"));
        }
    }

    @GetMapping("/upcoming-invoices")
    @PreAuthorize("hasAnyRole('COMMERCIAL_METIER')")
    public ResponseEntity<?> getUpcomingInvoices() {
        try {
            LocalDate today = LocalDate.now();
            LocalDate nextMonth = today.plusMonths(1);

            List<Facture> upcomingInvoices = factureRepository
                    .findByDateEcheanceBetweenAndStatutPaiementNot(today, nextMonth, "PAYE");

            List<CalendarEventDTO> events = upcomingInvoices.stream()
                    .map(this::convertFactureToEvent)
                    .collect(Collectors.toList());

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", events);
            response.put("count", events.size());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error fetching upcoming invoices: ", e);
            return ResponseEntity.badRequest().body(createErrorResponse("Failed to fetch upcoming invoices"));
        }
    }

    @GetMapping("/overdue-invoices")
    @PreAuthorize("hasAnyRole('COMMERCIAL_METIER')")
    public ResponseEntity<?> getOverdueInvoices() {
        try {
            LocalDate today = LocalDate.now();
            List<Facture> overdueInvoices = factureRepository.findFacturesEnRetard(today);

            List<CalendarEventDTO> events = overdueInvoices.stream()
                    .map(this::convertFactureToEvent)
                    .collect(Collectors.toList());

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", events);
            response.put("count", events.size());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error fetching overdue invoices: ", e);
            return ResponseEntity.badRequest().body(createErrorResponse("Failed to fetch overdue invoices"));
        }
    }

    @GetMapping("/stats")
    @PreAuthorize("hasAnyRole('COMMERCIAL_METIER')")
    public ResponseEntity<?> getCalendarStats() {
        try {
            LocalDate today = LocalDate.now();
            LocalDate startOfMonth = today.withDayOfMonth(1);
            LocalDate endOfMonth = today.withDayOfMonth(today.lengthOfMonth());

            // Today's invoices
            List<Facture> todayInvoices = factureRepository.findByDateEcheance(today);

            // This week's invoices (next 7 days)
            LocalDate endOfWeek = today.plusDays(7);
            List<Facture> weekInvoices = factureRepository.findByDateEcheanceBetween(today, endOfWeek);

            // This month's invoices
            List<Facture> monthInvoices = factureRepository.findByDateEcheanceBetween(startOfMonth, endOfMonth);

            // Overdue invoices
            List<Facture> overdueInvoices = factureRepository.findFacturesEnRetard(today);

            Map<String, Object> stats = new HashMap<>();
            stats.put("today", todayInvoices.size());
            stats.put("thisWeek", weekInvoices.size());
            stats.put("thisMonth", monthInvoices.size());
            stats.put("overdue", overdueInvoices.size());

            // Calculate amounts
            BigDecimal totalOverdueAmount = overdueInvoices.stream()
                    .map(Facture::getMontantTTC)
                    .filter(amount -> amount != null)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            stats.put("overdueAmount", totalOverdueAmount);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", stats);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error fetching calendar stats: ", e);
            return ResponseEntity.badRequest().body(createErrorResponse("Failed to fetch calendar stats"));
        }
    }

    @GetMapping("/events")
    @PreAuthorize("hasAnyRole('COMMERCIAL_METIER')")
    public ResponseEntity<?> getAllEvents(
            @RequestParam(required = false) LocalDate start,
            @RequestParam(required = false) LocalDate end) {
        try {
            List<CalendarEventDTO> allEvents = new ArrayList<>();

            // Get invoice events
            List<Facture> invoices;
            if (start != null && end != null) {
                invoices = factureRepository.findByDateEcheanceBetween(start, end);
            } else {
                invoices = factureRepository.findAll();
            }

            List<CalendarEventDTO> invoiceEvents = invoices.stream()
                    .map(this::convertFactureToEvent)
                    .collect(Collectors.toList());
            allEvents.addAll(invoiceEvents);

            // Get project events (we'll implement this later)
            // List<Project> projects = ...;
            // List<CalendarEventDTO> projectEvents = projects.stream()
            //         .map(this::convertProjectToEvent)
            //         .collect(Collectors.toList());
            // allEvents.addAll(projectEvents);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", allEvents);
            response.put("count", allEvents.size());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error fetching all calendar events: ", e);
            return ResponseEntity.badRequest().body(createErrorResponse("Failed to fetch calendar events"));
        }
    }


    private CalendarEventDTO convertFactureToEvent(Facture facture) {
        CalendarEventDTO event = new CalendarEventDTO();
        event.setId(facture.getId());
        event.setTitle("Facture #" + facture.getNumeroFacture());

        // Debug logging
        log.info("📅 Converting facture: ID={}, Numero={}, Status={}, DueDate={}, IsOverdue={}",
                facture.getId(),
                facture.getNumeroFacture(),
                facture.getStatutPaiement(),
                facture.getDateEcheance(),
                facture.isEnRetard());

        // Set start date (due date)
        if (facture.getDateEcheance() != null) {
            event.setStart(LocalDateTime.of(facture.getDateEcheance(), LocalTime.of(9, 0)));
        } else {
            event.setStart(LocalDateTime.now());
        }

        // Set end date (same as start for one-day events)
        event.setEnd(LocalDateTime.of(
                facture.getDateEcheance() != null ? facture.getDateEcheance() : LocalDate.now(),
                LocalTime.of(17, 0)));

        event.setAllDay(false);
        event.setType("INVOICE");

        // Set color based on status - add debug logging
        String color = getInvoiceColor(facture);
        log.info("📅 Invoice color determined: {}", color);
        event.setColor(color);

        // Set extended properties
        Map<String, Object> extendedProps = new HashMap<>();
        extendedProps.put("status", facture.getStatutPaiement());
        extendedProps.put("amount", facture.getMontantTTC());
        extendedProps.put("isOverdue", facture.isEnRetard());

        // Safely handle convention
        if (facture.getConvention() != null) {
            String clientName = "N/A";
            if (facture.getConvention().getProject() != null) {
                clientName = facture.getConvention().getProject().getClientName() != null ?
                        facture.getConvention().getProject().getClientName() : "N/A";
            }
            extendedProps.put("clientName", clientName);
            extendedProps.put("conventionId", facture.getConvention().getId());
            extendedProps.put("conventionReference", facture.getConvention().getReferenceConvention() != null ?
                    facture.getConvention().getReferenceConvention() : "N/A");
        } else {
            extendedProps.put("clientName", "N/A");
            extendedProps.put("conventionId", null);
            extendedProps.put("conventionReference", "N/A");
        }

        extendedProps.put("notes", facture.getNotes());

        event.setExtendedProps(extendedProps);

        return event;
    }

    private String getInvoiceColor(Facture facture) {
        if ("PAYE".equals(facture.getStatutPaiement())) {
            return "#10B981"; // Green for paid
        }
        else if (facture.isEnRetard() || "EN_RETARD".equals(facture.getStatutPaiement())) {
            return "#EF4444"; // Red for any overdue/late invoice
        }
        else if ("NON_PAYE".equals(facture.getStatutPaiement())) {
            return "#F59E0B"; // Yellow for unpaid but not yet due
        }
        else {
            return "#6B7280"; // Gray default
        }
    }


    private Map<String, Object> createErrorResponse(String message) {
        Map<String, Object> response = new HashMap<>();
        response.put("success", false);
        response.put("message", message);
        return response;
    }
}