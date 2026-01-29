package com.example.back.controller;

import com.example.back.entity.*;
import com.example.back.payload.request.NomenclatureRequest;
import com.example.back.payload.request.StructureRequest;
import com.example.back.repository.*;
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
@RequestMapping("/admin/nomenclatures")
@PreAuthorize("hasRole('ADMIN')")
@Slf4j
public class NomenclatureController {

    @Autowired
    private ApplicationRepository applicationRepository;

    @Autowired
    private ZoneGeographiqueRepository zoneGeographiqueRepository;

    @Autowired
    private StructureRepository structureRepository;

    @Autowired
    private FactureRepository factureRepository;

    @Autowired
    private ConventionRepository conventionRepository;

    // ==================== APPLICATIONS ====================

    @GetMapping("/applications")
    @PreAuthorize("hasAnyRole('ADMIN', 'COMMERCIAL_METIER', 'DECIDEUR', 'CHEF_PROJET')")
    public ResponseEntity<?> getAllApplications() {
        try {
            List<Application> applications = applicationRepository.findAll();
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", applications);
            response.put("count", applications.size());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(createErrorResponse("Failed to fetch applications"));
        }
    }

    @GetMapping("/applications/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'COMMERCIAL_METIER', 'DECIDEUR', 'CHEF_PROJET')")
    public ResponseEntity<?> getApplicationById(@PathVariable Long id) {
        try {
            Optional<Application> application = applicationRepository.findById(id);
            if (application.isEmpty()) {
                return ResponseEntity.notFound().build();
            }
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", application.get());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(createErrorResponse("Failed to fetch application"));
        }
    }

    @PostMapping("/applications")
    public ResponseEntity<?> createApplication(@Valid @RequestBody NomenclatureRequest request) {
        try {
            if (applicationRepository.existsByCode(request.getCode())) {
                return ResponseEntity.badRequest()
                        .body(createErrorResponse("Application with this code already exists"));
            }

            if (applicationRepository.existsByName(request.getName())) {
                return ResponseEntity.badRequest()
                        .body(createErrorResponse("Application with this name already exists"));
            }

            Application application = new Application();
            application.setCode(request.getCode());
            application.setName(request.getName());
            application.setDescription(request.getDescription());

            Application saved = applicationRepository.save(application);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Application created successfully");
            response.put("data", saved);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(createErrorResponse("Failed to create application"));
        }
    }

    @PutMapping("/applications/{id}")
    public ResponseEntity<?> updateApplication(@PathVariable Long id,
                                               @Valid @RequestBody NomenclatureRequest request) {
        try {
            Optional<Application> existing = applicationRepository.findById(id);
            if (existing.isEmpty()) {
                return ResponseEntity.notFound().build();
            }

            Application application = existing.get();

            // Check if new code conflicts with another application
            if (!application.getCode().equals(request.getCode()) &&
                    applicationRepository.existsByCode(request.getCode())) {
                return ResponseEntity.badRequest()
                        .body(createErrorResponse("Application with this code already exists"));
            }

            // Check if new name conflicts with another application
            if (!application.getName().equals(request.getName()) &&
                    applicationRepository.existsByName(request.getName())) {
                return ResponseEntity.badRequest()
                        .body(createErrorResponse("Application with this name already exists"));
            }

            application.setCode(request.getCode());
            application.setName(request.getName());
            application.setDescription(request.getDescription());

            Application updated = applicationRepository.save(application);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Application updated successfully");
            response.put("data", updated);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(createErrorResponse("Failed to update application"));
        }
    }

    @DeleteMapping("/applications/{id}")
    public ResponseEntity<?> deleteApplication(@PathVariable Long id) {
        try {
            if (!applicationRepository.existsById(id)) {
                return ResponseEntity.notFound().build();
            }

            // Check if application is used in conventions
            Long conventionCount = conventionRepository.countByProjectId(id);
            if (conventionCount > 0) {
                return ResponseEntity.badRequest()
                        .body(createErrorResponse(
                                String.format("Cannot delete application. It is used in %d convention(s).",
                                        conventionCount)));
            }

            applicationRepository.deleteById(id);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Application deleted successfully");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(createErrorResponse("Failed to delete application"));
        }
    }

    // ==================== ZONES GÉOGRAPHIQUES ====================

    @GetMapping("/zones")
    @PreAuthorize("hasAnyRole('ADMIN', 'COMMERCIAL_METIER', 'DECIDEUR', 'CHEF_PROJET')")
    public ResponseEntity<?> getAllZones() {
        try {
            List<ZoneGeographique> zones = zoneGeographiqueRepository.findAll();
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", zones);
            response.put("count", zones.size());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(createErrorResponse("Failed to fetch zones"));
        }
    }

    @GetMapping("/zones/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'COMMERCIAL_METIER', 'DECIDEUR', 'CHEF_PROJET')")
    public ResponseEntity<?> getZoneById(@PathVariable Long id) {
        try {
            Optional<ZoneGeographique> zone = zoneGeographiqueRepository.findById(id);
            if (zone.isEmpty()) {
                return ResponseEntity.notFound().build();
            }
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", zone.get());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(createErrorResponse("Failed to fetch zone"));
        }
    }

    @PostMapping("/zones")
    public ResponseEntity<?> createZone(@Valid @RequestBody NomenclatureRequest request) {
        try {
            if (zoneGeographiqueRepository.existsByCode(request.getCode())) {
                return ResponseEntity.badRequest()
                        .body(createErrorResponse("Zone with this code already exists"));
            }

            if (zoneGeographiqueRepository.existsByName(request.getName())) {
                return ResponseEntity.badRequest()
                        .body(createErrorResponse("Zone with this name already exists"));
            }

            ZoneGeographique zone = new ZoneGeographique();
            zone.setCode(request.getCode());
            zone.setName(request.getName());
            zone.setDescription(request.getDescription());

            ZoneGeographique saved = zoneGeographiqueRepository.save(zone);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Zone created successfully");
            response.put("data", saved);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(createErrorResponse("Failed to create zone"));
        }
    }

    @PutMapping("/zones/{id}")
    public ResponseEntity<?> updateZone(@PathVariable Long id,
                                        @Valid @RequestBody NomenclatureRequest request) {
        try {
            Optional<ZoneGeographique> existing = zoneGeographiqueRepository.findById(id);
            if (existing.isEmpty()) {
                return ResponseEntity.notFound().build();
            }

            ZoneGeographique zone = existing.get();

            if (!zone.getCode().equals(request.getCode()) &&
                    zoneGeographiqueRepository.existsByCode(request.getCode())) {
                return ResponseEntity.badRequest()
                        .body(createErrorResponse("Zone with this code already exists"));
            }

            if (!zone.getName().equals(request.getName()) &&
                    zoneGeographiqueRepository.existsByName(request.getName())) {
                return ResponseEntity.badRequest()
                        .body(createErrorResponse("Zone with this name already exists"));
            }

            zone.setCode(request.getCode());
            zone.setName(request.getName());
            zone.setDescription(request.getDescription());

            ZoneGeographique updated = zoneGeographiqueRepository.save(zone);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Zone updated successfully");
            response.put("data", updated);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(createErrorResponse("Failed to update zone"));
        }
    }

    @DeleteMapping("/zones/{id}")
    public ResponseEntity<?> deleteZone(@PathVariable Long id) {
        try {
            if (!zoneGeographiqueRepository.existsById(id)) {
                return ResponseEntity.notFound().build();
            }

            // Check if zone is used in conventions
            Long conventionCount = conventionRepository.countByZoneId(id);
            if (conventionCount > 0) {
                return ResponseEntity.badRequest()
                        .body(createErrorResponse(
                                String.format("Cannot delete zone. It is used in %d convention(s).",
                                        conventionCount)));
            }

            zoneGeographiqueRepository.deleteById(id);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Zone deleted successfully");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(createErrorResponse("Failed to delete zone"));
        }
    }

    // ==================== STRUCTURES ====================

    @GetMapping("/structures")
    @PreAuthorize("hasAnyRole('ADMIN', 'COMMERCIAL_METIER', 'DECIDEUR', 'CHEF_PROJET')")
    public ResponseEntity<?> getAllStructures() {
        try {
            List<Structure> structures = structureRepository.findAll();
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", structures);
            response.put("count", structures.size());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(createErrorResponse("Failed to fetch structures"));
        }
    }

    @GetMapping("/structures/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'COMMERCIAL_METIER', 'DECIDEUR', 'CHEF_PROJET')")
    public ResponseEntity<?> getStructureById(@PathVariable Long id) {
        try {
            Optional<Structure> structure = structureRepository.findById(id);
            if (structure.isEmpty()) {
                return ResponseEntity.notFound().build();
            }
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", structure.get());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(createErrorResponse("Failed to fetch structure"));
        }
    }

    @PostMapping("/structures")
    public ResponseEntity<?> createStructure(@Valid @RequestBody StructureRequest request) {
        try {
            if (structureRepository.existsByCode(request.getCode())) {
                return ResponseEntity.badRequest()
                        .body(createErrorResponse("Structure with this code already exists"));
            }

            if (structureRepository.existsByName(request.getName())) {
                return ResponseEntity.badRequest()
                        .body(createErrorResponse("Structure with this name already exists"));
            }

            Structure structure = new Structure();
            structure.setCode(request.getCode());
            structure.setName(request.getName());
            structure.setDescription(request.getDescription());
            structure.setAddress(request.getAddress());
            structure.setPhone(request.getPhone());
            structure.setEmail(request.getEmail());
            structure.setTypeStructure(request.getTypeStructure());

            Structure saved = structureRepository.save(structure);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Structure created successfully");
            response.put("data", saved);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(createErrorResponse("Failed to create structure"));
        }
    }

    @PutMapping("/structures/{id}")
    public ResponseEntity<?> updateStructure(@PathVariable Long id,
                                             @Valid @RequestBody StructureRequest request) {
        try {
            Optional<Structure> existing = structureRepository.findById(id);
            if (existing.isEmpty()) {
                return ResponseEntity.notFound().build();
            }

            Structure structure = existing.get();

            if (!structure.getCode().equals(request.getCode()) &&
                    structureRepository.existsByCode(request.getCode())) {
                return ResponseEntity.badRequest()
                        .body(createErrorResponse("Structure with this code already exists"));
            }

            if (!structure.getName().equals(request.getName()) &&
                    structureRepository.existsByName(request.getName())) {
                return ResponseEntity.badRequest()
                        .body(createErrorResponse("Structure with this name already exists"));
            }

            structure.setCode(request.getCode());
            structure.setName(request.getName());
            structure.setDescription(request.getDescription());
            structure.setAddress(request.getAddress());
            structure.setPhone(request.getPhone());
            structure.setEmail(request.getEmail());
            structure.setTypeStructure(request.getTypeStructure());

            Structure updated = structureRepository.save(structure);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Structure updated successfully");
            response.put("data", updated);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(createErrorResponse("Failed to update structure"));
        }
    }

    @DeleteMapping("/structures/{id}")
    public ResponseEntity<?> deleteStructure(@PathVariable Long id) {
        try {
            if (!structureRepository.existsById(id)) {
                return ResponseEntity.notFound().build();
            }

            // Check if structure is used in conventions (as interne or externe)
            Long totalCount = conventionRepository.countByStructureInterneIdOrStructureExterneId(id);

            if (totalCount > 0) {
                return ResponseEntity.badRequest()
                        .body(createErrorResponse(
                                String.format("Cannot delete structure. It is used in %d convention(s).",
                                        totalCount)));
            }

            structureRepository.deleteById(id);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Structure deleted successfully");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(createErrorResponse("Failed to delete structure"));
        }
    }

    // ==================== UTILITY METHODS ====================

    private Map<String, Object> createErrorResponse(String message) {
        Map<String, Object> response = new HashMap<>();
        response.put("success", false);
        response.put("message", message);
        return response;
    }

    @GetMapping("/stats")
    public ResponseEntity<?> getStats() {
        try {
            long appsCount = applicationRepository.count();
            long zonesCount = zoneGeographiqueRepository.count();
            long structuresCount = structureRepository.count();

            Map<String, Object> stats = new HashMap<>();
            stats.put("applications", appsCount);
            stats.put("zones", zonesCount);
            stats.put("structures", structuresCount);
            stats.put("total", appsCount + zonesCount + structuresCount);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", stats);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(createErrorResponse("Failed to fetch stats"));
        }
    }

    @GetMapping("/commercial/stats")
    public ResponseEntity<?> getCommercialStats() {
        try {
            // Get convention stats
            long conventionsCount = conventionRepository.count();
            long conventionsEnCours = conventionRepository.findByEtat("EN_COURS").size();

            // Find expired conventions manually using streams
            long conventionsExpirees = conventionRepository.findAll().stream()
                    .filter(c -> c.getDateFin() != null &&
                            c.getDateFin().isBefore(LocalDate.now()) &&
                            !"TERMINE".equals(c.getEtat()) &&
                            !"ARCHIVE".equals(c.getEtat()) &&
                            !Boolean.TRUE.equals(c.getArchived()))
                    .count();

            // Get invoice stats
            long facturesCount = factureRepository.count();
            long facturesPayees = factureRepository.findByStatutPaiement("PAYE").size();
            long facturesNonPayees = factureRepository.findByStatutPaiement("NON_PAYE").size();
            long facturesEnRetard = factureRepository.findFacturesEnRetard(LocalDate.now()).size();

            Map<String, Object> stats = new HashMap<>();
            stats.put("conventionsTotal", conventionsCount);
            stats.put("conventionsEnCours", conventionsEnCours);
            stats.put("conventionsExpirees", conventionsExpirees);
            stats.put("facturesTotal", facturesCount);
            stats.put("facturesPayees", facturesPayees);
            stats.put("facturesNonPayees", facturesNonPayees);
            stats.put("facturesEnRetard", facturesEnRetard);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", stats);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(createErrorResponse("Failed to fetch commercial stats"));
        }
    }
}