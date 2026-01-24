package com.example.back.controller;

import com.example.back.entity.Convention;
import com.example.back.entity.Facture;
import com.example.back.payload.request.FactureRequest;
import com.example.back.payload.request.PaiementRequest;
import com.example.back.repository.ConventionRepository;
import com.example.back.repository.FactureRepository;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/factures")
@PreAuthorize("hasAnyRole('ADMIN', 'COMMERCIAL_METIER')")
@Slf4j
public class FactureController {

    @Autowired
    private FactureRepository factureRepository;

    @Autowired
    private ConventionRepository conventionRepository;

    // CRUD Operations

    @GetMapping
    public ResponseEntity<?> getAllFactures() {
        try {
            List<Facture> factures = factureRepository.findAll();

            // Add color coding for overdue invoices
            for (Facture facture : factures) {
                if (facture.isEnRetard()) {
                    facture.setStatutPaiement("EN_RETARD");
                }
            }

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", factures);
            response.put("count", factures.size());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
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

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", f);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(createErrorResponse("Failed to fetch invoice"));
        }
    }

    @PostMapping("/generate")
    @PreAuthorize("hasAnyRole('ADMIN', 'COMMERCIAL_METIER')")
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
            facture.setTva(request.getTva() != null ? request.getTva() : facture.getTva());
            facture.setNotes(request.getNotes());

            // Calculate TTC
            if (request.getMontantHT() != null && facture.getTva() != null) {
                facture.setMontantTTC(
                        request.getMontantHT().add(
                                request.getMontantHT().multiply(facture.getTva())
                                        .divide(java.math.BigDecimal.valueOf(100))
                        )
                );
            }

            Facture saved = factureRepository.save(facture);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Invoice generated successfully");
            response.put("data", saved);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error generating invoice: ", e);
            return ResponseEntity.badRequest().body(createErrorResponse("Failed to generate invoice"));
        }
    }

    @PostMapping("/payer")
    @PreAuthorize("hasAnyRole('ADMIN', 'COMPTABLE')")
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
            facture.setModePaiement(request.getModePaiement());
            facture.setReferencePaiement(request.getReferencePaiement());
            facture.setDatePaiement(request.getDatePaiement() != null ?
                    request.getDatePaiement() : LocalDate.now());

            Facture updated = factureRepository.save(facture);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Payment registered successfully");
            response.put("data", updated);
            return ResponseEntity.ok(response);
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

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", factures);
            response.put("count", factures.size());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(createErrorResponse("Failed to fetch invoices"));
        }
    }

    @GetMapping("/statut/{statut}")
    public ResponseEntity<?> getFacturesByStatut(@PathVariable String statut) {
        try {
            List<Facture> factures = factureRepository.findByStatutPaiement(statut);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", factures);
            response.put("count", factures.size());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(createErrorResponse("Failed to fetch invoices"));
        }
    }

    @GetMapping("/retard")
    public ResponseEntity<?> getFacturesEnRetard() {
        try {
            List<Facture> factures = factureRepository.findFacturesEnRetard(LocalDate.now());

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", factures);
            response.put("count", factures.size());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
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
            return ResponseEntity.badRequest().body(createErrorResponse("Failed to fetch stats"));
        }
    }

    private Map<String, Object> createErrorResponse(String message) {
        Map<String, Object> response = new HashMap<>();
        response.put("success", false);
        response.put("message", message);
        return response;
    }
}