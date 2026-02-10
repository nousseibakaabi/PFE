// ConventionMapper.java - UPDATED
package com.example.back.service.mapper;

import com.example.back.entity.Convention;
import com.example.back.entity.Facture;
import com.example.back.payload.response.ConventionResponse;
import com.example.back.payload.response.FactureResponse;
import org.springframework.stereotype.Service;

import java.util.stream.Collectors;

@Service
public class ConventionMapper {

    public ConventionResponse toResponse(Convention convention) {
        ConventionResponse response = new ConventionResponse();

        // Basic fields
        response.setId(convention.getId());
        response.setReferenceConvention(convention.getReferenceConvention());
        response.setReferenceERP(convention.getReferenceERP());
        response.setLibelle(convention.getLibelle());
        response.setDateDebut(convention.getDateDebut());
        response.setDateFin(convention.getDateFin());
        response.setDateSignature(convention.getDateSignature());
        response.setMontantTotal(convention.getMontantTotal());
        response.setPeriodicite(convention.getPeriodicite());
        response.setEtat(convention.getEtat());
        response.setArchived(convention.getArchived());
        response.setArchivedAt(convention.getArchivedAt());
        response.setArchivedBy(convention.getArchivedBy());
        response.setArchivedReason(convention.getArchivedReason());
        response.setCreatedAt(convention.getCreatedAt());
        response.setUpdatedAt(convention.getUpdatedAt());

        // Structure Interne
        if (convention.getStructureResponsable() != null) {
            response.setStructureResponsableId(convention.getStructureResponsable().getId());
            response.setStructureResponsableName(convention.getStructureResponsable().getName());
            response.setStructureResponsableCode(convention.getStructureResponsable().getCode());
        }

        // Structure Externe
        if (convention.getStructureBeneficiel() != null) {
            response.setStructureBeneficielId(convention.getStructureBeneficiel().getId());
            response.setStructureBeneficielName(convention.getStructureBeneficiel().getName());
            response.setStructureBeneficielCode(convention.getStructureBeneficiel().getCode());
        }

        // Zone
        if (convention.getZone() != null) {
            response.setZoneId(convention.getZone().getId());
            response.setZoneName(convention.getZone().getName());
            response.setZoneCode(convention.getZone().getCode());
        }

        // Application info (NEW)
        if (convention.getApplication() != null) {
            response.setApplicationId(convention.getApplication().getId());
            response.setApplicationCode(convention.getApplication().getCode());
            response.setApplicationName(convention.getApplication().getName());
            response.setApplicationClientName(convention.getApplication().getClientName());



            // Chef de projet info through Application
            if (convention.getApplication().getChefDeProjet() != null) {
                response.setChefDeProjetId(convention.getApplication().getChefDeProjet().getId());
                response.setChefDeProjetName(convention.getApplication().getChefProjetName());
            }
        }

        // Factures
        if (convention.getFactures() != null && !convention.getFactures().isEmpty()) {
            // Convert factures to DTOs
            response.setFactures(convention.getFactures().stream()
                    .map(this::toFactureResponse)
                    .collect(Collectors.toList()));

            // Calculate statistics
            long total = convention.getFactures().size();
            long payees = convention.getFactures().stream()
                    .filter(f -> "PAYE".equals(f.getStatutPaiement()))
                    .count();
            long nonPayees = convention.getFactures().stream()
                    .filter(f -> "NON_PAYE".equals(f.getStatutPaiement()))
                    .count();
            long enRetard = convention.getFactures().stream()
                    .filter(Facture::isEnRetard)
                    .count();

            response.setTotalFactures((int) total);
            response.setFacturesPayees((int) payees);
            response.setFacturesNonPayees((int) nonPayees);
            response.setFacturesEnRetard((int) enRetard);
        } else {
            response.setTotalFactures(0);
            response.setFacturesPayees(0);
            response.setFacturesNonPayees(0);
            response.setFacturesEnRetard(0);
        }

        if (convention.getCreatedBy() != null) {
            response.setCreatedById(convention.getCreatedBy().getId());
            response.setCreatedByUsername(convention.getCreatedBy().getUsername());
            response.setCreatedByFullName(convention.getCreatedBy().getFirstName() + " " +
                    convention.getCreatedBy().getLastName());
        }

        return response;
    }


    // In ConventionMapper.java - Fix the toFactureResponse method
    public FactureResponse toFactureResponse(Facture facture) {
        FactureResponse response = new FactureResponse();

        // Basic fields
        response.setId(facture.getId());
        response.setNumeroFacture(facture.getNumeroFacture());
        response.setDateFacturation(facture.getDateFacturation());
        response.setDateEcheance(facture.getDateEcheance());
        response.setMontantHT(facture.getMontantHT());
        response.setMontantTTC(facture.getMontantTTC());
        response.setTva(facture.getTva());
        response.setStatutPaiement(facture.getStatutPaiement());
        response.setDatePaiement(facture.getDatePaiement());
        response.setReferencePaiement(facture.getReferencePaiement());
        response.setNotes(facture.getNotes());
        response.setArchived(facture.getArchived());
        response.setArchivedAt(facture.getArchivedAt());
        response.setCreatedAt(facture.getCreatedAt());
        response.setUpdatedAt(facture.getUpdatedAt());
        response.setEnRetard(facture.isEnRetard());

        // Convention reference
        if (facture.getConvention() != null) {
            response.setConventionId(facture.getConvention().getId());
            response.setConventionReference(facture.getConvention().getReferenceConvention());
            response.setConventionLibelle(facture.getConvention().getLibelle());

            // Get structure info
            if (facture.getConvention().getStructureResponsable() != null) {
                response.setStructureInterneName(facture.getConvention().getStructureResponsable().getName());
            }
            if (facture.getConvention().getStructureBeneficiel() != null) {
                response.setStructureExterneName(facture.getConvention().getStructureBeneficiel().getName());
            }
            if (facture.getConvention().getZone() != null) {
                response.setZoneName(facture.getConvention().getZone().getName());
            }

            // Project info through convention (NEW)
            if (facture.getConvention().getApplication() != null) {
                response.setProjectId(facture.getConvention().getApplication().getId());
                response.setProjectCode(facture.getConvention().getApplication().getCode());
                response.setProjectName(facture.getConvention().getApplication().getName());
                response.setProjectClientName(facture.getConvention().getApplication().getClientName());



                // Chef de projet info through Application
                if (facture.getConvention().getApplication().getChefDeProjet() != null) {
                    response.setChefDeProjetId(facture.getConvention().getApplication().getChefDeProjet().getId());
                    response.setChefDeProjetName(facture.getConvention().getApplication().getChefProjetName());
                }
            }
        }

        return response;
    }
}