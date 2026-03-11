package com.example.back.service;

import com.example.back.entity.*;
import com.example.back.payload.history.*;
import com.example.back.payload.response.HistoryResponse;
import com.example.back.repository.HistoryRepository;
import com.example.back.repository.UserRepository;
import com.example.back.service.mapper.HistoryMapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Slf4j
public class HistoryService {

    @Autowired
    private HistoryRepository historyRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private HistoryMapper historyMapper;

    private final ObjectMapper objectMapper;

    public HistoryService() {
        this.objectMapper = new ObjectMapper();
        this.objectMapper.registerModule(new JavaTimeModule());
    }

    // ==================== CONVERSION METHODS ====================

    private ConventionHistoryData convertToHistoryData(Convention convention) {
        if (convention == null) return null;

        ConventionHistoryData data = new ConventionHistoryData();
        data.setId(convention.getId());
        data.setReferenceConvention(convention.getReferenceConvention());
        data.setReferenceERP(convention.getReferenceERP());
        data.setLibelle(convention.getLibelle());
        data.setDateDebut(convention.getDateDebut());
        data.setDateFin(convention.getDateFin());
        data.setDateSignature(convention.getDateSignature());
        data.setMontantHT(convention.getMontantHT());
        data.setTva(convention.getTva());
        data.setMontantTTC(convention.getMontantTTC());
        data.setNbUsers(convention.getNbUsers());
        data.setPeriodicite(convention.getPeriodicite());
        data.setEtat(convention.getEtat());
        data.setArchived(convention.getArchived());

        if (convention.getApplication() != null) {
            data.setApplicationId(convention.getApplication().getId());
            data.setApplicationName(convention.getApplication().getName());
        }

        if (convention.getStructureResponsable() != null) {
            data.setStructureResponsableId(convention.getStructureResponsable().getId());
            data.setStructureResponsableName(convention.getStructureResponsable().getName());
        }

        if (convention.getStructureBeneficiel() != null) {
            data.setStructureBeneficielId(convention.getStructureBeneficiel().getId());
            data.setStructureBeneficielName(convention.getStructureBeneficiel().getName());
        }

        if (convention.getCreatedBy() != null) {
            data.setCreatedById(convention.getCreatedBy().getId());
            data.setCreatedByName(convention.getCreatedBy().getFirstName() + " " +
                    convention.getCreatedBy().getLastName());
        }

        return data;
    }

    private ApplicationHistoryData convertToHistoryData(Application application) {
        if (application == null) return null;

        ApplicationHistoryData data = new ApplicationHistoryData();
        data.setId(application.getId());
        data.setCode(application.getCode());
        data.setName(application.getName());
        data.setDescription(application.getDescription());
        data.setClientName(application.getClientName());
        data.setClientEmail(application.getClientEmail());
        data.setClientPhone(application.getClientPhone());
        data.setDateDebut(application.getDateDebut());
        data.setDateFin(application.getDateFin());
        data.setMinUser(application.getMinUser());
        data.setMaxUser(application.getMaxUser());
        data.setStatus(application.getStatus());

        if (application.getChefDeProjet() != null) {
            data.setChefDeProjetId(application.getChefDeProjet().getId());
            data.setChefDeProjetName(application.getChefDeProjet().getFirstName() + " " +
                    application.getChefDeProjet().getLastName());
        }

        return data;
    }

    private FactureHistoryData convertToHistoryData(Facture facture) {
        if (facture == null) return null;

        FactureHistoryData data = new FactureHistoryData();
        data.setId(facture.getId());
        data.setNumeroFacture(facture.getNumeroFacture());
        data.setDateFacturation(facture.getDateFacturation());
        data.setDateEcheance(facture.getDateEcheance());
        data.setMontantHT(facture.getMontantHT());
        data.setTva(facture.getTva());
        data.setMontantTTC(facture.getMontantTTC());
        data.setStatutPaiement(facture.getStatutPaiement());
        data.setDatePaiement(facture.getDatePaiement());
        data.setReferencePaiement(facture.getReferencePaiement());
        data.setNotes(facture.getNotes());
        data.setArchived(facture.getArchived());

        if (facture.getConvention() != null) {
            data.setConventionId(facture.getConvention().getId());
            data.setConventionReference(facture.getConvention().getReferenceConvention());
        }

        return data;
    }

    private UserHistoryData convertToHistoryData(User user) {
        if (user == null) return null;

        UserHistoryData data = new UserHistoryData();
        data.setId(user.getId());
        data.setUsername(user.getUsername());
        data.setEmail(user.getEmail());
        data.setFirstName(user.getFirstName());
        data.setLastName(user.getLastName());
        data.setPhone(user.getPhone());
        data.setDepartment(user.getDepartment());
        data.setEnabled(user.getEnabled());
        data.setLockedByAdmin(user.getLockedByAdmin());

        // Convert roles to simple map
        if (user.getRoles() != null && !user.getRoles().isEmpty()) {
            Map<String, Object> rolesMap = new HashMap<>();
            user.getRoles().forEach(role -> {
                rolesMap.put("id", role.getId());
                rolesMap.put("name", role.getName().name());
            });
            data.setRoles(rolesMap);
        }

        return data;
    }

    // ==================== USER HISTORY METHODS ====================

    public void logUserLogin(User user) {
        try {
            UserHistoryData data = convertToHistoryData(user);
            String description = String.format("Utilisateur %s %s (%s) s'est connecté",
                    user.getFirstName(), user.getLastName(), user.getUsername());
            createHistory("LOGIN", "USER", user.getId(), user.getUsername(),
                    user.getFirstName() + " " + user.getLastName(), description, null, data, user);
        } catch (Exception e) {
            log.error("Failed to log user login: {}", e.getMessage());
        }
    }

    public void logUserLogout(User user) {
        try {
            String description = String.format("Utilisateur %s %s (%s) s'est déconnecté",
                    user.getFirstName(), user.getLastName(), user.getUsername());
            createHistory("LOGOUT", "USER", user.getId(), user.getUsername(),
                    user.getFirstName() + " " + user.getLastName(), description, null, null, user);
        } catch (Exception e) {
            log.error("Failed to log user logout: {}", e.getMessage());
        }
    }

    public void logUserCreate(User newUser, User createdBy) {
        try {
            UserHistoryData data = convertToHistoryData(newUser);
            String description = String.format("Création de l'utilisateur %s %s (%s) par %s %s",
                    newUser.getFirstName(), newUser.getLastName(), newUser.getUsername(),
                    createdBy.getFirstName(), createdBy.getLastName());
            createHistory("CREATE", "USER", newUser.getId(), newUser.getUsername(),
                    newUser.getFirstName() + " " + newUser.getLastName(), description, null, data, createdBy);
        } catch (Exception e) {
            log.error("Failed to log user creation: {}", e.getMessage());
        }
    }

    public void logUserUpdate(User oldUser, User newUser) {
        try {
            UserHistoryData oldData = convertToHistoryData(oldUser);
            UserHistoryData newData = convertToHistoryData(newUser);

            String description = String.format("Mise à jour de l'utilisateur %s %s",
                    newUser.getFirstName(), newUser.getLastName());
            createHistory("UPDATE", "USER", newUser.getId(), newUser.getUsername(),
                    newUser.getFirstName() + " " + newUser.getLastName(), description, oldData, newData, newUser);
        } catch (Exception e) {
            log.error("Failed to log user update: {}", e.getMessage());
        }
    }

    public void logUserLock(User user, User lockedBy) {
        try {
            UserHistoryData data = convertToHistoryData(user);
            String description = String.format("Verrouillage de l'utilisateur %s %s par %s %s",
                    user.getFirstName(), user.getLastName(),
                    lockedBy.getFirstName(), lockedBy.getLastName());
            createHistory("LOCK", "USER", user.getId(), user.getUsername(),
                    user.getFirstName() + " " + user.getLastName(), description, null, data, lockedBy);
        } catch (Exception e) {
            log.error("Failed to log user lock: {}", e.getMessage());
        }
    }

    public void logUserUnlock(User user, User unlockedBy) {
        try {
            UserHistoryData data = convertToHistoryData(user);
            String description = String.format("Déverrouillage de l'utilisateur %s %s par %s %s",
                    user.getFirstName(), user.getLastName(),
                    unlockedBy.getFirstName(), unlockedBy.getLastName());
            createHistory("UNLOCK", "USER", user.getId(), user.getUsername(),
                    user.getFirstName() + " " + user.getLastName(), description, null, data, unlockedBy);
        } catch (Exception e) {
            log.error("Failed to log user unlock: {}", e.getMessage());
        }
    }

    public void logPasswordChange(User user) {
        try {
            String description = String.format("Changement de mot de passe pour l'utilisateur %s %s",
                    user.getFirstName(), user.getLastName());
            createHistory("PASSWORD_CHANGE", "USER", user.getId(), user.getUsername(),
                    user.getFirstName() + " " + user.getLastName(), description, null, null, user);
        } catch (Exception e) {
            log.error("Failed to log password change: {}", e.getMessage());
        }
    }

    public void logUserRoleChange(User user, User changedBy, List<String> oldRoles, List<String> newRoles) {
        try {
            String oldRolesStr = String.join(", ", oldRoles);
            String newRolesStr = String.join(", ", newRoles);
            String description = String.format("Changement de rôle pour %s %s par %s %s: %s → %s",
                    user.getFirstName(), user.getLastName(),
                    changedBy.getFirstName(), changedBy.getLastName(),
                    oldRolesStr, newRolesStr);

            Map<String, Object> oldData = new HashMap<>();
            oldData.put("roles", oldRoles);
            Map<String, Object> newData = new HashMap<>();
            newData.put("roles", newRoles);

            createHistory("ROLE_CHANGE", "USER", user.getId(), user.getUsername(),
                    user.getFirstName() + " " + user.getLastName(), description, oldData, newData, changedBy);
        } catch (Exception e) {
            log.error("Failed to log role change: {}", e.getMessage());
        }
    }

    public void logUserDepartmentChange(User user, User changedBy, String oldDepartment, String newDepartment) {
        try {
            String description = String.format("Changement de département pour %s %s par %s %s: %s → %s",
                    user.getFirstName(), user.getLastName(),
                    changedBy.getFirstName(), changedBy.getLastName(),
                    oldDepartment, newDepartment);

            Map<String, Object> oldData = new HashMap<>();
            oldData.put("department", oldDepartment);
            Map<String, Object> newData = new HashMap<>();
            newData.put("department", newDepartment);

            createHistory("DEPARTMENT_CHANGE", "USER", user.getId(), user.getUsername(),
                    user.getFirstName() + " " + user.getLastName(), description, oldData, newData, changedBy);
        } catch (Exception e) {
            log.error("Failed to log department change: {}", e.getMessage());
        }
    }

    // ==================== APPLICATION HISTORY METHODS ====================

    public void logApplicationCreate(Application application, User createdBy) {
        try {
            ApplicationHistoryData data = convertToHistoryData(application);
            String description = String.format("Création de l'application %s (%s) par %s %s",
                    application.getName(), application.getCode(),
                    createdBy.getFirstName(), createdBy.getLastName());
            createHistory("CREATE", "APPLICATION", application.getId(), application.getCode(),
                    application.getName(), description, null, data, createdBy);
        } catch (Exception e) {
            log.error("Failed to log application creation: {}", e.getMessage());
        }
    }

    public void logApplicationUpdate(Application oldApp, Application newApp, User updatedBy) {
        try {
            ApplicationHistoryData oldData = convertToHistoryData(oldApp);
            ApplicationHistoryData newData = convertToHistoryData(newApp);

            String description = String.format("Mise à jour de l'application %s par %s %s",
                    newApp.getName(), updatedBy.getFirstName(), updatedBy.getLastName());
            createHistory("UPDATE", "APPLICATION", newApp.getId(), newApp.getCode(),
                    newApp.getName(), description, oldData, newData, updatedBy);
        } catch (Exception e) {
            log.error("Failed to log application update: {}", e.getMessage());
        }
    }

    public void logApplicationDelete(Application application, User deletedBy) {
        try {
            ApplicationHistoryData data = convertToHistoryData(application);
            String description = String.format("Suppression de l'application %s (%s) par %s %s",
                    application.getName(), application.getCode(),
                    deletedBy.getFirstName(), deletedBy.getLastName());
            createHistory("DELETE", "APPLICATION", application.getId(), application.getCode(),
                    application.getName(), description, data, null, deletedBy);
        } catch (Exception e) {
            log.error("Failed to log application deletion: {}", e.getMessage());
        }
    }

    public void logApplicationAssignChef(Application application, User oldChef, User newChef, User assignedBy) {
        try {
            String oldChefName = oldChef != null ? oldChef.getFirstName() + " " + oldChef.getLastName() : "Non assigné";
            String newChefName = newChef != null ? newChef.getFirstName() + " " + newChef.getLastName() : "Non assigné";

            String description = String.format("Assignation du chef de projet pour %s: %s → %s par %s %s",
                    application.getName(), oldChefName, newChefName,
                    assignedBy.getFirstName(), assignedBy.getLastName());

            Map<String, Object> oldData = new HashMap<>();
            oldData.put("chefDeProjet", oldChefName);
            Map<String, Object> newData = new HashMap<>();
            newData.put("chefDeProjet", newChefName);

            createHistory("ASSIGN_CHEF", "APPLICATION", application.getId(), application.getCode(),
                    application.getName(), description, oldData, newData, assignedBy);
        } catch (Exception e) {
            log.error("Failed to log chef assignment: {}", e.getMessage());
        }
    }

    public void logApplicationStatusChange(Application application, String oldStatus, String newStatus) {
        try {
            User currentUser = getCurrentUser();
            String description = String.format("Changement de statut de l'application %s: %s → %s",
                    application.getName(), oldStatus, newStatus);

            Map<String, Object> oldData = new HashMap<>();
            oldData.put("status", oldStatus);
            Map<String, Object> newData = new HashMap<>();
            newData.put("status", newStatus);

            createHistory("STATUS_CHANGE", "APPLICATION", application.getId(), application.getCode(),
                    application.getName(), description, oldData, newData, currentUser);
        } catch (Exception e) {
            log.error("Failed to log status change: {}", e.getMessage());
        }
    }

    public void logApplicationDatesSync(Application application, LocalDate oldStart, LocalDate oldEnd,
                                        LocalDate newStart, LocalDate newEnd) {
        try {
            User currentUser = getCurrentUser();
            String description = String.format("Synchronisation des dates de l'application %s",
                    application.getName());

            Map<String, Object> oldData = new HashMap<>();
            oldData.put("dateDebut", oldStart);
            oldData.put("dateFin", oldEnd);
            Map<String, Object> newData = new HashMap<>();
            newData.put("dateDebut", newStart);
            newData.put("dateFin", newEnd);

            createHistory("DATES_SYNC", "APPLICATION", application.getId(), application.getCode(),
                    application.getName(), description, oldData, newData, currentUser);
        } catch (Exception e) {
            log.error("Failed to log dates sync: {}", e.getMessage());
        }
    }

    // ==================== CONVENTION HISTORY METHODS ====================

    public void logConventionCreate(Convention convention, User createdBy) {
        try {
            ConventionHistoryData data = convertToHistoryData(convention);
            String userRole = getRoleDisplay(createdBy);
            String description = String.format("Création de la convention %s (%s) par %s %s (%s)",
                    convention.getReferenceConvention(), convention.getLibelle(),
                    createdBy.getFirstName(), createdBy.getLastName(), userRole);
            createHistory("CREATE", "CONVENTION", convention.getId(), convention.getReferenceConvention(),
                    convention.getLibelle(), description, null, data, createdBy);
            log.info("Convention creation history logged for {}", convention.getReferenceConvention());
        } catch (Exception e) {
            log.error("Failed to log convention creation: {}", e.getMessage(), e);
        }
    }


    public void logConventionUpdate(Convention oldConv, Convention newConv, User updatedBy) {
        try {
            ConventionHistoryData oldData = convertToHistoryData(oldConv);
            ConventionHistoryData newData = convertToHistoryData(newConv);

            // Only log if there are actual changes
            Map<String, Object> changes = new HashMap<>();
            StringBuilder changesDesc = new StringBuilder();

            // Check each field for changes
            if (!oldConv.getReferenceConvention().equals(newConv.getReferenceConvention())) {
                changes.put("referenceConvention", Map.of("old", oldConv.getReferenceConvention(), "new", newConv.getReferenceConvention()));
                changesDesc.append(" Référence: ").append(oldConv.getReferenceConvention()).append("→").append(newConv.getReferenceConvention());
            }

            if (!oldConv.getLibelle().equals(newConv.getLibelle())) {
                changes.put("libelle", Map.of("old", oldConv.getLibelle(), "new", newConv.getLibelle()));
                changesDesc.append(" Libellé: ").append(oldConv.getLibelle()).append("→").append(newConv.getLibelle());
            }

            if (!oldConv.getDateDebut().equals(newConv.getDateDebut())) {
                changes.put("dateDebut", Map.of("old", oldConv.getDateDebut(), "new", newConv.getDateDebut()));
                changesDesc.append(" Date début: ").append(oldConv.getDateDebut()).append("→").append(newConv.getDateDebut());
            }

            if (!oldConv.getDateFin().equals(newConv.getDateFin())) {
                changes.put("dateFin", Map.of("old", oldConv.getDateFin(), "new", newConv.getDateFin()));
                changesDesc.append(" Date fin: ").append(oldConv.getDateFin()).append("→").append(newConv.getDateFin());
            }

            // Check financial fields
            if (!oldConv.getMontantHT().equals(newConv.getMontantHT())) {
                changes.put("montantHT", Map.of("old", oldConv.getMontantHT(), "new", newConv.getMontantHT()));
                changesDesc.append(" Montant HT: ").append(oldConv.getMontantHT()).append("→").append(newConv.getMontantHT());
            }

            if (!oldConv.getTva().equals(newConv.getTva())) {
                changes.put("tva", Map.of("old", oldConv.getTva(), "new", newConv.getTva()));
                changesDesc.append(" TVA: ").append(oldConv.getTva()).append("→").append(newConv.getTva());
            }

            if (!oldConv.getMontantTTC().equals(newConv.getMontantTTC())) {
                changes.put("montantTTC", Map.of("old", oldConv.getMontantTTC(), "new", newConv.getMontantTTC()));
                changesDesc.append(" Montant TTC: ").append(oldConv.getMontantTTC()).append("→").append(newConv.getMontantTTC());
            }

            if (!oldConv.getNbUsers().equals(newConv.getNbUsers())) {
                changes.put("nbUsers", Map.of("old", oldConv.getNbUsers(), "new", newConv.getNbUsers()));
                changesDesc.append(" Utilisateurs: ").append(oldConv.getNbUsers()).append("→").append(newConv.getNbUsers());
            }

            if (!oldConv.getEtat().equals(newConv.getEtat())) {
                changes.put("etat", Map.of("old", oldConv.getEtat(), "new", newConv.getEtat()));
                changesDesc.append(" Statut: ").append(oldConv.getEtat()).append("→").append(newConv.getEtat());
            }

            // Only log if there are actual changes
            if (!changes.isEmpty()) {
                String description = String.format("Mise à jour de la convention %s par %s %s%s",
                        newConv.getReferenceConvention(),
                        updatedBy.getFirstName(), updatedBy.getLastName(),
                        changesDesc.toString());

                createHistory("UPDATE", "CONVENTION", newConv.getId(),
                        newConv.getReferenceConvention(),
                        newConv.getLibelle(),
                        description, oldData, newData, updatedBy);

                log.info("Convention update history logged for {} with {} changes",
                        newConv.getReferenceConvention(), changes.size());
            }
        } catch (Exception e) {
            log.error("Failed to log convention update: {}", e.getMessage(), e);
        }
    }


    public void logConventionDelete(Convention convention, User deletedBy) {
        try {
            ConventionHistoryData data = convertToHistoryData(convention);
            String description = String.format("Suppression de la convention %s (%s) par %s %s",
                    convention.getReferenceConvention(), convention.getLibelle(),
                    deletedBy.getFirstName(), deletedBy.getLastName());
            createHistory("DELETE", "CONVENTION", convention.getId(), convention.getReferenceConvention(),
                    convention.getLibelle(), description, data, null, deletedBy);
        } catch (Exception e) {
            log.error("Failed to log convention deletion: {}", e.getMessage());
        }
    }

    public void logConventionArchive(Convention convention, User archivedBy, String reason) {
        try {
            ConventionHistoryData data = convertToHistoryData(convention);
            String description = String.format("Archivage de la convention %s par %s %s - Raison: %s",
                    convention.getReferenceConvention(),
                    archivedBy.getFirstName(), archivedBy.getLastName(),
                    reason != null ? reason : "Non spécifiée");
            createHistory("ARCHIVE", "CONVENTION", convention.getId(), convention.getReferenceConvention(),
                    convention.getLibelle(), description, data, null, archivedBy);
        } catch (Exception e) {
            log.error("Failed to log convention archive: {}", e.getMessage());
        }
    }

    public void logConventionRestore(Convention convention, User restoredBy) {
        try {
            ConventionHistoryData data = convertToHistoryData(convention);
            String description = String.format("Restauration de la convention %s par %s %s",
                    convention.getReferenceConvention(),
                    restoredBy.getFirstName(), restoredBy.getLastName());
            createHistory("RESTORE", "CONVENTION", convention.getId(), convention.getReferenceConvention(),
                    convention.getLibelle(), description, null, data, restoredBy);
        } catch (Exception e) {
            log.error("Failed to log convention restore: {}", e.getMessage());
        }
    }

    public void logConventionStatusChange(Convention convention, String oldStatus, String newStatus) {
        try {
            User currentUser = getCurrentUser();
            String description = String.format("Changement de statut de la convention %s: %s → %s",
                    convention.getReferenceConvention(),
                    getConventionStatusLabel(oldStatus), getConventionStatusLabel(newStatus));

            Map<String, Object> oldData = new HashMap<>();
            oldData.put("etat", oldStatus);
            Map<String, Object> newData = new HashMap<>();
            newData.put("etat", newStatus);

            createHistory("STATUS_CHANGE", "CONVENTION", convention.getId(), convention.getReferenceConvention(),
                    convention.getLibelle(), description, oldData, newData, currentUser);
        } catch (Exception e) {
            log.error("Failed to log status change: {}", e.getMessage());
        }
    }

    public void logConventionFinancialUpdate(Convention convention,
                                             BigDecimal oldMontantHT, BigDecimal oldMontantTTC, Long oldNbUsers,
                                             BigDecimal newMontantHT, BigDecimal newMontantTTC, Long newNbUsers) {
        try {
            User currentUser = getCurrentUser();
            String description = String.format("Mise à jour financière de la convention %s",
                    convention.getReferenceConvention());

            Map<String, Object> oldData = new HashMap<>();
            oldData.put("montantHT", oldMontantHT);
            oldData.put("montantTTC", oldMontantTTC);
            oldData.put("nbUsers", oldNbUsers);

            Map<String, Object> newData = new HashMap<>();
            newData.put("montantHT", newMontantHT);
            newData.put("montantTTC", newMontantTTC);
            newData.put("nbUsers", newNbUsers);

            createHistory("FINANCIAL_UPDATE", "CONVENTION", convention.getId(), convention.getReferenceConvention(),
                    convention.getLibelle(), description, oldData, newData, currentUser);
        } catch (Exception e) {
            log.error("Failed to log financial update: {}", e.getMessage());
        }
    }

    // ==================== FACTURE HISTORY METHODS ====================

    public void logFactureCreate(Facture facture, User createdBy) {
        try {
            FactureHistoryData data = convertToHistoryData(facture);
            String description = String.format("Création de la facture %s pour la convention %s par %s %s - Montant: %s TND",
                    facture.getNumeroFacture(),
                    facture.getConvention().getReferenceConvention(),
                    createdBy.getFirstName(), createdBy.getLastName(),
                    facture.getMontantTTC() != null ? facture.getMontantTTC().toString() : "0");
            createHistory("CREATE", "FACTURE", facture.getId(), facture.getNumeroFacture(),
                    "Facture " + facture.getNumeroFacture(), description, null, data, createdBy);
        } catch (Exception e) {
            log.error("Failed to log facture creation: {}", e.getMessage());
        }
    }

    public void logFactureUpdate(Facture oldFacture, Facture newFacture, User updatedBy) {
        try {
            FactureHistoryData oldData = convertToHistoryData(oldFacture);
            FactureHistoryData newData = convertToHistoryData(newFacture);

            String description = String.format("Mise à jour de la facture %s par %s %s",
                    newFacture.getNumeroFacture(),
                    updatedBy.getFirstName(), updatedBy.getLastName());
            createHistory("UPDATE", "FACTURE", newFacture.getId(), newFacture.getNumeroFacture(),
                    "Facture " + newFacture.getNumeroFacture(), description, oldData, newData, updatedBy);
        } catch (Exception e) {
            log.error("Failed to log facture update: {}", e.getMessage());
        }
    }

    public void logFactureDelete(Facture facture, User deletedBy) {
        try {
            FactureHistoryData data = convertToHistoryData(facture);
            String description = String.format("Suppression de la facture %s par %s %s",
                    facture.getNumeroFacture(),
                    deletedBy.getFirstName(), deletedBy.getLastName());
            createHistory("DELETE", "FACTURE", facture.getId(), facture.getNumeroFacture(),
                    "Facture " + facture.getNumeroFacture(), description, data, null, deletedBy);
        } catch (Exception e) {
            log.error("Failed to log facture deletion: {}", e.getMessage());
        }
    }

    public void logFacturePayment(Facture facture, User registeredBy, String referencePaiement) {
        try {
            FactureHistoryData oldData = new FactureHistoryData();
            oldData.setStatutPaiement("NON_PAYE");

            FactureHistoryData newData = convertToHistoryData(facture);

            String paymentStatus = facture.getDatePaiement() != null &&
                    facture.getDateEcheance() != null &&
                    facture.getDatePaiement().isAfter(facture.getDateEcheance()) ?
                    "en retard de " + getDaysBetween(facture.getDateEcheance(), facture.getDatePaiement()) + " jours" :
                    "à temps";

            String description = String.format("Enregistrement du paiement de la facture %s par %s %s - Réf: %s - Payé %s",
                    facture.getNumeroFacture(),
                    registeredBy.getFirstName(), registeredBy.getLastName(),
                    referencePaiement, paymentStatus);

            createHistory("PAYMENT", "FACTURE", facture.getId(), facture.getNumeroFacture(),
                    "Facture " + facture.getNumeroFacture(), description, oldData, newData, registeredBy);

            log.info("Facture payment history logged for {}", facture.getNumeroFacture());
        } catch (Exception e) {
            log.error("Failed to log facture payment: {}", e.getMessage(), e);
        }
    }

    public void logFactureStatusChange(Facture facture, String oldStatus, String newStatus) {
        try {
            User currentUser = getCurrentUser();
            String description = String.format("Changement de statut de la facture %s: %s → %s",
                    facture.getNumeroFacture(),
                    getInvoiceStatusLabel(oldStatus), getInvoiceStatusLabel(newStatus));

            Map<String, Object> oldData = new HashMap<>();
            oldData.put("statutPaiement", oldStatus);
            Map<String, Object> newData = new HashMap<>();
            newData.put("statutPaiement", newStatus);

            createHistory("STATUS_CHANGE", "FACTURE", facture.getId(), facture.getNumeroFacture(),
                    "Facture " + facture.getNumeroFacture(), description, oldData, newData, currentUser);
        } catch (Exception e) {
            log.error("Failed to log status change: {}", e.getMessage());
        }
    }

    public void logFactureOverdue(Facture facture) {
        try {
            User currentUser = getCurrentUser();
            long daysOverdue = java.time.temporal.ChronoUnit.DAYS.between(
                    facture.getDateEcheance(), LocalDate.now());

            String description = String.format("Facture %s en retard de %d jours - Échéance: %s",
                    facture.getNumeroFacture(), daysOverdue,
                    facture.getDateEcheance().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")));

            Map<String, Object> oldData = new HashMap<>();
            oldData.put("statutPaiement", facture.getStatutPaiement());
            Map<String, Object> newData = new HashMap<>();
            newData.put("statutPaiement", "EN_RETARD");
            newData.put("joursRetard", daysOverdue);

            createHistory("OVERDUE", "FACTURE", facture.getId(), facture.getNumeroFacture(),
                    "Facture " + facture.getNumeroFacture(), description, oldData, newData, currentUser);
        } catch (Exception e) {
            log.error("Failed to log overdue: {}", e.getMessage());
        }
    }

    // ==================== CORE HISTORY METHOD ====================

    private void createHistory(String actionType, String entityType, Long entityId,
                               String entityCode, String entityName, String description,
                               Object oldObject, Object newObject, User user) {
        try {
            History history = new History();
            history.setActionType(actionType);
            history.setEntityType(entityType);
            history.setEntityId(entityId);
            history.setEntityCode(entityCode);
            history.setEntityName(entityName);
            history.setUser(user);
            history.setDescription(description);
            history.setIpAddress(getClientIpAddress());
            history.setUserAgent(getUserAgent());

            // Convert objects to JSON using the safe DTOs or Maps
            if (oldObject != null) {
                history.setOldValues(objectMapper.writeValueAsString(oldObject));
            }
            if (newObject != null) {
                history.setNewValues(objectMapper.writeValueAsString(newObject));
            }

            historyRepository.save(history);
            log.info("History created: {} - {} - {}", actionType, entityType, entityCode);
        } catch (JsonProcessingException e) {
            log.error("Error serializing objects for history", e);
        }
    }

    // ==================== HELPER METHODS ====================

    private User getCurrentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated() && !"anonymousUser".equals(auth.getName())) {
            return userRepository.findByUsername(auth.getName()).orElse(null);
        }
        return null;
    }

    private String getClientIpAddress() {
        try {
            ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attributes != null) {
                HttpServletRequest request = attributes.getRequest();
                String ipAddress = request.getHeader("X-Forwarded-For");
                if (ipAddress == null || ipAddress.isEmpty()) {
                    ipAddress = request.getRemoteAddr();
                }
                return ipAddress;
            }
        } catch (Exception e) {
            log.error("Error getting IP address", e);
        }
        return null;
    }

    private String getUserAgent() {
        try {
            ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attributes != null) {
                return attributes.getRequest().getHeader("User-Agent");
            }
        } catch (Exception e) {
            log.error("Error getting user agent", e);
        }
        return null;
    }

    private long getDaysBetween(LocalDate start, LocalDate end) {
        return java.time.temporal.ChronoUnit.DAYS.between(start, end);
    }

    private String getRoleDisplay(User user) {
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

    private String getConventionStatusLabel(String status) {
        if (status == null) return "Inconnu";
        switch (status) {
            case "PLANIFIE": return "Planifié";
            case "EN COURS": return "En Cours";
            case "TERMINE": return "Terminé";
            case "ARCHIVE": return "Archivé";
            default: return status;
        }
    }

    private String getInvoiceStatusLabel(String status) {
        if (status == null) return "Inconnu";
        switch (status) {
            case "PAYE": return "Payée";
            case "NON_PAYE": return "Non Payée";
            case "EN_RETARD": return "En Retard";
            default: return status;
        }
    }

    // ==================== HISTORY RETRIEVAL METHODS ====================

    public List<HistoryResponse> getAllHistory() {
        return historyRepository.findAll().stream()
                .map(historyMapper::toResponse)
                .collect(Collectors.toList());
    }

    public List<HistoryResponse> getRecentHistory(int limit) {
        Pageable pageable = PageRequest.of(0, limit);
        return historyRepository.findRecent(pageable).stream()
                .map(historyMapper::toResponse)
                .collect(Collectors.toList());
    }

    public List<HistoryResponse> getHistoryByUser(Long userId) {
        return historyRepository.findByUserIdOrderByTimestampDesc(userId).stream()
                .map(historyMapper::toResponse)
                .collect(Collectors.toList());
    }

    public List<HistoryResponse> getHistoryByEntity(String entityType, Long entityId) {
        return historyRepository.findByEntityTypeAndEntityIdOrderByTimestampDesc(entityType, entityId).stream()
                .map(historyMapper::toResponse)
                .collect(Collectors.toList());
    }

    public List<HistoryResponse> getHistoryByDate(LocalDate date) {
        return historyRepository.findByDate(date).stream()
                .map(historyMapper::toResponse)
                .collect(Collectors.toList());
    }

    public Map<LocalDate, List<HistoryResponse>> getHistoryGroupedByDay() {
        List<LocalDate> dates = historyRepository.findDistinctDates();
        Map<LocalDate, List<HistoryResponse>> grouped = new HashMap<>();

        for (LocalDate date : dates) {
            grouped.put(date, getHistoryByDate(date));
        }

        return grouped;
    }

    public List<HistoryResponse> searchHistory(String entityType, String actionType, Long userId, LocalDate date) {
        return historyRepository.searchHistory(entityType, actionType, userId, date).stream()
                .map(historyMapper::toResponse)
                .collect(Collectors.toList());
    }

    // In HistoryService.java - Add this method

    // Add to HistoryService:
    public void logConventionRenewal(Convention oldConvention, Convention newConvention, User renewedBy) {
        String description = String.format("Renouvellement de la convention %s - Version %d → %d",
                newConvention.getReferenceConvention(),
                oldConvention.getRenewalVersion(),
                newConvention.getRenewalVersion());

        Map<String, Object> data = new HashMap<>();
        data.put("oldVersion", oldConvention.getRenewalVersion());
        data.put("newVersion", newConvention.getRenewalVersion());
        data.put("oldMontantTTC", oldConvention.getMontantTTC());
        data.put("newMontantTTC", newConvention.getMontantTTC());

        createHistory("RENEW", "CONVENTION", newConvention.getId(),
                newConvention.getReferenceConvention(),
                newConvention.getLibelle(), description, null, data, renewedBy);


    }


    /**
     * Log chef de projet reassignment (when approved by admin)
     */
    public void logChefReassignment(Application application, User oldChef, User newChef, User approvedBy, String reason) {
        try {
            String oldChefName = oldChef != null ?
                    oldChef.getFirstName() + " " + oldChef.getLastName() + " (" + oldChef.getUsername() + ")" :
                    "Non assigné";
            String newChefName = newChef != null ?
                    newChef.getFirstName() + " " + newChef.getLastName() + " (" + newChef.getUsername() + ")" :
                    "Non assigné";

            String description = String.format("Réassignation du chef de projet pour %s: %s → %s par %s %s",
                    application.getName(),
                    oldChefName,
                    newChefName,
                    approvedBy.getFirstName(),
                    approvedBy.getLastName());

            if (reason != null && !reason.isEmpty()) {
                description += " - Raison: " + reason;
            }

            Map<String, Object> oldData = new HashMap<>();
            oldData.put("chefDeProjet", oldChef != null ? oldChef.getId() : null);
            oldData.put("chefDeProjetName", oldChefName);

            Map<String, Object> newData = new HashMap<>();
            newData.put("chefDeProjet", newChef != null ? newChef.getId() : null);
            newData.put("chefDeProjetName", newChefName);
            newData.put("approvedBy", approvedBy.getId());
            newData.put("approvedByName", approvedBy.getFirstName() + " " + approvedBy.getLastName());
            newData.put("reason", reason);

            createHistory("REASSIGN_CHEF", "APPLICATION", application.getId(),
                    application.getCode(), application.getName(),
                    description, oldData, newData, approvedBy);

            log.info("Chef reassignment history logged for application {}: {} → {}",
                    application.getCode(), oldChefName, newChefName);

        } catch (Exception e) {
            log.error("Failed to log chef reassignment: {}", e.getMessage(), e);
        }
    }

    /**
     * Log chef reassignment request processed (approved/denied)
     */
    public void logReassignmentRequestProcessed(Request request, User processor, String action, String reason) {
        try {
            String actionLabel = "APPROVE".equals(action) ? "APPROUVÉE" : "REFUSÉE";

            String description = String.format("Demande de réassignation %s par %s %s - Application: %s",
                    actionLabel,
                    processor.getFirstName(),
                    processor.getLastName(),
                    request.getApplication().getName());

            Map<String, Object> data = new HashMap<>();
            data.put("requestId", request.getId());
            data.put("requestType", request.getRequestType());
            data.put("action", action);
            data.put("processor", processor.getId());
            data.put("processorName", processor.getFirstName() + " " + processor.getLastName());
            data.put("requester", request.getRequester() != null ? request.getRequester().getId() : null);
            data.put("requesterName", request.getRequester() != null ?
                    request.getRequester().getFirstName() + " " + request.getRequester().getLastName() : null);
            data.put("recommendedChef", request.getRecommendedChef() != null ? request.getRecommendedChef().getId() : null);
            data.put("recommendedChefName", request.getRecommendedChef() != null ?
                    request.getRecommendedChef().getFirstName() + " " + request.getRecommendedChef().getLastName() : null);

            if (reason != null && !reason.isEmpty()) {
                data.put("reason", reason);
            }

            createHistory("REQUEST_PROCESSED", "REQUEST", request.getId(),
                    "REQ-" + request.getId(), "Demande de réassignation",
                    description, null, data, processor);

            log.info("Reassignment request {} processed: {}", request.getId(), actionLabel);

        } catch (Exception e) {
            log.error("Failed to log request processing: {}", e.getMessage(), e);
        }
    }


}