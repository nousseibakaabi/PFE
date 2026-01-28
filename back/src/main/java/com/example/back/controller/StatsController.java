package com.example.back.controller;

import com.example.back.entity.*;
import com.example.back.repository.*;
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

    // ==================== DASHBOARD OVERALL STATS ====================

    @GetMapping("/dashboard")
    public ResponseEntity<?> getDashboardStats() {
        try {
            Map<String, Object> stats = new HashMap<>();

            // Convention Stats
            stats.putAll(getConventionStats());

            // Invoice Stats
            stats.putAll(getFactureStats());

            // User Stats
            stats.putAll(getUserStats());

            // Nomenclature Stats
            stats.putAll(getNomenclatureStats());

            // Financial Stats
            stats.putAll(getFinancialStats());

            // Recent Activity
            stats.putAll(getRecentActivity());

            // Monthly Trends
            stats.putAll(getMonthlyTrends());

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", stats);

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
            Map<String, Object> stats = new HashMap<>();

            // Status Distribution
            List<Map<String, Object>> statusDistribution = new ArrayList<>();
            List<String> etats = Arrays.asList(null, "EN_COURS", "TERMINE", "RESILIE", "EN_RETARD", "ARCHIVE");

            for (String etat : etats) {
                long count;
                if (etat == null) {
                    count = conventionRepository.findAll().stream()
                            .filter(c -> c.getEtat() == null)
                            .count();
                } else {
                    count = conventionRepository.findByEtat(etat).size();
                }
                Map<String, Object> status = new HashMap<>();
                status.put("name", etat != null ? etat : "NO_STATUS");
                status.put("count", count);
                statusDistribution.add(status);
            }
            stats.put("statusDistribution", statusDistribution);

            // Monthly Conventions
            Map<String, Long> monthlyConventions = new HashMap<>();
            LocalDate sixMonthsAgo = LocalDate.now().minusMonths(6);

            List<Convention> recentConventions = conventionRepository.findAll().stream()
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

            // By Structure Type (using interne structure)
            Map<String, Long> byStructureType = conventionRepository.findAll().stream()
                    .filter(c -> c.getStructureInterne() != null && c.getStructureInterne().getTypeStructure() != null)
                    .collect(Collectors.groupingBy(
                            c -> c.getStructureInterne().getTypeStructure(),
                            Collectors.counting()
                    ));
            stats.put("byStructureType", byStructureType);

            // Amount by Status
            Map<String, BigDecimal> amountByStatus = new HashMap<>();
            for (Convention convention : conventionRepository.findAll()) {
                String etat = convention.getEtat() != null ? convention.getEtat() : "NO_STATUS";
                BigDecimal montant = convention.getMontantTotal() != null ? convention.getMontantTotal() : BigDecimal.ZERO;

                amountByStatus.merge(etat, montant, BigDecimal::add);
            }
            stats.put("amountByStatus", amountByStatus);

            // Top Structures (interne) by Convention Count
            List<Map<String, Object>> topStructures = conventionRepository.findAll().stream()
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

            // By Zone
            Map<String, Long> byZone = conventionRepository.findAll().stream()
                    .filter(c -> c.getZone() != null)
                    .collect(Collectors.groupingBy(
                            c -> c.getZone().getName(),
                            Collectors.counting()
                    ));
            stats.put("byZone", byZone);

            // By Application
            Map<String, Long> byApplication = conventionRepository.findAll().stream()
                    .filter(c -> c.getApplication() != null)
                    .collect(Collectors.groupingBy(
                            c -> c.getApplication().getName(),
                            Collectors.counting()
                    ));
            stats.put("byApplication", byApplication);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", stats);

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error fetching convention detailed stats: ", e);
            return ResponseEntity.badRequest().body(createErrorResponse("Failed to fetch convention statistics"));
        }
    }

    @GetMapping("/factures/detailed")
    public ResponseEntity<?> getFactureDetailedStats() {
        try {
            Map<String, Object> stats = new HashMap<>();

            // Payment Status Distribution
            List<Map<String, Object>> paymentStatus = new ArrayList<>();
            List<String> statuses = Arrays.asList("PAYE", "NON_PAYE", "EN_RETARD");

            for (String status : statuses) {
                List<Facture> factures;
                if ("EN_RETARD".equals(status)) {
                    factures = factureRepository.findFacturesEnRetard(LocalDate.now());
                } else {
                    factures = factureRepository.findByStatutPaiement(status);
                }

                Map<String, Object> stat = new HashMap<>();
                stat.put("status", status);
                stat.put("count", factures.size());
                stat.put("amount", factures.stream()
                        .map(f -> f.getMontantTTC() != null ? f.getMontantTTC() : BigDecimal.ZERO)
                        .reduce(BigDecimal.ZERO, BigDecimal::add));
                paymentStatus.add(stat);
            }
            stats.put("paymentStatus", paymentStatus);

            // Monthly Invoice Amounts
            Map<String, Map<String, BigDecimal>> monthlyAmounts = new HashMap<>();
            LocalDate startDate = LocalDate.now().with(TemporalAdjusters.firstDayOfYear());

            List<Facture> yearlyFactures = factureRepository.findAll().stream()
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
            List<Facture> overdueInvoices = factureRepository.findFacturesEnRetard(LocalDate.now());
            List<Map<String, Object>> overdueDetails = overdueInvoices.stream()
                    .sorted(Comparator.comparing(Facture::getDateEcheance))
                    .limit(10)
                    .map(f -> {
                        Map<String, Object> detail = new HashMap<>();
                        detail.put("numero", f.getNumeroFacture());
                        detail.put("convention", f.getConvention().getReferenceConvention());
                        detail.put("montant", f.getMontantTTC());
                        detail.put("dateEcheance", f.getDateEcheance());
                        detail.put("joursRetard",
                                java.time.temporal.ChronoUnit.DAYS.between(f.getDateEcheance(), LocalDate.now()));
                        return detail;
                    })
                    .collect(Collectors.toList());
            stats.put("overdueDetails", overdueDetails);



            // Top Convention by Invoice Amount
            List<Map<String, Object>> topConventionAmounts = factureRepository.findAll().stream()
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

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error fetching facture detailed stats: ", e);
            return ResponseEntity.badRequest().body(createErrorResponse("Failed to fetch invoice statistics"));
        }
    }

    @GetMapping("/users/detailed")
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

            // New Users by Month
            Map<String, Long> newUsersByMonth = new HashMap<>();
            LocalDate sixMonthsAgo = LocalDate.now().minusMonths(6);

            List<User> recentUsers = userRepository.findAll().stream()
                    .filter(u -> u.getCreatedAt() != null &&
                            u.getCreatedAt().toLocalDate().isAfter(sixMonthsAgo))
                    .collect(Collectors.toList());

            for (int i = 0; i < 6; i++) {
                YearMonth month = YearMonth.now().minusMonths(i);
                long count = recentUsers.stream()
                        .filter(u -> YearMonth.from(u.getCreatedAt().toLocalDate()).equals(month))
                        .count();
                newUsersByMonth.put(month.format(DateTimeFormatter.ofPattern("MMM yyyy")), count);
            }
            stats.put("newUsersByMonth", newUsersByMonth);

            // Top Active Users
            List<Map<String, Object>> topActiveUsers = userRepository.findAll().stream()
                    .filter(user -> user.getLastLogin() != null)
                    .sorted(Comparator.comparing(User::getLastLogin).reversed())
                    .limit(5)
                    .map(user -> {
                        Map<String, Object> userInfo = new HashMap<>();
                        userInfo.put("username", user.getUsername());
                        userInfo.put("fullName", user.getFirstName() + " " + user.getLastName());
                        userInfo.put("lastLogin", user.getLastLogin());
                        userInfo.put("email", user.getEmail());
                        userInfo.put("roles", user.getRoles().stream()
                                .map(role -> role.getName().name())
                                .collect(Collectors.toList()));
                        return userInfo;
                    })
                    .collect(Collectors.toList());
            stats.put("topActiveUsers", topActiveUsers);

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

            // Conventions by Zone
            Map<String, Long> conventionsByZone = conventionRepository.findAll().stream()
                    .filter(c -> c.getZone() != null)
                    .collect(Collectors.groupingBy(
                            c -> c.getZone().getName(),
                            Collectors.counting()
                    ));
            stats.put("conventionsByZone", conventionsByZone);

            // Amount by Zone
            Map<String, BigDecimal> amountByZone = new HashMap<>();
            for (Convention convention : conventionRepository.findAll()) {
                if (convention.getZone() != null && convention.getMontantTotal() != null) {
                    String zoneName = convention.getZone().getName();
                    BigDecimal montant = convention.getMontantTotal();

                    amountByZone.merge(zoneName, montant, BigDecimal::add);
                }
            }
            stats.put("amountByZone", amountByZone);

            // Structures with Most Conventions
            List<Map<String, Object>> topStructuresWithConventions = structureRepository.findAll().stream()
                    .map(structure -> {
                        long conventionCount = conventionRepository.findAll().stream()
                                .filter(c -> (c.getStructureInterne() != null && c.getStructureInterne().getId().equals(structure.getId())) ||
                                        (c.getStructureExterne() != null && c.getStructureExterne().getId().equals(structure.getId())))
                                .count();
                        Map<String, Object> structInfo = new HashMap<>();
                        structInfo.put("structure", structure.getName());
                        structInfo.put("type", structure.getTypeStructure());
                        structInfo.put("conventionCount", conventionCount);
                        return structInfo;
                    })
                    .filter(s -> (Long) s.get("conventionCount") > 0)
                    .sorted((s1, s2) -> Long.compare(
                            (Long) s2.get("conventionCount"),
                            (Long) s1.get("conventionCount")))
                    .limit(10)
                    .collect(Collectors.toList());
            stats.put("topStructuresWithConventions", topStructuresWithConventions);

            // Nomenclature Counts
            Map<String, Long> nomenclatureCounts = new HashMap<>();
            nomenclatureCounts.put("applications", applicationRepository.count());
            nomenclatureCounts.put("zones", zoneGeographiqueRepository.count());
            nomenclatureCounts.put("structures", structureRepository.count());
            stats.put("nomenclatureCounts", nomenclatureCounts);

            // Usage statistics
            Map<String, Object> usageStats = new HashMap<>();
            usageStats.put("structuresUsedInConventions", structureRepository.findAll().stream()
                    .filter(s -> conventionRepository.findAll().stream()
                            .anyMatch(c -> (c.getStructureInterne() != null && c.getStructureInterne().getId().equals(s.getId())) ||
                                    (c.getStructureExterne() != null && c.getStructureExterne().getId().equals(s.getId()))))
                    .count());
            usageStats.put("zonesUsedInConventions", zoneGeographiqueRepository.findAll().stream()
                    .filter(z -> conventionRepository.findAll().stream()
                            .anyMatch(c -> c.getZone() != null && c.getZone().getId().equals(z.getId())))
                    .count());
            usageStats.put("applicationsUsedInConventions", applicationRepository.findAll().stream()
                    .filter(a -> conventionRepository.findAll().stream()
                            .anyMatch(c -> c.getApplication() != null && c.getApplication().getId().equals(a.getId())))
                    .count());
            stats.put("usageStats", usageStats);

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
            Map<String, Object> stats = new HashMap<>();

            // Total Revenue
            BigDecimal totalRevenue = factureRepository.findAll().stream()
                    .filter(f -> "PAYE".equals(f.getStatutPaiement()))
                    .map(f -> f.getMontantTTC() != null ? f.getMontantTTC() : BigDecimal.ZERO)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            stats.put("totalRevenue", totalRevenue);

            // Pending Payments
            BigDecimal pendingPayments = factureRepository.findAll().stream()
                    .filter(f -> "NON_PAYE".equals(f.getStatutPaiement()) || "EN_RETARD".equals(f.getStatutPaiement()))
                    .map(f -> f.getMontantTTC() != null ? f.getMontantTTC() : BigDecimal.ZERO)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            stats.put("pendingPayments", pendingPayments);

            // Overdue Amount
            BigDecimal overdueAmount = factureRepository.findFacturesEnRetard(LocalDate.now()).stream()
                    .map(f -> f.getMontantTTC() != null ? f.getMontantTTC() : BigDecimal.ZERO)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            stats.put("overdueAmount", overdueAmount);

            // Total Contract Value
            BigDecimal totalContractValue = conventionRepository.findAll().stream()
                    .map(c -> c.getMontantTotal() != null ? c.getMontantTotal() : BigDecimal.ZERO)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            stats.put("totalContractValue", totalContractValue);

            // Revenue by Month (Current Year)
            Map<String, BigDecimal> revenueByMonth = new HashMap<>();
            LocalDate startOfYear = LocalDate.now().with(TemporalAdjusters.firstDayOfYear());

            List<Facture> yearlyFactures = factureRepository.findAll().stream()
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
            List<Map<String, Object>> topEarningConventions = conventionRepository.findAll().stream()
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
            BigDecimal totalInvoiced = factureRepository.findAll().stream()
                    .map(f -> f.getMontantTTC() != null ? f.getMontantTTC() : BigDecimal.ZERO)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            double collectionRate = totalInvoiced.compareTo(BigDecimal.ZERO) > 0 ?
                    totalRevenue.divide(totalInvoiced, 4, RoundingMode.HALF_UP).doubleValue() * 100 : 0;
            stats.put("collectionRate", collectionRate);

            // Average Invoice Amount
            long invoiceCount = factureRepository.count();
            BigDecimal averageInvoiceAmount = invoiceCount > 0 ?
                    totalInvoiced.divide(BigDecimal.valueOf(invoiceCount), 2, RoundingMode.HALF_UP) : BigDecimal.ZERO;
            stats.put("averageInvoiceAmount", averageInvoiceAmount);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", stats);

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error fetching financial detailed stats: ", e);
            return ResponseEntity.badRequest().body(createErrorResponse("Failed to fetch financial statistics"));
        }
    }

    // ==================== HELPER METHODS ====================

    private Map<String, Object> getConventionStats() {
        Map<String, Object> stats = new HashMap<>();

        long totalConventions = conventionRepository.count();
        List<Convention> enCoursConventions = conventionRepository.findByEtat("EN_COURS");
        long activeConventions = enCoursConventions.size();

        // Find expired conventions (dateFin < today and not TERMINE or ARCHIVE)
        List<Convention> expiredConventions = conventionRepository.findAll().stream()
                .filter(c -> c.getDateFin() != null &&
                        c.getDateFin().isBefore(LocalDate.now()) &&
                        !"TERMINE".equals(c.getEtat()) &&
                        !"ARCHIVE".equals(c.getEtat()) &&
                        !Boolean.TRUE.equals(c.getArchived()))
                .collect(Collectors.toList());

        long expiredConventionsCount = expiredConventions.size();
        long terminatedConventions = conventionRepository.findByEtat("TERMINE").size();

        stats.put("totalConventions", totalConventions);
        stats.put("activeConventions", activeConventions);
        stats.put("expiredConventions", expiredConventionsCount);
        stats.put("terminatedConventions", terminatedConventions);
        stats.put("conventionCompletionRate",
                totalConventions > 0 ? ((double) terminatedConventions / totalConventions) * 100 : 0);

        // Late conventions
        long lateConventions = conventionRepository.findAll().stream()
                .filter(c -> "EN_RETARD".equals(c.getEtat()))
                .count();
        stats.put("lateConventions", lateConventions);

        // Conventions with no status
        long noStatusConventions = conventionRepository.findAll().stream()
                .filter(c -> c.getEtat() == null)
                .count();
        stats.put("noStatusConventions", noStatusConventions);

        return stats;
    }

    private Map<String, Object> getFactureStats() {
        Map<String, Object> stats = new HashMap<>();

        long totalFactures = factureRepository.count();
        long paidFactures = factureRepository.findByStatutPaiement("PAYE").size();
        long unpaidFactures = factureRepository.findByStatutPaiement("NON_PAYE").size();
        long overdueFactures = factureRepository.findFacturesEnRetard(LocalDate.now()).size();

        stats.put("totalFactures", totalFactures);
        stats.put("paidFactures", paidFactures);
        stats.put("unpaidFactures", unpaidFactures);
        stats.put("overdueFactures", overdueFactures);
        stats.put("paymentRate",
                totalFactures > 0 ? ((double) paidFactures / totalFactures) * 100 : 0);

        // Total amounts
        BigDecimal totalPaidAmount = factureRepository.findByStatutPaiement("PAYE").stream()
                .map(f -> f.getMontantTTC() != null ? f.getMontantTTC() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        stats.put("totalPaidAmount", totalPaidAmount);

        BigDecimal totalUnpaidAmount = factureRepository.findByStatutPaiement("NON_PAYE").stream()
                .map(f -> f.getMontantTTC() != null ? f.getMontantTTC() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        stats.put("totalUnpaidAmount", totalUnpaidAmount);

        return stats;
    }

    private Map<String, Object> getUserStats() {
        Map<String, Object> stats = new HashMap<>();

        long totalUsers = userRepository.count();
        long activeUsers = userRepository.findAll().stream()
                .filter(user -> user.getEnabled() != null && user.getEnabled())
                .count();
        long lockedUsers = userRepository.findAll().stream()
                .filter(user -> (user.getLockedByAdmin() != null && user.getLockedByAdmin()) ||
                        (user.getAccountLockedUntil() != null &&
                                user.getAccountLockedUntil().isAfter(LocalDateTime.now())))
                .count();

        stats.put("totalUsers", totalUsers);
        stats.put("activeUsers", activeUsers);
        stats.put("lockedUsers", lockedUsers);
        stats.put("userActivityRate",
                totalUsers > 0 ? ((double) activeUsers / totalUsers) * 100 : 0);

        return stats;
    }

    private Map<String, Object> getNomenclatureStats() {
        Map<String, Object> stats = new HashMap<>();

        stats.put("totalStructures", structureRepository.count());
        stats.put("totalZones", zoneGeographiqueRepository.count());
        stats.put("totalApplications", applicationRepository.count());

        return stats;
    }

    private Map<String, Object> getFinancialStats() {
        Map<String, Object> stats = new HashMap<>();

        BigDecimal totalRevenue = factureRepository.findAll().stream()
                .filter(f -> "PAYE".equals(f.getStatutPaiement()))
                .map(f -> f.getMontantTTC() != null ? f.getMontantTTC() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal pendingRevenue = factureRepository.findAll().stream()
                .filter(f -> "NON_PAYE".equals(f.getStatutPaiement()) || "EN_RETARD".equals(f.getStatutPaiement()))
                .map(f -> f.getMontantTTC() != null ? f.getMontantTTC() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalContractValue = conventionRepository.findAll().stream()
                .map(c -> c.getMontantTotal() != null ? c.getMontantTotal() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        stats.put("totalRevenue", totalRevenue);
        stats.put("pendingRevenue", pendingRevenue);
        stats.put("totalContractValue", totalContractValue);
        stats.put("revenueGrowth", calculateRevenueGrowth()); // Percentage growth

        return stats;
    }

    private double calculateRevenueGrowth() {
        LocalDate currentMonthStart = LocalDate.now().with(TemporalAdjusters.firstDayOfMonth());
        LocalDate lastMonthStart = currentMonthStart.minusMonths(1);
        LocalDate twoMonthsAgoStart = lastMonthStart.minusMonths(1);

        BigDecimal currentMonthRevenue = factureRepository.findAll().stream()
                .filter(f -> "PAYE".equals(f.getStatutPaiement()) &&
                        f.getDatePaiement() != null &&
                        f.getDatePaiement().isAfter(currentMonthStart))
                .map(f -> f.getMontantTTC() != null ? f.getMontantTTC() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal lastMonthRevenue = factureRepository.findAll().stream()
                .filter(f -> "PAYE".equals(f.getStatutPaiement()) &&
                        f.getDatePaiement() != null &&
                        f.getDatePaiement().isAfter(lastMonthStart) &&
                        f.getDatePaiement().isBefore(currentMonthStart))
                .map(f -> f.getMontantTTC() != null ? f.getMontantTTC() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        if (lastMonthRevenue.compareTo(BigDecimal.ZERO) > 0) {
            return currentMonthRevenue.subtract(lastMonthRevenue)
                    .divide(lastMonthRevenue, 4, RoundingMode.HALF_UP)
                    .doubleValue() * 100;
        }
        return currentMonthRevenue.compareTo(BigDecimal.ZERO) > 0 ? 100.0 : 0.0;
    }

    private Map<String, Object> getRecentActivity() {
        Map<String, Object> activity = new HashMap<>();

        // Recent Conventions
        List<Map<String, Object>> recentConventions = conventionRepository.findAll().stream()
                .sorted(Comparator.comparing(Convention::getCreatedAt).reversed())
                .limit(5)
                .map(c -> {
                    Map<String, Object> conv = new HashMap<>();
                    conv.put("reference", c.getReferenceConvention());
                    conv.put("libelle", c.getLibelle());
                    conv.put("structureInterne", c.getStructureInterne() != null ? c.getStructureInterne().getName() : "N/A");
                    conv.put("dateSignature", c.getDateSignature());
                    conv.put("etat", c.getEtat() != null ? c.getEtat() : "NO_STATUS");
                    conv.put("createdAt", c.getCreatedAt());
                    return conv;
                })
                .collect(Collectors.toList());

        // Recent Invoices
        List<Map<String, Object>> recentInvoices = factureRepository.findAll().stream()
                .sorted(Comparator.comparing(Facture::getCreatedAt).reversed())
                .limit(5)
                .map(f -> {
                    Map<String, Object> inv = new HashMap<>();
                    inv.put("numero", f.getNumeroFacture());
                    inv.put("convention", f.getConvention().getReferenceConvention());
                    inv.put("montant", f.getMontantTTC());
                    inv.put("statut", f.getStatutPaiement());
                    inv.put("dateEcheance", f.getDateEcheance());
                    inv.put("createdAt", f.getCreatedAt());
                    return inv;
                })
                .collect(Collectors.toList());

        activity.put("recentConventions", recentConventions);
        activity.put("recentInvoices", recentInvoices);

        return activity;
    }

    private Map<String, Object> getMonthlyTrends() {
        Map<String, Object> trends = new HashMap<>();

        // Convention trends by month
        Map<String, Long> conventionTrends = new HashMap<>();
        for (int i = 0; i < 6; i++) {
            YearMonth month = YearMonth.now().minusMonths(i);
            long count = conventionRepository.findAll().stream()
                    .filter(c -> c.getCreatedAt() != null &&
                            YearMonth.from(c.getCreatedAt().toLocalDate()).equals(month))
                    .count();
            conventionTrends.put(month.format(DateTimeFormatter.ofPattern("MMM")), count);
        }

        // Revenue trends by month
        Map<String, BigDecimal> revenueTrends = new HashMap<>();
        for (int i = 0; i < 6; i++) {
            YearMonth month = YearMonth.now().minusMonths(i);
            BigDecimal revenue = factureRepository.findAll().stream()
                    .filter(f -> "PAYE".equals(f.getStatutPaiement()) &&
                            f.getDatePaiement() != null &&
                            YearMonth.from(f.getDatePaiement()).equals(month))
                    .map(f -> f.getMontantTTC() != null ? f.getMontantTTC() : BigDecimal.ZERO)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            revenueTrends.put(month.format(DateTimeFormatter.ofPattern("MMM")), revenue);
        }

        // Invoice count trends
        Map<String, Long> invoiceTrends = new HashMap<>();
        for (int i = 0; i < 6; i++) {
            YearMonth month = YearMonth.now().minusMonths(i);
            long count = factureRepository.findAll().stream()
                    .filter(f -> f.getCreatedAt() != null &&
                            YearMonth.from(f.getCreatedAt().toLocalDate()).equals(month))
                    .count();
            invoiceTrends.put(month.format(DateTimeFormatter.ofPattern("MMM")), count);
        }

        trends.put("conventionTrends", conventionTrends);
        trends.put("revenueTrends", revenueTrends);
        trends.put("invoiceTrends", invoiceTrends);

        return trends;
    }

    @GetMapping("/summary")
    public ResponseEntity<?> getSummaryStats() {
        try {
            Map<String, Object> summary = new HashMap<>();

            // Quick Stats
            summary.put("totalConventions", conventionRepository.count());
            summary.put("totalFactures", factureRepository.count());
            summary.put("totalUsers", userRepository.count());
            summary.put("totalStructures", structureRepository.count());

            // Today's Stats
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

            long overdueFacturesToday = factureRepository.findFacturesEnRetard(today).size();

            summary.put("conventionsToday", conventionsCreatedToday);
            summary.put("facturesToday", facturesCreatedToday);
            summary.put("dueToday", facturesDueToday);
            summary.put("overdueToday", overdueFacturesToday);

            // Financial summary for today
            BigDecimal todayRevenue = factureRepository.findAll().stream()
                    .filter(f -> "PAYE".equals(f.getStatutPaiement()) &&
                            f.getDatePaiement() != null &&
                            f.getDatePaiement().equals(today))
                    .map(f -> f.getMontantTTC() != null ? f.getMontantTTC() : BigDecimal.ZERO)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            summary.put("todayRevenue", todayRevenue);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", summary);

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error fetching summary stats: ", e);
            return ResponseEntity.badRequest().body(createErrorResponse("Failed to fetch summary statistics"));
        }
    }

    @GetMapping("/overdue/alert")
    public ResponseEntity<?> getOverdueAlerts() {
        try {
            List<Map<String, Object>> alerts = new ArrayList<>();

            // Overdue invoices
            List<Facture> overdueInvoices = factureRepository.findFacturesEnRetard(LocalDate.now());
            for (Facture invoice : overdueInvoices) {
                Map<String, Object> alert = new HashMap<>();
                alert.put("type", "INVOICE_OVERDUE");
                alert.put("message", String.format("Facture %s en retard de %d jours",
                        invoice.getNumeroFacture(),
                        java.time.temporal.ChronoUnit.DAYS.between(invoice.getDateEcheance(), LocalDate.now())));
                alert.put("convention", invoice.getConvention().getReferenceConvention());
                alert.put("amount", invoice.getMontantTTC());
                alert.put("dueDate", invoice.getDateEcheance());
                alert.put("priority", "HIGH");
                alerts.add(alert);
            }

            // Expired conventions
            List<Convention> expiredConventions = conventionRepository.findAll().stream()
                    .filter(c -> c.getDateFin() != null &&
                            c.getDateFin().isBefore(LocalDate.now()) &&
                            !"TERMINE".equals(c.getEtat()) &&
                            !"ARCHIVE".equals(c.getEtat()) &&
                            !Boolean.TRUE.equals(c.getArchived()))
                    .collect(Collectors.toList());

            for (Convention convention : expiredConventions) {
                Map<String, Object> alert = new HashMap<>();
                alert.put("type", "CONVENTION_EXPIRED");
                alert.put("message", String.format("Convention %s expirée depuis %d jours",
                        convention.getReferenceConvention(),
                        java.time.temporal.ChronoUnit.DAYS.between(convention.getDateFin(), LocalDate.now())));
                alert.put("convention", convention.getReferenceConvention());
                alert.put("endDate", convention.getDateFin());
                alert.put("priority", "MEDIUM");
                alerts.add(alert);
            }

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", alerts);
            response.put("count", alerts.size());

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