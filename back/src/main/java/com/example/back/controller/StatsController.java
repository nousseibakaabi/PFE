package com.example.back.controller;

import com.example.back.entity.*;
import com.example.back.repository.*;
import com.example.back.service.UserContextService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/stats")
@Slf4j
public class StatsController {

    private final ConventionRepository conventionRepository;

    private final FactureRepository factureRepository;

    private final UserRepository userRepository;

    private final StructureRepository structureRepository;

    private final ZoneGeographiqueRepository zoneGeographiqueRepository;

    private final ApplicationRepository applicationRepository;

    private final UserContextService userContextService;

    public StatsController(ConventionRepository conventionRepository, FactureRepository factureRepository, UserRepository userRepository, StructureRepository structureRepository, ZoneGeographiqueRepository zoneGeographiqueRepository, ApplicationRepository applicationRepository, UserContextService userContextService) {
        this.conventionRepository = conventionRepository;
        this.factureRepository = factureRepository;
        this.userRepository = userRepository;
        this.structureRepository = structureRepository;
        this.zoneGeographiqueRepository = zoneGeographiqueRepository;
        this.applicationRepository = applicationRepository;
        this.userContextService = userContextService;
    }

    // ==================== HELPER METHODS FOR ACCESS CONTROL ====================

    private String getHighestRole(User user) {
        if (user.getRoles().stream().anyMatch(r -> r.getName() == ERole.ROLE_ADMIN)) {
            return "ADMIN";
        } else if (user.getRoles().stream().anyMatch(r -> r.getName() == ERole.ROLE_CHEF_PROJET)) {
            return "CHEF_PROJET";
        } else if (user.getRoles().stream().anyMatch(r -> r.getName() == ERole.ROLE_COMMERCIAL_METIER)) {
            return "COMMERCIAL_METIER";
        } else if (user.getRoles().stream().anyMatch(r -> r.getName() == ERole.ROLE_DECIDEUR)) {
            return "DECIDEUR";
        }
        return "USER";
    }

    private List<Application> getApplicationsForChef(User chef) {
        return applicationRepository.findByChefDeProjet(chef);
    }

    private List<Convention> getConventionsForChef(User chef) {
        List<Application> chefApplications = getApplicationsForChef(chef);
        return conventionRepository.findAll().stream()
                .filter(c -> c.getApplication() != null &&
                        chefApplications.stream()
                                .anyMatch(app -> app.getId().equals(c.getApplication().getId())))
                .collect(Collectors.toList());
    }

    private List<Facture> getFacturesForChef(User chef) {
        List<Convention> chefConventions = getConventionsForChef(chef);
        return factureRepository.findAll().stream()
                .filter(f -> f.getConvention() != null &&
                        chefConventions.stream()
                                .anyMatch(conv -> conv.getId().equals(f.getConvention().getId())))
                .collect(Collectors.toList());
    }

    private List<Convention> getConventionsForCommercial(User commercial) {
        return conventionRepository.findAll().stream()
                .filter(c -> c.getCreatedBy() != null &&
                        c.getCreatedBy().getId().equals(commercial.getId()))
                .collect(Collectors.toList());
    }

    private List<Facture> getFacturesForCommercial(User commercial) {
        return factureRepository.findAll().stream()
                .filter(f -> f.getConvention() != null &&
                        f.getConvention().getCreatedBy() != null &&
                        f.getConvention().getCreatedBy().getId().equals(commercial.getId()))
                .collect(Collectors.toList());
    }

    private List<Application> getApplicationsForCommercial(User commercial) {
        List<Convention> commercialConventions = getConventionsForCommercial(commercial);
        return applicationRepository.findAll().stream()
                .filter(a -> a.getConventions() != null &&
                        a.getConventions().stream()
                                .anyMatch(c -> commercialConventions.stream()
                                        .anyMatch(cc -> cc.getId().equals(c.getId()))))
                .distinct()
                .collect(Collectors.toList());
    }

    // ==================== DASHBOARD OVERALL STATS ====================

    @GetMapping("/dashboard")
    @PreAuthorize("hasAnyRole('ADMIN', 'COMMERCIAL_METIER', 'DECIDEUR', 'CHEF_PROJET')")
    public ResponseEntity<?> getDashboardStats() {
        try {
            User currentUser = userContextService.getCurrentUser();
            String userRole = getHighestRole(currentUser);

            Map<String, Object> stats = new HashMap<>();

            switch (userRole) {
                case "ADMIN" -> {
                    stats.putAll(getConventionStats());
                    stats.putAll(getFactureStats());
                    stats.putAll(getFinancialStats());
                    stats.putAll(getApplicationStats());
                    stats.putAll(getUserStats());
                    stats.putAll(getNomenclatureStats());
                    stats.putAll(getRecentActivity());
                    stats.putAll(getMonthlyTrends());
                }
                case "DECIDEUR" -> {
                    stats.putAll(getConventionStats());
                    stats.putAll(getFactureStats());
                    stats.putAll(getFinancialStats());
                    stats.putAll(getApplicationStats());
                    stats.putAll(getNomenclatureStats());
                    stats.putAll(getRecentActivity());
                    stats.putAll(getMonthlyTrends());
                }
                case "CHEF_PROJET" -> {
                    stats.putAll(getConventionStatsForChef(currentUser));
                    stats.putAll(getFactureStatsForChef(currentUser));
                    stats.putAll(getFinancialStatsForChef(currentUser));
                    stats.putAll(getApplicationStatsForChef(currentUser));
                    stats.putAll(getRecentActivityForChef(currentUser));
                    stats.putAll(getMonthlyTrendsForChef(currentUser));
                }
                case "COMMERCIAL_METIER" -> {
                    stats.putAll(getConventionStatsForCommercial(currentUser));
                    stats.putAll(getFactureStatsForCommercial(currentUser));
                    stats.putAll(getFinancialStatsForCommercial(currentUser));
                    stats.putAll(getApplicationStatsForCommercial(currentUser));
                    stats.putAll(getRecentActivityForCommercial(currentUser));
                    stats.putAll(getMonthlyTrendsForCommercial(currentUser));
                }

                default -> throw new IllegalStateException("Unexpected value: " + userRole);
            }

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", stats);
            response.put("userRole", userRole);

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error fetching dashboard stats: ", e);
            return ResponseEntity.badRequest().body(createErrorResponse("Failed to fetch dashboard statistics"));
        }
    }

    // ==================== CONVENTION DETAILED STATS ====================

    @GetMapping("/conventions/detailed")
    @PreAuthorize("hasAnyRole('ADMIN', 'COMMERCIAL_METIER', 'DECIDEUR', 'CHEF_PROJET')")
    public ResponseEntity<?> getConventionDetailedStats() {
        try {
            User currentUser = userContextService.getCurrentUser();
            String userRole = getHighestRole(currentUser);

            Map<String, Object> stats = new HashMap<>();

            stats = switch (userRole) {
                case "ADMIN", "DECIDEUR" -> getConventionDetailedStatsForAdmin();
                case "CHEF_PROJET" -> getConventionDetailedStatsForChef(currentUser);
                case "COMMERCIAL_METIER" -> getConventionDetailedStatsForCommercial(currentUser);
                default -> stats;
            };

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", stats);
            response.put("userRole", userRole);

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error fetching convention detailed stats: ", e);
            return ResponseEntity.badRequest().body(createErrorResponse("Failed to fetch convention statistics"));
        }
    }

    // ==================== FACTURE DETAILED STATS ====================

    @GetMapping("/factures/detailed")
    @PreAuthorize("hasAnyRole('ADMIN', 'COMMERCIAL_METIER', 'DECIDEUR', 'CHEF_PROJET')")
    public ResponseEntity<?> getFactureDetailedStats() {
        try {
            User currentUser = userContextService.getCurrentUser();
            String userRole = getHighestRole(currentUser);

            Map<String, Object> stats = new HashMap<>();

            stats = switch (userRole) {
                case "ADMIN", "DECIDEUR" -> getFactureDetailedStatsForAdmin();
                case "CHEF_PROJET" -> getFactureDetailedStatsForChef(currentUser);
                case "COMMERCIAL_METIER" -> getFactureDetailedStatsForCommercial(currentUser);
                default -> stats;
            };

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", stats);
            response.put("userRole", userRole);

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error fetching facture detailed stats: ", e);
            return ResponseEntity.badRequest().body(createErrorResponse("Failed to fetch invoice statistics"));
        }
    }

    // ==================== APPLICATION DETAILED STATS ====================

    @GetMapping("/applications/detailed")
    @PreAuthorize("hasAnyRole('ADMIN', 'DECIDEUR', 'CHEF_PROJET', 'COMMERCIAL_METIER')")
    public ResponseEntity<?> getApplicationDetailedStats() {
        try {
            User currentUser = userContextService.getCurrentUser();
            String userRole = getHighestRole(currentUser);

            Map<String, Object> stats = new HashMap<>();

            stats = switch (userRole) {
                case "ADMIN", "DECIDEUR" -> getApplicationDetailedStatsForAdmin();
                case "CHEF_PROJET" -> getApplicationDetailedStatsForChef(currentUser);
                case "COMMERCIAL_METIER" -> getApplicationDetailedStatsForCommercial(currentUser);
                default -> stats;
            };

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", stats);
            response.put("userRole", userRole);

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error fetching application detailed stats: ", e);
            return ResponseEntity.badRequest().body(createErrorResponse("Failed to fetch application statistics"));
        }
    }

    // ==================== FINANCIAL DETAILED STATS ====================

    @GetMapping("/financial/detailed")
    @PreAuthorize("hasAnyRole('ADMIN', 'COMMERCIAL_METIER', 'DECIDEUR', 'CHEF_PROJET')")
    public ResponseEntity<?> getFinancialDetailedStats() {
        try {
            User currentUser = userContextService.getCurrentUser();
            String userRole = getHighestRole(currentUser);

            Map<String, Object> stats = new HashMap<>();

            stats = switch (userRole) {
                case "ADMIN", "DECIDEUR" -> getFinancialDetailedStatsForAdmin();
                case "CHEF_PROJET" -> getFinancialDetailedStatsForChef(currentUser);
                case "COMMERCIAL_METIER" -> getFinancialDetailedStatsForCommercial(currentUser);
                default -> stats;
            };

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", stats);
            response.put("userRole", userRole);

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error fetching financial detailed stats: ", e);
            return ResponseEntity.badRequest().body(createErrorResponse("Failed to fetch financial statistics"));
        }
    }

    // ==================== NOMENCLATURE DETAILED STATS ====================

    @GetMapping("/nomenclatures/detailed")
    @PreAuthorize("hasAnyRole('ADMIN', 'DECIDEUR')")
    public ResponseEntity<?> getNomenclatureDetailedStats() {
        try {
            User currentUser = userContextService.getCurrentUser();
            String userRole = getHighestRole(currentUser);

            Map<String, Object> stats = new HashMap<>();

            // Structures by Type
            Map<String, Long> structuresByType = structureRepository.findAll().stream()
                    .filter(s -> s.getTypeStructure() != null)
                    .collect(Collectors.groupingBy(
                            Structure::getTypeStructure,
                            Collectors.counting()
                    ));
            stats.put("structuresByType", structuresByType);

            // Structures Responsable (not Client)
            long structuresResponsableCount = structureRepository.findByTypeStructureNot("Client").size();
            stats.put("structuresResponsable", structuresResponsableCount);

            // Structures Beneficiaire (Client)
            long structuresBeneficiaireCount = structureRepository.findByTypeStructure("Client").size();
            stats.put("structuresBeneficiaire", structuresBeneficiaireCount);

            // Zones by Type
            Map<String, Long> zonesByType = zoneGeographiqueRepository.findAll().stream()
                    .collect(Collectors.groupingBy(
                            z -> z.getType() != null ? z.getType().toString() : "CUSTOM",
                            Collectors.counting()
                    ));
            stats.put("zonesByType", zonesByType);

            // Tunisian Zones
            long tunisianZonesCount = zoneGeographiqueRepository.findByType(ZoneType.TUNISIAN_ZONE).size();
            stats.put("tunisianZones", tunisianZonesCount);

            // Custom Zones
            long customZonesCount = zoneGeographiqueRepository.findByType(ZoneType.CUSTOM_ZONE).size();
            stats.put("customZones", customZonesCount);

            // Nomenclature Counts
            Map<String, Long> nomenclatureCounts = new HashMap<>();
            nomenclatureCounts.put("applications", applicationRepository.count());
            nomenclatureCounts.put("zones", zoneGeographiqueRepository.count());
            nomenclatureCounts.put("structures", structureRepository.count());
            nomenclatureCounts.put("structuresResponsable", structuresResponsableCount);
            nomenclatureCounts.put("structuresBeneficiaire", structuresBeneficiaireCount);
            stats.put("nomenclatureCounts", nomenclatureCounts);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", stats);
            response.put("userRole", userRole);

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error fetching nomenclature detailed stats: ", e);
            return ResponseEntity.badRequest().body(createErrorResponse("Failed to fetch nomenclature statistics"));
        }
    }

    // ==================== USER DETAILED STATS (ADMIN ONLY) ====================

    @GetMapping("/users/detailed")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> getUserDetailedStats() {
        try {
            Map<String, Object> stats = new HashMap<>();

            // Get all non-admin users first
            List<User> nonAdminUsers = userRepository.findAll().stream()
                    .filter(user -> user.getRoles().stream()
                            .noneMatch(role -> role.getName() == ERole.ROLE_ADMIN))
                    .toList();

            // Role Distribution (excluding admins)
            Map<String, Long> roleDistribution = nonAdminUsers.stream()
                    .flatMap(user -> user.getRoles().stream())
                    .collect(Collectors.groupingBy(
                            role -> role.getName().name(),
                            Collectors.counting()
                    ));
            stats.put("roleDistribution", roleDistribution);

            // User Activity (excluding admins)
            Map<String, Long> userActivity = new HashMap<>();
            userActivity.put("total", (long) nonAdminUsers.size());
            userActivity.put("active", nonAdminUsers.stream()
                    .filter(user -> user.getEnabled() != null && user.getEnabled())
                    .count());
            userActivity.put("locked", nonAdminUsers.stream()
                    .filter(user -> (user.getLockedByAdmin() != null && user.getLockedByAdmin()) ||
                            (user.getAccountLockedUntil() != null &&
                                    user.getAccountLockedUntil().isAfter(LocalDateTime.now())))
                    .count());
            userActivity.put("inactive", nonAdminUsers.stream()
                    .filter(user -> user.getLastLogin() != null &&
                            user.getLastLogin().isBefore(LocalDateTime.now().minusMonths(3)))
                    .count());
            stats.put("userActivity", userActivity);

            // Users by Role Counts (excluding admins)
            Map<String, Long> usersByRole = new HashMap<>();
            usersByRole.put("chefDeProjet", nonAdminUsers.stream()
                    .filter(user -> user.getRoles().stream()
                            .anyMatch(role -> role.getName() == ERole.ROLE_CHEF_PROJET))
                    .count());
            usersByRole.put("commercialMetier", nonAdminUsers.stream()
                    .filter(user -> user.getRoles().stream()
                            .anyMatch(role -> role.getName() == ERole.ROLE_COMMERCIAL_METIER))
                    .count());
            usersByRole.put("decideur", nonAdminUsers.stream()
                    .filter(user -> user.getRoles().stream()
                            .anyMatch(role -> role.getName() == ERole.ROLE_DECIDEUR))
                    .count());

            // Optional: Add admin count separately if needed
            long adminCount = userRepository.findAll().stream()
                    .filter(user -> user.getRoles().stream()
                            .anyMatch(role -> role.getName() == ERole.ROLE_ADMIN))
                    .count();
            usersByRole.put("admin", adminCount);

            stats.put("usersByRole", usersByRole);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", stats);

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error fetching user detailed stats: ", e);
            return ResponseEntity.badRequest().body(createErrorResponse("Failed to fetch user statistics"));
        }
    }

    // ==================== SUMMARY STATS ====================

    @GetMapping("/summary")
    @PreAuthorize("hasAnyRole('ADMIN', 'COMMERCIAL_METIER', 'DECIDEUR', 'CHEF_PROJET')")
    public ResponseEntity<?> getSummaryStats() {
        try {
            User currentUser = userContextService.getCurrentUser();
            String userRole = getHighestRole(currentUser);

            Map<String, Object> summary = new HashMap<>();

            summary = switch (userRole) {
                case "ADMIN", "DECIDEUR" -> getSummaryStatsForAdmin();
                case "CHEF_PROJET" -> getSummaryStatsForChef(currentUser);
                case "COMMERCIAL_METIER" -> getSummaryStatsForCommercial(currentUser);
                default -> summary;
            };

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", summary);
            response.put("userRole", userRole);

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error fetching summary stats: ", e);
            return ResponseEntity.badRequest().body(createErrorResponse("Failed to fetch summary statistics"));
        }
    }

    // ==================== OVERDUE ALERTS ====================

    @GetMapping("/overdue/alert")
    @PreAuthorize("hasAnyRole('ADMIN', 'COMMERCIAL_METIER', 'DECIDEUR', 'CHEF_PROJET')")
    public ResponseEntity<?> getOverdueAlerts() {
        try {
            User currentUser = userContextService.getCurrentUser();
            String userRole = getHighestRole(currentUser);

            List<Map<String, Object>> alerts = new ArrayList<>();

            alerts = switch (userRole) {
                case "ADMIN", "DECIDEUR" -> getOverdueAlertsForAdmin();
                case "CHEF_PROJET" -> getOverdueAlertsForChef(currentUser);
                case "COMMERCIAL_METIER" -> getOverdueAlertsForCommercial(currentUser);
                default -> alerts;
            };

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", alerts);
            response.put("count", alerts.size());
            response.put("userRole", userRole);

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error fetching overdue alerts: ", e);
            return ResponseEntity.badRequest().body(createErrorResponse("Failed to fetch overdue alerts"));
        }
    }

    // ==================== ADMIN METHODS (ALL DATA) ====================

    private Map<String, Object> getConventionStats() {
        Map<String, Object> stats = new HashMap<>();
        List<Convention> allConventions = conventionRepository.findAll();

        long totalConventions = allConventions.size();
        long activeConventions = allConventions.stream()
                .filter(c -> "EN COURS".equals(c.getEtat()))
                .count();
        long planifiedConventions = allConventions.stream()
                .filter(c -> "PLANIFIE".equals(c.getEtat()))
                .count();
        long terminatedConventions = allConventions.stream()
                .filter(c -> "TERMINE".equals(c.getEtat()))
                .count();
        long archivedConventions = allConventions.stream()
                .filter(c -> "ARCHIVE".equals(c.getEtat()))
                .count();

        stats.put("totalConventions", totalConventions);
        stats.put("activeConventions", activeConventions);
        stats.put("planifiedConventions", planifiedConventions);
        stats.put("terminatedConventions", terminatedConventions);
        stats.put("archivedConventions", archivedConventions);
        stats.put("conventionCompletionRate",
                totalConventions > 0 ? ((double) terminatedConventions / totalConventions) * 100 : 0);

        return stats;
    }

    private Map<String, Object> getFactureStats() {
        Map<String, Object> stats = new HashMap<>();
        List<Facture> allFactures = factureRepository.findAll();

        long totalFactures = allFactures.size();
        long paidFactures = allFactures.stream()
                .filter(f -> "PAYE".equals(f.getStatutPaiement()))
                .count();
        long unpaidFactures = allFactures.stream()
                .filter(f -> "NON_PAYE".equals(f.getStatutPaiement()) && !f.isEnRetard())
                .count();
        long overdueFactures = allFactures.stream()
                .filter(f -> f.isEnRetard() || "EN_RETARD".equals(f.getStatutPaiement()))
                .count();

        stats.put("totalFactures", totalFactures);
        stats.put("paidFactures", paidFactures);
        stats.put("unpaidFactures", unpaidFactures);
        stats.put("overdueFactures", overdueFactures);
        stats.put("paymentRate",
                totalFactures > 0 ? ((double) paidFactures / totalFactures) * 100 : 0);

        BigDecimal totalPaidAmount = allFactures.stream()
                .filter(f -> "PAYE".equals(f.getStatutPaiement()))
                .map(f -> f.getMontantTTC() != null ? f.getMontantTTC() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        stats.put("totalPaidAmount", totalPaidAmount);

        BigDecimal totalUnpaidAmount = allFactures.stream()
                .filter(f -> !"PAYE".equals(f.getStatutPaiement()))
                .map(f -> f.getMontantTTC() != null ? f.getMontantTTC() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        stats.put("totalUnpaidAmount", totalUnpaidAmount);

        return stats;
    }

    private Map<String, Object> getApplicationStats() {
        Map<String, Object> stats = new HashMap<>();
        List<Application> allApplications = applicationRepository.findAll();

        long totalApplications = allApplications.size();
        long activeApplications = allApplications.stream()
                .filter(a -> "EN_COURS".equals(a.getStatus()))
                .count();
        long plannedApplications = allApplications.stream()
                .filter(a -> "PLANIFIE".equals(a.getStatus()))
                .count();
        long completedApplications = allApplications.stream()
                .filter(a -> "TERMINE".equals(a.getStatus()))
                .count();

        double avgProgress = allApplications.stream()
                .mapToInt(a -> a.getTimeBasedProgress() != null ? a.getTimeBasedProgress() : 0)
                .average()
                .orElse(0.0);

        stats.put("totalApplications", totalApplications);
        stats.put("activeApplications", activeApplications);
        stats.put("plannedApplications", plannedApplications);
        stats.put("completedApplications", completedApplications);
        stats.put("averageProgress", Math.round(avgProgress * 100.0) / 100.0);
        stats.put("applicationCompletionRate",
                totalApplications > 0 ? ((double) completedApplications / totalApplications) * 100 : 0);

        return stats;
    }

    private Map<String, Object> getUserStats() {
        Map<String, Object> stats = new HashMap<>();

        List<User> allUsers = userRepository.findAll();

        long totalUsers = allUsers.size();
        long activeUsers = allUsers.stream()
                .filter(user -> user.getEnabled() != null && user.getEnabled())
                .count();

        long chefDeProjetCount = allUsers.stream()
                .filter(user -> user.getRoles().stream()
                        .anyMatch(role -> role.getName().name().equals("ROLE_CHEF_PROJET")))
                .count();

        long commercialMetierCount = allUsers.stream()
                .filter(user -> user.getRoles().stream()
                        .anyMatch(role -> role.getName().name().equals("ROLE_COMMERCIAL_METIER")))
                .count();

        long decideurCount = allUsers.stream()
                .filter(user -> user.getRoles().stream()
                        .anyMatch(role -> role.getName().name().equals("ROLE_DECIDEUR")))
                .count();


        stats.put("totalUsers", totalUsers);
        stats.put("activeUsers", activeUsers);
        stats.put("chefDeProjetCount", chefDeProjetCount);
        stats.put("commercialMetierCount", commercialMetierCount);
        stats.put("decideurCount", decideurCount);

        return stats;
    }

    private Map<String, Object> getNomenclatureStats() {
        Map<String, Object> stats = new HashMap<>();

        long structuresResponsable = structureRepository.findByTypeStructureNot("Client").size();
        long structuresBeneficiaire = structureRepository.findByTypeStructure("Client").size();
        long tunisianZones = zoneGeographiqueRepository.findByType(ZoneType.TUNISIAN_ZONE).size();
        long customZones = zoneGeographiqueRepository.findByType(ZoneType.CUSTOM_ZONE).size();

        stats.put("structuresResponsable", structuresResponsable);
        stats.put("structuresBeneficiaire", structuresBeneficiaire);
        stats.put("totalStructures", structureRepository.count());
        stats.put("tunisianZones", tunisianZones);
        stats.put("customZones", customZones);
        stats.put("totalZones", zoneGeographiqueRepository.count());
        stats.put("totalApplications", applicationRepository.count());

        return stats;
    }

    private Map<String, Object> getFinancialStats() {
        Map<String, Object> stats = new HashMap<>();

        List<Facture> allFactures = factureRepository.findAll();
        List<Convention> allConventions = conventionRepository.findAll();

        BigDecimal totalRevenue = allFactures.stream()
                .filter(f -> "PAYE".equals(f.getStatutPaiement()))
                .map(f -> f.getMontantTTC() != null ? f.getMontantTTC() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal pendingRevenue = allFactures.stream()
                .filter(f -> "NON_PAYE".equals(f.getStatutPaiement()) || "EN_RETARD".equals(f.getStatutPaiement()))
                .map(f -> f.getMontantTTC() != null ? f.getMontantTTC() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalContractValue = allConventions.stream()
                .map(c -> c.getMontantTTC() != null ? c.getMontantTTC() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        stats.put("totalRevenue", totalRevenue);
        stats.put("pendingRevenue", pendingRevenue);
        stats.put("totalContractValue", totalContractValue);

        return stats;
    }

    private Map<String, Object> getRecentActivity() {
        Map<String, Object> activity = new HashMap<>();

        // Recent Applications
        List<Map<String, Object>> recentApplications = applicationRepository.findAll().stream()
                .sorted(Comparator.comparing(Application::getCreatedAt).reversed())
                .limit(5)
                .map(a -> {
                    Map<String, Object> app = new HashMap<>();
                    app.put("code", a.getCode());
                    app.put("name", a.getName());
                    app.put("clientName", a.getClientName());
                    app.put("chef", a.getChefProjetName());
                    app.put("status", a.getStatus());
                    app.put("progress", a.getTimeBasedProgress());
                    app.put("createdAt", a.getCreatedAt());
                    return app;
                })
                .collect(Collectors.toList());

        // Recent Conventions
        List<Map<String, Object>> recentConventions = conventionRepository.findAll().stream()
                .sorted(Comparator.comparing(Convention::getCreatedAt).reversed())
                .limit(5)
                .map(c -> {
                    Map<String, Object> conv = new HashMap<>();
                    conv.put("reference", c.getReferenceConvention());
                    conv.put("libelle", c.getLibelle());
                    conv.put("structure", c.getStructureResponsable() != null ? c.getStructureResponsable().getName() : "N/A");
                    conv.put("etat", c.getEtat() != null ? c.getEtat() : "NO_STATUS");
                    conv.put("createdAt", c.getCreatedAt());
                    return conv;
                })
                .collect(Collectors.toList());

        // Recent Factures
        List<Map<String, Object>> recentFactures = factureRepository.findAll().stream()
                .sorted(Comparator.comparing(Facture::getCreatedAt).reversed())
                .limit(5)
                .map(f -> {
                    Map<String, Object> fact = new HashMap<>();
                    fact.put("numero", f.getNumeroFacture());
                    fact.put("convention", f.getConvention() != null ? f.getConvention().getReferenceConvention() : "N/A");
                    fact.put("montant", f.getMontantTTC());
                    fact.put("statut", f.getStatutPaiement());
                    fact.put("dateEcheance", f.getDateEcheance());
                    fact.put("createdAt", f.getCreatedAt());
                    return fact;
                })
                .collect(Collectors.toList());

        activity.put("recentApplications", recentApplications);
        activity.put("recentConventions", recentConventions);
        activity.put("recentFactures", recentFactures);

        return activity;
    }

    private Map<String, Object> getMonthlyTrends() {
        Map<String, Object> trends = new HashMap<>();

        // Application trends by month
        Map<String, Long> applicationTrends = new LinkedHashMap<>();
        // Convention trends by month
        Map<String, Long> conventionTrends = new LinkedHashMap<>();
        // Revenue trends by month
        Map<String, BigDecimal> revenueTrends = new LinkedHashMap<>();

        for (int i = 5; i >= 0; i--) {
            YearMonth month = YearMonth.now().minusMonths(i);
            String monthKey = month.format(DateTimeFormatter.ofPattern("MMM yyyy"));

            long appsCount = applicationRepository.findAll().stream()
                    .filter(a -> a.getCreatedAt() != null &&
                            YearMonth.from(a.getCreatedAt().toLocalDate()).equals(month))
                    .count();
            applicationTrends.put(monthKey, appsCount);

            long convCount = conventionRepository.findAll().stream()
                    .filter(c -> c.getCreatedAt() != null &&
                            YearMonth.from(c.getCreatedAt().toLocalDate()).equals(month))
                    .count();
            conventionTrends.put(monthKey, convCount);

            BigDecimal revenue = factureRepository.findAll().stream()
                    .filter(f -> "PAYE".equals(f.getStatutPaiement()) &&
                            f.getDatePaiement() != null &&
                            YearMonth.from(f.getDatePaiement()).equals(month))
                    .map(f -> f.getMontantTTC() != null ? f.getMontantTTC() : BigDecimal.ZERO)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            revenueTrends.put(monthKey, revenue);
        }

        trends.put("applicationTrends", applicationTrends);
        trends.put("conventionTrends", conventionTrends);
        trends.put("revenueTrends", revenueTrends);

        return trends;
    }

    private Map<String, Object> getConventionDetailedStatsForAdmin() {
        Map<String, Object> stats = new HashMap<>();
        List<Convention> allConventions = conventionRepository.findAll();

        // Status Distribution
        List<Map<String, Object>> statusDistribution = new ArrayList<>();
        Map<String, Long> statusCount = allConventions.stream()
                .collect(Collectors.groupingBy(
                        c -> c.getEtat() != null ? c.getEtat() : "NO_STATUS",
                        Collectors.counting()
                ));

        List<String> allStatuses = Arrays.asList("PLANIFIE", "EN COURS", "TERMINE", "ARCHIVE");
        for (String status : allStatuses) {
            Map<String, Object> statusMap = new HashMap<>();
            statusMap.put("name", status);
            statusMap.put("count", statusCount.getOrDefault(status, 0L));
            statusDistribution.add(statusMap);
        }
        stats.put("statusDistribution", statusDistribution);

        // Monthly Conventions
        Map<String, Long> monthlyConventions = new LinkedHashMap<>();
        for (int i = 5; i >= 0; i--) {
            YearMonth month = YearMonth.now().minusMonths(i);
            long count = allConventions.stream()
                    .filter(c -> c.getCreatedAt() != null &&
                            YearMonth.from(c.getCreatedAt().toLocalDate()).equals(month))
                    .count();
            monthlyConventions.put(month.format(DateTimeFormatter.ofPattern("MMM yyyy")), count);
        }
        stats.put("monthlyConventions", monthlyConventions);

        // Amount by Status
        Map<String, BigDecimal> amountByStatus = new HashMap<>();
        for (Convention convention : allConventions) {
            String etat = convention.getEtat() != null ? convention.getEtat() : "NO_STATUS";
            BigDecimal montant = convention.getMontantTTC() != null ? convention.getMontantTTC() : BigDecimal.ZERO;
            amountByStatus.merge(etat, montant, BigDecimal::add);
        }
        stats.put("amountByStatus", amountByStatus);

        // Top Structures by Convention Count
        List<Map<String, Object>> topStructures = allConventions.stream()
                .filter(c -> c.getStructureResponsable() != null)
                .collect(Collectors.groupingBy(
                        Convention::getStructureResponsable,
                        Collectors.counting()
                ))
                .entrySet().stream()
                .sorted((e1, e2) -> Long.compare(e2.getValue(), e1.getValue()))
                .limit(5)
                .map(entry -> {
                    Map<String, Object> struct = new HashMap<>();
                    struct.put("structure", entry.getKey().getName());
                    struct.put("count", entry.getValue());
                    return struct;
                })
                .collect(Collectors.toList());
        stats.put("topStructures", topStructures);

        return stats;
    }

    private Map<String, Object> getFactureDetailedStatsForAdmin() {
        Map<String, Object> stats = new HashMap<>();
        List<Facture> allFactures = factureRepository.findAll();

        // Payment Status Distribution
        List<Map<String, Object>> paymentStatus = new ArrayList<>();

        long paidCount = allFactures.stream()
                .filter(f -> "PAYE".equals(f.getStatutPaiement()))
                .count();
        BigDecimal paidAmount = allFactures.stream()
                .filter(f -> "PAYE".equals(f.getStatutPaiement()))
                .map(f -> f.getMontantTTC() != null ? f.getMontantTTC() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        long unpaidCount = allFactures.stream()
                .filter(f -> "NON_PAYE".equals(f.getStatutPaiement()) && !f.isEnRetard())
                .count();
        BigDecimal unpaidAmount = allFactures.stream()
                .filter(f -> "NON_PAYE".equals(f.getStatutPaiement()) && !f.isEnRetard())
                .map(f -> f.getMontantTTC() != null ? f.getMontantTTC() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        long overdueCount = allFactures.stream()
                .filter(f -> f.isEnRetard() || "EN_RETARD".equals(f.getStatutPaiement()))
                .count();
        BigDecimal overdueAmount = allFactures.stream()
                .filter(f -> f.isEnRetard() || "EN_RETARD".equals(f.getStatutPaiement()))
                .map(f -> f.getMontantTTC() != null ? f.getMontantTTC() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        Map<String, Object> paidStatus = new HashMap<>();
        paidStatus.put("status", "PAYE");
        paidStatus.put("count", paidCount);
        paidStatus.put("amount", paidAmount);
        paymentStatus.add(paidStatus);

        Map<String, Object> unpaidStatus = new HashMap<>();
        unpaidStatus.put("status", "NON_PAYE");
        unpaidStatus.put("count", unpaidCount);
        unpaidStatus.put("amount", unpaidAmount);
        paymentStatus.add(unpaidStatus);

        Map<String, Object> overdueStatus = new HashMap<>();
        overdueStatus.put("status", "EN_RETARD");
        overdueStatus.put("count", overdueCount);
        overdueStatus.put("amount", overdueAmount);
        paymentStatus.add(overdueStatus);

        stats.put("paymentStatus", paymentStatus);

        // Monthly Invoice Amounts
        Map<String, Map<String, BigDecimal>> monthlyAmounts = new LinkedHashMap<>();
        for (int i = 5; i >= 0; i--) {
            YearMonth month = YearMonth.now().minusMonths(i);
            List<Facture> monthFactures = allFactures.stream()
                    .filter(f -> f.getDateFacturation() != null &&
                            YearMonth.from(f.getDateFacturation()).equals(month))
                    .toList();

            Map<String, BigDecimal> monthStats = new HashMap<>();
            monthStats.put("total", monthFactures.stream()
                    .map(f -> f.getMontantTTC() != null ? f.getMontantTTC() : BigDecimal.ZERO)
                    .reduce(BigDecimal.ZERO, BigDecimal::add));
            monthStats.put("paid", monthFactures.stream()
                    .filter(f -> "PAYE".equals(f.getStatutPaiement()))
                    .map(f -> f.getMontantTTC() != null ? f.getMontantTTC() : BigDecimal.ZERO)
                    .reduce(BigDecimal.ZERO, BigDecimal::add));

            monthlyAmounts.put(month.format(DateTimeFormatter.ofPattern("MMM yyyy")), monthStats);
        }
        stats.put("monthlyAmounts", monthlyAmounts);

        // Overdue Invoices Details
        List<Map<String, Object>> overdueDetails = allFactures.stream()
                .filter(f -> f.isEnRetard() || "EN_RETARD".equals(f.getStatutPaiement()))
                .sorted(Comparator.comparing(Facture::getDateEcheance))
                .map(f -> {
                    Map<String, Object> detail = new HashMap<>();
                    detail.put("numero", f.getNumeroFacture());
                    detail.put("convention", f.getConvention() != null ? f.getConvention().getReferenceConvention() : "N/A");
                    detail.put("montant", f.getMontantTTC());
                    detail.put("dateEcheance", f.getDateEcheance());
                    detail.put("joursRetard",
                            ChronoUnit.DAYS.between(f.getDateEcheance(), LocalDate.now()));
                    return detail;
                })
                .collect(Collectors.toList());
        stats.put("overdueDetails", overdueDetails);

        // Top Convention by Invoice Amount
        List<Map<String, Object>> topConventionAmounts = allFactures.stream()
                .filter(f -> f.getConvention() != null)
                .collect(Collectors.groupingBy(
                        Facture::getConvention,
                        Collectors.reducing(BigDecimal.ZERO,
                                f -> f.getMontantTTC() != null ? f.getMontantTTC() : BigDecimal.ZERO,
                                BigDecimal::add)
                ))
                .entrySet().stream()
                .sorted((e1, e2) -> e2.getValue().compareTo(e1.getValue()))
                .limit(5)
                .map(entry -> {
                    Map<String, Object> conv = new HashMap<>();
                    conv.put("convention", entry.getKey().getReferenceConvention());
                    conv.put("totalAmount", entry.getValue());
                    conv.put("etat", entry.getKey().getEtat());
                    return conv;
                })
                .collect(Collectors.toList());
        stats.put("topConventionAmounts", topConventionAmounts);

        return stats;
    }

    private Map<String, Object> getFinancialDetailedStatsForAdmin() {
        Map<String, Object> stats = new HashMap<>();

        List<Facture> allFactures = factureRepository.findAll();
        List<Convention> allConventions = conventionRepository.findAll();

        // Total Revenue
        BigDecimal totalRevenue = allFactures.stream()
                .filter(f -> "PAYE".equals(f.getStatutPaiement()))
                .map(f -> f.getMontantTTC() != null ? f.getMontantTTC() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        stats.put("totalRevenue", totalRevenue);

        // Pending Payments
        BigDecimal pendingPayments = allFactures.stream()
                .filter(f -> "NON_PAYE".equals(f.getStatutPaiement()) || "EN_RETARD".equals(f.getStatutPaiement()))
                .map(f -> f.getMontantTTC() != null ? f.getMontantTTC() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        stats.put("pendingPayments", pendingPayments);

        // Overdue Amount
        BigDecimal overdueAmount = allFactures.stream()
                .filter(f -> f.isEnRetard() || "EN_RETARD".equals(f.getStatutPaiement()))
                .map(f -> f.getMontantTTC() != null ? f.getMontantTTC() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        stats.put("overdueAmount", overdueAmount);

        // Total Contract Value
        BigDecimal totalContractValue = allConventions.stream()
                .map(c -> c.getMontantTTC() != null ? c.getMontantTTC() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        stats.put("totalContractValue", totalContractValue);

        // Revenue by Month
        Map<String, BigDecimal> revenueByMonth = new LinkedHashMap<>();
        for (int i = 5; i >= 0; i--) {
            YearMonth month = YearMonth.now().minusMonths(i);
            BigDecimal monthRevenue = allFactures.stream()
                    .filter(f -> "PAYE".equals(f.getStatutPaiement()) &&
                            f.getDatePaiement() != null &&
                            YearMonth.from(f.getDatePaiement()).equals(month))
                    .map(f -> f.getMontantTTC() != null ? f.getMontantTTC() : BigDecimal.ZERO)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            revenueByMonth.put(month.format(DateTimeFormatter.ofPattern("MMM yyyy")), monthRevenue);
        }
        stats.put("revenueByMonth", revenueByMonth);

        // Top Earning Conventions
        List<Map<String, Object>> topEarningConventions = allConventions.stream()
                .filter(c -> c.getMontantTTC() != null && c.getMontantTTC().compareTo(BigDecimal.ZERO) > 0)
                .sorted((c1, c2) -> c2.getMontantTTC().compareTo(c1.getMontantTTC()))
                .limit(5)
                .map(c -> {
                    Map<String, Object> conventionInfo = new HashMap<>();
                    conventionInfo.put("reference", c.getReferenceConvention());
                    conventionInfo.put("libelle", c.getLibelle());
                    conventionInfo.put("structure", c.getStructureResponsable() != null ? c.getStructureResponsable().getName() : "N/A");
                    conventionInfo.put("montantTotal", c.getMontantTTC());
                    conventionInfo.put("etat", c.getEtat());
                    return conventionInfo;
                })
                .collect(Collectors.toList());
        stats.put("topEarningConventions", topEarningConventions);

        // Payment Collection Rate
        BigDecimal totalInvoiced = allFactures.stream()
                .map(f -> f.getMontantTTC() != null ? f.getMontantTTC() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        double collectionRate = totalInvoiced.compareTo(BigDecimal.ZERO) > 0 ?
                totalRevenue.divide(totalInvoiced, 4, RoundingMode.HALF_UP).doubleValue() * 100 : 0;
        stats.put("collectionRate", collectionRate);

        return stats;
    }

    private Map<String, Object> getApplicationDetailedStatsForAdmin() {
        Map<String, Object> stats = new HashMap<>();
        List<Application> allApplications = applicationRepository.findAll();

        // Status Distribution
        List<Map<String, Object>> statusDistribution = new ArrayList<>();
        Map<String, Long> statusCount = allApplications.stream()
                .collect(Collectors.groupingBy(
                        a -> a.getStatus() != null ? a.getStatus() : "NO_STATUS",
                        Collectors.counting()
                ));

        List<String> allStatuses = Arrays.asList("PLANIFIE", "EN_COURS", "TERMINE");
        for (String status : allStatuses) {
            Map<String, Object> statusMap = new HashMap<>();
            statusMap.put("name", status);
            statusMap.put("count", statusCount.getOrDefault(status, 0L));
            statusDistribution.add(statusMap);
        }
        stats.put("statusDistribution", statusDistribution);

        // Monthly Applications
        Map<String, Long> monthlyApplications = new LinkedHashMap<>();
        for (int i = 5; i >= 0; i--) {
            YearMonth month = YearMonth.now().minusMonths(i);
            long count = allApplications.stream()
                    .filter(a -> a.getCreatedAt() != null &&
                            YearMonth.from(a.getCreatedAt().toLocalDate()).equals(month))
                    .count();
            monthlyApplications.put(month.format(DateTimeFormatter.ofPattern("MMM yyyy")), count);
        }
        stats.put("monthlyApplications", monthlyApplications);

        // Applications by Chef
        List<Map<String, Object>> applicationsByChef = allApplications.stream()
                .filter(a -> a.getChefDeProjet() != null)
                .collect(Collectors.groupingBy(
                        Application::getChefDeProjet,
                        Collectors.counting()
                ))
                .entrySet().stream()
                .sorted((e1, e2) -> Long.compare(e2.getValue(), e1.getValue()))
                .limit(5)
                .map(entry -> {
                    Map<String, Object> chef = new HashMap<>();
                    chef.put("chef", entry.getKey().getFirstName() + " " + entry.getKey().getLastName());
                    chef.put("count", entry.getValue());
                    return chef;
                })
                .collect(Collectors.toList());
        stats.put("applicationsByChef", applicationsByChef);

        // Unassigned Applications
        long unassignedCount = allApplications.stream()
                .filter(a -> a.getChefDeProjet() == null)
                .count();
        stats.put("unassignedApplications", unassignedCount);

        // Progress Statistics
        Map<String, Object> progressStats = new HashMap<>();
        double avgProgress = allApplications.stream()
                .mapToInt(a -> a.getTimeBasedProgress() != null ? a.getTimeBasedProgress() : 0)
                .average()
                .orElse(0.0);
        progressStats.put("averageProgress", Math.round(avgProgress * 100.0) / 100.0);

        long onTrackApplications = allApplications.stream()
                .filter(a -> a.getTimeBasedProgress() != null && a.getTimeBasedProgress() >= 70)
                .count();
        progressStats.put("onTrackApplications", onTrackApplications);

        long delayedApplications = allApplications.stream()
                .filter(a -> a.getDaysRemaining() < 0 && !"TERMINE".equals(a.getStatus()))
                .count();
        progressStats.put("delayedApplications", delayedApplications);

        stats.put("progressStats", progressStats);

        return stats;
    }

    private Map<String, Object> getSummaryStatsForAdmin() {
        Map<String, Object> summary = new HashMap<>();

        summary.put("totalConventions", conventionRepository.count());
        summary.put("totalFactures", factureRepository.count());
        summary.put("totalApplications", applicationRepository.count());

        LocalDate today = LocalDate.now();

        long conventionsCreatedToday = conventionRepository.findAll().stream()
                .filter(c -> c.getCreatedAt() != null &&
                        c.getCreatedAt().toLocalDate().equals(today))
                .count();

        long facturesCreatedToday = factureRepository.findAll().stream()
                .filter(f -> f.getCreatedAt() != null &&
                        f.getCreatedAt().toLocalDate().equals(today))
                .count();

        long facturesDueToday = factureRepository.findAll().stream()
                .filter(f -> f.getDateEcheance() != null &&
                        f.getDateEcheance().equals(today))
                .count();

        long overdueFacturesToday = factureRepository.findAll().stream()
                .filter(f -> f.isEnRetard() || "EN_RETARD".equals(f.getStatutPaiement()))
                .count();

        summary.put("conventionsToday", conventionsCreatedToday);
        summary.put("facturesToday", facturesCreatedToday);
        summary.put("dueToday", facturesDueToday);
        summary.put("overdueToday", overdueFacturesToday);

        BigDecimal todayRevenue = factureRepository.findAll().stream()
                .filter(f -> "PAYE".equals(f.getStatutPaiement()) &&
                        f.getDatePaiement() != null &&
                        f.getDatePaiement().equals(today))
                .map(f -> f.getMontantTTC() != null ? f.getMontantTTC() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        summary.put("todayRevenue", todayRevenue);

        return summary;
    }

    private List<Map<String, Object>> getOverdueAlertsForAdmin() {
        List<Map<String, Object>> alerts = new ArrayList<>();

        // Overdue invoices
        List<Facture> overdueInvoices = factureRepository.findAll().stream()
                .filter(f -> f.isEnRetard() || "EN_RETARD".equals(f.getStatutPaiement()))
                .toList();

        for (Facture invoice : overdueInvoices) {
            Map<String, Object> alert = new HashMap<>();
            alert.put("type", "INVOICE_OVERDUE");
            alert.put("message", String.format("Facture %s en retard de %d jours",
                    invoice.getNumeroFacture(),
                    ChronoUnit.DAYS.between(invoice.getDateEcheance(), LocalDate.now())));
            alert.put("convention", invoice.getConvention() != null ? invoice.getConvention().getReferenceConvention() : "N/A");
            alert.put("project", invoice.getConvention() != null && invoice.getConvention().getApplication() != null ?
                    invoice.getConvention().getApplication().getName() : "N/A");
            alert.put("amount", invoice.getMontantTTC());
            alert.put("dueDate", invoice.getDateEcheance());
            alert.put("priority", getOverduePriority(invoice));
            alerts.add(alert);
        }

        // Delayed applications
        List<Application> delayedApplications = applicationRepository.findAll().stream()
                .filter(a -> a.getDaysRemaining() < 0 && !"TERMINE".equals(a.getStatus()))
                .toList();

        for (Application app : delayedApplications) {
            Map<String, Object> alert = new HashMap<>();
            alert.put("type", "APPLICATION_DELAYED");
            alert.put("message", String.format("Application %s en retard (échéance dépassée de %d jours)",
                    app.getName(), Math.abs(app.getDaysRemaining())));
            alert.put("application", app.getName());
            alert.put("code", app.getCode());
            alert.put("progress", app.getTimeBasedProgress());
            alert.put("endDate", app.getDateFin());
            alert.put("priority", "HIGH");
            alerts.add(alert);
        }

        return alerts;
    }

    private String getOverduePriority(Facture invoice) {
        long daysOverdue = ChronoUnit.DAYS.between(invoice.getDateEcheance(), LocalDate.now());
        if (daysOverdue > 30) return "CRITICAL";
        if (daysOverdue > 15) return "HIGH";
        if (daysOverdue > 7) return "MEDIUM";
        return "LOW";
    }

    // ==================== CHEF DE PROJET METHODS ====================

    private Map<String, Object> getConventionStatsForChef(User chef) {
        Map<String, Object> stats = new HashMap<>();
        List<Convention> chefConventions = getConventionsForChef(chef);

        long totalConventions = chefConventions.size();
        long activeConventions = chefConventions.stream()
                .filter(c -> "EN COURS".equals(c.getEtat()))
                .count();
        long planifiedConventions = chefConventions.stream()
                .filter(c -> "PLANIFIE".equals(c.getEtat()))
                .count();
        long terminatedConventions = chefConventions.stream()
                .filter(c -> "TERMINE".equals(c.getEtat()))
                .count();
        long archivedConventions = chefConventions.stream()
                .filter(c -> "ARCHIVE".equals(c.getEtat()))
                .count();

        stats.put("totalConventions", totalConventions);
        stats.put("activeConventions", activeConventions);
        stats.put("planifiedConventions", planifiedConventions);
        stats.put("terminatedConventions", terminatedConventions);
        stats.put("archivedConventions", archivedConventions);
        stats.put("conventionCompletionRate",
                totalConventions > 0 ? ((double) terminatedConventions / totalConventions) * 100 : 0);

        return stats;
    }

    private Map<String, Object> getFactureStatsForChef(User chef) {
        Map<String, Object> stats = new HashMap<>();
        List<Facture> chefFactures = getFacturesForChef(chef);

        long totalFactures = chefFactures.size();
        long paidFactures = chefFactures.stream()
                .filter(f -> "PAYE".equals(f.getStatutPaiement()))
                .count();
        long unpaidFactures = chefFactures.stream()
                .filter(f -> "NON_PAYE".equals(f.getStatutPaiement()) && !f.isEnRetard())
                .count();
        long overdueFactures = chefFactures.stream()
                .filter(f -> f.isEnRetard() || "EN_RETARD".equals(f.getStatutPaiement()))
                .count();

        stats.put("totalFactures", totalFactures);
        stats.put("paidFactures", paidFactures);
        stats.put("unpaidFactures", unpaidFactures);
        stats.put("overdueFactures", overdueFactures);
        stats.put("paymentRate",
                totalFactures > 0 ? ((double) paidFactures / totalFactures) * 100 : 0);

        BigDecimal totalPaidAmount = chefFactures.stream()
                .filter(f -> "PAYE".equals(f.getStatutPaiement()))
                .map(f -> f.getMontantTTC() != null ? f.getMontantTTC() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        stats.put("totalPaidAmount", totalPaidAmount);

        BigDecimal totalUnpaidAmount = chefFactures.stream()
                .filter(f -> !"PAYE".equals(f.getStatutPaiement()))
                .map(f -> f.getMontantTTC() != null ? f.getMontantTTC() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        stats.put("totalUnpaidAmount", totalUnpaidAmount);

        return stats;
    }

    private Map<String, Object> getApplicationStatsForChef(User chef) {
        Map<String, Object> stats = new HashMap<>();
        List<Application> chefApplications = getApplicationsForChef(chef);

        long totalApplications = chefApplications.size();
        long activeApplications = chefApplications.stream()
                .filter(a -> "EN_COURS".equals(a.getStatus()))
                .count();
        long plannedApplications = chefApplications.stream()
                .filter(a -> "PLANIFIE".equals(a.getStatus()))
                .count();
        long completedApplications = chefApplications.stream()
                .filter(a -> "TERMINE".equals(a.getStatus()))
                .count();

        double avgProgress = chefApplications.stream()
                .mapToInt(a -> a.getTimeBasedProgress() != null ? a.getTimeBasedProgress() : 0)
                .average()
                .orElse(0.0);

        stats.put("totalApplications", totalApplications);
        stats.put("activeApplications", activeApplications);
        stats.put("plannedApplications", plannedApplications);
        stats.put("completedApplications", completedApplications);
        stats.put("averageProgress", Math.round(avgProgress * 100.0) / 100.0);
        stats.put("applicationCompletionRate",
                totalApplications > 0 ? ((double) completedApplications / totalApplications) * 100 : 0);

        return stats;
    }

    private Map<String, Object> getFinancialStatsForChef(User chef) {
        Map<String, Object> stats = new HashMap<>();

        List<Facture> chefFactures = getFacturesForChef(chef);
        List<Convention> chefConventions = getConventionsForChef(chef);

        BigDecimal totalRevenue = chefFactures.stream()
                .filter(f -> "PAYE".equals(f.getStatutPaiement()))
                .map(f -> f.getMontantTTC() != null ? f.getMontantTTC() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal pendingRevenue = chefFactures.stream()
                .filter(f -> "NON_PAYE".equals(f.getStatutPaiement()) || "EN_RETARD".equals(f.getStatutPaiement()))
                .map(f -> f.getMontantTTC() != null ? f.getMontantTTC() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalContractValue = chefConventions.stream()
                .map(c -> c.getMontantTTC() != null ? c.getMontantTTC() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        stats.put("totalRevenue", totalRevenue);
        stats.put("pendingRevenue", pendingRevenue);
        stats.put("totalContractValue", totalContractValue);

        return stats;
    }

    private Map<String, Object> getRecentActivityForChef(User chef) {
        Map<String, Object> activity = new HashMap<>();

        List<Application> chefApplications = getApplicationsForChef(chef);
        List<Convention> chefConventions = getConventionsForChef(chef);
        List<Facture> chefFactures = getFacturesForChef(chef);

        // Recent Applications
        List<Map<String, Object>> recentApplications = chefApplications.stream()
                .sorted(Comparator.comparing(Application::getCreatedAt).reversed())
                .limit(5)
                .map(a -> {
                    Map<String, Object> app = new HashMap<>();
                    app.put("code", a.getCode());
                    app.put("name", a.getName());
                    app.put("clientName", a.getClientName());
                    app.put("status", a.getStatus());
                    app.put("progress", a.getTimeBasedProgress());
                    app.put("createdAt", a.getCreatedAt());
                    return app;
                })
                .collect(Collectors.toList());

        // Recent Conventions
        List<Map<String, Object>> recentConventions = chefConventions.stream()
                .sorted(Comparator.comparing(Convention::getCreatedAt).reversed())
                .limit(5)
                .map(c -> {
                    Map<String, Object> conv = new HashMap<>();
                    conv.put("reference", c.getReferenceConvention());
                    conv.put("libelle", c.getLibelle());
                    conv.put("structure", c.getStructureResponsable() != null ? c.getStructureResponsable().getName() : "N/A");
                    conv.put("etat", c.getEtat() != null ? c.getEtat() : "NO_STATUS");
                    conv.put("createdAt", c.getCreatedAt());
                    return conv;
                })
                .collect(Collectors.toList());

        // Recent Factures
        List<Map<String, Object>> recentFactures = chefFactures.stream()
                .sorted(Comparator.comparing(Facture::getCreatedAt).reversed())
                .limit(5)
                .map(f -> {
                    Map<String, Object> fact = new HashMap<>();
                    fact.put("numero", f.getNumeroFacture());
                    fact.put("convention", f.getConvention() != null ? f.getConvention().getReferenceConvention() : "N/A");
                    fact.put("montant", f.getMontantTTC());
                    fact.put("statut", f.getStatutPaiement());
                    fact.put("dateEcheance", f.getDateEcheance());
                    fact.put("createdAt", f.getCreatedAt());
                    return fact;
                })
                .collect(Collectors.toList());

        activity.put("recentApplications", recentApplications);
        activity.put("recentConventions", recentConventions);
        activity.put("recentFactures", recentFactures);

        return activity;
    }

    private Map<String, Object> getMonthlyTrendsForChef(User chef) {
        Map<String, Object> trends = new HashMap<>();

        List<Application> chefApplications = getApplicationsForChef(chef);
        List<Convention> chefConventions = getConventionsForChef(chef);
        List<Facture> chefFactures = getFacturesForChef(chef);

        Map<String, Long> applicationTrends = new LinkedHashMap<>();
        Map<String, Long> conventionTrends = new LinkedHashMap<>();
        Map<String, BigDecimal> revenueTrends = new LinkedHashMap<>();

        for (int i = 5; i >= 0; i--) {
            YearMonth month = YearMonth.now().minusMonths(i);
            String monthKey = month.format(DateTimeFormatter.ofPattern("MMM yyyy"));

            long appsCount = chefApplications.stream()
                    .filter(a -> a.getCreatedAt() != null &&
                            YearMonth.from(a.getCreatedAt().toLocalDate()).equals(month))
                    .count();
            applicationTrends.put(monthKey, appsCount);

            long convCount = chefConventions.stream()
                    .filter(c -> c.getCreatedAt() != null &&
                            YearMonth.from(c.getCreatedAt().toLocalDate()).equals(month))
                    .count();
            conventionTrends.put(monthKey, convCount);

            BigDecimal revenue = chefFactures.stream()
                    .filter(f -> "PAYE".equals(f.getStatutPaiement()) &&
                            f.getDatePaiement() != null &&
                            YearMonth.from(f.getDatePaiement()).equals(month))
                    .map(f -> f.getMontantTTC() != null ? f.getMontantTTC() : BigDecimal.ZERO)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            revenueTrends.put(monthKey, revenue);
        }

        trends.put("applicationTrends", applicationTrends);
        trends.put("conventionTrends", conventionTrends);
        trends.put("revenueTrends", revenueTrends);

        return trends;
    }

    private Map<String, Object> getConventionDetailedStatsForChef(User chef) {
        Map<String, Object> stats = new HashMap<>();
        List<Convention> chefConventions = getConventionsForChef(chef);

        // Status Distribution
        List<Map<String, Object>> statusDistribution = new ArrayList<>();
        Map<String, Long> statusCount = chefConventions.stream()
                .collect(Collectors.groupingBy(
                        c -> c.getEtat() != null ? c.getEtat() : "NO_STATUS",
                        Collectors.counting()
                ));

        List<String> allStatuses = Arrays.asList("PLANIFIE", "EN COURS", "TERMINE", "ARCHIVE");
        for (String status : allStatuses) {
            Map<String, Object> statusMap = new HashMap<>();
            statusMap.put("name", status);
            statusMap.put("count", statusCount.getOrDefault(status, 0L));
            statusDistribution.add(statusMap);
        }
        stats.put("statusDistribution", statusDistribution);

        // Monthly Conventions
        Map<String, Long> monthlyConventions = new LinkedHashMap<>();
        for (int i = 5; i >= 0; i--) {
            YearMonth month = YearMonth.now().minusMonths(i);
            long count = chefConventions.stream()
                    .filter(c -> c.getCreatedAt() != null &&
                            YearMonth.from(c.getCreatedAt().toLocalDate()).equals(month))
                    .count();
            monthlyConventions.put(month.format(DateTimeFormatter.ofPattern("MMM yyyy")), count);
        }
        stats.put("monthlyConventions", monthlyConventions);

        // Amount by Status
        Map<String, BigDecimal> amountByStatus = new HashMap<>();
        for (Convention convention : chefConventions) {
            String etat = convention.getEtat() != null ? convention.getEtat() : "NO_STATUS";
            BigDecimal montant = convention.getMontantTTC() != null ? convention.getMontantTTC() : BigDecimal.ZERO;
            amountByStatus.merge(etat, montant, BigDecimal::add);
        }
        stats.put("amountByStatus", amountByStatus);

        return stats;
    }

    private Map<String, Object> getFactureDetailedStatsForChef(User chef) {
        Map<String, Object> stats = new HashMap<>();
        List<Facture> chefFactures = getFacturesForChef(chef);

        // Payment Status Distribution
        List<Map<String, Object>> paymentStatus = new ArrayList<>();

        long paidCount = chefFactures.stream()
                .filter(f -> "PAYE".equals(f.getStatutPaiement()))
                .count();
        BigDecimal paidAmount = chefFactures.stream()
                .filter(f -> "PAYE".equals(f.getStatutPaiement()))
                .map(f -> f.getMontantTTC() != null ? f.getMontantTTC() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        long unpaidCount = chefFactures.stream()
                .filter(f -> "NON_PAYE".equals(f.getStatutPaiement()) && !f.isEnRetard())
                .count();
        BigDecimal unpaidAmount = chefFactures.stream()
                .filter(f -> "NON_PAYE".equals(f.getStatutPaiement()) && !f.isEnRetard())
                .map(f -> f.getMontantTTC() != null ? f.getMontantTTC() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        long overdueCount = chefFactures.stream()
                .filter(f -> f.isEnRetard() || "EN_RETARD".equals(f.getStatutPaiement()))
                .count();
        BigDecimal overdueAmount = chefFactures.stream()
                .filter(f -> f.isEnRetard() || "EN_RETARD".equals(f.getStatutPaiement()))
                .map(f -> f.getMontantTTC() != null ? f.getMontantTTC() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        Map<String, Object> paidStatus = new HashMap<>();
        paidStatus.put("status", "PAYE");
        paidStatus.put("count", paidCount);
        paidStatus.put("amount", paidAmount);
        paymentStatus.add(paidStatus);

        Map<String, Object> unpaidStatus = new HashMap<>();
        unpaidStatus.put("status", "NON_PAYE");
        unpaidStatus.put("count", unpaidCount);
        unpaidStatus.put("amount", unpaidAmount);
        paymentStatus.add(unpaidStatus);

        Map<String, Object> overdueStatus = new HashMap<>();
        overdueStatus.put("status", "EN_RETARD");
        overdueStatus.put("count", overdueCount);
        overdueStatus.put("amount", overdueAmount);
        paymentStatus.add(overdueStatus);

        stats.put("paymentStatus", paymentStatus);

        // Monthly Invoice Amounts
        Map<String, Map<String, BigDecimal>> monthlyAmounts = new LinkedHashMap<>();
        for (int i = 5; i >= 0; i--) {
            YearMonth month = YearMonth.now().minusMonths(i);
            List<Facture> monthFactures = chefFactures.stream()
                    .filter(f -> f.getDateFacturation() != null &&
                            YearMonth.from(f.getDateFacturation()).equals(month))
                    .toList();

            Map<String, BigDecimal> monthStats = new HashMap<>();
            monthStats.put("total", monthFactures.stream()
                    .map(f -> f.getMontantTTC() != null ? f.getMontantTTC() : BigDecimal.ZERO)
                    .reduce(BigDecimal.ZERO, BigDecimal::add));
            monthStats.put("paid", monthFactures.stream()
                    .filter(f -> "PAYE".equals(f.getStatutPaiement()))
                    .map(f -> f.getMontantTTC() != null ? f.getMontantTTC() : BigDecimal.ZERO)
                    .reduce(BigDecimal.ZERO, BigDecimal::add));

            monthlyAmounts.put(month.format(DateTimeFormatter.ofPattern("MMM yyyy")), monthStats);
        }
        stats.put("monthlyAmounts", monthlyAmounts);

        // Overdue Invoices Details
        List<Map<String, Object>> overdueDetails = chefFactures.stream()
                .filter(f -> f.isEnRetard() || "EN_RETARD".equals(f.getStatutPaiement()))
                .sorted(Comparator.comparing(Facture::getDateEcheance))
                .map(f -> {
                    Map<String, Object> detail = new HashMap<>();
                    detail.put("numero", f.getNumeroFacture());
                    detail.put("convention", f.getConvention() != null ? f.getConvention().getReferenceConvention() : "N/A");
                    detail.put("montant", f.getMontantTTC());
                    detail.put("dateEcheance", f.getDateEcheance());
                    detail.put("joursRetard",
                            ChronoUnit.DAYS.between(f.getDateEcheance(), LocalDate.now()));
                    return detail;
                })
                .collect(Collectors.toList());
        stats.put("overdueDetails", overdueDetails);

        return stats;
    }

    private Map<String, Object> getFinancialDetailedStatsForChef(User chef) {
        Map<String, Object> stats = new HashMap<>();

        List<Facture> chefFactures = getFacturesForChef(chef);
        List<Convention> chefConventions = getConventionsForChef(chef);

        BigDecimal totalRevenue = chefFactures.stream()
                .filter(f -> "PAYE".equals(f.getStatutPaiement()))
                .map(f -> f.getMontantTTC() != null ? f.getMontantTTC() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        stats.put("totalRevenue", totalRevenue);

        BigDecimal pendingPayments = chefFactures.stream()
                .filter(f -> "NON_PAYE".equals(f.getStatutPaiement()) || "EN_RETARD".equals(f.getStatutPaiement()))
                .map(f -> f.getMontantTTC() != null ? f.getMontantTTC() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        stats.put("pendingPayments", pendingPayments);

        BigDecimal overdueAmount = chefFactures.stream()
                .filter(f -> f.isEnRetard() || "EN_RETARD".equals(f.getStatutPaiement()))
                .map(f -> f.getMontantTTC() != null ? f.getMontantTTC() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        stats.put("overdueAmount", overdueAmount);

        BigDecimal totalContractValue = chefConventions.stream()
                .map(c -> c.getMontantTTC() != null ? c.getMontantTTC() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        stats.put("totalContractValue", totalContractValue);

        // Revenue by Month
        Map<String, BigDecimal> revenueByMonth = new LinkedHashMap<>();
        for (int i = 5; i >= 0; i--) {
            YearMonth month = YearMonth.now().minusMonths(i);
            BigDecimal monthRevenue = chefFactures.stream()
                    .filter(f -> "PAYE".equals(f.getStatutPaiement()) &&
                            f.getDatePaiement() != null &&
                            YearMonth.from(f.getDatePaiement()).equals(month))
                    .map(f -> f.getMontantTTC() != null ? f.getMontantTTC() : BigDecimal.ZERO)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            revenueByMonth.put(month.format(DateTimeFormatter.ofPattern("MMM yyyy")), monthRevenue);
        }
        stats.put("revenueByMonth", revenueByMonth);

        // Payment Collection Rate
        BigDecimal totalInvoiced = chefFactures.stream()
                .map(f -> f.getMontantTTC() != null ? f.getMontantTTC() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        double collectionRate = totalInvoiced.compareTo(BigDecimal.ZERO) > 0 ?
                totalRevenue.divide(totalInvoiced, 4, RoundingMode.HALF_UP).doubleValue() * 100 : 0;
        stats.put("collectionRate", collectionRate);

        return stats;
    }

    private Map<String, Object> getApplicationDetailedStatsForChef(User chef) {
        Map<String, Object> stats = new HashMap<>();
        List<Application> chefApplications = getApplicationsForChef(chef);

        // Status Distribution
        List<Map<String, Object>> statusDistribution = new ArrayList<>();
        Map<String, Long> statusCount = chefApplications.stream()
                .collect(Collectors.groupingBy(
                        a -> a.getStatus() != null ? a.getStatus() : "NO_STATUS",
                        Collectors.counting()
                ));

        List<String> allStatuses = Arrays.asList("PLANIFIE", "EN_COURS", "TERMINE");
        for (String status : allStatuses) {
            Map<String, Object> statusMap = new HashMap<>();
            statusMap.put("name", status);
            statusMap.put("count", statusCount.getOrDefault(status, 0L));
            statusDistribution.add(statusMap);
        }
        stats.put("statusDistribution", statusDistribution);

        // Monthly Applications
        Map<String, Long> monthlyApplications = new LinkedHashMap<>();
        for (int i = 5; i >= 0; i--) {
            YearMonth month = YearMonth.now().minusMonths(i);
            long count = chefApplications.stream()
                    .filter(a -> a.getCreatedAt() != null &&
                            YearMonth.from(a.getCreatedAt().toLocalDate()).equals(month))
                    .count();
            monthlyApplications.put(month.format(DateTimeFormatter.ofPattern("MMM yyyy")), count);
        }
        stats.put("monthlyApplications", monthlyApplications);

        // Progress Statistics
        Map<String, Object> progressStats = new HashMap<>();
        double avgProgress = chefApplications.stream()
                .mapToInt(a -> a.getTimeBasedProgress() != null ? a.getTimeBasedProgress() : 0)
                .average()
                .orElse(0.0);
        progressStats.put("averageProgress", Math.round(avgProgress * 100.0) / 100.0);

        long onTrackApplications = chefApplications.stream()
                .filter(a -> a.getTimeBasedProgress() != null && a.getTimeBasedProgress() >= 70)
                .count();
        progressStats.put("onTrackApplications", onTrackApplications);

        long delayedApplications = chefApplications.stream()
                .filter(a -> a.getDaysRemaining() < 0 && !"TERMINE".equals(a.getStatus()))
                .count();
        progressStats.put("delayedApplications", delayedApplications);

        stats.put("progressStats", progressStats);

        return stats;
    }

    private Map<String, Object> getSummaryStatsForChef(User chef) {
        Map<String, Object> summary = new HashMap<>();

        List<Application> chefApplications = getApplicationsForChef(chef);
        List<Convention> chefConventions = getConventionsForChef(chef);
        List<Facture> chefFactures = getFacturesForChef(chef);

        summary.put("totalConventions", chefConventions.size());
        summary.put("totalFactures", chefFactures.size());
        summary.put("totalApplications", chefApplications.size());

        LocalDate today = LocalDate.now();

        long conventionsCreatedToday = chefConventions.stream()
                .filter(c -> c.getCreatedAt() != null &&
                        c.getCreatedAt().toLocalDate().equals(today))
                .count();

        long facturesCreatedToday = chefFactures.stream()
                .filter(f -> f.getCreatedAt() != null &&
                        f.getCreatedAt().toLocalDate().equals(today))
                .count();

        long facturesDueToday = chefFactures.stream()
                .filter(f -> f.getDateEcheance() != null &&
                        f.getDateEcheance().equals(today))
                .count();

        long overdueFacturesToday = chefFactures.stream()
                .filter(f -> f.isEnRetard() || "EN_RETARD".equals(f.getStatutPaiement()))
                .count();

        summary.put("conventionsToday", conventionsCreatedToday);
        summary.put("facturesToday", facturesCreatedToday);
        summary.put("dueToday", facturesDueToday);
        summary.put("overdueToday", overdueFacturesToday);

        BigDecimal todayRevenue = chefFactures.stream()
                .filter(f -> "PAYE".equals(f.getStatutPaiement()) &&
                        f.getDatePaiement() != null &&
                        f.getDatePaiement().equals(today))
                .map(f -> f.getMontantTTC() != null ? f.getMontantTTC() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        summary.put("todayRevenue", todayRevenue);

        return summary;
    }

    private List<Map<String, Object>> getOverdueAlertsForChef(User chef) {
        List<Map<String, Object>> alerts = new ArrayList<>();

        List<Facture> chefFactures = getFacturesForChef(chef);
        List<Application> chefApplications = getApplicationsForChef(chef);

        // Overdue invoices
        List<Facture> overdueInvoices = chefFactures.stream()
                .filter(f -> f.isEnRetard() || "EN_RETARD".equals(f.getStatutPaiement()))
                .toList();

        for (Facture invoice : overdueInvoices) {
            Map<String, Object> alert = new HashMap<>();
            alert.put("type", "INVOICE_OVERDUE");
            alert.put("message", String.format("Facture %s en retard de %d jours",
                    invoice.getNumeroFacture(),
                    ChronoUnit.DAYS.between(invoice.getDateEcheance(), LocalDate.now())));
            alert.put("convention", invoice.getConvention() != null ? invoice.getConvention().getReferenceConvention() : "N/A");
            alert.put("amount", invoice.getMontantTTC());
            alert.put("dueDate", invoice.getDateEcheance());
            alert.put("priority", getOverduePriority(invoice));
            alerts.add(alert);
        }

        // Delayed applications
        List<Application> delayedApplications = chefApplications.stream()
                .filter(a -> a.getDaysRemaining() < 0 && !"TERMINE".equals(a.getStatus()))
                .toList();

        for (Application app : delayedApplications) {
            Map<String, Object> alert = new HashMap<>();
            alert.put("type", "APPLICATION_DELAYED");
            alert.put("message", String.format("Application %s en retard (échéance dépassée de %d jours)",
                    app.getName(), Math.abs(app.getDaysRemaining())));
            alert.put("application", app.getName());
            alert.put("code", app.getCode());
            alert.put("progress", app.getTimeBasedProgress());
            alert.put("endDate", app.getDateFin());
            alert.put("priority", "HIGH");
            alerts.add(alert);
        }

        return alerts;
    }

    // ==================== COMMERCIAL METHODS ====================

    private Map<String, Object> getConventionStatsForCommercial(User commercial) {
        Map<String, Object> stats = new HashMap<>();
        List<Convention> commercialConventions = getConventionsForCommercial(commercial);

        long totalConventions = commercialConventions.size();
        long activeConventions = commercialConventions.stream()
                .filter(c -> "EN COURS".equals(c.getEtat()))
                .count();
        long planifiedConventions = commercialConventions.stream()
                .filter(c -> "PLANIFIE".equals(c.getEtat()))
                .count();
        long terminatedConventions = commercialConventions.stream()
                .filter(c -> "TERMINE".equals(c.getEtat()))
                .count();
        long archivedConventions = commercialConventions.stream()
                .filter(c -> "ARCHIVE".equals(c.getEtat()))
                .count();

        stats.put("totalConventions", totalConventions);
        stats.put("activeConventions", activeConventions);
        stats.put("planifiedConventions", planifiedConventions);
        stats.put("terminatedConventions", terminatedConventions);
        stats.put("archivedConventions", archivedConventions);
        stats.put("conventionCompletionRate",
                totalConventions > 0 ? ((double) terminatedConventions / totalConventions) * 100 : 0);

        return stats;
    }

    private Map<String, Object> getFactureStatsForCommercial(User commercial) {
        Map<String, Object> stats = new HashMap<>();
        List<Facture> commercialFactures = getFacturesForCommercial(commercial);

        long totalFactures = commercialFactures.size();
        long paidFactures = commercialFactures.stream()
                .filter(f -> "PAYE".equals(f.getStatutPaiement()))
                .count();
        long unpaidFactures = commercialFactures.stream()
                .filter(f -> "NON_PAYE".equals(f.getStatutPaiement()) && !f.isEnRetard())
                .count();
        long overdueFactures = commercialFactures.stream()
                .filter(f -> f.isEnRetard() || "EN_RETARD".equals(f.getStatutPaiement()))
                .count();

        stats.put("totalFactures", totalFactures);
        stats.put("paidFactures", paidFactures);
        stats.put("unpaidFactures", unpaidFactures);
        stats.put("overdueFactures", overdueFactures);
        stats.put("paymentRate",
                totalFactures > 0 ? ((double) paidFactures / totalFactures) * 100 : 0);

        BigDecimal totalPaidAmount = commercialFactures.stream()
                .filter(f -> "PAYE".equals(f.getStatutPaiement()))
                .map(f -> f.getMontantTTC() != null ? f.getMontantTTC() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        stats.put("totalPaidAmount", totalPaidAmount);

        BigDecimal totalUnpaidAmount = commercialFactures.stream()
                .filter(f -> !"PAYE".equals(f.getStatutPaiement()))
                .map(f -> f.getMontantTTC() != null ? f.getMontantTTC() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        stats.put("totalUnpaidAmount", totalUnpaidAmount);

        return stats;
    }

    private Map<String, Object> getApplicationStatsForCommercial(User commercial) {
        Map<String, Object> stats = new HashMap<>();
        List<Application> commercialApplications = getApplicationsForCommercial(commercial);

        long totalApplications = commercialApplications.size();
        long activeApplications = commercialApplications.stream()
                .filter(a -> "EN_COURS".equals(a.getStatus()))
                .count();
        long plannedApplications = commercialApplications.stream()
                .filter(a -> "PLANIFIE".equals(a.getStatus()))
                .count();
        long completedApplications = commercialApplications.stream()
                .filter(a -> "TERMINE".equals(a.getStatus()))
                .count();

        double avgProgress = commercialApplications.stream()
                .mapToInt(a -> a.getTimeBasedProgress() != null ? a.getTimeBasedProgress() : 0)
                .average()
                .orElse(0.0);

        stats.put("totalApplications", totalApplications);
        stats.put("activeApplications", activeApplications);
        stats.put("plannedApplications", plannedApplications);
        stats.put("completedApplications", completedApplications);
        stats.put("averageProgress", Math.round(avgProgress * 100.0) / 100.0);
        stats.put("applicationCompletionRate",
                totalApplications > 0 ? ((double) completedApplications / totalApplications) * 100 : 0);

        return stats;
    }

    private Map<String, Object> getFinancialStatsForCommercial(User commercial) {
        Map<String, Object> stats = new HashMap<>();

        List<Facture> commercialFactures = getFacturesForCommercial(commercial);
        List<Convention> commercialConventions = getConventionsForCommercial(commercial);

        BigDecimal totalRevenue = commercialFactures.stream()
                .filter(f -> "PAYE".equals(f.getStatutPaiement()))
                .map(f -> f.getMontantTTC() != null ? f.getMontantTTC() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal pendingRevenue = commercialFactures.stream()
                .filter(f -> "NON_PAYE".equals(f.getStatutPaiement()) || "EN_RETARD".equals(f.getStatutPaiement()))
                .map(f -> f.getMontantTTC() != null ? f.getMontantTTC() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalContractValue = commercialConventions.stream()
                .map(c -> c.getMontantTTC() != null ? c.getMontantTTC() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        stats.put("totalRevenue", totalRevenue);
        stats.put("pendingRevenue", pendingRevenue);
        stats.put("totalContractValue", totalContractValue);

        return stats;
    }

    private Map<String, Object> getRecentActivityForCommercial(User commercial) {
        Map<String, Object> activity = new HashMap<>();

        List<Application> commercialApplications = getApplicationsForCommercial(commercial);
        List<Convention> commercialConventions = getConventionsForCommercial(commercial);
        List<Facture> commercialFactures = getFacturesForCommercial(commercial);

        // Recent Applications
        List<Map<String, Object>> recentApplications = commercialApplications.stream()
                .sorted(Comparator.comparing(Application::getCreatedAt).reversed())
                .limit(5)
                .map(a -> {
                    Map<String, Object> app = new HashMap<>();
                    app.put("code", a.getCode());
                    app.put("name", a.getName());
                    app.put("clientName", a.getClientName());
                    app.put("status", a.getStatus());
                    app.put("progress", a.getTimeBasedProgress());
                    app.put("createdAt", a.getCreatedAt());
                    return app;
                })
                .collect(Collectors.toList());

        // Recent Conventions
        List<Map<String, Object>> recentConventions = commercialConventions.stream()
                .sorted(Comparator.comparing(Convention::getCreatedAt).reversed())
                .limit(5)
                .map(c -> {
                    Map<String, Object> conv = new HashMap<>();
                    conv.put("reference", c.getReferenceConvention());
                    conv.put("libelle", c.getLibelle());
                    conv.put("structure", c.getStructureResponsable() != null ? c.getStructureResponsable().getName() : "N/A");
                    conv.put("etat", c.getEtat() != null ? c.getEtat() : "NO_STATUS");
                    conv.put("createdAt", c.getCreatedAt());
                    return conv;
                })
                .collect(Collectors.toList());

        // Recent Factures
        List<Map<String, Object>> recentFactures = commercialFactures.stream()
                .sorted(Comparator.comparing(Facture::getCreatedAt).reversed())
                .limit(5)
                .map(f -> {
                    Map<String, Object> fact = new HashMap<>();
                    fact.put("numero", f.getNumeroFacture());
                    fact.put("convention", f.getConvention() != null ? f.getConvention().getReferenceConvention() : "N/A");
                    fact.put("montant", f.getMontantTTC());
                    fact.put("statut", f.getStatutPaiement());
                    fact.put("dateEcheance", f.getDateEcheance());
                    fact.put("createdAt", f.getCreatedAt());
                    return fact;
                })
                .collect(Collectors.toList());

        activity.put("recentApplications", recentApplications);
        activity.put("recentConventions", recentConventions);
        activity.put("recentFactures", recentFactures);

        return activity;
    }

    private Map<String, Object> getMonthlyTrendsForCommercial(User commercial) {
        Map<String, Object> trends = new HashMap<>();

        List<Application> commercialApplications = getApplicationsForCommercial(commercial);
        List<Convention> commercialConventions = getConventionsForCommercial(commercial);
        List<Facture> commercialFactures = getFacturesForCommercial(commercial);

        Map<String, Long> applicationTrends = new LinkedHashMap<>();
        Map<String, Long> conventionTrends = new LinkedHashMap<>();
        Map<String, BigDecimal> revenueTrends = new LinkedHashMap<>();

        for (int i = 5; i >= 0; i--) {
            YearMonth month = YearMonth.now().minusMonths(i);
            String monthKey = month.format(DateTimeFormatter.ofPattern("MMM yyyy"));

            long appsCount = commercialApplications.stream()
                    .filter(a -> a.getCreatedAt() != null &&
                            YearMonth.from(a.getCreatedAt().toLocalDate()).equals(month))
                    .count();
            applicationTrends.put(monthKey, appsCount);

            long convCount = commercialConventions.stream()
                    .filter(c -> c.getCreatedAt() != null &&
                            YearMonth.from(c.getCreatedAt().toLocalDate()).equals(month))
                    .count();
            conventionTrends.put(monthKey, convCount);

            BigDecimal revenue = commercialFactures.stream()
                    .filter(f -> "PAYE".equals(f.getStatutPaiement()) &&
                            f.getDatePaiement() != null &&
                            YearMonth.from(f.getDatePaiement()).equals(month))
                    .map(f -> f.getMontantTTC() != null ? f.getMontantTTC() : BigDecimal.ZERO)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            revenueTrends.put(monthKey, revenue);
        }

        trends.put("applicationTrends", applicationTrends);
        trends.put("conventionTrends", conventionTrends);
        trends.put("revenueTrends", revenueTrends);

        return trends;
    }

    private Map<String, Object> getConventionDetailedStatsForCommercial(User commercial) {
        Map<String, Object> stats = new HashMap<>();
        List<Convention> commercialConventions = getConventionsForCommercial(commercial);

        // Status Distribution
        List<Map<String, Object>> statusDistribution = new ArrayList<>();
        Map<String, Long> statusCount = commercialConventions.stream()
                .collect(Collectors.groupingBy(
                        c -> c.getEtat() != null ? c.getEtat() : "NO_STATUS",
                        Collectors.counting()
                ));

        List<String> allStatuses = Arrays.asList("PLANIFIE", "EN COURS", "TERMINE", "ARCHIVE");
        for (String status : allStatuses) {
            Map<String, Object> statusMap = new HashMap<>();
            statusMap.put("name", status);
            statusMap.put("count", statusCount.getOrDefault(status, 0L));
            statusDistribution.add(statusMap);
        }
        stats.put("statusDistribution", statusDistribution);

        // Monthly Conventions
        Map<String, Long> monthlyConventions = new LinkedHashMap<>();
        for (int i = 5; i >= 0; i--) {
            YearMonth month = YearMonth.now().minusMonths(i);
            long count = commercialConventions.stream()
                    .filter(c -> c.getCreatedAt() != null &&
                            YearMonth.from(c.getCreatedAt().toLocalDate()).equals(month))
                    .count();
            monthlyConventions.put(month.format(DateTimeFormatter.ofPattern("MMM yyyy")), count);
        }
        stats.put("monthlyConventions", monthlyConventions);

        // Amount by Status
        Map<String, BigDecimal> amountByStatus = new HashMap<>();
        for (Convention convention : commercialConventions) {
            String etat = convention.getEtat() != null ? convention.getEtat() : "NO_STATUS";
            BigDecimal montant = convention.getMontantTTC() != null ? convention.getMontantTTC() : BigDecimal.ZERO;
            amountByStatus.merge(etat, montant, BigDecimal::add);
        }
        stats.put("amountByStatus", amountByStatus);

        return stats;
    }

    private Map<String, Object> getFactureDetailedStatsForCommercial(User commercial) {
        Map<String, Object> stats = new HashMap<>();
        List<Facture> commercialFactures = getFacturesForCommercial(commercial);

        // Payment Status Distribution
        List<Map<String, Object>> paymentStatus = new ArrayList<>();

        long paidCount = commercialFactures.stream()
                .filter(f -> "PAYE".equals(f.getStatutPaiement()))
                .count();
        BigDecimal paidAmount = commercialFactures.stream()
                .filter(f -> "PAYE".equals(f.getStatutPaiement()))
                .map(f -> f.getMontantTTC() != null ? f.getMontantTTC() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        long unpaidCount = commercialFactures.stream()
                .filter(f -> "NON_PAYE".equals(f.getStatutPaiement()) && !f.isEnRetard())
                .count();
        BigDecimal unpaidAmount = commercialFactures.stream()
                .filter(f -> "NON_PAYE".equals(f.getStatutPaiement()) && !f.isEnRetard())
                .map(f -> f.getMontantTTC() != null ? f.getMontantTTC() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        long overdueCount = commercialFactures.stream()
                .filter(f -> f.isEnRetard() || "EN_RETARD".equals(f.getStatutPaiement()))
                .count();
        BigDecimal overdueAmount = commercialFactures.stream()
                .filter(f -> f.isEnRetard() || "EN_RETARD".equals(f.getStatutPaiement()))
                .map(f -> f.getMontantTTC() != null ? f.getMontantTTC() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        Map<String, Object> paidStatus = new HashMap<>();
        paidStatus.put("status", "PAYE");
        paidStatus.put("count", paidCount);
        paidStatus.put("amount", paidAmount);
        paymentStatus.add(paidStatus);

        Map<String, Object> unpaidStatus = new HashMap<>();
        unpaidStatus.put("status", "NON_PAYE");
        unpaidStatus.put("count", unpaidCount);
        unpaidStatus.put("amount", unpaidAmount);
        paymentStatus.add(unpaidStatus);

        Map<String, Object> overdueStatus = new HashMap<>();
        overdueStatus.put("status", "EN_RETARD");
        overdueStatus.put("count", overdueCount);
        overdueStatus.put("amount", overdueAmount);
        paymentStatus.add(overdueStatus);

        stats.put("paymentStatus", paymentStatus);

        // Monthly Invoice Amounts
        Map<String, Map<String, BigDecimal>> monthlyAmounts = new LinkedHashMap<>();
        for (int i = 5; i >= 0; i--) {
            YearMonth month = YearMonth.now().minusMonths(i);
            List<Facture> monthFactures = commercialFactures.stream()
                    .filter(f -> f.getDateFacturation() != null &&
                            YearMonth.from(f.getDateFacturation()).equals(month))
                    .toList();

            Map<String, BigDecimal> monthStats = new HashMap<>();
            monthStats.put("total", monthFactures.stream()
                    .map(f -> f.getMontantTTC() != null ? f.getMontantTTC() : BigDecimal.ZERO)
                    .reduce(BigDecimal.ZERO, BigDecimal::add));
            monthStats.put("paid", monthFactures.stream()
                    .filter(f -> "PAYE".equals(f.getStatutPaiement()))
                    .map(f -> f.getMontantTTC() != null ? f.getMontantTTC() : BigDecimal.ZERO)
                    .reduce(BigDecimal.ZERO, BigDecimal::add));

            monthlyAmounts.put(month.format(DateTimeFormatter.ofPattern("MMM yyyy")), monthStats);
        }
        stats.put("monthlyAmounts", monthlyAmounts);

        // Overdue Invoices Details
        List<Map<String, Object>> overdueDetails = commercialFactures.stream()
                .filter(f -> f.isEnRetard() || "EN_RETARD".equals(f.getStatutPaiement()))
                .sorted(Comparator.comparing(Facture::getDateEcheance))
                .map(f -> {
                    Map<String, Object> detail = new HashMap<>();
                    detail.put("numero", f.getNumeroFacture());
                    detail.put("convention", f.getConvention() != null ? f.getConvention().getReferenceConvention() : "N/A");
                    detail.put("montant", f.getMontantTTC());
                    detail.put("dateEcheance", f.getDateEcheance());
                    detail.put("joursRetard",
                            ChronoUnit.DAYS.between(f.getDateEcheance(), LocalDate.now()));
                    return detail;
                })
                .collect(Collectors.toList());
        stats.put("overdueDetails", overdueDetails);

        return stats;
    }

    private Map<String, Object> getFinancialDetailedStatsForCommercial(User commercial) {
        Map<String, Object> stats = new HashMap<>();

        List<Facture> commercialFactures = getFacturesForCommercial(commercial);
        List<Convention> commercialConventions = getConventionsForCommercial(commercial);

        BigDecimal totalRevenue = commercialFactures.stream()
                .filter(f -> "PAYE".equals(f.getStatutPaiement()))
                .map(f -> f.getMontantTTC() != null ? f.getMontantTTC() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        stats.put("totalRevenue", totalRevenue);

        BigDecimal pendingPayments = commercialFactures.stream()
                .filter(f -> "NON_PAYE".equals(f.getStatutPaiement()) || "EN_RETARD".equals(f.getStatutPaiement()))
                .map(f -> f.getMontantTTC() != null ? f.getMontantTTC() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        stats.put("pendingPayments", pendingPayments);

        BigDecimal overdueAmount = commercialFactures.stream()
                .filter(f -> f.isEnRetard() || "EN_RETARD".equals(f.getStatutPaiement()))
                .map(f -> f.getMontantTTC() != null ? f.getMontantTTC() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        stats.put("overdueAmount", overdueAmount);

        BigDecimal totalContractValue = commercialConventions.stream()
                .map(c -> c.getMontantTTC() != null ? c.getMontantTTC() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        stats.put("totalContractValue", totalContractValue);

        // Revenue by Month
        Map<String, BigDecimal> revenueByMonth = new LinkedHashMap<>();
        for (int i = 5; i >= 0; i--) {
            YearMonth month = YearMonth.now().minusMonths(i);
            BigDecimal monthRevenue = commercialFactures.stream()
                    .filter(f -> "PAYE".equals(f.getStatutPaiement()) &&
                            f.getDatePaiement() != null &&
                            YearMonth.from(f.getDatePaiement()).equals(month))
                    .map(f -> f.getMontantTTC() != null ? f.getMontantTTC() : BigDecimal.ZERO)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            revenueByMonth.put(month.format(DateTimeFormatter.ofPattern("MMM yyyy")), monthRevenue);
        }
        stats.put("revenueByMonth", revenueByMonth);

        // Top Earning Conventions
        List<Map<String, Object>> topEarningConventions = commercialConventions.stream()
                .filter(c -> c.getMontantTTC() != null && c.getMontantTTC().compareTo(BigDecimal.ZERO) > 0)
                .sorted((c1, c2) -> c2.getMontantTTC().compareTo(c1.getMontantTTC()))
                .limit(5)
                .map(c -> {
                    Map<String, Object> conventionInfo = new HashMap<>();
                    conventionInfo.put("reference", c.getReferenceConvention());
                    conventionInfo.put("libelle", c.getLibelle());
                    conventionInfo.put("structure", c.getStructureResponsable() != null ? c.getStructureResponsable().getName() : "N/A");
                    conventionInfo.put("montantTotal", c.getMontantTTC());
                    conventionInfo.put("etat", c.getEtat());
                    return conventionInfo;
                })
                .collect(Collectors.toList());
        stats.put("topEarningConventions", topEarningConventions);

        // Payment Collection Rate
        BigDecimal totalInvoiced = commercialFactures.stream()
                .map(f -> f.getMontantTTC() != null ? f.getMontantTTC() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        double collectionRate = totalInvoiced.compareTo(BigDecimal.ZERO) > 0 ?
                totalRevenue.divide(totalInvoiced, 4, RoundingMode.HALF_UP).doubleValue() * 100 : 0;
        stats.put("collectionRate", collectionRate);

        return stats;
    }

    private Map<String, Object> getApplicationDetailedStatsForCommercial(User commercial) {
        Map<String, Object> stats = new HashMap<>();
        List<Application> commercialApplications = getApplicationsForCommercial(commercial);

        // Status Distribution
        List<Map<String, Object>> statusDistribution = new ArrayList<>();
        Map<String, Long> statusCount = commercialApplications.stream()
                .collect(Collectors.groupingBy(
                        a -> a.getStatus() != null ? a.getStatus() : "NO_STATUS",
                        Collectors.counting()
                ));

        List<String> allStatuses = Arrays.asList("PLANIFIE", "EN_COURS", "TERMINE");
        for (String status : allStatuses) {
            Map<String, Object> statusMap = new HashMap<>();
            statusMap.put("name", status);
            statusMap.put("count", statusCount.getOrDefault(status, 0L));
            statusDistribution.add(statusMap);
        }
        stats.put("statusDistribution", statusDistribution);

        // Monthly Applications
        Map<String, Long> monthlyApplications = new LinkedHashMap<>();
        for (int i = 5; i >= 0; i--) {
            YearMonth month = YearMonth.now().minusMonths(i);
            long count = commercialApplications.stream()
                    .filter(a -> a.getCreatedAt() != null &&
                            YearMonth.from(a.getCreatedAt().toLocalDate()).equals(month))
                    .count();
            monthlyApplications.put(month.format(DateTimeFormatter.ofPattern("MMM yyyy")), count);
        }
        stats.put("monthlyApplications", monthlyApplications);

        // Progress Statistics
        Map<String, Object> progressStats = new HashMap<>();
        double avgProgress = commercialApplications.stream()
                .mapToInt(a -> a.getTimeBasedProgress() != null ? a.getTimeBasedProgress() : 0)
                .average()
                .orElse(0.0);
        progressStats.put("averageProgress", Math.round(avgProgress * 100.0) / 100.0);

        long onTrackApplications = commercialApplications.stream()
                .filter(a -> a.getTimeBasedProgress() != null && a.getTimeBasedProgress() >= 70)
                .count();
        progressStats.put("onTrackApplications", onTrackApplications);

        long delayedApplications = commercialApplications.stream()
                .filter(a -> a.getDaysRemaining() < 0 && !"TERMINE".equals(a.getStatus()))
                .count();
        progressStats.put("delayedApplications", delayedApplications);

        stats.put("progressStats", progressStats);

        return stats;
    }

    private Map<String, Object> getSummaryStatsForCommercial(User commercial) {
        Map<String, Object> summary = new HashMap<>();

        List<Application> commercialApplications = getApplicationsForCommercial(commercial);
        List<Convention> commercialConventions = getConventionsForCommercial(commercial);
        List<Facture> commercialFactures = getFacturesForCommercial(commercial);

        summary.put("totalConventions", commercialConventions.size());
        summary.put("totalFactures", commercialFactures.size());
        summary.put("totalApplications", commercialApplications.size());

        LocalDate today = LocalDate.now();

        long conventionsCreatedToday = commercialConventions.stream()
                .filter(c -> c.getCreatedAt() != null &&
                        c.getCreatedAt().toLocalDate().equals(today))
                .count();

        long facturesCreatedToday = commercialFactures.stream()
                .filter(f -> f.getCreatedAt() != null &&
                        f.getCreatedAt().toLocalDate().equals(today))
                .count();

        long facturesDueToday = commercialFactures.stream()
                .filter(f -> f.getDateEcheance() != null &&
                        f.getDateEcheance().equals(today))
                .count();

        long overdueFacturesToday = commercialFactures.stream()
                .filter(f -> f.isEnRetard() || "EN_RETARD".equals(f.getStatutPaiement()))
                .count();

        summary.put("conventionsToday", conventionsCreatedToday);
        summary.put("facturesToday", facturesCreatedToday);
        summary.put("dueToday", facturesDueToday);
        summary.put("overdueToday", overdueFacturesToday);

        BigDecimal todayRevenue = commercialFactures.stream()
                .filter(f -> "PAYE".equals(f.getStatutPaiement()) &&
                        f.getDatePaiement() != null &&
                        f.getDatePaiement().equals(today))
                .map(f -> f.getMontantTTC() != null ? f.getMontantTTC() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        summary.put("todayRevenue", todayRevenue);

        return summary;
    }

    private List<Map<String, Object>> getOverdueAlertsForCommercial(User commercial) {
        List<Map<String, Object>> alerts = new ArrayList<>();

        List<Facture> commercialFactures = getFacturesForCommercial(commercial);
        List<Application> commercialApplications = getApplicationsForCommercial(commercial);

        // Overdue invoices
        List<Facture> overdueInvoices = commercialFactures.stream()
                .filter(f -> f.isEnRetard() || "EN_RETARD".equals(f.getStatutPaiement()))
                .toList();

        for (Facture invoice : overdueInvoices) {
            Map<String, Object> alert = new HashMap<>();
            alert.put("type", "INVOICE_OVERDUE");
            alert.put("message", String.format("Facture %s en retard de %d jours",
                    invoice.getNumeroFacture(),
                    ChronoUnit.DAYS.between(invoice.getDateEcheance(), LocalDate.now())));
            alert.put("convention", invoice.getConvention() != null ? invoice.getConvention().getReferenceConvention() : "N/A");
            alert.put("amount", invoice.getMontantTTC());
            alert.put("dueDate", invoice.getDateEcheance());
            alert.put("priority", getOverduePriority(invoice));
            alerts.add(alert);
        }

        // Delayed applications
        List<Application> delayedApplications = commercialApplications.stream()
                .filter(a -> a.getDaysRemaining() < 0 && !"TERMINE".equals(a.getStatus()))
                .toList();

        for (Application app : delayedApplications) {
            Map<String, Object> alert = new HashMap<>();
            alert.put("type", "APPLICATION_DELAYED");
            alert.put("message", String.format("Application %s en retard (échéance dépassée de %d jours)",
                    app.getName(), Math.abs(app.getDaysRemaining())));
            alert.put("application", app.getName());
            alert.put("code", app.getCode());
            alert.put("progress", app.getTimeBasedProgress());
            alert.put("endDate", app.getDateFin());
            alert.put("priority", "HIGH");
            alerts.add(alert);
        }

        return alerts;
    }

    private Map<String, Object> createErrorResponse(String message) {
        Map<String, Object> response = new HashMap<>();
        response.put("success", false);
        response.put("message", message);
        return response;
    }
}