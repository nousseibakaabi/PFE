package com.example.back.service;

import com.example.back.entity.*;
import com.example.back.payload.request.ConventionRequest;
import com.example.back.payload.request.RenewalRequestDTO;
import com.example.back.repository.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Slf4j
public class ConventionService {

    @Autowired
    private EntitySyncService entitySyncService;

    @Autowired
    private ConventionRepository conventionRepository;

    @Autowired
    private FactureRepository factureRepository;

    @Autowired
    private ApplicationRepository applicationRepository;

    @Autowired
    private StructureRepository structureRepository;

    @Autowired
    private ApplicationService applicationService;

    @Autowired
    private HistoryService historyService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private NotificationService notificationService;

    @Autowired
    private MailService mailService;


    @Autowired
    private RequestService requestService;


    @Autowired
    private OldConventionRepository oldConventionRepository;

    @Autowired
    private OldFactureRepository oldFactureRepository;

    @Autowired
    private WorkloadService workloadService;



    // ============= FINANCIAL CALCULATION METHODS =============

    /**
     * Calculate TTC from HT and TVA
     */
    public BigDecimal calculateTTC(BigDecimal montantHT, BigDecimal tva) {
        if (montantHT == null) {
            return BigDecimal.ZERO;
        }
        if (tva == null) {
            tva = BigDecimal.valueOf(19.00);
        }
        BigDecimal tvaAmount = montantHT.multiply(tva).divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
        return montantHT.add(tvaAmount);
    }

    /**
     * Calculate HT from TTC and TVA
     */

    private BigDecimal calculateHT(BigDecimal montantTTC, BigDecimal tva) {
        if (montantTTC == null || tva == null) {
            return BigDecimal.ZERO;
        }
        BigDecimal divisor = BigDecimal.ONE.add(tva.divide(BigDecimal.valueOf(100), 4, RoundingMode.HALF_UP));
        return montantTTC.divide(divisor, 2, RoundingMode.HALF_UP);
    }
    /**
     * Determine number of users based on application min/max and user selection
     */
    public Long determineNbUsers(Long selectedUsers, Application application) {
        if (application == null) {
            return selectedUsers != null ? selectedUsers : 0;
        }

        Long minUserLong = application.getMinUser();
        Long maxUserLong = application.getMaxUser();

        Long minUser = Long.valueOf(minUserLong != null ? minUserLong.intValue() : null);
        Long maxUser = Long.valueOf(maxUserLong != null ? maxUserLong.intValue() : null);

        log.info("Determining nb users - Selected: {}, Min: {}, Max: {}", selectedUsers, minUser, maxUser);

        // If no selected users and min exists, use min
        if (selectedUsers == null || selectedUsers <= 0) {
            Long result = minUser != null ? minUser : (maxUser != null ? maxUser : 1);
            log.info("No valid selection, using: {}", result);
            return result;
        }

        // Apply min constraint - if selected is below min, use min
        if (minUser != null && selectedUsers < minUser) {
            log.info("Selected {} below minimum {}, using minimum", selectedUsers, minUser);
            return minUser;
        }

        // If above max, keep selected value (no upper cap as per requirements)
        if (maxUser != null && selectedUsers > maxUser) {
            log.info("Selected {} above maximum {}, using selected", selectedUsers, maxUser);
        }

        return selectedUsers;
    }

    /**
     * Update convention financials and regenerate invoices if needed
     */
    @Transactional
    public Convention updateConventionFinancials(Convention convention,
                                                 BigDecimal montantHT,
                                                 BigDecimal tva,
                                                 Long nbUsers,
                                                 Application application,
                                                 boolean regenerateInvoices) {

        // Store old values for logging
        BigDecimal oldMontantHT = convention.getMontantHT();
        BigDecimal oldTva = convention.getTva();
        BigDecimal oldMontantTTC = convention.getMontantTTC();
        Long oldNbUsers = convention.getNbUsers();

        // Set the new values
        convention.setMontantHT(montantHT);
        convention.setTva(tva != null ? tva : BigDecimal.valueOf(19.00));

        // Determine nb users based on application limits
        if (application != null) {
            Long determinedNbUsers = determineNbUsers(nbUsers, application);
            convention.setNbUsers(determinedNbUsers);
        } else {
            convention.setNbUsers(nbUsers);
        }

        // Calculate TTC
        BigDecimal ttc = calculateTTC(convention.getMontantHT(), convention.getTva());
        convention.setMontantTTC(ttc);

        log.info("Updated convention financials - HT: {} → {}, TVA: {} → {}, TTC: {} → {}, Users: {} → {}",
                oldMontantHT, montantHT, oldTva, convention.getTva(), oldMontantTTC, ttc, oldNbUsers, convention.getNbUsers());

        return convention;
    }

    // ============= INVOICE GENERATION METHODS =============
// Update this method in ConventionService.java

    private LocalDate calculateInvoiceDate(LocalDate startDate, int invoiceIndex, String periodicite) {
        log.info("Calculating invoice date for index {} with periodicity {}", invoiceIndex, periodicite);

        switch (periodicite.toUpperCase()) {
            case "MENSUEL":
                return startDate.plusMonths(invoiceIndex);

            case "TRIMESTRIEL":
                return startDate.plusMonths(invoiceIndex * 3);

            case "SEMESTRIEL":
                return startDate.plusMonths(invoiceIndex * 6);

            case "ANNUEL":
                return startDate.plusYears(invoiceIndex);

            default:
                log.warn("Unknown periodicity for date calculation: {}, defaulting to monthly", periodicite);
                return startDate.plusMonths(invoiceIndex);
        }
    }
    /**
     * Generate sequential invoice number that aligns with existing numbering
     */
    private String generateSequentialInvoiceNumber(Convention convention, int sequence) {
        // Format: FACT-YYYY-CONV-YYYY-SEQ
        return String.format("FACT-%s-%s-%03d",
                LocalDate.now().getYear(),
                convention.getReferenceConvention(),
                sequence);
    }

/**
 * Generate invoices for a NEW convention
 * - Creates ALL invoices from scratch
 * - Used ONLY for new conventions (creation)
 */
@Transactional
public void generateInvoicesForConvention(Convention convention) {
    log.info("========== GENERATING INVOICES FOR NEW CONVENTION ==========");
    log.info("Convention ID: {}", convention.getId());
    log.info("Reference: {}", convention.getReferenceConvention());
    log.info("Periodicite: '{}'", convention.getPeriodicite());
    log.info("Date Debut: {}", convention.getDateDebut());
    log.info("Date Fin: {}", convention.getDateFin());
    log.info("Montant TTC: {}", convention.getMontantTTC());

    // Check if any invoices already exist (they shouldn't for new convention)
    List<Facture> existingInvoices = factureRepository.findByConventionId(convention.getId());
    if (!existingInvoices.isEmpty()) {
        log.warn("Found {} existing invoices for new convention. This shouldn't happen.", existingInvoices.size());
        // Delete all existing invoices to start fresh
        for (Facture invoice : existingInvoices) {
            factureRepository.delete(invoice);
            log.info("Deleted existing invoice: {}", invoice.getNumeroFacture());
        }
        factureRepository.flush();
    }

    // Validate required fields
    if (convention.getDateDebut() == null || convention.getDateFin() == null ||
            convention.getMontantTTC() == null || convention.getPeriodicite() == null) {
        log.error("Missing required fields for invoice generation");
        return;
    }

    // Calculate total periods
    int totalPeriods = calculateTotalPeriods(convention);
    log.info("Total periods calculated: {} for periodicity: {}", totalPeriods, convention.getPeriodicite());

    // Calculate amount per period
    BigDecimal amountPerPeriod = convention.getMontantTTC()
            .divide(BigDecimal.valueOf(totalPeriods), 2, RoundingMode.HALF_UP);
    log.info("Amount per period: {} TND", amountPerPeriod);

    BigDecimal sum = BigDecimal.ZERO;


    User currentUser = getCurrentUser();
    String conventionName = convention.getReferenceConvention();

    // Generate all invoices from scratch
    for (int i = 0; i < totalPeriods; i++) {
        int sequenceNumber = i + 1;

        // Generate invoice number
        String invoiceNumber = generateFormattedInvoiceNumber(convention, sequenceNumber);

        LocalDate invoiceDate = calculateInvoiceDate(convention.getDateDebut(), i, convention.getPeriodicite());

        // Calculate amount (last invoice takes remainder to avoid rounding issues)
        BigDecimal amount;
        if (i == totalPeriods - 1) {
            amount = convention.getMontantTTC().subtract(sum);
            log.info("  Last invoice takes remainder: {} = {} - {}", amount, convention.getMontantTTC(), sum);
        } else {
            amount = amountPerPeriod;
            sum = sum.add(amount);
        }

        Facture facture = new Facture();
        facture.setNumeroFacture(invoiceNumber);
        facture.setConvention(convention);
        facture.setDateFacturation(invoiceDate);
        facture.setDateEcheance(calculateDueDate(invoiceDate, convention.getPeriodicite()));
        facture.setMontantHT(calculateHT(amount, convention.getTva()));
        facture.setTva(convention.getTva());
        facture.setMontantTTC(amount);
        facture.setStatutPaiement("NON_PAYE");
        facture.setNotes(String.format("Facture %d/%d pour la convention %s",
                sequenceNumber, totalPeriods, convention.getReferenceConvention()));

        Facture savedFacture = factureRepository.save(facture);
        log.info("ADDED new invoice {}: {} TND, date {}", 
                savedFacture.getNumeroFacture(), amount, invoiceDate);


        try {
            historyService.logFactureAutoCreate(savedFacture, conventionName);
        } catch (Exception e) {
            log.error("Failed to log auto-creation for invoice {}: {}", savedFacture.getNumeroFacture(), e.getMessage());
        }

        log.info("ADDED new invoice {}: {} TND, date {}",
                savedFacture.getNumeroFacture(), amount, invoiceDate);

    }




    log.info("========== INVOICE GENERATION COMPLETE ==========");
    checkNotificationsForAllInvoices(convention);
}



    /**
     * Generate a unique invoice number
     */
    private String generateUniqueInvoiceNumber(Convention convention, int sequence) {
        String baseNumber = String.format("FACT-%d-%s-%03d",
                LocalDate.now().getYear(),
                convention.getReferenceConvention(),
                sequence);

        // Check if this number already exists
        boolean exists = factureRepository.existsByNumeroFacture(baseNumber);
        if (!exists) {
            return baseNumber;
        }

        // If it exists, add a timestamp to make it unique
        String timestamp = String.valueOf(System.currentTimeMillis()).substring(7);
        return String.format("FACT-%d-%s-%03d-%s",
                LocalDate.now().getYear(),
                convention.getReferenceConvention(),
                sequence,
                timestamp);
    }

    private void checkNotificationsForAllInvoices(Convention convention) {
        log.info("Checking notifications for all invoices of convention {}", convention.getReferenceConvention());

        LocalDate today = LocalDate.now();

        // Fetch fresh factures from database to avoid detached entity issues
        List<Facture> factures = factureRepository.findByConventionId(convention.getId());

        for (Facture facture : factures) {
            if (!"PAYE".equals(facture.getStatutPaiement())) {
                try {
                    checkAndCreateNotificationForFacture(facture, today);
                } catch (Exception e) {
                    log.error("Failed to check notification for facture {}: {}",
                            facture.getNumeroFacture(), e.getMessage());
                }
            }
        }
    }

    private void checkAndCreateNotificationForFacture(Facture facture, LocalDate today) {
        if (facture.getDateEcheance() == null) return;

        long daysUntilDue = ChronoUnit.DAYS.between(today, facture.getDateEcheance());

        // Créer des notifications pour les jours J-5 à J0 et pour les retards jusqu'à 5 jours
        if (daysUntilDue <= 5 && daysUntilDue >= -5) {
            log.info("Facture {} due in {} days - Creating notification",
                    facture.getNumeroFacture(), daysUntilDue);

            try {
                notificationService.createFactureDueNotification(facture, (int) daysUntilDue);
            } catch (Exception e) {
                log.error("Failed to create notification for facture {}: {}",
                        facture.getNumeroFacture(), e.getMessage());
            }
        }
    }
    /**
     * Calculate total number of periods based on periodicity
     */
    private int calculateTotalPeriods(Convention convention) {
        LocalDate start = convention.getDateDebut();
        LocalDate end = convention.getDateFin();
        String periodicite = convention.getPeriodicite();

        log.info("Calculating total periods for {} from {} to {}", periodicite, start, end);

        switch (periodicite.toUpperCase()) {
            case "MENSUEL":
                // Number of months between dates
                int months = (int) ChronoUnit.MONTHS.between(start, end.plusDays(1));
                log.info("MENSUEL: {} months", months);
                return months;

            case "TRIMESTRIEL":
                // Number of quarters (3-month periods)
                months = (int) ChronoUnit.MONTHS.between(start, end.plusDays(1));
                int quarters = (int) Math.ceil(months / 3.0);
                log.info("TRIMESTRIEL: {} months = {} quarters", months, quarters);
                return quarters;

            case "SEMESTRIEL":
                // Number of semesters (6-month periods)
                months = (int) ChronoUnit.MONTHS.between(start, end.plusDays(1));
                int semesters = (int) Math.ceil(months / 6.0);
                log.info("SEMESTRIEL: {} months = {} semesters", months, semesters);
                return semesters;

            case "ANNUEL":
                // Number of years
                int years = (int) ChronoUnit.YEARS.between(start, end.plusDays(1));
                log.info("ANNUEL: {} years", years);
                return years;

            default:
                log.warn("Unknown periodicity: {}, defaulting to 1", periodicite);
                return 1;
        }
    }
    

    /**
     * Calculate total amount paid from paid invoices
     */
    private BigDecimal calculateTotalPaidAmount(List<Facture> paidInvoices) {
        return paidInvoices.stream()
                .map(Facture::getMontantTTC)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }


/**
 * Regenerate invoices when convention is UPDATED
 * - PRESERVES paid invoices (never modify them)
 * - UPDATES existing unpaid invoices with new amounts/dates
 * - ADDS new invoices if more periods needed
 * - DELETES excess unpaid invoices if fewer periods needed - **GUARANTEED TO DELETE FROM THE END**
 */


    /**
     * Regenerate invoices when convention is UPDATED
     * - PRESERVES paid invoices (never modify them)
     * - UPDATES existing unpaid invoices with new amounts/dates
     * - ADDS new invoices if more periods needed
     * - DELETES excess unpaid invoices if fewer periods needed
     */
    @Transactional
    public void regenerateInvoicesForConvention(Long conventionId) {
        Optional<Convention> conventionOpt = conventionRepository.findById(conventionId);
        if (conventionOpt.isEmpty()) {
            log.warn("Convention not found for invoice regeneration: {}", conventionId);
            return;
        }

        Convention convention = conventionOpt.get();
        User currentUser = getCurrentUser();
        String conventionName = convention.getReferenceConvention();

        // Only regenerate if convention is not terminated or archived
        if ("TERMINE".equals(convention.getEtat()) || "ARCHIVE".equals(convention.getEtat())) {
            log.info("Convention {} is {} - skipping invoice regeneration",
                    convention.getReferenceConvention(), convention.getEtat());
            return;
        }

        log.info("========== REGENERATING INVOICES FOR CONVENTION UPDATE ==========");
        log.info("Convention ID: {}", convention.getId());
        log.info("Reference: {}", convention.getReferenceConvention());

        // Get all existing invoices sorted by date (OLDEST FIRST)
        List<Facture> allInvoices = factureRepository.findByConventionIdOrderByDateFacturationAsc(convention.getId());

        // Separate paid and unpaid invoices (preserving order)
        List<Facture> paidInvoices = new ArrayList<>();
        List<Facture> unpaidInvoices = new ArrayList<>();

        for (Facture invoice : allInvoices) {
            if ("PAYE".equals(invoice.getStatutPaiement())) {
                paidInvoices.add(invoice);
            } else {
                unpaidInvoices.add(invoice);
            }
        }

        log.info("Found {} total invoices - {} paid, {} unpaid",
                allInvoices.size(), paidInvoices.size(), unpaidInvoices.size());

        // Calculate total paid amount
        BigDecimal totalPaidAmount = paidInvoices.stream()
                .map(Facture::getMontantTTC)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        log.info("Total paid amount: {} TND", totalPaidAmount);

        // Calculate new total periods needed
        int newTotalPeriods = calculateTotalPeriods(convention);
        log.info("New total periods needed: {}", newTotalPeriods);

        // Calculate remaining amount to distribute
        BigDecimal newTotalTTC = convention.getMontantTTC();
        BigDecimal remainingAmount = newTotalTTC.subtract(totalPaidAmount);
        log.info("Remaining amount to distribute: {} TND", remainingAmount);

        // Calculate how many unpaid invoices we need
        int unpaidNeeded = newTotalPeriods - paidInvoices.size();
        log.info("Paid invoices: {}, Unpaid invoices needed: {}", paidInvoices.size(), unpaidNeeded);

        if (unpaidNeeded < 0) {
            log.error("More paid invoices ({}) than total periods ({}) - cannot regenerate",
                    paidInvoices.size(), newTotalPeriods);
            return;
        }

        // ============= LOG MODIFICATIONS TO EXISTING UNPAID INVOICES =============
        List<Facture> modifiedInvoices = new ArrayList<>();

        // ============= STEP 1: DELETE EXCESS UNPAID INVOICES (FROM THE END) =============
        if (unpaidInvoices.size() > unpaidNeeded) {
            int excessCount = unpaidInvoices.size() - unpaidNeeded;
            log.info("Deleting {} excess unpaid invoices", excessCount);

            for (int i = unpaidInvoices.size() - 1; i >= unpaidNeeded; i--) {
                Facture toDelete = unpaidInvoices.get(i);
                log.info("  DELETING invoice: {}", toDelete.getNumeroFacture());

                // LOG HISTORY: Deletion
                try {
                    historyService.logFactureDelete(toDelete, currentUser);
                } catch (Exception e) {
                    log.error("Failed to log deletion for {}: {}", toDelete.getNumeroFacture(), e.getMessage());
                }

                if (convention.getFactures() != null) {
                    convention.getFactures().remove(toDelete);
                }
                factureRepository.delete(toDelete);
            }
            factureRepository.flush();

            // Refresh the list
            List<Facture> newUnpaidList = new ArrayList<>();
            for (int i = 0; i < unpaidNeeded; i++) {
                newUnpaidList.add(unpaidInvoices.get(i));
            }
            unpaidInvoices = newUnpaidList;
        }

        // ============= STEP 2: UPDATE EXISTING UNPAID INVOICES =============
        // Calculate amounts first
        BigDecimal amountPerUnpaid = BigDecimal.ZERO;
        if (unpaidNeeded > 0 && remainingAmount.compareTo(BigDecimal.ZERO) > 0) {
            amountPerUnpaid = remainingAmount.divide(BigDecimal.valueOf(unpaidNeeded), 2, RoundingMode.HALF_UP);
        }

        BigDecimal sum = BigDecimal.ZERO;

        for (int i = 0; i < unpaidInvoices.size(); i++) {
            Facture invoice = unpaidInvoices.get(i);
            int globalIndex = paidInvoices.size() + i;
            int sequenceNumber = globalIndex + 1;

            // Store old values for logging
            BigDecimal oldMontant = invoice.getMontantTTC();
            LocalDate oldEcheance = invoice.getDateEcheance();
            LocalDate oldFacturation = invoice.getDateFacturation();
            String oldNumero = invoice.getNumeroFacture();

            // Calculate new values
            LocalDate newInvoiceDate = calculateInvoiceDate(convention.getDateDebut(), globalIndex, convention.getPeriodicite());
            LocalDate newDueDate = calculateDueDate(newInvoiceDate, convention.getPeriodicite());

            BigDecimal amount;
            if (i == unpaidInvoices.size() - 1) {
                amount = remainingAmount.subtract(sum);
            } else {
                amount = amountPerUnpaid;
                sum = sum.add(amount);
            }

            String newInvoiceNumber = generateFormattedInvoiceNumber(convention, sequenceNumber);

            // Update invoice
            invoice.setNumeroFacture(newInvoiceNumber);
            invoice.setDateFacturation(newInvoiceDate);
            invoice.setDateEcheance(newDueDate);
            invoice.setMontantTTC(amount);
            invoice.setMontantHT(calculateHT(amount, convention.getTva()));
            invoice.setTva(convention.getTva());
            invoice.setNotes(String.format("Facture %d/%d pour la convention %s",
                    sequenceNumber, newTotalPeriods, convention.getReferenceConvention()));

            Facture updated = factureRepository.save(invoice);
            modifiedInvoices.add(updated);

            // LOG HISTORY: Modification if anything changed
            boolean hasChanges = !oldMontant.equals(amount) ||
                    !oldEcheance.equals(newDueDate) ||
                    !oldFacturation.equals(newInvoiceDate) ||
                    !oldNumero.equals(newInvoiceNumber);

            if (hasChanges) {
                try {
                    // Create a clone of old invoice for history
                    Facture oldClone = new Facture();
                    oldClone.setId(invoice.getId());
                    oldClone.setNumeroFacture(oldNumero);
                    oldClone.setDateFacturation(oldFacturation);
                    oldClone.setDateEcheance(oldEcheance);
                    oldClone.setMontantTTC(oldMontant);
                    oldClone.setMontantHT(oldMontant.divide(BigDecimal.ONE.add(convention.getTva().divide(BigDecimal.valueOf(100))), 2, RoundingMode.HALF_UP));
                    oldClone.setTva(convention.getTva());
                    oldClone.setStatutPaiement(invoice.getStatutPaiement());
                    oldClone.setNotes(invoice.getNotes());

                    historyService.logFactureUpdate(oldClone, updated, currentUser);
                    log.info("✅ Logged modification for invoice {}: amount {}→{}, date {}→{}",
                            oldNumero, oldMontant, amount, oldEcheance, newDueDate);
                } catch (Exception e) {
                    log.error("Failed to log modification for {}: {}", invoice.getNumeroFacture(), e.getMessage());
                }
            }

            log.info("  UPDATED invoice {}: amount {} TND, date {}",
                    invoice.getNumeroFacture(), amount, newInvoiceDate);
        }

        // ============= STEP 3: CREATE NEW UNPAID INVOICES IF NEEDED =============
        if (unpaidInvoices.size() < unpaidNeeded) {
            int missingCount = unpaidNeeded - unpaidInvoices.size();
            log.info("Creating {} new unpaid invoices", missingCount);

            for (int i = 0; i < missingCount; i++) {
                int globalIndex = paidInvoices.size() + unpaidInvoices.size() + i;
                int sequenceNumber = globalIndex + 1;

                String invoiceNumber = generateFormattedInvoiceNumber(convention, sequenceNumber);
                LocalDate invoiceDate = calculateInvoiceDate(convention.getDateDebut(), globalIndex, convention.getPeriodicite());

                // Calculate amount
                BigDecimal amount;
                int newIndex = unpaidInvoices.size() + i;
                if (newIndex == unpaidNeeded - 1) {
                    // Last invoice takes remainder
                    amount = remainingAmount.subtract(sum);
                } else {
                    amount = amountPerUnpaid;
                    sum = sum.add(amount);
                }

                Facture newFacture = new Facture();
                newFacture.setNumeroFacture(invoiceNumber);
                newFacture.setConvention(convention);
                newFacture.setDateFacturation(invoiceDate);
                newFacture.setDateEcheance(calculateDueDate(invoiceDate, convention.getPeriodicite()));
                newFacture.setMontantTTC(amount);
                newFacture.setMontantHT(calculateHT(amount, convention.getTva()));
                newFacture.setTva(convention.getTva());
                newFacture.setStatutPaiement("NON_PAYE");
                newFacture.setNotes(String.format("Facture %d/%d pour la convention %s",
                        sequenceNumber, newTotalPeriods, convention.getReferenceConvention()));

                Facture saved = factureRepository.save(newFacture);
                unpaidInvoices.add(saved);

                // LOG HISTORY: Creation
                try {
                    historyService.logFactureAutoCreate(saved, conventionName);
                    log.info("✅ Logged creation for new invoice: {}", saved.getNumeroFacture());
                } catch (Exception e) {
                    log.error("Failed to log creation for {}: {}", saved.getNumeroFacture(), e.getMessage());
                }

                log.info("  CREATED new invoice: {} at position {}", saved.getNumeroFacture(), globalIndex);
            }
        }

        // ============= FINAL VERIFICATION =============
        List<Facture> finalInvoices = factureRepository.findByConventionIdOrderByDateFacturationAsc(convention.getId());

        log.info("========== INVOICE REGENERATION COMPLETE ==========");
        log.info("Final invoice count: {} (should be {})", finalInvoices.size(), newTotalPeriods);

        // Verify amounts
        BigDecimal totalCheck = BigDecimal.ZERO;
        for (int i = 0; i < finalInvoices.size(); i++) {
            Facture f = finalInvoices.get(i);
            totalCheck = totalCheck.add(f.getMontantTTC());
            log.info("  [{}] {} - {} TND - {} - {}",
                    i+1, f.getNumeroFacture(), f.getMontantTTC(),
                    f.getStatutPaiement(), f.getDateFacturation());
        }

        log.info("Total from invoices: {} TND, Convention TTC: {} TND", totalCheck, convention.getMontantTTC());

        checkNotificationsForAllInvoices(convention);
        updateConventionStatusRealTime(conventionId);
    }

/**
 * Generate formatted invoice number like: FACT-2026-CONV-2026-005-001
 */
private String generateFormattedInvoiceNumber(Convention convention, int sequence) {
    String year = String.valueOf(LocalDate.now().getYear());
    String convRef = convention.getReferenceConvention(); // e.g., "CONV-2026-005"
    
    // Format sequence with leading zeros (001, 002, etc.)
    String seqFormatted = String.format("%03d", sequence);
    
    String invoiceNumber = String.format("FACT-%s-%s-%s", year, convRef, seqFormatted);
    
    // Check if this number already exists
    boolean exists = factureRepository.existsByNumeroFacture(invoiceNumber);
    if (exists) {
        // If it exists, add a timestamp to make it unique
        String timestamp = String.valueOf(System.currentTimeMillis()).substring(7);
        invoiceNumber = String.format("FACT-%s-%s-%s-%s", year, convRef, seqFormatted, timestamp);
        log.warn("Invoice number collision! Using: {}", invoiceNumber);
    }
    
    return invoiceNumber;
}


    private int calculateNumberOfInvoices(Convention convention) {
        LocalDate start = convention.getDateDebut();
        LocalDate end = convention.getDateFin();

        switch (convention.getPeriodicite().toUpperCase()) {
            case "MENSUEL":
                return (int) (ChronoUnit.MONTHS.between(start, end.plusDays(1)));
            case "TRIMESTRIEL":
                int months = (int) ChronoUnit.MONTHS.between(start, end.plusDays(1));
                return (int) Math.ceil(months / 3.0);
            case "SEMESTRIEL":
                months = (int) ChronoUnit.MONTHS.between(start, end.plusDays(1));
                return (int) Math.ceil(months / 6.0);
            case "ANNUEL":
                return (int) (ChronoUnit.YEARS.between(start, end.plusDays(1)));
            default:
                return 1;
        }
    }

    private LocalDate calculateDueDate(LocalDate invoiceDate, String periodicite) {
        switch (periodicite.toUpperCase()) {
            case "MENSUEL":
                return invoiceDate.plusMonths(1);
            case "TRIMESTRIEL":
                return invoiceDate.plusMonths(3);
            case "SEMESTRIEL":
                return invoiceDate.plusMonths(6);
            case "ANNUEL":
                return invoiceDate.plusYears(1);
            default:
                return invoiceDate.plusMonths(1);
        }
    }

    private LocalDate getNextPeriodDate(LocalDate currentDate, String periodicite) {
        return calculateDueDate(currentDate, periodicite);
    }

    private String generateInvoiceNumber(Convention convention, int sequence) {
        return String.format("FACT-%s-%s-%03d",
                LocalDate.now().getYear(),
                convention.getReferenceConvention(),
                sequence);
    }

    // ============= CRUD OPERATIONS WITH FINANCIALS =============

    /**
     * Create convention with financial calculations
     */
    @Transactional
    public Convention createConventionWithFinancials(ConventionRequest request, User currentUser) {
        log.info("Creating convention with financials for reference: {}", request.getReferenceConvention());

        // Fetch application
        Application application = applicationRepository.findById(request.getApplicationId())
                .orElseThrow(() -> new RuntimeException("Application not found"));

        // Fetch structures
        Structure structureResponsable = structureRepository.findById(request.getStructureResponsableId())
                .orElseThrow(() -> new RuntimeException("Structure responsable not found"));
        Structure structureBeneficiel = structureRepository.findById(request.getStructureBeneficielId())
                .orElseThrow(() -> new RuntimeException("Structure beneficiel not found"));

        // Create convention
        Convention convention = new Convention();
        convention.setReferenceConvention(request.getReferenceConvention());
        convention.setReferenceERP(request.getReferenceERP());
        convention.setLibelle(request.getLibelle());
        convention.setDateDebut(request.getDateDebut());
        convention.setDateFin(request.getDateFin());
        convention.setDateSignature(request.getDateSignature());
        convention.setPeriodicite(request.getPeriodicite());
        convention.setCreatedBy(currentUser);
        convention.setApplication(application);
        convention.setStructureResponsable(structureResponsable);
        convention.setStructureBeneficiel(structureBeneficiel);

        // Set financial data with user determination
        Long nbUsers = determineNbUsers(request.getNbUsers(), application);
        convention = updateConventionFinancials(
                convention,
                request.getMontantHT(),
                request.getTva(),
                nbUsers,
                application,
                false
        );

        // Save convention
        Convention savedConvention = conventionRepository.save(convention);
        log.info("Convention saved with ID: {}, TTC: {}, NbUsers: {}",
                savedConvention.getId(), savedConvention.getMontantTTC(), savedConvention.getNbUsers());

        // LOG HISTORY: Convention creation (AFTER saving)
        try {
            historyService.logConventionCreate(savedConvention, currentUser);
        } catch (Exception e) {
            log.error("Failed to log convention creation history: {}", e.getMessage());
            // Don't throw - we don't want history to break the main functionality
        }

        // CRITICAL: Update application dates using ApplicationService
        applicationService.updateApplicationDatesFromConvention(
                savedConvention.getApplication().getId(),
                savedConvention.getDateDebut(),
                savedConvention.getDateFin()
        );

        // Generate invoices based on TTC
        generateInvoicesForConvention(savedConvention);

        checkNotificationsForAllInvoices(savedConvention);


        return savedConvention;
    }

    /**
     * Update convention with financial calculations and regenerate invoices
     */
    @Transactional
    public Convention updateConventionWithFinancials(Long id, ConventionRequest request) {
        log.info("Updating convention with financials for ID: {}", id);

        Convention convention = conventionRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Convention not found"));

        // Store old values for comparison
        Convention oldConvention = cloneConvention(convention);
        BigDecimal oldMontantHT = convention.getMontantHT();
        BigDecimal oldMontantTTC = convention.getMontantTTC();
        Long oldNbUsers = convention.getNbUsers();
        String oldStatus = convention.getEtat();
        LocalDate oldStartDate = convention.getDateDebut();
        LocalDate oldEndDate = convention.getDateFin();
        String oldPeriodicite = convention.getPeriodicite();

        // Check if convention can be updated
        if ("TERMINE".equals(convention.getEtat())) {
            throw new RuntimeException("Cannot update a terminated convention");
        }
        if ("ARCHIVE".equals(convention.getEtat())) {
            throw new RuntimeException("Cannot update an archived convention");
        }

        boolean datesChanged = false;
        boolean periodiciteChanged = false;
        boolean financialDataChanged = false;

        // Fetch application
        Application application = null;
        if (request.getApplicationId() != null) {
            application = applicationRepository.findById(request.getApplicationId())
                    .orElseThrow(() -> new RuntimeException("Application not found"));
            convention.setApplication(application);
        }

        // Fetch structures if changed
        if (request.getStructureResponsableId() != null) {
            Structure structureResponsable = structureRepository.findById(request.getStructureResponsableId())
                    .orElseThrow(() -> new RuntimeException("Structure responsable not found"));
            convention.setStructureResponsable(structureResponsable);
        }

        if (request.getStructureBeneficielId() != null) {
            Structure structureBeneficiel = structureRepository.findById(request.getStructureBeneficielId())
                    .orElseThrow(() -> new RuntimeException("Structure beneficiel not found"));
            convention.setStructureBeneficiel(structureBeneficiel);
        }

        // Update basic fields and check if dates changed
        convention.setReferenceConvention(request.getReferenceConvention());
        convention.setReferenceERP(request.getReferenceERP());
        convention.setLibelle(request.getLibelle());

        if (!convention.getDateDebut().equals(request.getDateDebut())) {
            convention.setDateDebut(request.getDateDebut());
            datesChanged = true;
        }

        if (!Objects.equals(convention.getDateFin(), request.getDateFin())) {
            convention.setDateFin(request.getDateFin());
            datesChanged = true;
        }

        convention.setDateSignature(request.getDateSignature());

        if (!convention.getPeriodicite().equals(request.getPeriodicite())) {
            convention.setPeriodicite(request.getPeriodicite());
            periodiciteChanged = true;
        }

        // Determine nb users based on application limits
        Long nbUsers = determineNbUsers(request.getNbUsers(), convention.getApplication());

        // Check if financial data changed
        if (!compareBigDecimal(convention.getMontantHT(), request.getMontantHT()) ||
                !compareBigDecimal(convention.getTva(), request.getTva()) ||
                !Objects.equals(convention.getNbUsers(), nbUsers)) {
            financialDataChanged = true;
        }

        // Update financial data
        convention = updateConventionFinancials(
                convention,
                request.getMontantHT(),
                request.getTva(),
                nbUsers,
                convention.getApplication(),
                false
        );

        // Save convention
        Convention updatedConvention = conventionRepository.save(convention);


        // ===== SYNC: Propagate changes to related entities =====
        try {
            entitySyncService.syncConventionChanges(oldConvention, updatedConvention);
            log.info("✅ Successfully synced convention changes to related entities");
        } catch (Exception e) {
            log.error("Failed to sync convention changes: {}", e.getMessage(), e);
        }

        // LOG HISTORY: Convention update
        try {
            User currentUser = getCurrentUser();
            boolean hasImportantChanges = financialDataChanged || datesChanged || periodiciteChanged ||
                    !oldStatus.equals(updatedConvention.getEtat());

            if (hasImportantChanges) {
                historyService.logConventionUpdate(oldConvention, updatedConvention, currentUser);

                if (financialDataChanged) {
                    historyService.logConventionFinancialUpdate(updatedConvention,
                            oldMontantHT, oldMontantTTC, oldNbUsers,
                            updatedConvention.getMontantHT(), updatedConvention.getMontantTTC(), updatedConvention.getNbUsers());
                }

                if (!oldStatus.equals(updatedConvention.getEtat())) {
                    historyService.logConventionStatusChange(updatedConvention, oldStatus, updatedConvention.getEtat());
                }
            }
        } catch (Exception e) {
            log.error("Failed to log convention update history: {}", e.getMessage());
        }

        // If dates changed, update the application dates
        if (datesChanged && updatedConvention.getApplication() != null) {
            applicationService.updateApplicationDatesFromConvention(
                    updatedConvention.getApplication().getId(),
                    updatedConvention.getDateDebut(),
                    updatedConvention.getDateFin()
            );
            log.info("Updated application dates due to convention date change");
        }

        // CRITICAL: Regenerate invoices if ANY of these changed:
        // - Financial data (amount, TVA, nb users)
        // - Dates (start date, end date)
        // - Periodicity
        if (financialDataChanged || datesChanged || periodiciteChanged) {
            log.info("Changes detected - regenerating invoices. Financial: {}, Dates: {}, Periodicite: {}",
                    financialDataChanged, datesChanged, periodiciteChanged);
            regenerateInvoicesForConvention(updatedConvention.getId());
            checkNotificationsForAllInvoices(updatedConvention);
        } else {
            log.info("No changes that affect invoices detected, skipping regeneration");
        }

        return updatedConvention;
    }

    /**
     * Get current user
     */
    public User getCurrentUser() {
        String currentUsername = SecurityContextHolder.getContext().getAuthentication().getName();
        return userRepository.findByUsername(currentUsername).orElse(null);
    }

    /**
     * Check if financial data has changed
     */
    private boolean hasFinancialDataChanged(Convention convention, ConventionRequest request) {
        boolean htChanged = !compareBigDecimal(convention.getMontantHT(), request.getMontantHT());
        boolean tvaChanged = !compareBigDecimal(convention.getTva(), request.getTva());
        boolean nbUsersChanged = !Objects.equals(convention.getNbUsers(), request.getNbUsers());

        return htChanged || tvaChanged || nbUsersChanged;
    }

    private boolean hasDatesOrPeriodicityChanged(Convention convention, ConventionRequest request) {
        boolean datesChanged = !convention.getDateDebut().equals(request.getDateDebut()) ||
                !convention.getDateFin().equals(request.getDateFin());
        boolean periodiciteChanged = !convention.getPeriodicite().equals(request.getPeriodicite());

        if (datesChanged || periodiciteChanged) {
            log.info("Dates or periodicity changed - Dates: {}, Periodicite: {}",
                    datesChanged, periodiciteChanged);
            return true;
        }
        return false;
    }

    private boolean compareBigDecimal(BigDecimal bd1, BigDecimal bd2) {
        if (bd1 == null && bd2 == null) return true;
        if (bd1 == null || bd2 == null) return false;
        return bd1.compareTo(bd2) == 0;
    }

    private boolean compareInteger(Long i1, Long i2) {
        if (i1 == null && i2 == null) return true;
        if (i1 == null || i2 == null) return false;
        return i1.equals(i2);
    }

    // ============= STATUS UPDATE METHODS =============

    @Transactional
    public void updateConventionStatusRealTime(Long conventionId) {
        Optional<Convention> conventionOpt = conventionRepository.findById(conventionId);
        if (conventionOpt.isEmpty()) {
            log.warn("Convention not found for real-time status update: {}", conventionId);
            return;
        }

        Convention convention = conventionOpt.get();
        String oldStatus = convention.getEtat();
        convention.updateStatus();

        if (!oldStatus.equals(convention.getEtat())) {
            conventionRepository.save(convention);
            log.info("REAL-TIME: Convention {} status changed from {} to {}",
                    convention.getReferenceConvention(), oldStatus, convention.getEtat());

            // LOG HISTORY: Status change
            try {
                historyService.logConventionStatusChange(convention, oldStatus, convention.getEtat());
            } catch (Exception e) {
                log.error("Failed to log status change history: {}", e.getMessage());
            }
        }
    }

    @Scheduled(cron = "0 0 6 * * *")
    @Transactional
    public void checkDateTransitions() {
        log.info("Checking date transitions for conventions...");
        LocalDate today = LocalDate.now();

        List<Convention> waitingConventions = conventionRepository.findByEtat("PLANIFIE");
        for (Convention convention : waitingConventions) {
            if (!today.isBefore(convention.getDateDebut())) {
                String oldStatus = convention.getEtat();
                convention.setEtat("EN COURS");
                conventionRepository.save(convention);
                log.info("Convention {} transitioned from PLANIFIE to EN COURS",
                        convention.getReferenceConvention());

                // LOG HISTORY: Status change
                try {
                    historyService.logConventionStatusChange(convention, oldStatus, "EN COURS");
                } catch (Exception e) {
                    log.error("Failed to log status change history: {}", e.getMessage());
                }
            }
        }

        List<Convention> activeConventions = conventionRepository.findByEtat("EN COURS");
        for (Convention convention : activeConventions) {
            if (convention.getDateFin() != null && today.isAfter(convention.getDateFin())) {
                List<Facture> invoices = factureRepository.findByConventionId(convention.getId());
                boolean allInvoicesPaid = invoices.stream()
                        .allMatch(invoice -> "PAYE".equals(invoice.getStatutPaiement()));


            }
        }
    }

    @Scheduled(fixedRate = 300000)
    @Transactional
    public void checkForTerminedConventions() {
        log.debug("Checking for conventions with all invoices paid...");

        List<Convention> activeConventions = conventionRepository.findByArchivedFalse();
        for (Convention convention : activeConventions) {
            if ("TERMINE".equals(convention.getEtat()) || "ARCHIVE".equals(convention.getEtat())) {
                continue;
            }

            List<Facture> invoices = factureRepository.findByConventionId(convention.getId());
            if (!invoices.isEmpty()) {
                boolean allInvoicesPaid = invoices.stream()
                        .allMatch(invoice -> "PAYE".equals(invoice.getStatutPaiement()));

                if (allInvoicesPaid) {
                    String oldStatus = convention.getEtat();
                    convention.setEtat("TERMINE");
                    conventionRepository.save(convention);
                    log.info("Convention {} marked as TERMINE", convention.getReferenceConvention());

                    // LOG HISTORY: Status change
                    try {
                        historyService.logConventionStatusChange(convention, oldStatus, "TERMINE");
                    } catch (Exception e) {
                        log.error("Failed to log status change history: {}", e.getMessage());
                    }
                }
            }
        }
    }

    // ============= REFERENCE GENERATION =============

    public String generateSuggestedReference() {
        int currentYear = LocalDate.now().getYear();
        String prefix = "CONV-" + currentYear + "-";
        List<Integer> usedSequences = conventionRepository.findUsedSequencesByYear(prefix);

        if (usedSequences == null || usedSequences.isEmpty()) {
            return String.format("CONV-%d-%03d", currentYear, 1);
        }

        Collections.sort(usedSequences);
        int nextSequence = 1;
        for (int usedSequence : usedSequences) {
            if (usedSequence > nextSequence) {
                break;
            }
            nextSequence = usedSequence + 1;
        }

        if (nextSequence > 999) {
            nextSequence = findFirstMissingSequence(usedSequences);
        }

        return String.format("CONV-%d-%03d", currentYear, nextSequence);
    }

    private int findFirstMissingSequence(List<Integer> sequences) {
        Set<Integer> sequenceSet = new HashSet<>(sequences);
        for (int i = 1; i <= 999; i++) {
            if (!sequenceSet.contains(i)) {
                return i;
            }
        }
        return 1000;
    }

    // ============= API ENDPOINT HELPER METHODS =============

    public Map<String, Object> calculateTTCResponse(BigDecimal montantHT, BigDecimal tva) {
        BigDecimal ttc = calculateTTC(montantHT, tva);
        Map<String, Object> response = new HashMap<>();
        response.put("montantHT", montantHT);
        response.put("tva", tva != null ? tva : BigDecimal.valueOf(19.00));
        response.put("montantTTC", ttc);
        return response;
    }

    public Map<String, Object> determineNbUsersResponse(Long applicationId, Long selectedUsers) {
        Application application = applicationRepository.findById(applicationId)
                .orElseThrow(() -> new RuntimeException("Application not found"));

        Long nbUsers = determineNbUsers(selectedUsers, application);

        Map<String, Object> response = new HashMap<>();
        response.put("nbUsers", nbUsers);
        response.put("minUser", application.getMinUser());
        response.put("maxUser", application.getMaxUser());
        response.put("selectedUsers", selectedUsers);
        response.put("appliedRule", getAppliedUserRule(selectedUsers, application));

        return response;
    }

    private String getAppliedUserRule(Long selectedUsers, Application application) {
        Long minUser = application.getMinUser();
        Long maxUser = application.getMaxUser();

        if (selectedUsers == null || selectedUsers <= 0) {
            return "Aucune sélection - Utilisation du minimum";
        }
        if (minUser != null && selectedUsers < minUser) {
            return "Sélection inférieure au minimum - Forcé au minimum";
        }
        if (maxUser != null && selectedUsers > maxUser) {
            return "Sélection supérieure au maximum - Valeur sélectionnée conservée";
        }
        return "Valeur sélectionnée conservée";
    }

    @Transactional
    public void syncAllApplicationDates(Long applicationId) {
        applicationService.syncApplicationDatesWithAllConventions(applicationId);
    }



    @Transactional
    public Convention renewConvention(Long conventionId, RenewalRequestDTO renewalData, User currentUser) {
        log.info("========== RENEWING CONVENTION ==========");

        Convention convention = conventionRepository.findById(conventionId)
                .orElseThrow(() -> new RuntimeException("Convention not found"));

        if (!"TERMINE".equals(convention.getEtat())) {
            throw new RuntimeException("Only terminated conventions can be renewed");
        }

        if (convention.getArchived()) {
            throw new RuntimeException("Archived conventions cannot be renewed");
        }

        // 1. CREATE DEEP COPY of OLD state BEFORE any changes
        Convention oldState = cloneConvention(convention);

        // 2. Get and archive OLD invoices BEFORE updating
        List<Facture> oldFactures = factureRepository.findByConventionId(convention.getId());
        log.info("Found {} old factures to archive", oldFactures.size());

        // 3. UPDATE the current convention with new data
        updateConventionWithRenewalData(convention, renewalData, currentUser);

        // 4. SAVE the updated convention
        Convention updatedConvention = conventionRepository.save(convention);
        log.info("Updated convention saved with new data. New version: {}", updatedConvention.getRenewalVersion());

        // 5. FLUSH to ensure everything is saved
        conventionRepository.flush();

        // 6. Archive the OLD state (linking to NEW convention)
        archiveCurrentVersion(oldState, currentUser, updatedConvention, "Renouvellement - Ancienne version");

        // 7. Archive old factures and DELETE them from main table
        if (!oldFactures.isEmpty()) {
            archiveCurrentFactures(oldState, currentUser, updatedConvention, oldFactures);
            factureRepository.deleteByConventionId(conventionId);
            factureRepository.flush();
            log.info("Force deleted {} factures from main table", oldFactures.size());
        }

        // 8. Generate NEW invoices
        generateInvoicesForConvention(updatedConvention);

        // 9. FORCE status recalculation based on new data
        String newStatus = determineNewStatus(updatedConvention);
        updatedConvention.setEtat(newStatus);

        // 10. Save again with correct status
        updatedConvention = conventionRepository.save(updatedConvention);
        log.info("Convention renewed with status: {}", newStatus);

        // 11. Update application dates
        try {
            applicationService.updateApplicationDatesFromConvention(
                    updatedConvention.getApplication().getId(),
                    updatedConvention.getDateDebut(),
                    updatedConvention.getDateFin()
            );
            log.info("Application dates updated successfully");
        } catch (Exception e) {
            log.error("Failed to update application dates: {}", e.getMessage());
        }

        // 12. Handle application renewal
        Application application = updatedConvention.getApplication();
        if ("TERMINE".equals(application.getStatus())) {
            application.setStatus("EN_COURS");
            application.setTerminatedAt(null);
            application.setTerminatedBy(null);
            application.setTerminationReason(null);
            application.setRenewed(true);
            application.setRenewedAt(LocalDateTime.now());
            application.setRenewedBy(currentUser.getUsername());
            applicationRepository.save(application);
            log.info("Application {} renewed from TERMINE to EN_COURS", application.getCode());
            historyService.logApplicationRenewal(application, currentUser);
        }

        // 13. Handle chef de projet reassignment
        try {
            handleRenewalAssignment(application, updatedConvention, currentUser);
        } catch (Exception e) {
            log.error("Failed to handle renewal assignment: {}", e.getMessage());
        }

        // 14. Log history
        try {
            historyService.logConventionRenewal(oldState, updatedConvention, currentUser);
        } catch (Exception e) {
            log.error("Failed to log renewal history: {}", e.getMessage());
        }

        log.info("========== RENEWAL COMPLETED ==========");
        return updatedConvention;
    }





    private String determineNewStatus(Convention convention) {
        LocalDate today = LocalDate.now();

        // 1. If archived → ARCHIVE
        if (Boolean.TRUE.equals(convention.getArchived())) {
            return "ARCHIVE";
        }

        // 2. Check if all invoices are paid → TERMINE
        List<Facture> factures = factureRepository.findByConventionId(convention.getId());
        boolean allInvoicesPaid = !factures.isEmpty() && factures.stream()
                .allMatch(f -> "PAYE".equals(f.getStatutPaiement()));

        if (allInvoicesPaid) {
            return "TERMINE";
        }

        // 3. If today is before start date → PLANIFIE
        if (convention.getDateDebut() != null && today.isBefore(convention.getDateDebut())) {
            return "PLANIFIE";
        }

        // 4. Default: EN COURS
        return "EN COURS";
    }






    @Transactional
    public void archiveCurrentFactures(Convention oldConvention, User currentUser,
                                       Convention newConvention, List<Facture> oldFactures) {
        log.info("Archiving {} factures for old convention {}", oldFactures.size(),
                oldConvention.getReferenceConvention());

        // Get the old convention record we just created
        List<OldConvention> oldVersions = oldConventionRepository
                .findByCurrentConventionOrderByRenewalVersionDesc(newConvention);

        if (oldVersions.isEmpty()) {
            log.error("No old convention version found to link factures");
            return;
        }

        OldConvention latestOldConv = oldVersions.get(0);

        List<OldFacture> oldFacturesToSave = new ArrayList<>();

        for (Facture facture : oldFactures) {
            OldFacture oldFacture = new OldFacture();
            oldFacture.setOldConvention(latestOldConv);
            oldFacture.setNumeroFacture(facture.getNumeroFacture());
            oldFacture.setDateFacturation(facture.getDateFacturation());
            oldFacture.setDateEcheance(facture.getDateEcheance());
            oldFacture.setMontantHT(facture.getMontantHT());
            oldFacture.setTva(facture.getTva());
            oldFacture.setMontantTTC(facture.getMontantTTC());
            oldFacture.setStatutPaiement(facture.getStatutPaiement());
            oldFacture.setDatePaiement(facture.getDatePaiement());
            oldFacture.setReferencePaiement(facture.getReferencePaiement());
            oldFacture.setNotes(facture.getNotes());
            oldFacture.setArchivedAt(LocalDateTime.now());

            oldFacturesToSave.add(oldFacture);
        }

        // Save all old factures
        oldFactureRepository.saveAll(oldFacturesToSave);
        oldFactureRepository.flush();

        // CRITICAL: Delete the original factures from the main table
        factureRepository.deleteAll(oldFactures);
        factureRepository.flush();

        log.info("Archived and deleted {} factures successfully", oldFactures.size());
    }

 /**
     * Clone convention for history (deep copy)
     */
    private Convention cloneConvention(Convention conv) {
        Convention clone = new Convention();
        clone.setId(conv.getId());
        clone.setReferenceConvention(conv.getReferenceConvention());
        clone.setReferenceERP(conv.getReferenceERP());
        clone.setLibelle(conv.getLibelle());
        clone.setDateDebut(conv.getDateDebut());
        clone.setDateFin(conv.getDateFin());
        clone.setDateSignature(conv.getDateSignature());
        clone.setStructureResponsable(conv.getStructureResponsable());
        clone.setStructureBeneficiel(conv.getStructureBeneficiel());
        clone.setApplication(conv.getApplication());
        clone.setMontantHT(conv.getMontantHT());
        clone.setMontantTTC(conv.getMontantTTC());
        clone.setTva(conv.getTva());
        clone.setNbUsers(conv.getNbUsers());
        clone.setPeriodicite(conv.getPeriodicite());
        clone.setEtat(conv.getEtat());
        clone.setArchived(conv.getArchived());
        clone.setArchivedAt(conv.getArchivedAt());
        clone.setArchivedBy(conv.getArchivedBy());
        clone.setArchivedReason(conv.getArchivedReason());
        clone.setCreatedAt(conv.getCreatedAt());
        clone.setUpdatedAt(conv.getUpdatedAt());
        clone.setCreatedBy(conv.getCreatedBy());
        clone.setRenewalVersion(conv.getRenewalVersion());
        return clone;
    }

    /**
     * Update convention with renewal data
     */
    @Transactional
    public void updateConventionWithRenewalData(Convention convention, RenewalRequestDTO data, User currentUser) {
        log.info("Updating convention {} with new renewal data", convention.getReferenceConvention());

        // Increment renewal version
        convention.setRenewalVersion(convention.getRenewalVersion() != null ?
                convention.getRenewalVersion() + 1 : 1);

        // Update with new data
        if (data.getReferenceERP() != null) {
            convention.setReferenceERP(data.getReferenceERP());
        }

        if (data.getLibelle() != null) {
            convention.setLibelle(data.getLibelle());
        }

        if (data.getDateDebut() != null) {
            convention.setDateDebut(data.getDateDebut());
        }

        if (data.getDateFin() != null) {
            convention.setDateFin(data.getDateFin());
        }

        if (data.getDateSignature() != null) {
            convention.setDateSignature(data.getDateSignature());
        }

        if (data.getPeriodicite() != null) {
            convention.setPeriodicite(data.getPeriodicite());
        }

        if (data.getMontantHT() != null) {
            convention.setMontantHT(data.getMontantHT());
        }

        if (data.getTva() != null) {
            convention.setTva(data.getTva());
        }

        if (data.getMontantTTC() != null) {
            convention.setMontantTTC(data.getMontantTTC());
        }

        if (data.getNbUsers() != null) {
            convention.setNbUsers(data.getNbUsers());
        }

        if (data.getStructureResponsableId() != null) {
            Structure newStructureResponsable = structureRepository.findById(data.getStructureResponsableId())
                    .orElseThrow(() -> new RuntimeException("Structure responsable not found with ID: " + data.getStructureResponsableId()));
            convention.setStructureResponsable(newStructureResponsable);
            log.info("Updated structure responsable from {} to {}",
                    convention.getStructureResponsable() != null ? convention.getStructureResponsable().getId() : "null",
                    data.getStructureResponsableId());
        }

        // DO NOT set status here! Let determineNewStatus handle it after invoices are generated
        log.info("Convention updated successfully. New version: {}", convention.getRenewalVersion());
    }
    
    /**
     * Archive current version to old_conventions
     */
    @Transactional
    public void archiveCurrentVersion(Convention oldConvention, User currentUser,
                                      Convention newConvention, String reason) {
        log.info("Archiving old version of convention {} to old_conventions",
                oldConvention.getReferenceConvention());

        // Get next version number
        Integer nextVersion = 1;
        Integer maxVersion = oldConventionRepository.findMaxRenewalVersion(newConvention);
        if (maxVersion != null) {
            nextVersion = maxVersion + 1;
        }

        // Create old convention record from the OLD convention data
        OldConvention oldConv = new OldConvention();
        oldConv.setCurrentConvention(newConvention); // Link to NEW convention
        oldConv.setReferenceConvention(oldConvention.getReferenceConvention());
        oldConv.setReferenceERP(oldConvention.getReferenceERP());
        oldConv.setLibelle(oldConvention.getLibelle());
        oldConv.setDateDebut(oldConvention.getDateDebut());
        oldConv.setDateFin(oldConvention.getDateFin());
        oldConv.setDateSignature(oldConvention.getDateSignature());
        oldConv.setStructureResponsable(oldConvention.getStructureResponsable());
        oldConv.setStructureBeneficiel(oldConvention.getStructureBeneficiel());
        oldConv.setApplication(oldConvention.getApplication());
        oldConv.setMontantHT(oldConvention.getMontantHT());
        oldConv.setTva(oldConvention.getTva());
        oldConv.setMontantTTC(oldConvention.getMontantTTC());
        oldConv.setNbUsers(oldConvention.getNbUsers());
        oldConv.setPeriodicite(oldConvention.getPeriodicite());
        oldConv.setEtat(oldConvention.getEtat());
        oldConv.setArchivedAt(LocalDateTime.now());
        oldConv.setArchivedBy(currentUser.getUsername());
        oldConv.setArchivedReason(reason);
        oldConv.setRenewalVersion(nextVersion);
        oldConv.setCreatedAt(oldConvention.getCreatedAt());

        oldConventionRepository.save(oldConv);
        log.info("Old convention version {} saved with ID: {}", nextVersion, oldConv.getId());
    }


    /**
     * Handle chef de projet assignment during renewal
     */

    @Transactional
    public void handleRenewalAssignment(Application application, Convention convention, User currentUser) {
        User currentChef = application.getChefDeProjet();

        log.info("Handling renewal assignment for application: {}", application.getCode());
        log.info("Current chef: {}", currentChef != null ? currentChef.getUsername() : "None");
        log.info("Current user (who initiated renewal): {}", currentUser.getUsername());

        if (currentChef == null) {
            log.warn("Application {} has no chef assigned during renewal", application.getCode());
            return;
        }

        // Check who created the application
        boolean isAppCreatedByAdmin = false;

        if (application.getCreatedBy() != null) {
            isAppCreatedByAdmin = application.getCreatedBy().getRoles().stream()
                    .anyMatch(r -> r.getName().name().equals("ROLE_ADMIN"));
            log.info("Application created by: {} (isAdmin: {})",
                    application.getCreatedBy().getUsername(), isAppCreatedByAdmin);
        }

        // Get admin
        User admin = userRepository.findAll().stream()
                .filter(u -> u.getRoles().stream()
                        .anyMatch(r -> r.getName().name().equals("ROLE_ADMIN")))
                .findFirst()
                .orElse(null);

        if (admin == null) {
            log.error("No admin user found");
            return;
        }

        if (isAppCreatedByAdmin) {
            // SCENARIO 1: APP CREATED BY ADMIN
            log.info("SCENARIO 1: App created by admin");

            // First, notify admin that app is renewed
            requestService.sendRenewalNotificationToAdmin(application, convention, admin);

            // Check chef's workload
            boolean chefHasWorkload = checkChefWorkload(currentChef, application);

            if (chefHasWorkload) {
                // SCENARIO 1.1: Chef has workload - admin sends request
                log.info("SCENARIO 1.1: Chef has workload - creating request");
                requestService.createRenewalAcceptanceRequest(convention, currentChef, admin);
            } else {
                // SCENARIO 1.2: Chef is free - just reassign
                log.info("SCENARIO 1.2: Chef is free - sending reassignment notification");
                requestService.sendReassignmentNotificationToChef(currentChef, application, convention, admin);
            }

        } else {
            // SCENARIO 2: APP CREATED BY CHEF
            log.info("SCENARIO 2: App created by chef");

            // Send notification to chef
            requestService.sendRenewalNotificationToChef(currentChef, application, convention);

            // The chef will decide whether to accept or request reassignment via the requests tab
            // No automatic action here - chef will create request if needed
        }
    }

    // Helper method to check workload
    private boolean checkChefWorkload(User chef, Application application) {
        // Implement your workload check logic
        // Return true if chef has high workload, false if free
        // This could use the WorkloadService
        try {
            WorkloadService.AssignmentCheck check = workloadService.checkAssignment(chef.getId(), application.getId());
            // If workload > 75%, consider as "has workload"
            return check.getAnalysis() != null && check.getAnalysis().getProjectedWorkload() > 75.0;
        } catch (Exception e) {
            log.error("Error checking workload: {}", e.getMessage());
            // Default to false if can't check
            return false;
        }
    }

    /**
     * Send renewal notification to chef
     */


    private void sendRenewalNotificationToChef(User chef, Application application, Convention newConvention) {
        try {
            log.info("Attempting to send renewal notification to chef: {}", chef.getEmail());

            String subject = "🔄 Application renouvelée - " + application.getCode();

            // FIXED: Don't use String.format with HTML containing % signs
            // Build the content using concatenation instead
            String content =
                    "<!DOCTYPE html>" +
                            "<html>" +
                            "<head><meta charset='UTF-8'></head>" +
                            "<body style='font-family: Arial, sans-serif;'>" +
                            "<div style='max-width: 600px; margin: 0 auto; padding: 20px; background-color: #f9f9f9; border-radius: 10px;'>" +
                            "<div style='background: linear-gradient(135deg, #667eea 0%, #764ba2 100%); color: white; padding: 20px; border-radius: 10px 10px 0 0;'>" +
                            "<h2 style='margin: 0;'>🔄 Application renouvelée</h2>" +
                            "</div>" +
                            "<div style='background: white; padding: 20px; border-radius: 0 0 10px 10px;'>" +
                            "<p>Bonjour <strong>" + chef.getFirstName() + " " + chef.getLastName() + "</strong>,</p>" +
                            "<p>L'application <strong>" + application.getCode() + " - " + application.getName() + "</strong> a été renouvelée.</p>" +
                            "<div style='background: #f0f0f0; padding: 15px; border-radius: 5px; margin: 20px 0;'>" +
                            "<p><strong>Nouvelle convention :</strong> " + newConvention.getReferenceConvention() + "</p>" +
                            "</div>" +
                            "<p>Vous restez assigné à cette application. Si vous ne pouvez pas continuer à travailler dessus, veuillez soumettre une demande de réassignation.</p>" +
                            "<div style='text-align: center; margin: 30px 0;'>" +
                            "<a href='http://localhost:4200/applications/" + application.getId() + "' style='background: linear-gradient(135deg, #667eea 0%, #764ba2 100%); color: white; padding: 12px 30px; text-decoration: none; border-radius: 5px; display: inline-block;'>Voir l'application</a>" +
                            "</div>" +
                            "<p style='color: #666; font-size: 12px; margin-top: 20px;'>Cet email a été envoyé automatiquement.</p>" +
                            "</div>" +
                            "</div>" +
                            "</body>" +
                            "</html>";

            // Get admin user as sender
            User admin = userRepository.findAll().stream()
                    .filter(u -> u.getRoles().stream()
                            .anyMatch(r -> r.getName().name().equals("ROLE_ADMIN")))
                    .findFirst()
                    .orElse(null);

            if (admin != null) {
                com.example.back.payload.request.MailRequest request = new com.example.back.payload.request.MailRequest();
                request.setSubject(subject);
                request.setContent(content);
                request.setTo(List.of(chef.getEmail()));
                request.setImportance("NORMAL");

                mailService.sendMail(request, admin, null);
                log.info("✅ Renewal notification sent to chef {}", chef.getEmail());
            } else {
                log.error("No admin user found to send email");
            }

        } catch (Exception e) {
            log.error("Failed to send renewal notification: {}", e.getMessage(), e);
        }
    }
}