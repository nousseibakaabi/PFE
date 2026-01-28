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
        if (convention.getStructureInterne() != null) {
            response.setStructureInterneId(convention.getStructureInterne().getId());
            response.setStructureInterneName(convention.getStructureInterne().getName());
            response.setStructureInterneCode(convention.getStructureInterne().getCode());
        }

        // Structure Externe
        if (convention.getStructureExterne() != null) {
            response.setStructureExterneId(convention.getStructureExterne().getId());
            response.setStructureExterneName(convention.getStructureExterne().getName());
            response.setStructureExterneCode(convention.getStructureExterne().getCode());
        }

        // Zone
        if (convention.getZone() != null) {
            response.setZoneId(convention.getZone().getId());
            response.setZoneName(convention.getZone().getName());
            response.setZoneCode(convention.getZone().getCode());
        }

        // Application
        if (convention.getApplication() != null) {
            response.setApplicationId(convention.getApplication().getId());
            response.setApplicationName(convention.getApplication().getName());
            response.setApplicationCode(convention.getApplication().getCode());
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

        return response;
    }

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

        // Convention reference (without circular dependency)
        if (facture.getConvention() != null) {
            response.setConventionId(facture.getConvention().getId());
            response.setConventionReference(facture.getConvention().getReferenceConvention());
            response.setConventionLibelle(facture.getConvention().getLibelle());
        }

        return response;
    }
}