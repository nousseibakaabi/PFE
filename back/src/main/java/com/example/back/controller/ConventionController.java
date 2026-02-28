package com.example.back.controller;

import com.example.back.entity.*;
import com.example.back.payload.request.ArchiveConventionRequest;
import com.example.back.payload.request.ConventionRequest;
import com.example.back.payload.request.RenewalRequestDTO;
import com.example.back.payload.response.ConventionResponse;
import com.example.back.repository.*;
import com.example.back.service.ApplicationService;
import com.example.back.service.ConventionService;
import com.example.back.service.HistoryService;
import com.example.back.service.mapper.ConventionMapper;
import jakarta.transaction.Transactional;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/conventions")
@Slf4j
public class ConventionController {

    @Autowired
    private ConventionRepository conventionRepository;

    @Autowired
    private StructureRepository structureRepository;

    @Autowired
    private ZoneGeographiqueRepository zoneGeographiqueRepository;

    @Autowired
    private ApplicationRepository applicationRepository;

    @Autowired
    private FactureRepository factureRepository;

    @Autowired
    private ConventionService conventionService;

    @Autowired
    private ConventionMapper conventionMapper;


    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ApplicationService applicationService;

    @Autowired
    private HistoryService historyService;


    @Autowired
    private OldFactureRepository oldFactureRepository;

    @Autowired
    private OldConventionRepository oldConventionRepository;


    @PostMapping("/calculate-ttc")
    @PreAuthorize("hasAnyRole('ADMIN', 'COMMERCIAL_METIER')")
    public ResponseEntity<?> calculateTTC(@RequestParam BigDecimal montantHT,
                                          @RequestParam(required = false) BigDecimal tva) {
        try {
            Map<String, Object> result = conventionService.calculateTTCResponse(montantHT, tva);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", result);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error calculating TTC: ", e);
            return ResponseEntity.badRequest().body(createErrorResponse("Failed to calculate TTC: " + e.getMessage()));
        }
    }

    @PostMapping("/determine-nb-users")
    @PreAuthorize("hasAnyRole('ADMIN', 'COMMERCIAL_METIER')")
    public ResponseEntity<?> determineNbUsers(@RequestParam Long applicationId,
                                              @RequestParam(required = false) Long selectedUsers) {
        try {
            Map<String, Object> result = conventionService.determineNbUsersResponse(applicationId, selectedUsers);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", result);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error determining nb users: ", e);
            return ResponseEntity.badRequest().body(createErrorResponse("Failed to determine nb users: " + e.getMessage()));
        }
    }


    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'COMMERCIAL_METIER', 'DECIDEUR', 'CHEF_PROJET')")
    public ResponseEntity<?> getConventionById(@PathVariable Long id) {
        try {
            Optional<Convention> convention = conventionRepository.findById(id);
            if (convention.isEmpty()) {
                return ResponseEntity.notFound().build();
            }

            ConventionResponse response = conventionMapper.toResponse(convention.get());

            Map<String, Object> apiResponse = new HashMap<>();
            apiResponse.put("success", true);
            apiResponse.put("data", response);
            return ResponseEntity.ok(apiResponse);
        } catch (Exception e) {
            log.error("Error fetching convention: ", e);
            return ResponseEntity.badRequest().body(createErrorResponse("Failed to fetch convention"));
        }
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'COMMERCIAL_METIER')")
    public ResponseEntity<?> createConvention(@Valid @RequestBody ConventionRequest request) {
        try {
            String currentUsername = getCurrentUsername();
            User currentUser = userRepository.findByUsername(currentUsername)
                    .orElseThrow(() -> new RuntimeException("User not found"));

            log.info("Creating convention with reference: {}", request.getReferenceConvention());

            // Check if reference already exists
            if (conventionRepository.existsByReferenceConvention(request.getReferenceConvention())) {
                return ResponseEntity.badRequest()
                        .body(createErrorResponse("Convention with this reference already exists"));
            }

            // Create convention using the service
            Convention convention = conventionService.createConventionWithFinancials(request, currentUser);

            ConventionResponse response = conventionMapper.toResponse(convention);

            Map<String, Object> apiResponse = new HashMap<>();
            apiResponse.put("success", true);
            apiResponse.put("message", "Convention created successfully");
            apiResponse.put("data", response);
            return ResponseEntity.ok(apiResponse);

        } catch (Exception e) {
            log.error("Error creating convention: ", e);
            return ResponseEntity.badRequest().body(createErrorResponse("Failed to create convention: " + e.getMessage()));
        }
    }


    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'COMMERCIAL_METIER')")
    public ResponseEntity<?> updateConvention(@PathVariable Long id,
                                              @Valid @RequestBody ConventionRequest request) {
        try {
            log.info("Updating convention with ID: {}", id);

            Convention updatedConvention = conventionService.updateConventionWithFinancials(id, request);
            ConventionResponse response = conventionMapper.toResponse(updatedConvention);

            Map<String, Object> apiResponse = new HashMap<>();
            apiResponse.put("success", true);
            apiResponse.put("message", "Convention updated successfully");
            apiResponse.put("data", response);
            return ResponseEntity.ok(apiResponse);

        } catch (Exception e) {
            log.error("Error updating convention: ", e);
            return ResponseEntity.badRequest().body(createErrorResponse("Failed to update convention: " + e.getMessage()));
        }
    }



    @GetMapping("/archives")
    @PreAuthorize("hasAnyRole('ADMIN', 'COMMERCIAL_METIER')")
    public ResponseEntity<?> getArchivedConventions() {
        try {
            List<Convention> archivedConventions = conventionRepository.findByArchivedTrue();

            List<ConventionResponse> conventionResponses = archivedConventions.stream()
                    .map(conventionMapper::toResponse)
                    .collect(Collectors.toList());

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", conventionResponses);
            response.put("count", conventionResponses.size());
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Error fetching archived conventions: ", e);
            return ResponseEntity.badRequest()
                    .body(createErrorResponse("Failed to fetch archived conventions"));
        }
    }



    // Add to ConventionController.java - Update archive and restore methods

    @PostMapping("/{id}/archive")
    @PreAuthorize("hasAnyRole('ADMIN', 'COMMERCIAL_METIER')")
    @Transactional
    public ResponseEntity<?> archiveConvention(
            @PathVariable Long id,
            @Valid @RequestBody ArchiveConventionRequest request) {

        try {
            log.info("Attempting to archive convention with ID: {}", id);
            String currentUsername = getCurrentUsername();
            User currentUser = userRepository.findByUsername(currentUsername)
                    .orElseThrow(() -> new RuntimeException("User not found"));

            // Check if convention exists
            Convention convention = conventionRepository.findById(id)
                    .orElseThrow(() -> {
                        log.warn("Convention not found with ID: {}", id);
                        return new RuntimeException("Convention not found");
                    });

            // Check if already archived
            if (convention.getArchived()) {
                Map<String, Object> response = new HashMap<>();
                response.put("success", false);
                response.put("message", "Cette convention est déjà archivée");
                response.put("conventionId", id);
                return ResponseEntity.badRequest().body(response);
            }

            // Get all invoices for this convention
            List<Facture> invoices = factureRepository.findByConventionId(id);

            if (!invoices.isEmpty()) {
                // Check if any invoice is unpaid
                boolean hasUnpaidInvoices = invoices.stream()
                        .anyMatch(f -> !"PAYE".equals(f.getStatutPaiement()));

                if (hasUnpaidInvoices) {
                    // Count unpaid invoices
                    List<Facture> unpaidInvoices = invoices.stream()
                            .filter(f -> !"PAYE".equals(f.getStatutPaiement()))
                            .toList();

                    log.warn("Cannot archive convention {}: has {} unpaid invoices",
                            id, unpaidInvoices.size());

                    Map<String, Object> errorResponse = new HashMap<>();
                    errorResponse.put("success", false);
                    errorResponse.put("message", "Impossible d'archiver la convention car elle a des factures impayées");
                    errorResponse.put("details", String.format(
                            "Cette convention a %d facture(s) impayée(s). " +
                                    "Toutes les factures doivent être marquées comme 'PAYE' avant d'archiver la convention.",
                            unpaidInvoices.size()
                    ));
                    errorResponse.put("unpaidInvoices", unpaidInvoices.size());
                    errorResponse.put("unpaidInvoiceNumbers", unpaidInvoices.stream()
                            .map(Facture::getNumeroFacture)
                            .toList());
                    errorResponse.put("conventionId", id);
                    errorResponse.put("errorType", "UNPAID_INVOICES");

                    return ResponseEntity.badRequest().body(errorResponse);
                }

                // All invoices are paid - archive them
                for (Facture facture : invoices) {
                    facture.archive();
                    factureRepository.save(facture);
                    log.info("Archived invoice {} for convention {}",
                            facture.getNumeroFacture(), id);
                }
            }

            // Archive the convention
            convention.archive(currentUsername, request.getReason());
            Convention archivedConvention = conventionRepository.save(convention);

            // LOG HISTORY: Convention archive
            historyService.logConventionArchive(archivedConvention, currentUser, request.getReason());

            log.info("Convention archived successfully: ID={}, by={}, reason={}",
                    id, currentUsername, request.getReason());

            ConventionResponse response = conventionMapper.toResponse(archivedConvention);

            Map<String, Object> apiResponse = new HashMap<>();
            apiResponse.put("success", true);
            apiResponse.put("message", "Convention archivée avec succès");
            apiResponse.put("data", response);
            return ResponseEntity.ok(apiResponse);

        } catch (RuntimeException e) {
            if (e.getMessage().equals("Convention not found")) {
                return ResponseEntity.notFound().build();
            }
            log.error("Error archiving convention: {}", e.getMessage(), e);
            return ResponseEntity.badRequest()
                    .body(createErrorResponse("Erreur lors de l'archivage: " + e.getMessage()));

        } catch (Exception e) {
            log.error("Unexpected error archiving convention {}: {}", id, e.getMessage(), e);
            return ResponseEntity.internalServerError()
                    .body(createErrorResponse("Erreur serveur: " + e.getMessage()));
        }
    }

    @PostMapping("/{id}/restore")
    @PreAuthorize("hasAnyRole('ADMIN', 'COMMERCIAL_METIER')")
    @Transactional
    public ResponseEntity<?> restoreConvention(@PathVariable Long id) {
        try {
            log.info("Attempting to restore convention with ID: {}", id);
            String currentUsername = getCurrentUsername();
            User currentUser = userRepository.findByUsername(currentUsername)
                    .orElseThrow(() -> new RuntimeException("User not found"));

            Convention convention = conventionRepository.findById(id)
                    .orElseThrow(() -> {
                        log.warn("Convention not found with ID: {}", id);
                        return new RuntimeException("Convention not found");
                    });

            if (!convention.getArchived()) {
                Map<String, Object> response = new HashMap<>();
                response.put("success", false);
                response.put("message", "Cette convention n'est pas archivée");
                return ResponseEntity.badRequest().body(response);
            }

            // Restore convention
            convention.restore();

            // Restore related invoices
            List<Facture> invoices = factureRepository.findByConventionId(id);
            for (Facture facture : invoices) {
                facture.setArchived(false);
                facture.setArchivedAt(null);
                factureRepository.save(facture);
            }

            Convention restoredConvention = conventionRepository.save(convention);
            log.info("Convention restored successfully: ID={}, by={}", id, currentUsername);

            // LOG HISTORY: Convention restore
            historyService.logConventionRestore(restoredConvention, currentUser);

            // Update convention status
            conventionService.updateConventionStatusRealTime(id);

            ConventionResponse response = conventionMapper.toResponse(restoredConvention);

            Map<String, Object> apiResponse = new HashMap<>();
            apiResponse.put("success", true);
            apiResponse.put("message", "Convention restaurée avec succès");
            apiResponse.put("data", response);
            return ResponseEntity.ok(apiResponse);

        } catch (Exception e) {
            log.error("Error restoring convention: {}", e.getMessage(), e);
            return ResponseEntity.badRequest()
                    .body(createErrorResponse("Erreur lors de la restauration: " + e.getMessage()));
        }
    }

    @GetMapping("/active")
    @PreAuthorize("hasAnyRole('ADMIN', 'COMMERCIAL_METIER', 'DECIDEUR', 'CHEF_PROJET')")
    public ResponseEntity<?> getActiveConventions() {
        try {
            String currentUsername = getCurrentUsername();
            User currentUser = userRepository.findByUsername(currentUsername)
                    .orElseThrow(() -> new RuntimeException("User not found"));

            List<Convention> activeConventions;

            if (currentUser.getRoles().stream().anyMatch(r ->
                    r.getName() == ERole.ROLE_ADMIN)) {
                activeConventions = conventionRepository.findByArchivedFalse();
            } else {
                activeConventions = conventionRepository.findByCreatedByAndArchivedFalse(currentUser);
            }


            List<ConventionResponse> conventionResponses = activeConventions.stream()
                    .map(conventionMapper::toResponse)
                    .collect(Collectors.toList());

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", conventionResponses);
            response.put("count", conventionResponses.size());
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Error fetching active conventions: ", e);
            return ResponseEntity.badRequest()
                    .body(createErrorResponse("Failed to fetch conventions"));
        }
    }


    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'COMMERCIAL_METIER', 'DECIDEUR', 'CHEF_PROJET')")
    public ResponseEntity<?> getAllConventions(
            @RequestParam(required = false) Boolean showArchived) {
        try {
            String currentUsername = getCurrentUsername();
            User currentUser = userRepository.findByUsername(currentUsername)
                    .orElseThrow(() -> new RuntimeException("User not found"));

            List<Convention> conventions;

            if (Boolean.TRUE.equals(showArchived)) {
                // For ADMIN or users with special permission, show all
                if (currentUser.getRoles().stream().anyMatch(r ->
                        r.getName() == ERole.ROLE_ADMIN)) {
                    conventions = conventionRepository.findAll();
                } else {
                    // For COMMERCIAL_METIER, show only their own conventions
                    conventions = conventionRepository.findByCreatedByAndArchivedTrue(currentUser);
                }
            } else {
                // For ADMIN or users with special permission, show all
                if (currentUser.getRoles().stream().anyMatch(r ->
                        r.getName() == ERole.ROLE_ADMIN)) {
                    conventions = conventionRepository.findByArchivedFalse();
                } else {
                    // For COMMERCIAL_METIER, show only their own conventions
                    conventions = conventionRepository.findByCreatedByAndArchivedFalse(currentUser);
                }
            }

            List<ConventionResponse> conventionResponses = conventions.stream()
                    .map(conventionMapper::toResponse)
                    .collect(Collectors.toList());

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", conventionResponses);
            response.put("count", conventionResponses.size());
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Error fetching conventions: ", e);
            return ResponseEntity.badRequest().body(createErrorResponse("Failed to fetch conventions"));
        }
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN')")
    @Transactional
    public ResponseEntity<?> deleteConvention(@PathVariable Long id) {
        try {
            log.info("Attempting to delete convention with ID: {}", id);

            Convention convention = conventionRepository.findById(id)
                    .orElseThrow(() -> new RuntimeException("Convention not found"));

            if (convention.getArchived()) {
                Map<String, Object> response = new HashMap<>();
                response.put("success", false);
                response.put("message", "Cannot delete an archived convention. Please restore it first.");
                return ResponseEntity.badRequest().body(response);
            }

            List<Facture> invoices = factureRepository.findByConventionId(id);
            if (!invoices.isEmpty()) {
                Map<String, Object> response = new HashMap<>();
                response.put("success", false);
                response.put("message", "Cannot delete convention with existing invoices. Please delete invoices first.");
                response.put("invoiceCount", invoices.size());
                return ResponseEntity.badRequest().body(response);
            }

            conventionRepository.delete(convention);
            log.info("Convention deleted successfully: ID={}", id);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Convention deleted successfully");
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Error deleting convention: {}", e.getMessage(), e);
            return ResponseEntity.badRequest()
                    .body(createErrorResponse("Error deleting convention: " + e.getMessage()));
        }
    }


    @GetMapping("/stats")
    @PreAuthorize("hasAnyRole('ADMIN', 'COMMERCIAL_METIER', 'DECIDEUR', 'CHEF_PROJET')")
    public ResponseEntity<?> getConventionStats()
    {
        try {
            List<Convention> allConventions = conventionRepository.findByArchivedFalse();

            long totalConventions = allConventions.size();
            long Planifie = allConventions.stream()
                    .filter(c -> "PLANIFIE".equals(c.getEtat()))
                    .count();
            long enCours = allConventions.stream()
                    .filter(c -> "EN COURS".equals(c.getEtat()))
                    .count();
            long termine = allConventions.stream()
                    .filter(c -> "TERMINE".equals(c.getEtat()))
                    .count();


            // Financial statistics
            BigDecimal totalMontantHT = allConventions.stream()
                    .map(Convention::getMontantHT)
                    .filter(Objects::nonNull)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            BigDecimal totalMontantTTC = allConventions.stream()
                    .map(Convention::getMontantTTC)
                    .filter(Objects::nonNull)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            long totalNbUsers = allConventions.stream()
                    .map(Convention::getNbUsers)
                    .filter(Objects::nonNull)
                    .mapToLong(Long::longValue)
                    .sum();

            Map<String, Object> stats = new HashMap<>();
            stats.put("total", totalConventions);
            stats.put("Planifie", Planifie);
            stats.put("enCours", enCours);
            stats.put("termine", termine);
            stats.put("totalMontantHT", totalMontantHT);
            stats.put("totalMontantTTC", totalMontantTTC);
            stats.put("totalNbUsers", totalNbUsers);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", stats);
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Error fetching convention statistics: ", e);
            return ResponseEntity.badRequest().body(createErrorResponse("Failed to fetch statistics"));
        }
    }

    @GetMapping("/search")
    @PreAuthorize("hasAnyRole('ADMIN', 'COMMERCIAL_METIER', 'DECIDEUR', 'CHEF_PROJET')")
    public ResponseEntity<?> searchConventions(
            @RequestParam(required = false) String reference,
            @RequestParam(required = false) String libelle,
            @RequestParam(required = false) Long structureId,
            @RequestParam(required = false) Long zoneId,
            @RequestParam(required = false) Long applicationId,
            @RequestParam(required = false) String etat) {
        try {
            List<Convention> conventions = conventionRepository.findByArchivedFalse();

            // Apply filters
            List<Convention> filteredConventions = conventions.stream()
                    .filter(c -> reference == null ||
                            c.getReferenceConvention().toLowerCase().contains(reference.toLowerCase()))
                    .filter(c -> libelle == null ||
                            c.getLibelle().toLowerCase().contains(libelle.toLowerCase()))
                    .filter(c -> structureId == null ||
                            (c.getStructureResponsable().getId().equals(structureId) ||
                                    c.getStructureBeneficiel().getId().equals(structureId)))

                    .filter(c -> applicationId == null ||
                            (c.getApplication() != null && c.getApplication().getId().equals(applicationId)))
                    .filter(c -> etat == null ||
                            (c.getEtat() != null && c.getEtat().equals(etat)))
                    .collect(Collectors.toList());

            List<ConventionResponse> conventionResponses = filteredConventions.stream()
                    .map(conventionMapper::toResponse)
                    .collect(Collectors.toList());

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", conventionResponses);
            response.put("count", conventionResponses.size());
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Error searching conventions: ", e);
            return ResponseEntity.badRequest().body(createErrorResponse("Failed to search conventions"));
        }
    }

    @GetMapping("/by-structure/{structureId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'COMMERCIAL_METIER', 'DECIDEUR', 'CHEF_PROJET')")
    public ResponseEntity<?> getConventionsByStructure(@PathVariable Long structureId) {
        try {
            List<Convention> conventions = conventionRepository.findByArchivedFalse();

            List<Convention> filteredConventions = conventions.stream()
                    .filter(c -> c.getStructureResponsable().getId().equals(structureId) ||
                            c.getStructureBeneficiel().getId().equals(structureId))
                    .collect(Collectors.toList());

            List<ConventionResponse> conventionResponses = filteredConventions.stream()
                    .map(conventionMapper::toResponse)
                    .collect(Collectors.toList());

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", conventionResponses);
            response.put("count", conventionResponses.size());
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Error fetching conventions by structure: ", e);
            return ResponseEntity.badRequest().body(createErrorResponse("Failed to fetch conventions"));
        }
    }

    @GetMapping("/by-application/{applicationId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'COMMERCIAL_METIER', 'DECIDEUR', 'CHEF_PROJET')")
    public ResponseEntity<?> getConventionsByApplication(@PathVariable Long applicationId) {
        try {
            List<Convention> conventions = conventionRepository.findByArchivedFalse();

            List<Convention> filteredConventions = conventions.stream()
                    .filter(c -> c.getApplication() != null &&
                            c.getApplication().getId().equals(applicationId))
                    .collect(Collectors.toList());

            List<ConventionResponse> conventionResponses = filteredConventions.stream()
                    .map(conventionMapper::toResponse)
                    .collect(Collectors.toList());

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", conventionResponses);
            response.put("count", conventionResponses.size());
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Error fetching conventions by application: ", e);
            return ResponseEntity.badRequest().body(createErrorResponse("Failed to fetch conventions"));
        }
    }

    @GetMapping("/by-zone/{zoneId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'COMMERCIAL_METIER', 'DECIDEUR', 'CHEF_PROJET')")
    public ResponseEntity<?> getConventionsByZone(@PathVariable Long zoneId) {
        try {
            List<Convention> conventions = conventionRepository.findByArchivedFalse();

            List<Convention> filteredConventions = conventions.stream()
                    .filter(c -> c.getStructureResponsable().getZoneGeographique() != null &&
                            c.getStructureResponsable().getZoneGeographique().getId().equals(zoneId))
                    .collect(Collectors.toList());

            List<ConventionResponse> conventionResponses = filteredConventions.stream()
                    .map(conventionMapper::toResponse)
                    .collect(Collectors.toList());

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", conventionResponses);
            response.put("count", conventionResponses.size());
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Error fetching conventions by zone: ", e);
            return ResponseEntity.badRequest().body(createErrorResponse("Failed to fetch conventions"));
        }
    }

    @GetMapping("/by-etat/{etat}")
    @PreAuthorize("hasAnyRole('ADMIN', 'COMMERCIAL_METIER', 'DECIDEUR', 'CHEF_PROJET')")
    public ResponseEntity<?> getConventionsByEtat(@PathVariable String etat) {
        try {
            List<Convention> conventions = conventionRepository.findByEtat(etat);

            List<ConventionResponse> conventionResponses = conventions.stream()
                    .map(conventionMapper::toResponse)
                    .collect(Collectors.toList());

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", conventionResponses);
            response.put("count", conventionResponses.size());
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Error fetching conventions by status: ", e);
            return ResponseEntity.badRequest().body(createErrorResponse("Failed to fetch conventions"));
        }
    }

    @GetMapping("/expiring-soon")
    @PreAuthorize("hasAnyRole('ADMIN', 'COMMERCIAL_METIER', 'DECIDEUR', 'CHEF_PROJET')")
    public ResponseEntity<?> getExpiringSoonConventions() {
        try {
            List<Convention> conventions = conventionRepository.findByArchivedFalse();

            // Get conventions ending within the next 30 days
            List<Convention> expiringSoon = conventions.stream()
                    .filter(c -> c.getDateFin() != null)
                    .filter(c -> {
                        long daysUntilEnd = java.time.temporal.ChronoUnit.DAYS.between(
                                java.time.LocalDate.now(), c.getDateFin());
                        return daysUntilEnd >= 0 && daysUntilEnd <= 30;
                    })
                    .collect(Collectors.toList());

            List<ConventionResponse> conventionResponses = expiringSoon.stream()
                    .map(conventionMapper::toResponse)
                    .collect(Collectors.toList());

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", conventionResponses);
            response.put("count", conventionResponses.size());
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Error fetching expiring soon conventions: ", e);
            return ResponseEntity.badRequest().body(createErrorResponse("Failed to fetch conventions"));
        }
    }

    private String getCurrentUsername() {
        return SecurityContextHolder.getContext().getAuthentication().getName();
    }

    private Map<String, Object> createErrorResponse(String message) {
        Map<String, Object> response = new HashMap<>();
        response.put("success", false);
        response.put("message", message);
        return response;
    }



    @GetMapping("/projects/{projectId}/client-structure")
    @PreAuthorize("hasAnyRole('ADMIN', 'COMMERCIAL_METIER')")
    public ResponseEntity<?> getClientStructureFromApplication(@PathVariable Long applicationId) {
        try {
            Structure clientStructure = applicationService.getOrCreateStructureForApplication(applicationId);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", clientStructure);
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Error getting client structure from application: ", e);
            return ResponseEntity.badRequest().body(createErrorResponse(e.getMessage()));
        }
    }

    // Add this method to ConventionController.java
    @GetMapping("/generate-reference")
    @PreAuthorize("hasAnyRole('ADMIN', 'COMMERCIAL_METIER')")
    public ResponseEntity<?> generateReferenceSuggestion() {
        try {
            String suggestedReference = conventionService.generateSuggestedReference();

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("suggestedReference", suggestedReference);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error generating reference suggestion: ", e);
            return ResponseEntity.badRequest().body(createErrorResponse("Failed to generate reference suggestion"));
        }
    }


    /**
     * Sync application dates with all its conventions
     */
    @PostMapping("/applications/{applicationId}/sync-dates")
    @PreAuthorize("hasAnyRole('ADMIN', 'CHEF_PROJET', 'COMMERCIAL_METIER')")
    public ResponseEntity<?> syncApplicationDates(@PathVariable Long applicationId) {
        try {
            conventionService.syncAllApplicationDates(applicationId);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Application dates synced successfully with all conventions");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error syncing application dates: {}", e.getMessage(), e);
            return ResponseEntity.badRequest().body(createErrorResponse(e.getMessage()));
        }
    }


    @PostMapping("/{id}/renew")
    @PreAuthorize("hasAnyRole('ADMIN', 'COMMERCIAL_METIER')")
    @Transactional
    public ResponseEntity<?> renewConvention(@PathVariable Long id, @RequestBody RenewalRequestDTO renewalData) {
        try {
            log.info("========== RENEW CONVENTION ENDPOINT CALLED ==========");

            User currentUser = getCurrentUser();
            Convention newConvention = conventionService.renewConvention(id, renewalData, currentUser);
            ConventionResponse response = conventionMapper.toResponse(newConvention);

            Map<String, Object> apiResponse = new HashMap<>();
            apiResponse.put("success", true);
            apiResponse.put("message", "Convention renouvelée avec succès");
            apiResponse.put("data", response);

            return ResponseEntity.ok(apiResponse);

        } catch (Exception e) {
            log.error("Error renewing convention: {}", e.getMessage(), e);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "Erreur lors du renouvellement: " + e.getMessage());
            return ResponseEntity.badRequest().body(errorResponse);
        }
    }

// In ConventionController.java - Add this endpoint

    @GetMapping("/{id}/previous-versions")
    @PreAuthorize("hasAnyRole('ADMIN', 'COMMERCIAL_METIER', 'CHEF_PROJET', 'DECIDEUR')")
    public ResponseEntity<?> getPreviousVersions(@PathVariable Long id) {
        try {
            log.info("Fetching previous versions for convention ID: {}", id);

            Convention convention = conventionRepository.findById(id)
                    .orElseThrow(() -> new RuntimeException("Convention not found"));

            // Get all old versions for this convention
            List<OldConvention> oldVersions = oldConventionRepository
                    .findByCurrentConventionOrderByRenewalVersionDesc(convention);

            if (oldVersions.isEmpty()) {
                Map<String, Object> response = new HashMap<>();
                response.put("success", true);
                response.put("data", null);
                response.put("message", "Aucune ancienne version trouvée");
                return ResponseEntity.ok(response);
            }

            // For each old version, get its old factures
            List<Map<String, Object>> result = new ArrayList<>();

            for (OldConvention oldConv : oldVersions) {
                Map<String, Object> versionData = new HashMap<>();
                versionData.put("oldConvention", oldConv);

                List<OldFacture> oldFactures = oldFactureRepository.findByOldConvention(oldConv);
                versionData.put("oldFactures", oldFactures);

                result.add(versionData);
            }

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", result);
            response.put("count", result.size());
            response.put("currentVersion", convention.getRenewalVersion());

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Error fetching previous versions: {}", e.getMessage(), e);
            return ResponseEntity.badRequest().body(createErrorResponse(e.getMessage()));
        }
    }

    // Optional: Get a specific old version by its ID
    @GetMapping("/old/{oldId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'COMMERCIAL_METIER', 'CHEF_PROJET', 'DECIDEUR')")
    public ResponseEntity<?> getOldConventionById(@PathVariable Long oldId) {
        try {
            log.info("Fetching old convention with ID: {}", oldId);

            OldConvention oldConvention = oldConventionRepository.findById(oldId)
                    .orElseThrow(() -> new RuntimeException("Old convention not found"));

            List<OldFacture> oldFactures = oldFactureRepository.findByOldConvention(oldConvention);

            Map<String, Object> result = new HashMap<>();
            result.put("oldConvention", oldConvention);
            result.put("oldFactures", oldFactures);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", result);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Error fetching old convention: {}", e.getMessage(), e);
            return ResponseEntity.badRequest().body(createErrorResponse(e.getMessage()));
        }
    }

    private User getCurrentUser() {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));
    }
}