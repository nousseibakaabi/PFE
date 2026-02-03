package com.example.back.controller;

import com.example.back.entity.*;
import com.example.back.payload.request.ArchiveConventionRequest;
import com.example.back.payload.request.ConventionRequest;
import com.example.back.payload.response.ConventionResponse;
import com.example.back.repository.*;
import com.example.back.service.ConventionService;
import com.example.back.service.ProjectService;
import com.example.back.service.mapper.ConventionMapper;
import jakarta.transaction.Transactional;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
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
    private ProjectService projectService;

    @Autowired
    private ProjectRepository projectRepository;

    @Autowired
    private UserRepository userRepository;

    // CRUD Operations

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
            // Get current user
            String currentUsername = getCurrentUsername();
            User currentUser = userRepository.findByUsername(currentUsername)
                    .orElseThrow(() -> new RuntimeException("User not found"));
            log.info("Creating convention with reference: {}", request.getReferenceConvention());

            // Check if reference already exists
            if (conventionRepository.existsByReferenceConvention(request.getReferenceConvention())) {
                return ResponseEntity.badRequest()
                        .body(createErrorResponse("Convention with this reference already exists"));
            }

            // Check if ERP reference exists
            if (request.getReferenceERP() != null &&
                    conventionRepository.existsByReferenceERP(request.getReferenceERP())) {
                return ResponseEntity.badRequest()
                        .body(createErrorResponse("ERP reference already exists"));
            }

            // Fetch related entities
            Optional<Structure> structureInterne = structureRepository.findById(request.getStructureInterneId());
            Optional<Structure> structureExterne = structureRepository.findById(request.getStructureExterneId());
            Optional<ZoneGeographique> zone = zoneGeographiqueRepository.findById(request.getZoneId());
            Optional<Project> project = projectRepository.findById(request.getProjectId());

            if (structureInterne.isEmpty() || structureExterne.isEmpty() ||
                    zone.isEmpty() || project.isEmpty()) {
                return ResponseEntity.badRequest()
                        .body(createErrorResponse("Invalid structure, zone, or project"));
            }

            // Create new convention
            Convention convention = new Convention();
            convention.setReferenceConvention(request.getReferenceConvention());
            convention.setReferenceERP(request.getReferenceERP());
            convention.setLibelle(request.getLibelle());
            convention.setDateDebut(request.getDateDebut());
            convention.setDateFin(request.getDateFin());
            convention.setDateSignature(request.getDateSignature());
            convention.setStructureInterne(structureInterne.get());
            convention.setStructureExterne(structureExterne.get());
            convention.setZone(zone.get());
            convention.setProject(project.get());
            convention.setMontantTotal(request.getMontantTotal());
            convention.setPeriodicite(request.getPeriodicite());

            // Set the creator
            convention.setCreatedBy(currentUser);

            Convention saved = conventionRepository.save(convention);


            // Generate invoices automatically
            try {
                conventionService.generateInvoicesForConvention(saved);
                log.info("Invoices generated for convention: {}", saved.getReferenceConvention());
            } catch (Exception e) {
                log.error("Failed to generate invoices for convention {}: {}",
                        saved.getReferenceConvention(), e.getMessage());
            }

            // Update project progress
            try {
                projectService.calculateProjectProgress(saved.getProject().getId());
            } catch (Exception e) {
                log.error("Failed to update project progress: {}", e.getMessage());
            }

            // Trigger immediate status update
            conventionService.updateConventionStatusRealTime(saved.getId());

            // Refresh the saved convention to get latest data
            saved = conventionRepository.findById(saved.getId()).orElse(saved);
            ConventionResponse response = conventionMapper.toResponse(saved);

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

            Optional<Convention> existing = conventionRepository.findById(id);
            if (existing.isEmpty()) {
                log.warn("Convention not found with ID: {}", id);
                return ResponseEntity.notFound().build();
            }

            Convention convention = existing.get();

            // Check if new reference conflicts
            if (!convention.getReferenceConvention().equals(request.getReferenceConvention()) &&
                    conventionRepository.existsByReferenceConvention(request.getReferenceConvention())) {
                log.warn("Reference conflict for convention update: {}", request.getReferenceConvention());
                return ResponseEntity.badRequest()
                        .body(createErrorResponse("Convention with this reference already exists"));
            }

            // Check if new ERP reference conflicts
            if (request.getReferenceERP() != null &&
                    !request.getReferenceERP().equals(convention.getReferenceERP()) &&
                    conventionRepository.existsByReferenceERP(request.getReferenceERP())) {
                return ResponseEntity.badRequest()
                        .body(createErrorResponse("ERP reference already exists"));
            }

            // Fetch related entities
            Optional<Structure> structureInterne = structureRepository.findById(request.getStructureInterneId());
            Optional<Structure> structureExterne = structureRepository.findById(request.getStructureExterneId());
            Optional<ZoneGeographique> zone = zoneGeographiqueRepository.findById(request.getZoneId());
            Optional<Project> project = projectRepository.findById(request.getProjectId());

            if (structureInterne.isEmpty() || structureExterne.isEmpty() ||
                    zone.isEmpty() || project.isEmpty()) {
                log.warn("Invalid structure, zone, or application");
                return ResponseEntity.badRequest()
                        .body(createErrorResponse("Invalid structure, zone, or application"));
            }

            // Update convention
            convention.setReferenceConvention(request.getReferenceConvention());
            convention.setReferenceERP(request.getReferenceERP());
            convention.setLibelle(request.getLibelle());
            convention.setDateDebut(request.getDateDebut());
            convention.setDateFin(request.getDateFin());
            convention.setDateSignature(request.getDateSignature());
            convention.setStructureInterne(structureInterne.get());
            convention.setStructureExterne(structureExterne.get());
            convention.setZone(zone.get());
            convention.setProject(project.get());
            convention.setMontantTotal(request.getMontantTotal());
            convention.setPeriodicite(request.getPeriodicite());

            Convention updated = conventionRepository.save(convention);
            log.info("Convention updated successfully with ID: {}", id);

            ConventionResponse response = conventionMapper.toResponse(updated);

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

    @PostMapping("/{id}/archive")
    @PreAuthorize("hasAnyRole('ADMIN', 'COMMERCIAL_METIER')")
    @Transactional
    public ResponseEntity<?> archiveConvention(
            @PathVariable Long id,
            @Valid @RequestBody ArchiveConventionRequest request) {

        try {
            log.info("Attempting to archive convention with ID: {}", id);
            String currentUsername = getCurrentUsername();

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

    @PostMapping("/{id}/restore")
    @PreAuthorize("hasAnyRole('ADMIN', 'COMMERCIAL_METIER')")
    @Transactional
    public ResponseEntity<?> restoreConvention(@PathVariable Long id) {
        try {
            log.info("Attempting to restore convention with ID: {}", id);
            String currentUsername = getCurrentUsername();

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
                    .orElseThrow(() -> {
                        log.warn("Convention not found with ID: {}", id);
                        return new RuntimeException("Convention not found");
                    });

            // Check if convention can be deleted
            if (convention.getArchived()) {
                Map<String, Object> response = new HashMap<>();
                response.put("success", false);
                response.put("message", "Cannot delete an archived convention. Please restore it first.");
                return ResponseEntity.badRequest().body(response);
            }

            // Check if there are any invoices
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
    public ResponseEntity<?> getConventionStats() {
        try {
            List<Convention> allConventions = conventionRepository.findByArchivedFalse();

            long totalConventions = allConventions.size();
            long enAttente = allConventions.stream()
                    .filter(c -> "EN_ATTENTE".equals(c.getEtat()))
                    .count();
            long enCours = allConventions.stream()
                    .filter(c -> "EN_COURS".equals(c.getEtat()))
                    .count();
            long termine = allConventions.stream()
                    .filter(c -> "TERMINE".equals(c.getEtat()))
                    .count();
            long enRetard = allConventions.stream()
                    .filter(c -> "EN_RETARD".equals(c.getEtat()))
                    .count();

            Map<String, Object> stats = new HashMap<>();
            stats.put("total", totalConventions);
            stats.put("enAttente", enAttente);
            stats.put("enCours", enCours);
            stats.put("termine", termine);
            stats.put("enRetard", enRetard);

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
                            (c.getStructureInterne().getId().equals(structureId) ||
                                    c.getStructureExterne().getId().equals(structureId)))
                    .filter(c -> zoneId == null ||
                            (c.getZone() != null && c.getZone().getId().equals(zoneId)))
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
                    .filter(c -> c.getStructureInterne().getId().equals(structureId) ||
                            c.getStructureExterne().getId().equals(structureId))
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
                    .filter(c -> c.getZone() != null &&
                            c.getZone().getId().equals(zoneId))
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
    public ResponseEntity<?> getClientStructureFromProject(@PathVariable Long projectId) {
        try {
            Structure clientStructure = projectService.getOrCreateStructureForProject(projectId);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", clientStructure);
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Error getting client structure from project: ", e);
            return ResponseEntity.badRequest().body(createErrorResponse(e.getMessage()));
        }
    }
}