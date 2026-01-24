package com.example.back.controller;

import com.example.back.entity.Convention;
import com.example.back.entity.Structure;
import com.example.back.entity.ZoneGeographique;
import com.example.back.payload.request.ConventionRequest;
import com.example.back.repository.ConventionRepository;
import com.example.back.repository.StructureRepository;
import com.example.back.repository.ZoneGeographiqueRepository;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/conventions")
@PreAuthorize("hasAnyRole('ADMIN', 'COMMERCIAL_METIER')")
@Slf4j
public class ConventionController {

    @Autowired
    private ConventionRepository conventionRepository;

    @Autowired
    private StructureRepository structureRepository;

    @Autowired
    private ZoneGeographiqueRepository zoneGeographiqueRepository;

    // CRUD Operations

    @GetMapping
    public ResponseEntity<?> getAllConventions() {
        try {
            List<Convention> conventions = conventionRepository.findAll();
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", conventions);
            response.put("count", conventions.size());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(createErrorResponse("Failed to fetch conventions"));
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getConventionById(@PathVariable Long id) {
        try {
            Optional<Convention> convention = conventionRepository.findById(id);
            if (convention.isEmpty()) {
                return ResponseEntity.notFound().build();
            }
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", convention.get());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(createErrorResponse("Failed to fetch convention"));
        }
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'COMMERCIAL_METIER')")
    public ResponseEntity<?> createConvention(@Valid @RequestBody ConventionRequest request) {
        try {
            // Check if reference already exists
            if (conventionRepository.existsByReference(request.getReference())) {
                return ResponseEntity.badRequest()
                        .body(createErrorResponse("Convention with this reference already exists"));
            }

            // Fetch related entities
            Optional<Structure> structure = structureRepository.findById(request.getStructureId());
            Optional<ZoneGeographique> zone = zoneGeographiqueRepository.findById(request.getGouvernoratId());

            if (structure.isEmpty() || zone.isEmpty()) {
                return ResponseEntity.badRequest()
                        .body(createErrorResponse("Invalid structure or gouvernorat"));
            }

            // Create new convention
            Convention convention = new Convention();
            convention.setReference(request.getReference());
            convention.setLibelle(request.getLibelle());
            convention.setDateDebut(request.getDateDebut());
            convention.setDateFin(request.getDateFin());
            convention.setDateSignature(request.getDateSignature());
            convention.setStructure(structure.get());
            convention.setGouvernorat(zone.get());
            convention.setMontantTotal(request.getMontantTotal());
            convention.setModalitesPaiement(request.getModalitesPaiement());
            convention.setPeriodicite(request.getPeriodicite());
            convention.setEtat(request.getEtat());

            Convention saved = conventionRepository.save(convention);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Convention created successfully");
            response.put("data", saved);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error creating convention: ", e);
            return ResponseEntity.badRequest().body(createErrorResponse("Failed to create convention"));
        }
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'COMMERCIAL_METIER')")
    public ResponseEntity<?> updateConvention(@PathVariable Long id,
                                              @Valid @RequestBody ConventionRequest request) {
        try {
            Optional<Convention> existing = conventionRepository.findById(id);
            if (existing.isEmpty()) {
                return ResponseEntity.notFound().build();
            }

            Convention convention = existing.get();

            // Check if new reference conflicts
            if (!convention.getReference().equals(request.getReference()) &&
                    conventionRepository.existsByReference(request.getReference())) {
                return ResponseEntity.badRequest()
                        .body(createErrorResponse("Convention with this reference already exists"));
            }

            // Fetch related entities
            Optional<Structure> structure = structureRepository.findById(request.getStructureId());
            Optional<ZoneGeographique> zone = zoneGeographiqueRepository.findById(request.getGouvernoratId());

            if (structure.isEmpty() || zone.isEmpty()) {
                return ResponseEntity.badRequest()
                        .body(createErrorResponse("Invalid structure or gouvernorat"));
            }

            // Update convention
            convention.setReference(request.getReference());
            convention.setLibelle(request.getLibelle());
            convention.setDateDebut(request.getDateDebut());
            convention.setDateFin(request.getDateFin());
            convention.setDateSignature(request.getDateSignature());
            convention.setStructure(structure.get());
            convention.setGouvernorat(zone.get());
            convention.setMontantTotal(request.getMontantTotal());
            convention.setModalitesPaiement(request.getModalitesPaiement());
            convention.setPeriodicite(request.getPeriodicite());
            convention.setEtat(request.getEtat());

            Convention updated = conventionRepository.save(convention);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Convention updated successfully");
            response.put("data", updated);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error updating convention: ", e);
            return ResponseEntity.badRequest().body(createErrorResponse("Failed to update convention"));
        }
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> deleteConvention(@PathVariable Long id) {
        try {
            if (!conventionRepository.existsById(id)) {
                return ResponseEntity.notFound().build();
            }

            conventionRepository.deleteById(id);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Convention deleted successfully");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(createErrorResponse("Failed to delete convention"));
        }
    }

    // Additional endpoints

    @GetMapping("/structure/{structureId}")
    public ResponseEntity<?> getConventionsByStructure(@PathVariable Long structureId) {
        try {
            List<Convention> conventions = conventionRepository.findByStructureId(structureId);
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", conventions);
            response.put("count", conventions.size());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(createErrorResponse("Failed to fetch conventions"));
        }
    }

    @GetMapping("/etat/{etat}")
    public ResponseEntity<?> getConventionsByEtat(@PathVariable String etat) {
        try {
            List<Convention> conventions = conventionRepository.findByEtat(etat);
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", conventions);
            response.put("count", conventions.size());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(createErrorResponse("Failed to fetch conventions"));
        }
    }

    @GetMapping("/expirees")
    public ResponseEntity<?> getConventionsExpirees() {
        try {
            List<Convention> conventions = conventionRepository.findConventionsExpirees(java.time.LocalDate.now());
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", conventions);
            response.put("count", conventions.size());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(createErrorResponse("Failed to fetch conventions"));
        }
    }

    private Map<String, Object> createErrorResponse(String message) {
        Map<String, Object> response = new HashMap<>();
        response.put("success", false);
        response.put("message", message);
        return response;
    }
}