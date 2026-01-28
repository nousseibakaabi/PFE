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

            // Get related convention info
            if (facture.getConvention().getStructureInterne() != null) {
                response.setStructureInterneName(facture.getConvention().getStructureInterne().getName());
            }
            if (facture.getConvention().getStructureExterne() != null) {
                response.setStructureExterneName(facture.getConvention().getStructureExterne().getName());
            }
            if (facture.getConvention().getZone() != null) {
                response.setZoneName(facture.getConvention().getZone().getName());
            }
            if (facture.getConvention().getApplication() != null) {
                response.setApplicationName(facture.getConvention().getApplication().getName());
            }
        }

        return response;
    }
}