package com.example.back.controller;

import com.example.back.entity.*;
import com.example.back.payload.response.CalendarEventDTO;
import com.example.back.repository.FactureRepository;
import com.example.back.repository.ProjectRepository;
import com.example.back.service.UserContextService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
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

    @Autowired
    private UserContextService userContextService;

    // Helper method to filter invoices based on user role
    private List<Facture> getAccessibleInvoices(User currentUser, List<Facture> allInvoices) {
        List<Facture> accessibleInvoices = new ArrayList<>();

        // Admins see all invoices
        if (currentUser.getRoles().stream().anyMatch(r ->
                r.getName() == ERole.ROLE_ADMIN)) {
            return allInvoices;
        }

        // COMMERCIAL_METIER sees invoices from conventions they created
        if (currentUser.getRoles().stream().anyMatch(r ->
                r.getName() == ERole.ROLE_COMMERCIAL_METIER)) {
            accessibleInvoices = allInvoices.stream()
                    .filter(facture -> {
                        Convention convention = facture.getConvention();
                        return convention != null &&
                                convention.getCreatedBy() != null &&
                                convention.getCreatedBy().getId().equals(currentUser.getId());
                    })
                    .collect(Collectors.toList());
        }

        // CHEF_PROJET sees invoices from projects they manage
        if (currentUser.getRoles().stream().anyMatch(r ->
                r.getName() == ERole.ROLE_CHEF_PROJET)) {
            accessibleInvoices = allInvoices.stream()
                    .filter(facture -> {
                        Convention convention = facture.getConvention();
                        if (convention == null || convention.getProject() == null) {
                            return false;
                        }
                        Project project = convention.getProject();
                        return project.getChefDeProjet() != null &&
                                project.getChefDeProjet().getId().equals(currentUser.getId());
                    })
                    .collect(Collectors.toList());
        }

        // DECIDEUR sees all invoices
        if (currentUser.getRoles().stream().anyMatch(r ->
                r.getName() == ERole.ROLE_DECIDEUR)) {
            return allInvoices;
        }

        return accessibleInvoices;
    }

    // Helper method to filter invoices by date range with access control
    private List<Facture> getAccessibleInvoicesByDateRange(User currentUser, LocalDate start, LocalDate end) {
        List<Facture> allInvoices;

        if (start != null && end != null) {
            allInvoices = factureRepository.findByDateEcheanceBetween(start, end);
        } else {
            allInvoices = factureRepository.findAll();
        }

        return getAccessibleInvoices(currentUser, allInvoices);
    }

    @GetMapping("/invoices")
    @PreAuthorize("hasAnyRole('ADMIN', 'COMMERCIAL_METIER', 'CHEF_PROJET', 'DECIDEUR')")
    public ResponseEntity<?> getInvoiceEvents(
            @RequestParam(required = false) LocalDate start,
            @RequestParam(required = false) LocalDate end) {
        try {
            // Get current user
            User currentUser = userContextService.getCurrentUser();

            // Get accessible invoices based on user role
            List<Facture> accessibleInvoices = getAccessibleInvoicesByDateRange(currentUser, start, end);

            List<CalendarEventDTO> events = accessibleInvoices.stream()
                    .map(this::convertFactureToEvent)
                    .collect(Collectors.toList());

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", events);
            response.put("count", events.size());
            response.put("userRole", getHighestRole(currentUser));
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error fetching invoice calendar events: ", e);
            return ResponseEntity.badRequest().body(createErrorResponse("Failed to fetch invoice events"));
        }
    }

    @GetMapping("/upcoming-invoices")
    @PreAuthorize("hasAnyRole('ADMIN', 'COMMERCIAL_METIER', 'CHEF_PROJET', 'DECIDEUR')")
    public ResponseEntity<?> getUpcomingInvoices() {
        try {
            // Get current user
            User currentUser = userContextService.getCurrentUser();

            LocalDate today = LocalDate.now();
            LocalDate nextMonth = today.plusMonths(1);

            // Get all upcoming invoices
            List<Facture> allUpcomingInvoices = factureRepository
                    .findByDateEcheanceBetweenAndStatutPaiementNot(today, nextMonth, "PAYE");

            // Filter by user access
            List<Facture> accessibleInvoices = getAccessibleInvoices(currentUser, allUpcomingInvoices);

            List<CalendarEventDTO> events = accessibleInvoices.stream()
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
    @PreAuthorize("hasAnyRole('ADMIN', 'COMMERCIAL_METIER', 'CHEF_PROJET', 'DECIDEUR')")
    public ResponseEntity<?> getOverdueInvoices() {
        try {
            // Get current user
            User currentUser = userContextService.getCurrentUser();

            LocalDate today = LocalDate.now();

            // Get all overdue invoices
            List<Facture> allOverdueInvoices = factureRepository.findFacturesEnRetard(today);

            // Filter by user access
            List<Facture> accessibleInvoices = getAccessibleInvoices(currentUser, allOverdueInvoices);

            List<CalendarEventDTO> events = accessibleInvoices.stream()
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
    @PreAuthorize("hasAnyRole('ADMIN', 'COMMERCIAL_METIER', 'CHEF_PROJET', 'DECIDEUR')")
    public ResponseEntity<?> getCalendarStats() {
        try {
            // Get current user
            User currentUser = userContextService.getCurrentUser();

            LocalDate today = LocalDate.now();
            LocalDate startOfMonth = today.withDayOfMonth(1);
            LocalDate endOfMonth = today.withDayOfMonth(today.lengthOfMonth());

            // Get all invoices for different time periods
            List<Facture> allTodayInvoices = factureRepository.findByDateEcheance(today);
            List<Facture> allWeekInvoices = factureRepository.findByDateEcheanceBetween(today, today.plusDays(7));
            List<Facture> allMonthInvoices = factureRepository.findByDateEcheanceBetween(startOfMonth, endOfMonth);
            List<Facture> allOverdueInvoices = factureRepository.findFacturesEnRetard(today);

            // Filter by user access
            List<Facture> todayInvoices = getAccessibleInvoices(currentUser, allTodayInvoices);
            List<Facture> weekInvoices = getAccessibleInvoices(currentUser, allWeekInvoices);
            List<Facture> monthInvoices = getAccessibleInvoices(currentUser, allMonthInvoices);
            List<Facture> overdueInvoices = getAccessibleInvoices(currentUser, allOverdueInvoices);

            Map<String, Object> stats = new HashMap<>();
            stats.put("today", todayInvoices.size());
            stats.put("thisWeek", weekInvoices.size());
            stats.put("thisMonth", monthInvoices.size());
            stats.put("overdue", overdueInvoices.size());

            // Calculate amounts only for accessible invoices
            BigDecimal totalOverdueAmount = overdueInvoices.stream()
                    .map(Facture::getMontantTTC)
                    .filter(amount -> amount != null)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            stats.put("overdueAmount", totalOverdueAmount);
            stats.put("userRole", getHighestRole(currentUser));

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
    @PreAuthorize("hasAnyRole('ADMIN', 'COMMERCIAL_METIER', 'CHEF_PROJET', 'DECIDEUR')")
    public ResponseEntity<?> getAllEvents(
            @RequestParam(required = false) LocalDate start,
            @RequestParam(required = false) LocalDate end) {
        try {
            // Get current user
            User currentUser = userContextService.getCurrentUser();

            List<CalendarEventDTO> allEvents = new ArrayList<>();

            // Get accessible invoice events
            List<Facture> accessibleInvoices = getAccessibleInvoicesByDateRange(currentUser, start, end);

            List<CalendarEventDTO> invoiceEvents = accessibleInvoices.stream()
                    .map(this::convertFactureToEvent)
                    .collect(Collectors.toList());
            allEvents.addAll(invoiceEvents);

            // Get project events - add project-related events for CHEF_PROJET
            if (currentUser.getRoles().stream().anyMatch(r ->
                    r.getName() == ERole.ROLE_CHEF_PROJET) ||
                    currentUser.getRoles().stream().anyMatch(r ->
                            r.getName() == ERole.ROLE_ADMIN) ||
                    currentUser.getRoles().stream().anyMatch(r ->
                            r.getName() == ERole.ROLE_DECIDEUR)) {

                List<Project> accessibleProjects = getAccessibleProjects(currentUser);
                List<CalendarEventDTO> projectEvents = convertProjectsToEvents(accessibleProjects);
                allEvents.addAll(projectEvents);
            }

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", allEvents);
            response.put("count", allEvents.size());
            response.put("userRole", getHighestRole(currentUser));
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error fetching all calendar events: ", e);
            return ResponseEntity.badRequest().body(createErrorResponse("Failed to fetch calendar events"));
        }
    }

    // Helper method to get accessible projects
    private List<Project> getAccessibleProjects(User currentUser) {
        List<Project> allProjects = projectRepository.findAll();
        List<Project> accessibleProjects = new ArrayList<>();

        // Admins and DECIDEUR see all projects
        if (currentUser.getRoles().stream().anyMatch(r ->
                r.getName() == ERole.ROLE_ADMIN) ||
                currentUser.getRoles().stream().anyMatch(r ->
                        r.getName() == ERole.ROLE_DECIDEUR)) {
            return allProjects;
        }

        // CHEF_PROJET sees only projects they manage
        if (currentUser.getRoles().stream().anyMatch(r ->
                r.getName() == ERole.ROLE_CHEF_PROJET)) {
            accessibleProjects = allProjects.stream()
                    .filter(project -> project.getChefDeProjet() != null &&
                            project.getChefDeProjet().getId().equals(currentUser.getId()))
                    .collect(Collectors.toList());
        }

        // COMMERCIAL_METIER sees projects that have conventions they created
        if (currentUser.getRoles().stream().anyMatch(r ->
                r.getName() == ERole.ROLE_COMMERCIAL_METIER)) {
            accessibleProjects = allProjects.stream()
                    .filter(project -> {
                        if (project.getConventions() == null) return false;
                        return project.getConventions().stream()
                                .anyMatch(convention -> convention.getCreatedBy() != null &&
                                        convention.getCreatedBy().getId().equals(currentUser.getId()));
                    })
                    .distinct()
                    .collect(Collectors.toList());
        }

        return accessibleProjects;
    }

    // Convert projects to calendar events
    private List<CalendarEventDTO> convertProjectsToEvents(List<Project> projects) {
        return projects.stream()
                .map(this::convertProjectToEvent)
                .collect(Collectors.toList());
    }

    private CalendarEventDTO convertProjectToEvent(Project project) {
        CalendarEventDTO event = new CalendarEventDTO();
        event.setId(project.getId());
        event.setTitle("Projet: " + project.getName());
        event.setType("PROJECT");

        // Set start date (project start)
        if (project.getDateDebut() != null) {
            event.setStart(LocalDateTime.of(project.getDateDebut(), LocalTime.of(9, 0)));
        } else {
            event.setStart(LocalDateTime.now());
        }

        // Set end date (project end or same as start for one-day events)
        if (project.getDateFin() != null) {
            event.setEnd(LocalDateTime.of(project.getDateFin(), LocalTime.of(17, 0)));
            event.setAllDay(false);
        } else {
            event.setEnd(event.getStart().plusHours(8));
            event.setAllDay(false);
        }

        // Set color based on project status
        event.setColor(getProjectColor(project));

        // Set extended properties
        Map<String, Object> extendedProps = new HashMap<>();
        extendedProps.put("status", project.getStatus());
        extendedProps.put("progress", project.getProgress());
        extendedProps.put("clientName", project.getClientName());
        extendedProps.put("chefDeProjet", project.getChefProjetName());
        extendedProps.put("application", project.getApplicationName());

        if (project.getDateDebut() != null && project.getDateFin() != null) {
            extendedProps.put("duration", project.getTotalDays() + " jours");
        }

        event.setExtendedProps(extendedProps);

        return event;
    }

    private String getProjectColor(Project project) {
        switch (project.getStatus()) {
            case "PLANIFIE":
                return "#3B82F6"; // Blue
            case "EN_COURS":
                return "#10B981"; // Green
            case "TERMINE":
                return "#6B7280"; // Gray
            case "SUSPENDU":
                return "#F59E0B"; // Orange
            case "ANNULE":
                return "#EF4444"; // Red
            default:
                return "#6B7280"; // Gray default
        }
    }

    // Helper method to get user's highest role for UI display
    private String getHighestRole(User user) {
        if (user.getRoles().stream().anyMatch(r -> r.getName() == ERole.ROLE_ADMIN)) {
            return "ADMIN";
        } else if (user.getRoles().stream().anyMatch(r -> r.getName() == ERole.ROLE_CHEF_PROJET)) {
            return "CHEF_PROJET";
        } else if (user.getRoles().stream().anyMatch(r -> r.getName() == ERole.ROLE_COMMERCIAL_METIER)) {
            return "COMMERCIAL_METIER";
        } else if (user.getRoles().stream().anyMatch(r -> r.getName() == ERole.ROLE_DECIDEUR)) {
            return "DECIDEUR";
        }
        return "USER";
    }

    // ... existing convertFactureToEvent and getInvoiceColor methods remain the same ...

    private CalendarEventDTO convertFactureToEvent(Facture facture) {
        CalendarEventDTO event = new CalendarEventDTO();
        event.setId(facture.getId());
        event.setTitle("Facture #" + facture.getNumeroFacture());

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

        // Set color based on status
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

            // Add creator info for COMMERCIAL_METIER visibility
            if (facture.getConvention().getCreatedBy() != null) {
                extendedProps.put("createdById", facture.getConvention().getCreatedBy().getId());
                extendedProps.put("createdByUsername", facture.getConvention().getCreatedBy().getUsername());
                extendedProps.put("createdByName",
                        facture.getConvention().getCreatedBy().getFirstName() + " " +
                                facture.getConvention().getCreatedBy().getLastName());
            }
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