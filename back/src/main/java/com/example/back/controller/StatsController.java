package com.example.back.controller;

import com.example.back.entity.*;
import com.example.back.repository.*;
import com.example.back.service.UserContextService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAdjusters;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/stats")
@PreAuthorize("hasAnyRole('ADMIN', 'COMMERCIAL_METIER', 'DECIDEUR', 'CHEF_PROJET')")
@Slf4j
public class StatsController {

    @Autowired
    private ConventionRepository conventionRepository;

    @Autowired
    private FactureRepository factureRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private StructureRepository structureRepository;

    @Autowired
    private ZoneGeographiqueRepository zoneGeographiqueRepository;

    @Autowired
    private ApplicationRepository applicationRepository;

    @Autowired
    private ProjectRepository projectRepository;

    @Autowired
    private UserContextService userContextService;

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

    private List<Convention> getAccessibleConventions(User currentUser) {
        List<Convention> allConventions = conventionRepository.findAll();

        if (currentUser.getRoles().stream().anyMatch(r ->
                r.getName() == ERole.ROLE_ADMIN ||
                        r.getName() == ERole.ROLE_DECIDEUR)) {
            return allConventions;
        }

        if (currentUser.getRoles().stream().anyMatch(r ->
                r.getName() == ERole.ROLE_COMMERCIAL_METIER)) {
            return allConventions.stream()
                    .filter(c -> c.getCreatedBy() != null &&
                            c.getCreatedBy().getId().equals(currentUser.getId()))
                    .collect(Collectors.toList());
        }

        if (currentUser.getRoles().stream().anyMatch(r ->
                r.getName() == ERole.ROLE_CHEF_PROJET)) {
            return allConventions.stream()
                    .filter(c -> c.getProject() != null &&
                            c.getProject().getChefDeProjet() != null &&
                            c.getProject().getChefDeProjet().getId().equals(currentUser.getId()))
                    .collect(Collectors.toList());
        }

        return new ArrayList<>();
    }

    private List<Facture> getAccessibleFactures(User currentUser) {
        List<Facture> allFactures = factureRepository.findAll();

        if (currentUser.getRoles().stream().anyMatch(r ->
                r.getName() == ERole.ROLE_ADMIN ||
                        r.getName() == ERole.ROLE_DECIDEUR)) {
            return allFactures;
        }

        if (currentUser.getRoles().stream().anyMatch(r ->
                r.getName() == ERole.ROLE_COMMERCIAL_METIER)) {
            return allFactures.stream()
                    .filter(f -> f.getConvention() != null &&
                            f.getConvention().getCreatedBy() != null &&
                            f.getConvention().getCreatedBy().getId().equals(currentUser.getId()))
                    .collect(Collectors.toList());
        }

        if (currentUser.getRoles().stream().anyMatch(r ->
                r.getName() == ERole.ROLE_CHEF_PROJET)) {
            return allFactures.stream()
                    .filter(f -> f.getConvention() != null &&
                            f.getConvention().getProject() != null &&
                            f.getConvention().getProject().getChefDeProjet() != null &&
                            f.getConvention().getProject().getChefDeProjet().getId().equals(currentUser.getId()))
                    .collect(Collectors.toList());
        }

        return new ArrayList<>();
    }

    private List<Project> getAccessibleProjects(User currentUser) {
        List<Project> allProjects = projectRepository.findAll();

        if (currentUser.getRoles().stream().anyMatch(r ->
                r.getName() == ERole.ROLE_ADMIN ||
                        r.getName() == ERole.ROLE_DECIDEUR)) {
            return allProjects;
        }

        if (currentUser.getRoles().stream().anyMatch(r ->
                r.getName() == ERole.ROLE_CHEF_PROJET)) {
            return allProjects.stream()
                    .filter(p -> p.getChefDeProjet() != null &&
                            p.getChefDeProjet().getId().equals(currentUser.getId()))
                    .collect(Collectors.toList());
        }

        if (currentUser.getRoles().stream().anyMatch(r ->
                r.getName() == ERole.ROLE_COMMERCIAL_METIER)) {
            return allProjects.stream()
                    .filter(p -> p.getConventions() != null &&
                            p.getConventions().stream()
                                    .anyMatch(c -> c.getCreatedBy() != null &&
                                            c.getCreatedBy().getId().equals(currentUser.getId())))
                    .distinct()
                    .collect(Collectors.toList());
        }

        return new ArrayList<>();
    }

    // ==================== DASHBOARD OVERALL STATS ====================

    @GetMapping("/dashboard")
    public ResponseEntity<?> getDashboardStats() {
        try {
            User currentUser = userContextService.getCurrentUser();
            String userRole = getHighestRole(currentUser);

            Map<String, Object> stats = new HashMap<>();

            // Convention Stats
            stats.putAll(getConventionStats(currentUser));

            // Invoice Stats
            stats.putAll(getFactureStats(currentUser));

            // Financial Stats
            stats.putAll(getFinancialStats(currentUser));

            // Project Stats
            stats.putAll(getProjectStats(currentUser));

            // User Stats - Only for ADMIN
            if (userRole.equals("ADMIN")) {
                stats.putAll(getUserStats());
            }

            // Nomenclature Stats - Only for ADMIN
            if (userRole.equals("ADMIN")) {
                stats.putAll(getNomenclatureStats());
            }

            // Recent Activity
            stats.putAll(getRecentActivity(currentUser));

            // Monthly Trends
            stats.putAll(getMonthlyTrends(currentUser));

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

    // ==================== DETAILED STATS ENDPOINTS ====================

    @GetMapping("/conventions/detailed")
    public ResponseEntity<?> getConventionDetailedStats() {
        try {
            User currentUser = userContextService.getCurrentUser();
            List<Convention> accessibleConventions = getAccessibleConventions(currentUser);

            Map<String, Object> stats = new HashMap<>();

            // Status Distribution
            List<Map<String, Object>> statusDistribution = new ArrayList<>();
            Map<String, Long> statusCount = accessibleConventions.stream()
                    .collect(Collectors.groupingBy(
                            c -> c.getEtat() != null ? c.getEtat() : "NO_STATUS",
                            Collectors.counting()
                    ));

            List<String> allStatuses = Arrays.asList("NO_STATUS", "EN_ATTENTE", "EN_COURS", "EN_RETARD", "TERMINE", "ARCHIVE");
            for (String status : allStatuses) {
                Map<String, Object> statusMap = new HashMap<>();
                statusMap.put("name", status);
                statusMap.put("count", statusCount.getOrDefault(status, 0L));
                statusDistribution.add(statusMap);
            }
            stats.put("statusDistribution", statusDistribution);

            // Monthly Conventions
            Map<String, Long> monthlyConventions = new HashMap<>();
            LocalDate sixMonthsAgo = LocalDate.now().minusMonths(6);

            List<Convention> recentConventions = accessibleConventions.stream()
                    .filter(c -> c.getCreatedAt() != null &&
                            c.getCreatedAt().toLocalDate().isAfter(sixMonthsAgo))
                    .collect(Collectors.toList());

            for (int i = 0; i < 6; i++) {
                YearMonth month = YearMonth.now().minusMonths(i);
                long count = recentConventions.stream()
                        .filter(c -> YearMonth.from(c.getCreatedAt().toLocalDate()).equals(month))
                        .count();
                monthlyConventions.put(month.format(DateTimeFormatter.ofPattern("MMM yyyy")), count);
            }
            stats.put("monthlyConventions", monthlyConventions);

            // Amount by Status
            Map<String, BigDecimal> amountByStatus = new HashMap<>();
            for (Convention convention : accessibleConventions) {
                String etat = convention.getEtat() != null ? convention.getEtat() : "NO_STATUS";
                BigDecimal montant = convention.getMontantTotal() != null ? convention.getMontantTotal() : BigDecimal.ZERO;
                amountByStatus.merge(etat, montant, BigDecimal::add);
            }
            stats.put("amountByStatus", amountByStatus);

            // Top Structures by Convention Count
            List<Map<String, Object>> topStructures = accessibleConventions.stream()
                    .filter(c -> c.getStructureInterne() != null)
                    .collect(Collectors.groupingBy(
                            Convention::getStructureInterne,
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

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", stats);
            response.put("userRole", getHighestRole(currentUser));

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error fetching convention detailed stats: ", e);
            return ResponseEntity.badRequest().body(createErrorResponse("Failed to fetch convention statistics"));
        }
    }

    @GetMapping("/factures/detailed")
    public ResponseEntity<?> getFactureDetailedStats() {
        try {
            User currentUser = userContextService.getCurrentUser();
            List<Facture> accessibleFactures = getAccessibleFactures(currentUser);

            Map<String, Object> stats = new HashMap<>();

            // Payment Status Distribution
            List<Map<String, Object>> paymentStatus = new ArrayList<>();

            long paidCount = accessibleFactures.stream()
                    .filter(f -> "PAYE".equals(f.getStatutPaiement()))
                    .count();
            BigDecimal paidAmount = accessibleFactures.stream()
                    .filter(f -> "PAYE".equals(f.getStatutPaiement()))
                    .map(f -> f.getMontantTTC() != null ? f.getMontantTTC() : BigDecimal.ZERO)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            long unpaidCount = accessibleFactures.stream()
                    .filter(f -> "NON_PAYE".equals(f.getStatutPaiement()) && !f.isEnRetard())
                    .count();
            BigDecimal unpaidAmount = accessibleFactures.stream()
                    .filter(f -> "NON_PAYE".equals(f.getStatutPaiement()) && !f.isEnRetard())
                    .map(f -> f.getMontantTTC() != null ? f.getMontantTTC() : BigDecimal.ZERO)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            long overdueCount = accessibleFactures.stream()
                    .filter(f -> f.isEnRetard() || "EN_RETARD".equals(f.getStatutPaiement()))
                    .count();
            BigDecimal overdueAmount = accessibleFactures.stream()
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
            Map<String, Map<String, BigDecimal>> monthlyAmounts = new HashMap<>();
            LocalDate startDate = LocalDate.now().with(TemporalAdjusters.firstDayOfYear());

            List<Facture> yearlyFactures = accessibleFactures.stream()
                    .filter(f -> f.getDateFacturation() != null &&
                            f.getDateFacturation().isAfter(startDate))
                    .collect(Collectors.toList());

            for (int i = 1; i <= 12; i++) {
                YearMonth month = YearMonth.of(LocalDate.now().getYear(), i);
                List<Facture> monthFactures = yearlyFactures.stream()
                        .filter(f -> YearMonth.from(f.getDateFacturation()).equals(month))
                        .collect(Collectors.toList());

                Map<String, BigDecimal> monthStats = new HashMap<>();
                monthStats.put("total", monthFactures.stream()
                        .map(f -> f.getMontantTTC() != null ? f.getMontantTTC() : BigDecimal.ZERO)
                        .reduce(BigDecimal.ZERO, BigDecimal::add));
                monthStats.put("paid", monthFactures.stream()
                        .filter(f -> "PAYE".equals(f.getStatutPaiement()))
                        .map(f -> f.getMontantTTC() != null ? f.getMontantTTC() : BigDecimal.ZERO)
                        .reduce(BigDecimal.ZERO, BigDecimal::add));

                monthlyAmounts.put(month.format(DateTimeFormatter.ofPattern("MMM")), monthStats);
            }
            stats.put("monthlyAmounts", monthlyAmounts);

            // Overdue Invoices Details
            List<Facture> overdueInvoices = accessibleFactures.stream()
                    .filter(f -> f.isEnRetard() || "EN_RETARD".equals(f.getStatutPaiement()))
                    .collect(Collectors.toList());

            List<Map<String, Object>> overdueDetails = overdueInvoices.stream()
                    .sorted(Comparator.comparing(Facture::getDateEcheance))
                    .limit(10)
                    .map(f -> {
                        Map<String, Object> detail = new HashMap<>();
                        detail.put("numero", f.getNumeroFacture());
                        detail.put("convention", f.getConvention() != null ? f.getConvention().getReferenceConvention() : "N/A");
                        detail.put("montant", f.getMontantTTC());
                        detail.put("dateEcheance", f.getDateEcheance());
                        detail.put("joursRetard",
                                java.time.temporal.ChronoUnit.DAYS.between(f.getDateEcheance(), LocalDate.now()));
                        return detail;
                    })
                    .collect(Collectors.toList());
            stats.put("overdueDetails", overdueDetails);

            // Top Convention by Invoice Amount
            List<Map<String, Object>> topConventionAmounts = accessibleFactures.stream()
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
                        return conv;
                    })
                    .collect(Collectors.toList());
            stats.put("topConventionAmounts", topConventionAmounts);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", stats);
            response.put("userRole", getHighestRole(currentUser));

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error fetching facture detailed stats: ", e);
            return ResponseEntity.badRequest().body(createErrorResponse("Failed to fetch invoice statistics"));
        }
    }

    @GetMapping("/users/detailed")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> getUserDetailedStats() {
        try {
            Map<String, Object> stats = new HashMap<>();

            // Role Distribution
            Map<String, Long> roleDistribution = userRepository.findAll().stream()
                    .flatMap(user -> user.getRoles().stream())
                    .collect(Collectors.groupingBy(
                            role -> role.getName().name(),
                            Collectors.counting()
                    ));
            stats.put("roleDistribution", roleDistribution);

            // User Activity
            Map<String, Long> userActivity = new HashMap<>();
            userActivity.put("total", userRepository.count());
            userActivity.put("active", userRepository.findAll().stream()
                    .filter(user -> user.getEnabled() != null && user.getEnabled())
                    .count());
            userActivity.put("locked", userRepository.findAll().stream()
                    .filter(user -> (user.getLockedByAdmin() != null && user.getLockedByAdmin()) ||
                            (user.getAccountLockedUntil() != null &&
                                    user.getAccountLockedUntil().isAfter(LocalDateTime.now())))
                    .count());
            userActivity.put("inactive", userRepository.findAll().stream()
                    .filter(user -> user.getLastLogin() != null &&
                            user.getLastLogin().isBefore(LocalDateTime.now().minusMonths(3)))
                    .count());
            stats.put("userActivity", userActivity);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", stats);

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error fetching user detailed stats: ", e);
            return ResponseEntity.badRequest().body(createErrorResponse("Failed to fetch user statistics"));
        }
    }

    @GetMapping("/nomenclatures/detailed")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> getNomenclatureDetailedStats() {
        try {
            Map<String, Object> stats = new HashMap<>();

            // Structures by Type
            Map<String, Long> structuresByType = structureRepository.findAll().stream()
                    .filter(s -> s.getTypeStructure() != null)
                    .collect(Collectors.groupingBy(
                            Structure::getTypeStructure,
                            Collectors.counting()
                    ));
            stats.put("structuresByType", structuresByType);

            // Nomenclature Counts
            Map<String, Long> nomenclatureCounts = new HashMap<>();
            nomenclatureCounts.put("applications", applicationRepository.count());
            nomenclatureCounts.put("zones", zoneGeographiqueRepository.count());
            nomenclatureCounts.put("structures", structureRepository.count());
            stats.put("nomenclatureCounts", nomenclatureCounts);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", stats);

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error fetching nomenclature detailed stats: ", e);
            return ResponseEntity.badRequest().body(createErrorResponse("Failed to fetch nomenclature statistics"));
        }
    }

    @GetMapping("/financial/detailed")
    public ResponseEntity<?> getFinancialDetailedStats() {
        try {
            User currentUser = userContextService.getCurrentUser();
            List<Facture> accessibleFactures = getAccessibleFactures(currentUser);
            List<Convention> accessibleConventions = getAccessibleConventions(currentUser);

            Map<String, Object> stats = new HashMap<>();

            // Total Revenue
            BigDecimal totalRevenue = accessibleFactures.stream()
                    .filter(f -> "PAYE".equals(f.getStatutPaiement()))
                    .map(f -> f.getMontantTTC() != null ? f.getMontantTTC() : BigDecimal.ZERO)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            stats.put("totalRevenue", totalRevenue);

            // Pending Payments
            BigDecimal pendingPayments = accessibleFactures.stream()
                    .filter(f -> "NON_PAYE".equals(f.getStatutPaiement()) || "EN_RETARD".equals(f.getStatutPaiement()))
                    .map(f -> f.getMontantTTC() != null ? f.getMontantTTC() : BigDecimal.ZERO)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            stats.put("pendingPayments", pendingPayments);

            // Overdue Amount
            BigDecimal overdueAmount = accessibleFactures.stream()
                    .filter(f -> f.isEnRetard() || "EN_RETARD".equals(f.getStatutPaiement()))
                    .map(f -> f.getMontantTTC() != null ? f.getMontantTTC() : BigDecimal.ZERO)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            stats.put("overdueAmount", overdueAmount);

            // Total Contract Value
            BigDecimal totalContractValue = accessibleConventions.stream()
                    .map(c -> c.getMontantTotal() != null ? c.getMontantTotal() : BigDecimal.ZERO)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            stats.put("totalContractValue", totalContractValue);

            // Revenue by Month (Current Year)
            Map<String, BigDecimal> revenueByMonth = new HashMap<>();
            LocalDate startOfYear = LocalDate.now().with(TemporalAdjusters.firstDayOfYear());

            List<Facture> yearlyFactures = accessibleFactures.stream()
                    .filter(f -> "PAYE".equals(f.getStatutPaiement()) &&
                            f.getDatePaiement() != null &&
                            f.getDatePaiement().isAfter(startOfYear))
                    .collect(Collectors.toList());

            for (int i = 1; i <= 12; i++) {
                YearMonth month = YearMonth.of(LocalDate.now().getYear(), i);
                BigDecimal monthRevenue = yearlyFactures.stream()
                        .filter(f -> YearMonth.from(f.getDatePaiement()).equals(month))
                        .map(f -> f.getMontantTTC() != null ? f.getMontantTTC() : BigDecimal.ZERO)
                        .reduce(BigDecimal.ZERO, BigDecimal::add);

                revenueByMonth.put(month.format(DateTimeFormatter.ofPattern("MMM")), monthRevenue);
            }
            stats.put("revenueByMonth", revenueByMonth);

            // Top Earning Conventions
            List<Map<String, Object>> topEarningConventions = accessibleConventions.stream()
                    .filter(c -> c.getMontantTotal() != null)
                    .sorted((c1, c2) -> c2.getMontantTotal().compareTo(c1.getMontantTotal()))
                    .limit(5)
                    .map(c -> {
                        Map<String, Object> conventionInfo = new HashMap<>();
                        conventionInfo.put("reference", c.getReferenceConvention());
                        conventionInfo.put("libelle", c.getLibelle());
                        conventionInfo.put("structureInterne", c.getStructureInterne() != null ? c.getStructureInterne().getName() : "N/A");
                        conventionInfo.put("montantTotal", c.getMontantTotal());
                        conventionInfo.put("etat", c.getEtat());
                        return conventionInfo;
                    })
                    .collect(Collectors.toList());
            stats.put("topEarningConventions", topEarningConventions);

            // Payment Collection Rate
            BigDecimal totalInvoiced = accessibleFactures.stream()
                    .map(f -> f.getMontantTTC() != null ? f.getMontantTTC() : BigDecimal.ZERO)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            double collectionRate = totalInvoiced.compareTo(BigDecimal.ZERO) > 0 ?
                    totalRevenue.divide(totalInvoiced, 4, RoundingMode.HALF_UP).doubleValue() * 100 : 0;
            stats.put("collectionRate", collectionRate);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", stats);
            response.put("userRole", getHighestRole(currentUser));

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error fetching financial detailed stats: ", e);
            return ResponseEntity.badRequest().body(createErrorResponse("Failed to fetch financial statistics"));
        }
    }

    @GetMapping("/projects/detailed")
    public ResponseEntity<?> getProjectDetailedStats() {
        try {
            User currentUser = userContextService.getCurrentUser();
            List<Project> accessibleProjects = getAccessibleProjects(currentUser);

            Map<String, Object> stats = new HashMap<>();

            // Status Distribution
            List<Map<String, Object>> statusDistribution = new ArrayList<>();
            Map<String, Long> statusCount = accessibleProjects.stream()
                    .collect(Collectors.groupingBy(Project::getStatus, Collectors.counting()));

            List<String> allStatuses = Arrays.asList("PLANIFIE", "EN_COURS", "TERMINE", "SUSPENDU", "ANNULE");
            for (String status : allStatuses) {
                Map<String, Object> statusMap = new HashMap<>();
                statusMap.put("name", status);
                statusMap.put("count", statusCount.getOrDefault(status, 0L));
                statusDistribution.add(statusMap);
            }
            stats.put("statusDistribution", statusDistribution);

            // Monthly Projects Created
            Map<String, Long> monthlyProjects = new HashMap<>();
            LocalDate sixMonthsAgo = LocalDate.now().minusMonths(6);

            List<Project> recentProjects = accessibleProjects.stream()
                    .filter(p -> p.getCreatedAt() != null &&
                            p.getCreatedAt().toLocalDate().isAfter(sixMonthsAgo))
                    .collect(Collectors.toList());

            for (int i = 0; i < 6; i++) {
                YearMonth month = YearMonth.now().minusMonths(i);
                long count = recentProjects.stream()
                        .filter(p -> YearMonth.from(p.getCreatedAt().toLocalDate()).equals(month))
                        .count();
                monthlyProjects.put(month.format(DateTimeFormatter.ofPattern("MMM yyyy")), count);
            }
            stats.put("monthlyProjects", monthlyProjects);

            // Projects by Application
            Map<String, Long> projectsByApplication = accessibleProjects.stream()
                    .filter(p -> p.getApplication() != null)
                    .collect(Collectors.groupingBy(
                            p -> p.getApplication().getName(),
                            Collectors.counting()
                    ));
            stats.put("projectsByApplication", projectsByApplication);

            // Unassigned Projects
            long unassignedProjects = accessibleProjects.stream()
                    .filter(p -> p.getChefDeProjet() == null)
                    .count();
            stats.put("unassignedProjects", unassignedProjects);

            // Top Applications by Project Count
            List<Map<String, Object>> topApplications = accessibleProjects.stream()
                    .filter(p -> p.getApplication() != null)
                    .collect(Collectors.groupingBy(
                            Project::getApplication,
                            Collectors.counting()
                    ))
                    .entrySet().stream()
                    .sorted((e1, e2) -> Long.compare(e2.getValue(), e1.getValue()))
                    .limit(5)
                    .map(entry -> {
                        Map<String, Object> app = new HashMap<>();
                        app.put("application", entry.getKey().getName());
                        app.put("count", entry.getValue());
                        return app;
                    })
                    .collect(Collectors.toList());
            stats.put("topApplications", topApplications);

            // Budget Statistics
            Map<String, Object> budgetStats = new HashMap<>();

            BigDecimal totalBudget = BigDecimal.valueOf(accessibleProjects.stream()
                    .filter(p -> p.getBudget() != null)
                    .mapToDouble(Project::getBudget)
                    .sum());
            budgetStats.put("totalBudget", totalBudget);

            BigDecimal avgBudget = accessibleProjects.isEmpty() ? BigDecimal.ZERO :
                    totalBudget.divide(BigDecimal.valueOf(accessibleProjects.size()), 2, RoundingMode.HALF_UP);
            budgetStats.put("averageBudget", avgBudget);

            stats.put("budgetStats", budgetStats);

            // Progress Statistics
            Map<String, Object> progressStats = new HashMap<>();
            double avgProgress = accessibleProjects.stream()
                    .mapToInt(p -> p.getProgress() != null ? p.getProgress() : 0)
                    .average()
                    .orElse(0.0);
            progressStats.put("averageProgress", Math.round(avgProgress * 100.0) / 100.0);

            long onTrackProjects = accessibleProjects.stream()
                    .filter(p -> p.getProgress() != null && p.getProgress() >= 70)
                    .count();
            progressStats.put("onTrackProjects", onTrackProjects);

            long delayedProjects = accessibleProjects.stream()
                    .filter(Project::isDelayed)
                    .count();
            progressStats.put("delayedProjects", delayedProjects);

            stats.put("progressStats", progressStats);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", stats);
            response.put("userRole", getHighestRole(currentUser));

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error fetching project detailed stats: ", e);
            return ResponseEntity.badRequest().body(createErrorResponse("Failed to fetch project statistics"));
        }
    }

    // ==================== HELPER METHODS FOR DASHBOARD ====================

    private Map<String, Object> getConventionStats(User currentUser) {
        Map<String, Object> stats = new HashMap<>();

        List<Convention> accessibleConventions = getAccessibleConventions(currentUser);

        long totalConventions = accessibleConventions.size();
        long activeConventions = accessibleConventions.stream()
                .filter(c -> "EN_COURS".equals(c.getEtat()))
                .count();
        long terminatedConventions = accessibleConventions.stream()
                .filter(c -> "TERMINE".equals(c.getEtat()))
                .count();
        long lateConventions = accessibleConventions.stream()
                .filter(c -> "EN_RETARD".equals(c.getEtat()))
                .count();
        long noStatusConventions = accessibleConventions.stream()
                .filter(c -> c.getEtat() == null)
                .count();

        stats.put("totalConventions", totalConventions);
        stats.put("activeConventions", activeConventions);
        stats.put("terminatedConventions", terminatedConventions);
        stats.put("lateConventions", lateConventions);
        stats.put("noStatusConventions", noStatusConventions);
        stats.put("conventionCompletionRate",
                totalConventions > 0 ? ((double) terminatedConventions / totalConventions) * 100 : 0);

        return stats;
    }

    private Map<String, Object> getFactureStats(User currentUser) {
        Map<String, Object> stats = new HashMap<>();

        List<Facture> accessibleFactures = getAccessibleFactures(currentUser);

        long totalFactures = accessibleFactures.size();
        long paidFactures = accessibleFactures.stream()
                .filter(f -> "PAYE".equals(f.getStatutPaiement()))
                .count();
        long unpaidFactures = accessibleFactures.stream()
                .filter(f -> "NON_PAYE".equals(f.getStatutPaiement()))
                .count();
        long overdueFactures = accessibleFactures.stream()
                .filter(f -> f.isEnRetard() || "EN_RETARD".equals(f.getStatutPaiement()))
                .count();

        stats.put("totalFactures", totalFactures);
        stats.put("paidFactures", paidFactures);
        stats.put("unpaidFactures", unpaidFactures);
        stats.put("overdueFactures", overdueFactures);
        stats.put("paymentRate",
                totalFactures > 0 ? ((double) paidFactures / totalFactures) * 100 : 0);

        // Total amounts
        BigDecimal totalPaidAmount = accessibleFactures.stream()
                .filter(f -> "PAYE".equals(f.getStatutPaiement()))
                .map(f -> f.getMontantTTC() != null ? f.getMontantTTC() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        stats.put("totalPaidAmount", totalPaidAmount);

        BigDecimal totalUnpaidAmount = accessibleFactures.stream()
                .filter(f -> "NON_PAYE".equals(f.getStatutPaiement()))
                .map(f -> f.getMontantTTC() != null ? f.getMontantTTC() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        stats.put("totalUnpaidAmount", totalUnpaidAmount);

        return stats;
    }

    private Map<String, Object> getUserStats() {
        Map<String, Object> stats = new HashMap<>();

        List<User> nonAdminUsers = userRepository.findAll().stream()
                .filter(user -> user.getRoles().stream()
                        .noneMatch(role -> role.getName().name().equals("ROLE_ADMIN")))
                .collect(Collectors.toList());

        long totalUsers = nonAdminUsers.size();
        long activeUsers = nonAdminUsers.stream()
                .filter(user -> user.getEnabled() != null && user.getEnabled())
                .count();

        long chefDeProjetCount = nonAdminUsers.stream()
                .filter(user -> user.getRoles().stream()
                        .anyMatch(role -> role.getName().name().equals("ROLE_CHEF_PROJET")))
                .count();

        long commercialMetierCount = nonAdminUsers.stream()
                .filter(user -> user.getRoles().stream()
                        .anyMatch(role -> role.getName().name().equals("ROLE_COMMERCIAL_METIER")))
                .count();

        long decideurCount = nonAdminUsers.stream()
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

        stats.put("totalStructures", structureRepository.count());
        stats.put("totalZones", zoneGeographiqueRepository.count());
        stats.put("totalApplications", applicationRepository.count());

        return stats;
    }

    private Map<String, Object> getFinancialStats(User currentUser) {
        Map<String, Object> stats = new HashMap<>();

        List<Facture> accessibleFactures = getAccessibleFactures(currentUser);
        List<Convention> accessibleConventions = getAccessibleConventions(currentUser);
        List<Project> accessibleProjects = getAccessibleProjects(currentUser);

        BigDecimal totalRevenue = accessibleFactures.stream()
                .filter(f -> "PAYE".equals(f.getStatutPaiement()))
                .map(f -> f.getMontantTTC() != null ? f.getMontantTTC() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal pendingRevenue = accessibleFactures.stream()
                .filter(f -> "NON_PAYE".equals(f.getStatutPaiement()) || "EN_RETARD".equals(f.getStatutPaiement()))
                .map(f -> f.getMontantTTC() != null ? f.getMontantTTC() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalContractValue = accessibleConventions.stream()
                .map(c -> c.getMontantTotal() != null ? c.getMontantTotal() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalProjectBudget = BigDecimal.valueOf(accessibleProjects.stream()
                .filter(p -> p.getBudget() != null)
                .mapToDouble(Project::getBudget)
                .sum());

        stats.put("totalRevenue", totalRevenue);
        stats.put("pendingRevenue", pendingRevenue);
        stats.put("totalContractValue", totalContractValue);
        stats.put("totalProjectBudget", totalProjectBudget);

        return stats;
    }

    private Map<String, Object> getProjectStats(User currentUser) {
        Map<String, Object> stats = new HashMap<>();

        List<Project> accessibleProjects = getAccessibleProjects(currentUser);

        long totalProjects = accessibleProjects.size();
        long activeProjects = accessibleProjects.stream()
                .filter(p -> "EN_COURS".equals(p.getStatus()))
                .count();
        long plannedProjects = accessibleProjects.stream()
                .filter(p -> "PLANIFIE".equals(p.getStatus()))
                .count();
        long completedProjects = accessibleProjects.stream()
                .filter(p -> "TERMINE".equals(p.getStatus()))
                .count();
        long delayedProjects = accessibleProjects.stream()
                .filter(Project::isDelayed)
                .count();

        double avgProgress = accessibleProjects.stream()
                .mapToInt(p -> p.getProgress() != null ? p.getProgress() : 0)
                .average()
                .orElse(0.0);

        stats.put("totalProjects", totalProjects);
        stats.put("activeProjects", activeProjects);
        stats.put("plannedProjects", plannedProjects);
        stats.put("completedProjects", completedProjects);
        stats.put("delayedProjects", delayedProjects);
        stats.put("averageProgress", Math.round(avgProgress * 100.0) / 100.0);
        stats.put("projectCompletionRate",
                totalProjects > 0 ? ((double) completedProjects / totalProjects) * 100 : 0);

        return stats;
    }

    private Map<String, Object> getRecentActivity(User currentUser) {
        Map<String, Object> activity = new HashMap<>();

        List<Convention> accessibleConventions = getAccessibleConventions(currentUser);
        List<Facture> accessibleFactures = getAccessibleFactures(currentUser);
        List<Project> accessibleProjects = getAccessibleProjects(currentUser);

        // Recent Conventions
        List<Map<String, Object>> recentConventions = accessibleConventions.stream()
                .sorted(Comparator.comparing(Convention::getCreatedAt).reversed())
                .limit(5)
                .map(c -> {
                    Map<String, Object> conv = new HashMap<>();
                    conv.put("reference", c.getReferenceConvention());
                    conv.put("libelle", c.getLibelle());
                    conv.put("structureInterne", c.getStructureInterne() != null ? c.getStructureInterne().getName() : "N/A");
                    conv.put("etat", c.getEtat() != null ? c.getEtat() : "NO_STATUS");
                    conv.put("createdAt", c.getCreatedAt());
                    return conv;
                })
                .collect(Collectors.toList());

        // Recent Invoices
        List<Map<String, Object>> recentInvoices = accessibleFactures.stream()
                .sorted(Comparator.comparing(Facture::getCreatedAt).reversed())
                .limit(5)
                .map(f -> {
                    Map<String, Object> inv = new HashMap<>();
                    inv.put("numero", f.getNumeroFacture());
                    inv.put("convention", f.getConvention() != null ? f.getConvention().getReferenceConvention() : "N/A");
                    inv.put("montant", f.getMontantTTC());
                    inv.put("statut", f.getStatutPaiement());
                    inv.put("dateEcheance", f.getDateEcheance());
                    inv.put("createdAt", f.getCreatedAt());
                    return inv;
                })
                .collect(Collectors.toList());

        // Recent Projects
        List<Map<String, Object>> recentProjects = accessibleProjects.stream()
                .sorted(Comparator.comparing(Project::getCreatedAt).reversed())
                .limit(5)
                .map(p -> {
                    Map<String, Object> project = new HashMap<>();
                    project.put("code", p.getCode());
                    project.put("name", p.getName());
                    project.put("clientName", p.getClientName());
                    project.put("chefDeProjet", p.getChefProjetName());
                    project.put("status", p.getStatus());
                    project.put("progress", p.getProgress());
                    project.put("createdAt", p.getCreatedAt());
                    return project;
                })
                .collect(Collectors.toList());

        activity.put("recentConventions", recentConventions);
        activity.put("recentInvoices", recentInvoices);
        activity.put("recentProjects", recentProjects);

        return activity;
    }

    private Map<String, Object> getMonthlyTrends(User currentUser) {
        Map<String, Object> trends = new HashMap<>();

        List<Convention> accessibleConventions = getAccessibleConventions(currentUser);
        List<Facture> accessibleFactures = getAccessibleFactures(currentUser);
        List<Project> accessibleProjects = getAccessibleProjects(currentUser);

        // Convention trends by month
        Map<String, Long> conventionTrends = new HashMap<>();
        for (int i = 0; i < 6; i++) {
            YearMonth month = YearMonth.now().minusMonths(i);
            long count = accessibleConventions.stream()
                    .filter(c -> c.getCreatedAt() != null &&
                            YearMonth.from(c.getCreatedAt().toLocalDate()).equals(month))
                    .count();
            conventionTrends.put(month.format(DateTimeFormatter.ofPattern("MMM")), count);
        }

        // Revenue trends by month
        Map<String, BigDecimal> revenueTrends = new HashMap<>();
        for (int i = 0; i < 6; i++) {
            YearMonth month = YearMonth.now().minusMonths(i);
            BigDecimal revenue = accessibleFactures.stream()
                    .filter(f -> "PAYE".equals(f.getStatutPaiement()) &&
                            f.getDatePaiement() != null &&
                            YearMonth.from(f.getDatePaiement()).equals(month))
                    .map(f -> f.getMontantTTC() != null ? f.getMontantTTC() : BigDecimal.ZERO)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            revenueTrends.put(month.format(DateTimeFormatter.ofPattern("MMM")), revenue);
        }

        trends.put("conventionTrends", conventionTrends);
        trends.put("revenueTrends", revenueTrends);

        return trends;
    }

    @GetMapping("/summary")
    public ResponseEntity<?> getSummaryStats() {
        try {
            User currentUser = userContextService.getCurrentUser();
            String userRole = getHighestRole(currentUser);

            Map<String, Object> summary = new HashMap<>();

            // Quick Stats
            summary.put("totalConventions", getAccessibleConventions(currentUser).size());
            summary.put("totalFactures", getAccessibleFactures(currentUser).size());
            summary.put("totalProjects", getAccessibleProjects(currentUser).size());

            // Today's Stats
            LocalDate today = LocalDate.now();

            long conventionsCreatedToday = getAccessibleConventions(currentUser).stream()
                    .filter(c -> c.getCreatedAt() != null &&
                            c.getCreatedAt().toLocalDate().equals(today))
                    .count();

            long facturesCreatedToday = getAccessibleFactures(currentUser).stream()
                    .filter(f -> f.getCreatedAt() != null &&
                            f.getCreatedAt().toLocalDate().equals(today))
                    .count();

            long facturesDueToday = getAccessibleFactures(currentUser).stream()
                    .filter(f -> f.getDateEcheance() != null &&
                            f.getDateEcheance().equals(today))
                    .count();

            long overdueFacturesToday = getAccessibleFactures(currentUser).stream()
                    .filter(f -> f.isEnRetard() || "EN_RETARD".equals(f.getStatutPaiement()))
                    .count();

            summary.put("conventionsToday", conventionsCreatedToday);
            summary.put("facturesToday", facturesCreatedToday);
            summary.put("dueToday", facturesDueToday);
            summary.put("overdueToday", overdueFacturesToday);

            // Financial summary for today
            BigDecimal todayRevenue = getAccessibleFactures(currentUser).stream()
                    .filter(f -> "PAYE".equals(f.getStatutPaiement()) &&
                            f.getDatePaiement() != null &&
                            f.getDatePaiement().equals(today))
                    .map(f -> f.getMontantTTC() != null ? f.getMontantTTC() : BigDecimal.ZERO)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            summary.put("todayRevenue", todayRevenue);

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

    @GetMapping("/overdue/alert")
    public ResponseEntity<?> getOverdueAlerts() {
        try {
            User currentUser = userContextService.getCurrentUser();
            String userRole = getHighestRole(currentUser);

            List<Map<String, Object>> alerts = new ArrayList<>();

            // Overdue invoices
            List<Facture> overdueInvoices = getAccessibleFactures(currentUser).stream()
                    .filter(f -> f.isEnRetard() || "EN_RETARD".equals(f.getStatutPaiement()))
                    .collect(Collectors.toList());

            for (Facture invoice : overdueInvoices) {
                Map<String, Object> alert = new HashMap<>();
                alert.put("type", "INVOICE_OVERDUE");
                alert.put("message", String.format("Facture %s en retard de %d jours",
                        invoice.getNumeroFacture(),
                        java.time.temporal.ChronoUnit.DAYS.between(invoice.getDateEcheance(), LocalDate.now())));
                alert.put("convention", invoice.getConvention() != null ? invoice.getConvention().getReferenceConvention() : "N/A");
                alert.put("project", invoice.getConvention() != null && invoice.getConvention().getProject() != null ?
                        invoice.getConvention().getProject().getName() : "N/A");
                alert.put("amount", invoice.getMontantTTC());
                alert.put("dueDate", invoice.getDateEcheance());
                alert.put("priority", "HIGH");
                alerts.add(alert);
            }

            // Delayed projects
            List<Project> delayedProjects = getAccessibleProjects(currentUser).stream()
                    .filter(Project::isDelayed)
                    .collect(Collectors.toList());

            for (Project project : delayedProjects) {
                Map<String, Object> alert = new HashMap<>();
                alert.put("type", "PROJECT_DELAYED");
                alert.put("message", String.format("Projet %s est en retard (progrès: %d%%)",
                        project.getName(), project.getProgress()));
                alert.put("project", project.getName());
                alert.put("code", project.getCode());
                alert.put("progress", project.getProgress());
                alert.put("endDate", project.getDateFin());
                alert.put("priority", "HIGH");
                alerts.add(alert);
            }

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

    private Map<String, Object> createErrorResponse(String message) {
        Map<String, Object> response = new HashMap<>();
        response.put("success", false);
        response.put("message", message);
        return response;
    }
}