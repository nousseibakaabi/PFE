package com.example.back.service.mapper;

import com.example.back.entity.History;
import com.example.back.payload.response.HistoryResponse;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.format.DateTimeFormatter;
import java.util.Map;

@Service
@Slf4j
public class HistoryMapper {

    private final ObjectMapper objectMapper = new ObjectMapper();

    public HistoryResponse toResponse(History history) {
        HistoryResponse response = new HistoryResponse();

        response.setId(history.getId());
        response.setTimestamp(history.getTimestamp());
        response.setTimeFormatted(history.getTimestamp().format(DateTimeFormatter.ofPattern("HH:mm:ss")));
        response.setDateFormatted(history.getTimestamp().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")));
        response.setDateTimeFormatted(history.getTimestamp().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss")));

        response.setActionType(history.getActionType());
        response.setActionTypeLabel(getActionTypeLabel(history.getActionType()));

        response.setEntityType(history.getEntityType());
        response.setEntityTypeLabel(getEntityTypeLabel(history.getEntityType()));

        response.setEntityId(history.getEntityId());
        response.setEntityCode(history.getEntityCode());
        response.setEntityName(history.getEntityName());

        // User info
        if (history.getUser() != null) {
            response.setUserId(history.getUser().getId());
            response.setUsername(history.getUser().getUsername());
            response.setUserFullName(history.getUser().getFirstName() + " " + history.getUser().getLastName());
            response.setUserRole(getUserRoleLabel(history.getUser()));
        }

        response.setDescription(history.getDescription());

        // Parse JSON values safely with null checks
        Map<String, Object> oldMap = null;
        Map<String, Object> newMap = null;

        if (history.getOldValues() != null) {
            try {
                oldMap = objectMapper.readValue(history.getOldValues(), Map.class);
                response.setOldValues(oldMap);
            } catch (JsonProcessingException e) {
                log.error("Error parsing old values for history {}: {}", history.getId(), e.getMessage());
                response.setOldValues(Map.of("error", "Could not parse old values"));
            }
        }

        if (history.getNewValues() != null) {
            try {
                newMap = objectMapper.readValue(history.getNewValues(), Map.class);
                response.setNewValues(newMap);
            } catch (JsonProcessingException e) {
                log.error("Error parsing new values for history {}: {}", history.getId(), e.getMessage());
                response.setNewValues(Map.of("error", "Could not parse new values"));
            }
        }

        // Calculate changes summary safely
        if (oldMap != null && newMap != null) {
            response.setHasChanges(true);
            try {
                int changedFields = 0;

                // Check all keys in newMap
                for (String key : newMap.keySet()) {
                    Object newValue = newMap.get(key);
                    Object oldValue = oldMap.get(key);

                    // Skip if both are null
                    if (newValue == null && oldValue == null) {
                        continue;
                    }

                    // If one is null and the other isn't, it's a change
                    if (newValue == null || oldValue == null) {
                        changedFields++;
                        continue;
                    }

                    // Compare non-null values
                    if (!newValue.equals(oldValue)) {
                        changedFields++;
                    }
                }

                // Also check for keys that are only in oldMap
                for (String key : oldMap.keySet()) {
                    if (!newMap.containsKey(key)) {
                        changedFields++;
                    }
                }

                response.setChangedFieldsCount(changedFields);
            } catch (Exception e) {
                log.error("Error counting changes for history {}: {}", history.getId(), e.getMessage());
                response.setChangedFieldsCount(0);
            }
        }

        response.setIpAddress(history.getIpAddress());
        response.setUserAgent(history.getUserAgent());

        return response;
    }

    private String getActionTypeLabel(String actionType) {
        if (actionType == null) return "Inconnu";
        switch (actionType) {
            case "CREATE": return "Création";
            case "UPDATE": return "Modification";
            case "DELETE": return "Suppression";
            case "LOGIN": return "Connexion";
            case "LOGOUT": return "Déconnexion";
            case "PASSWORD_CHANGE": return "Changement de mot de passe";
            case "LOCK": return "Verrouillage";
            case "UNLOCK": return "Déverrouillage";
            case "ROLE_CHANGE": return "Changement de rôle";
            case "DEPARTMENT_CHANGE": return "Changement de département";
            case "ASSIGN_CHEF": return "Assignation chef de projet";
            case "STATUS_CHANGE": return "Changement de statut";
            case "DATES_SYNC": return "Synchronisation des dates";
            case "ARCHIVE": return "Archivage";
            case "RESTORE": return "Restauration";
            case "FINANCIAL_UPDATE": return "Mise à jour financière";
            case "PAYMENT": return "Paiement";
            case "OVERDUE": return "Retard de paiement";
            case "RENEW" : return "Renouvellement";
            case "REASSIGN_CHEF": return "Réassignation chef de projet";
            case "REQUEST_PROCESSED": return "Traitement de demande";
            
            default: return actionType;
        }
    }

    private String getEntityTypeLabel(String entityType) {
        if (entityType == null) return "Inconnu";
        switch (entityType) {
            case "USER": return "Utilisateur";
            case "APPLICATION": return "Application";
            case "CONVENTION": return "Convention";
            case "FACTURE": return "Facture";
            default: return entityType;
        }
    }

    private String getUserRoleLabel(com.example.back.entity.User user) {
        if (user == null || user.getRoles() == null || user.getRoles().isEmpty()) {
            return "Utilisateur";
        }

        return user.getRoles().stream()
                .findFirst()
                .map(role -> {
                    switch (role.getName().name()) {
                        case "ROLE_ADMIN": return "Admin";
                        case "ROLE_COMMERCIAL_METIER": return "Commercial Métier";
                        case "ROLE_CHEF_PROJET": return "Chef de Projet";
                        case "ROLE_DECIDEUR": return "Décideur";
                        default: return "Utilisateur";
                    }
                })
                .orElse("Utilisateur");
    }
}