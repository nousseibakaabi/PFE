// FactureMapper.java - UPDATED
package com.example.back.service.mapper;

import com.example.back.entity.Facture;
import com.example.back.payload.response.FactureResponse;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
public class FactureMapper {

    public FactureResponse toResponse(Facture facture) {
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

        response.setJoursRetard(facture.getJoursRetard());
        response.setJoursRestants(facture.getJoursRestants());
        response.setStatutPaiementDetail(facture.getStatutPaiementDetail());
        response.setStatutPaiementColor(getStatutColor(facture));

        // Convention reference and related info
        if (facture.getConvention() != null) {
            response.setConventionId(facture.getConvention().getId());
            response.setConventionReference(facture.getConvention().getReferenceConvention());
            response.setConventionLibelle(facture.getConvention().getLibelle());

            // Get structure info
            if (facture.getConvention().getStructureResponsable() != null) {
                response.setStructureResponsableName(facture.getConvention().getStructureResponsable().getName());
            }
            if (facture.getConvention().getStructureBeneficiel() != null) {
                response.setStructureBeneficielName(facture.getConvention().getStructureBeneficiel().getName());
            }
            if (facture.getConvention().getStructureResponsable().getZoneGeographique() != null) {
                response.setZoneName(facture.getConvention().getStructureResponsable().getZoneGeographique().getName());
            }

            // Get application info (NEW)
            if (facture.getConvention().getApplication() != null) {
                response.setApplicationId(facture.getConvention().getApplication().getId());
                response.setApplicationCode(facture.getConvention().getApplication().getCode());
                response.setApplicationName(facture.getConvention().getApplication().getName());
                response.setApplicationClientName(facture.getConvention().getApplication().getClientName());
                response.setMinUser(facture.getConvention().getApplication().getMinUser());
                response.setMaxUser(facture.getConvention().getApplication().getMaxUser());


                // Get chef de projet info through project
                if (facture.getConvention().getApplication().getChefDeProjet() != null) {
                    response.setChefDeProjetId(facture.getConvention().getApplication().getChefDeProjet().getId());
                    response.setChefDeProjetName(facture.getConvention().getApplication().getChefProjetName());
                }
            }
        }

        Map<String, Object> paiementDetails = facture.getPaiementDetails();

        if (!paiementDetails.isEmpty()) {
            response.setPaiementType((String) paiementDetails.get("type"));
            response.setJoursDetails(facture.formatDuration((Long) paiementDetails.get("jours")));
            response.setJoursNumber((Long) paiementDetails.get("jours"));
        }

        response.setStatutPaiementDetail(facture.getStatutPaiementDetail());
        response.setStatutPaiementColor(facture.getStatutPaiementColor());

        return response;
    }


    private String getStatutColor(Facture facture) {
        if ("PAYE".equals(facture.getStatutPaiement())) {
            if (facture.getDatePaiement() != null && facture.getDateEcheance() != null) {
                if (facture.getDatePaiement().isAfter(facture.getDateEcheance())) {
                    return "warning"; // Paid late
                } else {
                    return "success"; // Paid on time
                }
            }
            return "success";
        } else if (facture.isEnRetard()) {
            return "danger"; // Overdue
        }
        return "info"; // Not due yet
    }
}