// FactureMapper.java - UPDATED
package com.example.back.service.mapper;

import com.example.back.entity.Facture;
import com.example.back.payload.response.FactureResponse;
import org.springframework.stereotype.Service;

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

        return response;
    }
}