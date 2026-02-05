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
            if (facture.getConvention().getStructureInterne() != null) {
                response.setStructureInterneName(facture.getConvention().getStructureInterne().getName());
            }
            if (facture.getConvention().getStructureExterne() != null) {
                response.setStructureExterneName(facture.getConvention().getStructureExterne().getName());
            }
            if (facture.getConvention().getZone() != null) {
                response.setZoneName(facture.getConvention().getZone().getName());
            }

            // Get project info (NEW)
            if (facture.getConvention().getProject() != null) {
                response.setProjectId(facture.getConvention().getProject().getId());
                response.setProjectCode(facture.getConvention().getProject().getCode());
                response.setProjectName(facture.getConvention().getProject().getName());
                response.setProjectClientName(facture.getConvention().getProject().getClientName());

                // Get application info through project
                if (facture.getConvention().getProject().getApplication() != null) {
                    response.setApplicationId(facture.getConvention().getProject().getApplication().getId());
                    response.setApplicationName(facture.getConvention().getProject().getApplication().getName());
                    response.setApplicationCode(facture.getConvention().getProject().getApplication().getCode());
                }

                // Get chef de projet info through project
                if (facture.getConvention().getProject().getChefDeProjet() != null) {
                    response.setChefDeProjetId(facture.getConvention().getProject().getChefDeProjet().getId());
                    response.setChefDeProjetName(facture.getConvention().getProject().getChefProjetName());
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