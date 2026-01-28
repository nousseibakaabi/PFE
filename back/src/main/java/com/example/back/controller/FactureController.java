package com.example.back.controller;

import com.example.back.entity.Convention;
import com.example.back.entity.Facture;
import com.example.back.payload.request.FactureRequest;
import com.example.back.payload.request.PaiementRequest;
import com.example.back.payload.response.FactureResponse;
import com.example.back.repository.ConventionRepository;
import com.example.back.repository.FactureRepository;
import com.example.back.service.ConventionService;
import com.example.back.service.mapper.FactureMapper;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
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

    // CRUD Operations

    @GetMapping
    public ResponseEntity<?> getAllFactures() {
        try {
            List<Facture> factures = factureRepository.findAll();

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

    @GetMapping("/{id}")
    public ResponseEntity<?> getFactureById(@PathVariable Long id) {
        try {
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

    @PostMapping("/generate")
    @PreAuthorize("hasAnyRole('COMMERCIAL_METIER')")
    public ResponseEntity<?> generateFacture(@Valid @RequestBody FactureRequest request) {
        try {
            Optional<Convention> convention = conventionRepository.findById(request.getConventionId());
            if (convention.isEmpty()) {
                return ResponseEntity.badRequest()
                        .body(createErrorResponse("Convention not found"));
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


    @PostMapping("/payer")
    @PreAuthorize("hasAnyRole('COMMERCIAL_METIER')")
    public ResponseEntity<?> registerPaiement(@Valid @RequestBody PaiementRequest request) {
        try {
            Optional<Facture> factureOpt = factureRepository.findById(request.getFactureId());
            if (factureOpt.isEmpty()) {
                return ResponseEntity.badRequest()
                        .body(createErrorResponse("Invoice not found"));
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

    @GetMapping("/convention/{conventionId}")
    public ResponseEntity<?> getFacturesByConvention(@PathVariable Long conventionId) {
        try {
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

    @GetMapping("/statut/{statut}")
    public ResponseEntity<?> getFacturesByStatut(@PathVariable String statut) {
        try {
            List<Facture> factures = factureRepository.findByStatutPaiement(statut);

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

    @GetMapping("/retard")
    public ResponseEntity<?> getFacturesEnRetard() {
        try {
            List<Facture> factures = factureRepository.findFacturesEnRetard(LocalDate.now());

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

    @GetMapping("/stats")
    public ResponseEntity<?> getStats() {
        try {
            long totalFactures = factureRepository.count();
            long facturesPayees = factureRepository.findByStatutPaiement("PAYE").size();
            long facturesNonPayees = factureRepository.findByStatutPaiement("NON_PAYE").size();
            long facturesEnRetard = factureRepository.findFacturesEnRetard(LocalDate.now()).size();

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

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('COMMERCIAL_METIER')")
    public ResponseEntity<?> updateFacture(@PathVariable Long id,
                                           @Valid @RequestBody FactureRequest request) {
        try {
            Optional<Facture> factureOpt = factureRepository.findById(id);
            if (factureOpt.isEmpty()) {
                return ResponseEntity.notFound().build();
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

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('COMMERCIAL_METIER')")
    public ResponseEntity<?> deleteFacture(@PathVariable Long id) {
        try {
            Optional<Facture> factureOpt = factureRepository.findById(id);
            if (factureOpt.isEmpty()) {
                return ResponseEntity.notFound().build();
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

    @GetMapping("/recent")
    public ResponseEntity<?> getRecentFactures() {
        try {
            // Get invoices from the last 30 days
            LocalDate thirtyDaysAgo = LocalDate.now().minusDays(30);

            // This query needs to be implemented in your repository
            // For now, we'll get all and filter in memory (not efficient for large datasets)
            List<Facture> allFactures = factureRepository.findAll();
            List<Facture> recentFactures = allFactures.stream()
                    .filter(f -> f.getDateFacturation() != null &&
                            !f.getDateFacturation().isBefore(thirtyDaysAgo))
                    .collect(Collectors.toList());

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

    @GetMapping("/montant-total")
    public ResponseEntity<?> getMontantTotal() {
        try {
            // Calculate total amount for paid invoices
            List<Facture> facturesPayees = factureRepository.findByStatutPaiement("PAYE");

            BigDecimal totalMontant = facturesPayees.stream()
                    .map(Facture::getMontantTTC)
                    .filter(amount -> amount != null)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            // Calculate total amount for unpaid invoices
            List<Facture> facturesNonPayees = factureRepository.findByStatutPaiement("NON_PAYE");

            BigDecimal totalMontantNonPaye = facturesNonPayees.stream()
                    .map(Facture::getMontantTTC)
                    .filter(amount -> amount != null)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

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