// com.example.back.service.EntitySyncService.java
package com.example.back.service;

import com.example.back.entity.*;
import com.example.back.repository.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Objects;

@Service
@Slf4j
public class EntitySyncService {

    private final ApplicationRepository applicationRepository;

    private final ConventionRepository conventionRepository;

    private final FactureRepository factureRepository;

    private final StructureRepository structureRepository;


    public EntitySyncService(ApplicationRepository applicationRepository, ConventionRepository conventionRepository, FactureRepository factureRepository, StructureRepository structureRepository) {
        this.applicationRepository = applicationRepository;
        this.conventionRepository = conventionRepository;
        this.factureRepository = factureRepository;
        this.structureRepository = structureRepository;
    }


    @Transactional
    public void syncApplicationChanges(Application oldApp, Application newApp) {
        log.info("========== SYNCING APPLICATION CHANGES ==========");
        log.info("Old App: {} - Client: {}", oldApp.getCode(), oldApp.getClientName());
        log.info("New App: {} - Client: {}", newApp.getCode(), newApp.getClientName());

        boolean clientNameChanged = !oldApp.getClientName().equals(newApp.getClientName());
        boolean clientEmailChanged = !Objects.equals(oldApp.getClientEmail(), newApp.getClientEmail());
        boolean clientPhoneChanged = !Objects.equals(oldApp.getClientPhone(), newApp.getClientPhone());

        if (!clientNameChanged && !clientEmailChanged && !clientPhoneChanged) {
            log.info("No client info changes detected, skipping sync");
            return;
        }

        // 1. Find all conventions for this application
        List<Convention> conventions = conventionRepository.findByApplication(newApp);
        log.info("Found {} conventions to update", conventions.size());

        for (Convention convention : conventions) {
            boolean conventionUpdated = false;

            // Update structure beneficiel (client structure)
            Structure structureBen = convention.getStructureBeneficiel();
            if (structureBen != null) {
                if (clientNameChanged) {
                    log.info("Updating structure name from '{}' to '{}'", structureBen.getName(), newApp.getClientName());
                    structureBen.setName(newApp.getClientName());
                    conventionUpdated = true;
                }
                if (clientEmailChanged && newApp.getClientEmail() != null) {
                    log.info("Updating structure email to '{}'", newApp.getClientEmail());
                    structureBen.setEmail(newApp.getClientEmail());
                    conventionUpdated = true;
                }
                if (clientPhoneChanged && newApp.getClientPhone() != null) {
                    log.info("Updating structure phone to '{}'", newApp.getClientPhone());
                    structureBen.setPhone(newApp.getClientPhone());
                    conventionUpdated = true;
                }

                if (conventionUpdated) {
                    structureRepository.save(structureBen);
                    log.info("✅ Updated structure beneficiel for convention: {}", convention.getReferenceConvention());
                }
            }

            // Update all factures for this convention
            List<Facture> factures = factureRepository.findByConventionId(convention.getId());
            for (Facture facture : factures) {
                if (clientNameChanged) {
                    String oldNotes = facture.getNotes();
                    String newNotes = oldNotes != null ?
                            oldNotes.replace(oldApp.getClientName(), newApp.getClientName()) :
                            String.format("Facture pour %s", newApp.getClientName());
                    facture.setNotes(newNotes);
                    factureRepository.save(facture);
                    log.info("✅ Updated facture notes: {}", facture.getNumeroFacture());
                }
            }
        }

        // 2. Also update any direct structure that matches the old client name
        if (clientNameChanged) {
            List<Structure> structures = structureRepository.findByNameContaining(oldApp.getClientName());
            for (Structure struct : structures) {
                if ("Client".equals(struct.getTypeStructure())) {
                    struct.setName(newApp.getClientName());
                    if (clientEmailChanged) struct.setEmail(newApp.getClientEmail());
                    if (clientPhoneChanged) struct.setPhone(newApp.getClientPhone());
                    structureRepository.save(struct);
                    log.info("✅ Updated client structure: {}", struct.getName());
                }
            }
        }

        log.info("========== FINISHED SYNCING APPLICATION CHANGES ==========");
    }


    @Transactional
    public void syncConventionChanges(Convention oldConv, Convention newConv) {
        log.info("========== SYNCING CONVENTION CHANGES ==========");

        boolean datesChanged = !oldConv.getDateDebut().equals(newConv.getDateDebut()) ||
                !Objects.equals(oldConv.getDateFin(), newConv.getDateFin());

        boolean financialChanged = !oldConv.getMontantHT().equals(newConv.getMontantHT()) ||
                !oldConv.getTva().equals(newConv.getTva());

        if (datesChanged && newConv.getApplication() != null) {
            Application app = newConv.getApplication();
            log.info("Updating application {} dates from convention", app.getCode());

            // Get the earliest start and latest end from all conventions
            List<Convention> allConvs = conventionRepository.findByApplication(app);
            LocalDate earliestStart = newConv.getDateDebut();
            LocalDate latestEnd = newConv.getDateFin();

            for (Convention conv : allConvs) {
                if (conv.getDateDebut() != null && conv.getDateDebut().isBefore(earliestStart)) {
                    earliestStart = conv.getDateDebut();
                }
                if (conv.getDateFin() != null && (latestEnd == null || conv.getDateFin().isAfter(latestEnd))) {
                    latestEnd = conv.getDateFin();
                }
            }

            app.setDateDebut(earliestStart);
            app.setDateFin(latestEnd);
            applicationRepository.save(app);
            log.info("✅ Updated application dates: {} to {}", earliestStart, latestEnd);
        }

        // Financial changes are already handled by regenerateInvoicesForConvention
        if (financialChanged) {
            log.info("Financial changes detected - invoices will be regenerated separately");
        }

        log.info("========== FINISHED SYNCING CONVENTION CHANGES ==========");
    }


    @Transactional
    public void syncStructureChanges(Structure oldStruct, Structure newStruct) {
        log.info("========== SYNCING STRUCTURE CHANGES ==========");

        boolean nameChanged = !oldStruct.getName().equals(newStruct.getName());
        boolean emailChanged = !Objects.equals(oldStruct.getEmail(), newStruct.getEmail());
        boolean phoneChanged = !Objects.equals(oldStruct.getPhone(), newStruct.getPhone());

        if (!nameChanged && !emailChanged && !phoneChanged) {
            log.info("No changes detected, skipping sync");
            return;
        }

        log.info("Structure changes - Name: {}→{}, Email: {}→{}, Phone: {}→{}",
                oldStruct.getName(), newStruct.getName(),
                oldStruct.getEmail(), newStruct.getEmail(),
                oldStruct.getPhone(), newStruct.getPhone());

        // 1. Find conventions where this structure is beneficiel (client)
        List<Convention> conventions = conventionRepository.findByStructureBeneficielId(newStruct.getId());
        log.info("Found {} conventions where this structure is beneficiel", conventions.size());

        for (Convention convention : conventions) {
            if (nameChanged) {
                log.info("Updating convention {} beneficiel name", convention.getReferenceConvention());
                // The reference will be updated automatically since it's the same object
            }
            conventionRepository.save(convention);

            // Update the associated application client info
            if (convention.getApplication() != null) {
                Application app = convention.getApplication();
                if (nameChanged) {
                    app.setClientName(newStruct.getName());
                }
                if (emailChanged) {
                    app.setClientEmail(newStruct.getEmail());
                }
                if (phoneChanged) {
                    app.setClientPhone(newStruct.getPhone());
                }
                applicationRepository.save(app);
                log.info("✅ Updated application {} client info", app.getCode());
            }
        }

        // 2. Find applications directly linked to this client name
        if (nameChanged) {
            List<Application> applications = applicationRepository.findByClientName(oldStruct.getName());
            for (Application app : applications) {
                app.setClientName(newStruct.getName());
                if (emailChanged) app.setClientEmail(newStruct.getEmail());
                if (phoneChanged) app.setClientPhone(newStruct.getPhone());
                applicationRepository.save(app);
                log.info("✅ Updated application {} directly", app.getCode());
            }
        }

        log.info("========== FINISHED SYNCING STRUCTURE CHANGES ==========");
    }
}