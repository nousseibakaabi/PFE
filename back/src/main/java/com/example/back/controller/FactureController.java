package com.example.back.controller;

import com.example.back.entity.*;
import com.example.back.payload.request.FactureRequest;
import com.example.back.payload.request.PaiementRequest;
import com.example.back.payload.response.FactureResponse;
import com.example.back.repository.*;
import com.example.back.service.ConventionService;
import com.example.back.service.UserContextService;
import com.example.back.service.mapper.FactureMapper;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/factures")
@PreAuthorize("hasAnyRole('COMMERCIAL_METIER')")
@Slf4j
public class FactureController {

    @Autowired
    private FactureRepository factureRepository;

    @Autowired
    private ConventionRepository conventionRepository;

    @Autowired
    private ConventionService conventionService;

    @Autowired
    private FactureMapper factureMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private UserContextService userContextService;

    // Helper method to check if user can access invoice
    private boolean canAccessFacture(Long factureId, User currentUser) {
        Optional<Facture> factureOpt = factureRepository.findById(factureId);
        if (factureOpt.isEmpty()) {
            return false;
        }

        Facture facture = factureOpt.get();

        // Admins can access everything
        if (currentUser.getRoles().stream().anyMatch(r ->
                r.getName() == ERole.ROLE_ADMIN)) {
            return true;
        }

        // Check if invoice belongs to a convention created by current user
        Convention convention = facture.getConvention();
        if (convention == null || convention.getCreatedBy() == null) {
            return false;
        }

        return convention.getCreatedBy().getId().equals(currentUser.getId());
    }

    // Helper method to check if user can access convention
    private boolean canAccessConvention(Long conventionId, User currentUser) {
        Optional<Convention> conventionOpt = conventionRepository.findById(conventionId);
        if (conventionOpt.isEmpty()) {
            return false;
        }

        Convention convention = conventionOpt.get();

        // Admins can access everything
        if (currentUser.getRoles().stream().anyMatch(r ->
                r.getName() == ERole.ROLE_ADMIN)) {
            return true;
        }

        // Check if convention was created by current user
        return convention.getCreatedBy() != null &&
                convention.getCreatedBy().getId().equals(currentUser.getId());
    }

    // GET FACTURE BY ID - WITH ACCESS CHECK
    @GetMapping("/{id}")
    public ResponseEntity<?> getFactureById(@PathVariable Long id) {
        try {
            // Get current user
            User currentUser = userContextService.getCurrentUser();

            // Check access
            if (!canAccessFacture(id, currentUser)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(createErrorResponse("Access denied to this invoice"));
            }

            Optional<Facture> facture = factureRepository.findById(id);
            if (facture.isEmpty()) {
                return ResponseEntity.notFound().build();
            }

            // Check if invoice is overdue
            Facture f = facture.get();
            if (f.isEnRetard() && "NON_PAYE".equals(f.getStatutPaiement())) {
                f.setStatutPaiement("EN_RETARD");
                factureRepository.save(f);
            }

            FactureResponse response = factureMapper.toResponse(f);

            Map<String, Object> apiResponse = new HashMap<>();
            apiResponse.put("success", true);
            apiResponse.put("data", response);
            return ResponseEntity.ok(apiResponse);
        } catch (Exception e) {
            log.error("Error fetching invoice: ", e);
            return ResponseEntity.badRequest().body(createErrorResponse("Failed to fetch invoice"));
        }
    }

    // UPDATE FACTURE - WITH ACCESS CHECK
    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('COMMERCIAL_METIER')")
    public ResponseEntity<?> updateFacture(@PathVariable Long id,
                                           @Valid @RequestBody FactureRequest request) {
        try {
            // Get current user
            User currentUser = userContextService.getCurrentUser();

            Optional<Facture> factureOpt = factureRepository.findById(id);
            if (factureOpt.isEmpty()) {
                return ResponseEntity.notFound().build();
            }

            // Check access
            if (!canAccessFacture(id, currentUser)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(createErrorResponse("Access denied to update this invoice"));
            }

            Facture facture = factureOpt.get();

            // Update fields
            if (request.getDateFacturation() != null) {
                facture.setDateFacturation(request.getDateFacturation());
            }
            if (request.getDateEcheance() != null) {
                facture.setDateEcheance(request.getDateEcheance());
            }
            if (request.getMontantHT() != null) {
                facture.setMontantHT(request.getMontantHT());
                // Recalculate TTC if needed
                if (facture.getTva() != null) {
                    BigDecimal tvaMontant = request.getMontantHT()
                            .multiply(facture.getTva())
                            .divide(new BigDecimal("100"));
                    facture.setMontantTTC(request.getMontantHT().add(tvaMontant));
                }
            }
            if (request.getTva() != null) {
                facture.setTva(request.getTva());
                // Recalculate TTC if needed
                if (facture.getMontantHT() != null) {
                    BigDecimal tvaMontant = facture.getMontantHT()
                            .multiply(request.getTva())
                            .divide(new BigDecimal("100"));
                    facture.setMontantTTC(facture.getMontantHT().add(tvaMontant));
                }
            }
            if (request.getNotes() != null) {
                facture.setNotes(request.getNotes());
            }

            Facture updated = factureRepository.save(facture);

            // Update convention status if needed
            conventionService.updateConventionStatusRealTime(updated.getConvention().getId());

            FactureResponse response = factureMapper.toResponse(updated);

            Map<String, Object> apiResponse = new HashMap<>();
            apiResponse.put("success", true);
            apiResponse.put("message", "Invoice updated successfully");
            apiResponse.put("data", response);
            return ResponseEntity.ok(apiResponse);
        } catch (Exception e) {
            log.error("Error updating invoice: ", e);
            return ResponseEntity.badRequest().body(createErrorResponse("Failed to update invoice"));
        }
    }

    // DELETE FACTURE - WITH ACCESS CHECK
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('COMMERCIAL_METIER')")
    public ResponseEntity<?> deleteFacture(@PathVariable Long id) {
        try {
            // Get current user
            User currentUser = userContextService.getCurrentUser();

            Optional<Facture> factureOpt = factureRepository.findById(id);
            if (factureOpt.isEmpty()) {
                return ResponseEntity.notFound().build();
            }

            // Check access
            if (!canAccessFacture(id, currentUser)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(createErrorResponse("Access denied to delete this invoice"));
            }

            Facture facture = factureOpt.get();

            // Save convention ID before deletion for status update
            Long conventionId = facture.getConvention().getId();

            factureRepository.delete(facture);

            // Update convention status
            conventionService.updateConventionStatusRealTime(conventionId);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Invoice deleted successfully");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error deleting invoice: ", e);
            return ResponseEntity.badRequest().body(createErrorResponse("Failed to delete invoice"));
        }
    }

    // REGISTER PAYMENT - WITH ACCESS CHECK
    @PostMapping("/payer")
    @PreAuthorize("hasAnyRole('COMMERCIAL_METIER')")
    public ResponseEntity<?> registerPaiement(@Valid @RequestBody PaiementRequest request) {
        try {
            // Get current user
            User currentUser = userContextService.getCurrentUser();

            Optional<Facture> factureOpt = factureRepository.findById(request.getFactureId());
            if (factureOpt.isEmpty()) {
                return ResponseEntity.badRequest()
                        .body(createErrorResponse("Invoice not found"));
            }

            // Check access
            if (!canAccessFacture(request.getFactureId(), currentUser)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(createErrorResponse("Access denied to register payment for this invoice"));
            }

            Facture facture = factureOpt.get();

            // Update payment information
            facture.setStatutPaiement("PAYE");
            facture.setReferencePaiement(request.getReferencePaiement());
            facture.setDatePaiement(request.getDatePaiement() != null ?
                    request.getDatePaiement() : LocalDate.now());

            Facture updated = factureRepository.save(facture);

            // IMPORTANT: Update convention status immediately
            conventionService.updateConventionStatusRealTime(facture.getConvention().getId());

            FactureResponse response = factureMapper.toResponse(updated);

            Map<String, Object> apiResponse = new HashMap<>();
            apiResponse.put("success", true);
            apiResponse.put("message", "Payment registered successfully");
            apiResponse.put("data", response);
            return ResponseEntity.ok(apiResponse);
        } catch (Exception e) {
            log.error("Error registering payment: ", e);
            return ResponseEntity.badRequest().body(createErrorResponse("Failed to register payment"));
        }
    }

    // GENERATE FACTURE - WITH ACCESS CHECK
    @PostMapping("/generate")
    @PreAuthorize("hasAnyRole('COMMERCIAL_METIER')")
    public ResponseEntity<?> generateFacture(@Valid @RequestBody FactureRequest request) {
        try {
            // Get current user
            User currentUser = userContextService.getCurrentUser();

            Optional<Convention> convention = conventionRepository.findById(request.getConventionId());
            if (convention.isEmpty()) {
                return ResponseEntity.badRequest()
                        .body(createErrorResponse("Convention not found"));
            }

            // Check access to the convention
            if (!canAccessConvention(request.getConventionId(), currentUser)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(createErrorResponse("Access denied to create invoice for this convention"));
            }

            // Generate invoice number
            String invoiceNumber = "FACT-" + LocalDate.now().getYear() + "-" +
                    String.format("%06d", factureRepository.count() + 1);

            // Create new invoice
            Facture facture = new Facture();
            facture.setNumeroFacture(invoiceNumber);
            facture.setConvention(convention.get());
            facture.setDateFacturation(request.getDateFacturation() != null ?
                    request.getDateFacturation() : LocalDate.now());
            facture.setDateEcheance(request.getDateEcheance());
            facture.setMontantHT(request.getMontantHT());
            facture.setTva(request.getTva() != null ? request.getTva() : new BigDecimal("19.00"));
            facture.setNotes(request.getNotes());
            facture.setStatutPaiement("NON_PAYE");

            // Calculate TTC
            if (request.getMontantHT() != null && facture.getTva() != null) {
                BigDecimal tvaMontant = request.getMontantHT()
                        .multiply(facture.getTva())
                        .divide(new BigDecimal("100"));
                facture.setMontantTTC(request.getMontantHT().add(tvaMontant));
            }

            Facture saved = factureRepository.save(facture);

            // Update convention status
            conventionService.updateConventionStatusRealTime(saved.getConvention().getId());

            FactureResponse response = factureMapper.toResponse(saved);

            Map<String, Object> apiResponse = new HashMap<>();
            apiResponse.put("success", true);
            apiResponse.put("message", "Invoice generated successfully");
            apiResponse.put("data", response);
            return ResponseEntity.ok(apiResponse);
        } catch (Exception e) {
            log.error("Error generating invoice: ", e);
            return ResponseEntity.badRequest().body(createErrorResponse("Failed to generate invoice"));
        }
    }

    // GET ALL FACTURES - WITH ACCESS CHECK
    @GetMapping
    public ResponseEntity<?> getAllFactures() {
        try {
            // Get current user
            User currentUser = userContextService.getCurrentUser();

            List<Facture> factures;

            // For COMMERCIAL_METIER, show only invoices from their conventions
            if (currentUser.getRoles().stream().anyMatch(r ->
                    r.getName() == ERole.ROLE_ADMIN)) {
                factures = factureRepository.findAll();
            } else {
                factures = factureRepository.findByConventionCreatedBy(currentUser);
            }

            // Update status for overdue invoices
            for (Facture facture : factures) {
                if (facture.isEnRetard() && "NON_PAYE".equals(facture.getStatutPaiement())) {
                    facture.setStatutPaiement("EN_RETARD");
                    factureRepository.save(facture);
                }
            }

            // Convert to DTOs
            List<FactureResponse> factureResponses = factures.stream()
                    .map(factureMapper::toResponse)
                    .collect(Collectors.toList());

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", factureResponses);
            response.put("count", factureResponses.size());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error fetching invoices: ", e);
            return ResponseEntity.badRequest().body(createErrorResponse("Failed to fetch invoices"));
        }
    }

    // GET FACTURES BY CONVENTION - WITH ACCESS CHECK
    @GetMapping("/convention/{conventionId}")
    public ResponseEntity<?> getFacturesByConvention(@PathVariable Long conventionId) {
        try {
            // Get current user
            User currentUser = userContextService.getCurrentUser();

            // Check if user has access to this convention
            if (!canAccessConvention(conventionId, currentUser)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(createErrorResponse("Access denied to this convention"));
            }

            List<Facture> factures = factureRepository.findByConventionId(conventionId);

            // Update status for overdue invoices
            for (Facture facture : factures) {
                if (facture.isEnRetard() && "NON_PAYE".equals(facture.getStatutPaiement())) {
                    facture.setStatutPaiement("EN_RETARD");
                }
            }

            // Convert to DTOs
            List<FactureResponse> factureResponses = factures.stream()
                    .map(factureMapper::toResponse)
                    .collect(Collectors.toList());

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", factureResponses);
            response.put("count", factureResponses.size());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error fetching invoices by convention: ", e);
            return ResponseEntity.badRequest().body(createErrorResponse("Failed to fetch invoices"));
        }
    }

    // GET FACTURES BY STATUT - WITH ACCESS CHECK
    @GetMapping("/statut/{statut}")
    public ResponseEntity<?> getFacturesByStatut(@PathVariable String statut) {
        try {
            // Get current user
            User currentUser = userContextService.getCurrentUser();

            List<Facture> factures;

            if (currentUser.getRoles().stream().anyMatch(r ->
                    r.getName() == ERole.ROLE_ADMIN)) {
                factures = factureRepository.findByStatutPaiement(statut);
            } else {
                factures = factureRepository.findByConventionCreatedByAndStatutPaiement(currentUser, statut);
            }

            // Convert to DTOs
            List<FactureResponse> factureResponses = factures.stream()
                    .map(factureMapper::toResponse)
                    .collect(Collectors.toList());

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", factureResponses);
            response.put("count", factureResponses.size());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error fetching invoices by status: ", e);
            return ResponseEntity.badRequest().body(createErrorResponse("Failed to fetch invoices"));
        }
    }

    // GET OVERDUE FACTURES - WITH ACCESS CHECK
    @GetMapping("/retard")
    public ResponseEntity<?> getFacturesEnRetard() {
        try {
            // Get current user
            User currentUser = userContextService.getCurrentUser();

            List<Facture> factures;
            LocalDate today = LocalDate.now();

            if (currentUser.getRoles().stream().anyMatch(r ->
                    r.getName() == ERole.ROLE_ADMIN)) {
                factures = factureRepository.findFacturesEnRetard(today);
            } else {
                // Get all invoices from user's conventions
                List<Facture> userFactures = factureRepository.findByConventionCreatedBy(currentUser);
                // Filter overdue invoices
                factures = userFactures.stream()
                        .filter(f -> f.getDateEcheance().isBefore(today) &&
                                "NON_PAYE".equals(f.getStatutPaiement()))
                        .collect(Collectors.toList());
            }

            // Convert to DTOs
            List<FactureResponse> factureResponses = factures.stream()
                    .map(factureMapper::toResponse)
                    .collect(Collectors.toList());

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", factureResponses);
            response.put("count", factureResponses.size());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error fetching overdue invoices: ", e);
            return ResponseEntity.badRequest().body(createErrorResponse("Failed to fetch overdue invoices"));
        }
    }

    // GET STATS - WITH ACCESS CHECK
    @GetMapping("/stats")
    public ResponseEntity<?> getStats() {
        try {
            // Get current user
            User currentUser = userContextService.getCurrentUser();

            long totalFactures;
            long facturesPayees;
            long facturesNonPayees;
            long facturesEnRetard;

            if (currentUser.getRoles().stream().anyMatch(r ->
                    r.getName() == ERole.ROLE_ADMIN)) {
                totalFactures = factureRepository.count();
                facturesPayees = factureRepository.findByStatutPaiement("PAYE").size();
                facturesNonPayees = factureRepository.findByStatutPaiement("NON_PAYE").size();
                facturesEnRetard = factureRepository.findFacturesEnRetard(LocalDate.now()).size();
            } else {
                // Get only user's invoices
                List<Facture> userFactures = factureRepository.findByConventionCreatedBy(currentUser);
                totalFactures = userFactures.size();
                facturesPayees = userFactures.stream()
                        .filter(f -> "PAYE".equals(f.getStatutPaiement()))
                        .count();
                facturesNonPayees = userFactures.stream()
                        .filter(f -> "NON_PAYE".equals(f.getStatutPaiement()))
                        .count();
                LocalDate today = LocalDate.now();
                facturesEnRetard = userFactures.stream()
                        .filter(f -> f.getDateEcheance().isBefore(today) &&
                                "NON_PAYE".equals(f.getStatutPaiement()))
                        .count();
            }

            Map<String, Object> stats = new HashMap<>();
            stats.put("total", totalFactures);
            stats.put("payees", facturesPayees);
            stats.put("nonPayees", facturesNonPayees);
            stats.put("enRetard", facturesEnRetard);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", stats);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error fetching invoice statistics: ", e);
            return ResponseEntity.badRequest().body(createErrorResponse("Failed to fetch stats"));
        }
    }

    // GET RECENT FACTURES - WITH ACCESS CHECK
    @GetMapping("/recent")
    public ResponseEntity<?> getRecentFactures() {
        try {
            // Get current user
            User currentUser = userContextService.getCurrentUser();

            // Get invoices from the last 30 days
            LocalDate thirtyDaysAgo = LocalDate.now().minusDays(30);

            List<Facture> recentFactures;

            if (currentUser.getRoles().stream().anyMatch(r ->
                    r.getName() == ERole.ROLE_ADMIN)) {
                // Get all and filter in memory (not efficient for large datasets)
                List<Facture> allFactures = factureRepository.findAll();
                recentFactures = allFactures.stream()
                        .filter(f -> f.getDateFacturation() != null &&
                                !f.getDateFacturation().isBefore(thirtyDaysAgo))
                        .collect(Collectors.toList());
            } else {
                // Get user's invoices and filter
                List<Facture> userFactures = factureRepository.findByConventionCreatedBy(currentUser);
                recentFactures = userFactures.stream()
                        .filter(f -> f.getDateFacturation() != null &&
                                !f.getDateFacturation().isBefore(thirtyDaysAgo))
                        .collect(Collectors.toList());
            }

            // Convert to DTOs
            List<FactureResponse> factureResponses = recentFactures.stream()
                    .map(factureMapper::toResponse)
                    .collect(Collectors.toList());

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", factureResponses);
            response.put("count", factureResponses.size());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error fetching recent invoices: ", e);
            return ResponseEntity.badRequest().body(createErrorResponse("Failed to fetch recent invoices"));
        }
    }

    // GET TOTAL AMOUNTS - WITH ACCESS CHECK
    @GetMapping("/montant-total")
    public ResponseEntity<?> getMontantTotal() {
        try {
            // Get current user
            User currentUser = userContextService.getCurrentUser();

            BigDecimal totalMontant;
            BigDecimal totalMontantNonPaye;

            if (currentUser.getRoles().stream().anyMatch(r ->
                    r.getName() == ERole.ROLE_ADMIN)) {
                // Calculate total amount for paid invoices
                List<Facture> facturesPayees = factureRepository.findByStatutPaiement("PAYE");
                totalMontant = facturesPayees.stream()
                        .map(Facture::getMontantTTC)
                        .filter(amount -> amount != null)
                        .reduce(BigDecimal.ZERO, BigDecimal::add);

                // Calculate total amount for unpaid invoices
                List<Facture> facturesNonPayees = factureRepository.findByStatutPaiement("NON_PAYE");
                totalMontantNonPaye = facturesNonPayees.stream()
                        .map(Facture::getMontantTTC)
                        .filter(amount -> amount != null)
                        .reduce(BigDecimal.ZERO, BigDecimal::add);
            } else {
                // Get only user's invoices
                List<Facture> userFactures = factureRepository.findByConventionCreatedBy(currentUser);

                // Calculate paid amount
                totalMontant = userFactures.stream()
                        .filter(f -> "PAYE".equals(f.getStatutPaiement()))
                        .map(Facture::getMontantTTC)
                        .filter(amount -> amount != null)
                        .reduce(BigDecimal.ZERO, BigDecimal::add);

                // Calculate unpaid amount
                totalMontantNonPaye = userFactures.stream()
                        .filter(f -> !"PAYE".equals(f.getStatutPaiement()))
                        .map(Facture::getMontantTTC)
                        .filter(amount -> amount != null)
                        .reduce(BigDecimal.ZERO, BigDecimal::add);
            }

            Map<String, Object> stats = new HashMap<>();
            stats.put("totalPaye", totalMontant);
            stats.put("totalNonPaye", totalMontantNonPaye);
            stats.put("totalGeneral", totalMontant.add(totalMontantNonPaye));

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", stats);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error calculating total amounts: ", e);
            return ResponseEntity.badRequest().body(createErrorResponse("Failed to calculate total amounts"));
        }
    }

    private Map<String, Object> createErrorResponse(String message) {
        Map<String, Object> response = new HashMap<>();
        response.put("success", false);
        response.put("message", message);
        return response;
    }
}