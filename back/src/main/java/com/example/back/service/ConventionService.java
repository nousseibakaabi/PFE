package com.example.back.service;

import com.example.back.entity.*;
import com.example.back.payload.request.ConventionRequest;
import com.example.back.repository.*;
import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.*;

@Service
@Slf4j
public class ConventionService {

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
    public BigDecimal calculateHT(BigDecimal montantTTC, BigDecimal tva) {
        if (montantTTC == null) {
            return BigDecimal.ZERO;
        }
        if (tva == null) {
            tva = BigDecimal.valueOf(19.00);
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
     * Generate or update invoices based on TTC amount
     * - PRESERVES paid invoices with their original amounts
     * - Only redistributes the REMAINING unpaid amount across remaining periods
     */

    @Transactional
    public void generateInvoicesForConvention(Convention convention) {
        log.info("========== GENERATING INVOICES FOR CONVENTION ==========");
        log.info("Convention ID: {}", convention.getId());
        log.info("Reference: {}", convention.getReferenceConvention());
        log.info("Periodicite: '{}'", convention.getPeriodicite());
        log.info("Date Debut: {}", convention.getDateDebut());
        log.info("Date Fin: {}", convention.getDateFin());
        log.info("Montant TTC: {}", convention.getMontantTTC());

        if (convention.getFactures() == null) {
            convention.setFactures(new ArrayList<>());
        }

        // Get existing invoices sorted
        List<Facture> existingInvoices = factureRepository.findByConventionIdOrderByNumeroFactureAsc(convention.getId());
        log.info("Existing invoices found: {}", existingInvoices.size());

        // Separate paid and unpaid invoices
        List<Facture> paidInvoices = new ArrayList<>();
        List<Facture> unpaidInvoices = new ArrayList<>();

        for (Facture invoice : existingInvoices) {
            if ("PAYE".equals(invoice.getStatutPaiement())) {
                paidInvoices.add(invoice);
            } else {
                unpaidInvoices.add(invoice);
            }
        }
        log.info("Paid invoices: {}, Unpaid invoices: {}", paidInvoices.size(), unpaidInvoices.size());

        // Validate required fields
        if (convention.getDateDebut() == null || convention.getDateFin() == null ||
                convention.getMontantTTC() == null || convention.getPeriodicite() == null) {
            log.error("Missing required fields for invoice generation");
            return;
        }

        // Calculate total duration based on periodicity
        int totalPeriods = calculateTotalPeriods(convention);
        log.info("Total periods calculated: {} for periodicity: {}", totalPeriods, convention.getPeriodicite());

        int paidPeriods = paidInvoices.size();
        int remainingPeriods = totalPeriods - paidPeriods;
        log.info("Paid periods: {}, Remaining periods: {}", paidPeriods, remainingPeriods);

        // Calculate amounts
        BigDecimal totalAmount = convention.getMontantTTC();
        BigDecimal paidAmount = calculateTotalPaidAmount(paidInvoices);
        BigDecimal remainingAmount = totalAmount.subtract(paidAmount);

        log.info("Total amount: {} TND, Paid amount: {} TND, Remaining amount: {} TND",
                totalAmount, paidAmount, remainingAmount);

        // Calculate new amount per remaining period
        BigDecimal newAmountPerRemainingPeriod = remainingAmount
                .divide(BigDecimal.valueOf(remainingPeriods), 2, RoundingMode.HALF_UP);
        log.info("New amount per remaining period: {} TND", newAmountPerRemainingPeriod);

        // STEP 1: UPDATE existing unpaid invoices
        for (int i = 0; i < unpaidInvoices.size(); i++) {
            Facture invoice = unpaidInvoices.get(i);
            BigDecimal oldAmount = invoice.getMontantTTC();
            invoice.setMontantHT(calculateHT(newAmountPerRemainingPeriod, convention.getTva()));
            invoice.setTva(convention.getTva());
            invoice.setMontantTTC(newAmountPerRemainingPeriod);
            invoice.setNotes(String.format("Facture pour la convention %s - TVA: %.2f%% - MISE À JOUR (était: %s TND)",
                    convention.getReferenceConvention(), convention.getTva(), oldAmount));

            factureRepository.save(invoice);
            log.info("UPDATED unpaid invoice {}: {} TND → {} TND",
                    invoice.getNumeroFacture(), oldAmount, newAmountPerRemainingPeriod);
        }

        // STEP 2: ADD new invoices if needed
        if (remainingPeriods > unpaidInvoices.size()) {
            int invoicesToAdd = remainingPeriods - unpaidInvoices.size();
            int startIndex = paidInvoices.size() + unpaidInvoices.size();
            log.info("Adding {} new invoices starting at index {}", invoicesToAdd, startIndex);

            for (int i = 0; i < invoicesToAdd; i++) {
                int sequenceNumber = startIndex + i + 1;
                String invoiceNumber = generateSequentialInvoiceNumber(convention, sequenceNumber);

                LocalDate invoiceDate = calculateInvoiceDate(convention.getDateDebut(), startIndex + i, convention.getPeriodicite());
                log.info("Creating invoice {} with date: {}", invoiceNumber, invoiceDate);

                Facture facture = new Facture();
                facture.setNumeroFacture(invoiceNumber);
                facture.setConvention(convention);
                facture.setDateFacturation(invoiceDate);
                facture.setDateEcheance(calculateDueDate(invoiceDate, convention.getPeriodicite()));
                facture.setMontantHT(calculateHT(newAmountPerRemainingPeriod, convention.getTva()));
                facture.setTva(convention.getTva());
                facture.setMontantTTC(newAmountPerRemainingPeriod);
                facture.setStatutPaiement("NON_PAYE");
                facture.setNotes(String.format("Facture %d/%d pour la convention %s",
                        sequenceNumber, totalPeriods, convention.getReferenceConvention()));

                Facture savedFacture = factureRepository.save(facture);
                convention.getFactures().add(savedFacture);
                log.info("ADDED new invoice {} at {} TND", savedFacture.getNumeroFacture(), newAmountPerRemainingPeriod);
            }
        }

        // STEP 3: REMOVE excess unpaid invoices
        if (remainingPeriods < unpaidInvoices.size()) {
            int invoicesToRemove = unpaidInvoices.size() - remainingPeriods;
            log.info("Removing {} excess invoices", invoicesToRemove);

            for (int i = 0; i < invoicesToRemove; i++) {
                Facture invoiceToRemove = unpaidInvoices.get(remainingPeriods + i);
                convention.getFactures().remove(invoiceToRemove);
                factureRepository.delete(invoiceToRemove);
                log.info("REMOVED excess invoice {}", invoiceToRemove.getNumeroFacture());
            }
        }

        // Ensure paid invoices are in the collection
        for (Facture paidInvoice : paidInvoices) {
            if (!convention.getFactures().contains(paidInvoice)) {
                convention.getFactures().add(paidInvoice);
            }
        }

        log.info("========== INVOICE GENERATION COMPLETE ==========");
    }

    // Add this method to ConventionService.java

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
     * Regenerate invoices when convention financial data changes
     */
    @Transactional
    public void regenerateInvoicesForConvention(Long conventionId) {
        Optional<Convention> conventionOpt = conventionRepository.findById(conventionId);
        if (conventionOpt.isEmpty()) {
            log.warn("Convention not found for invoice regeneration: {}", conventionId);
            return;
        }

        Convention convention = conventionOpt.get();

        // Only regenerate if convention is not terminated or archived
        if ("TERMINE".equals(convention.getEtat()) || "ARCHIVE".equals(convention.getEtat())) {
            log.info("Convention {} is {} - skipping invoice regeneration",
                    convention.getReferenceConvention(), convention.getEtat());
            return;
        }

        log.info("Regenerating invoices for convention {} due to financial data change",
                convention.getReferenceConvention());

        // Force a fresh fetch of the convention
        convention = conventionRepository.findById(conventionId).get();

        generateInvoicesForConvention(convention);

        // Update convention status after invoice changes
        updateConventionStatusRealTime(conventionId);
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

        // Store old values for history
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

        // Update financial data
        boolean financialDataChanged = hasFinancialDataChanged(convention, request);
        boolean datesOrPeriodicityChanged = datesChanged || periodiciteChanged;

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

        // LOG HISTORY: Convention update (AFTER saving, and only if there are changes)
        try {
            User currentUser = getCurrentUser();

            // Check if anything important changed
            boolean hasImportantChanges = financialDataChanged || datesChanged || periodiciteChanged ||
                    !oldStatus.equals(updatedConvention.getEtat());

            if (hasImportantChanges) {
                historyService.logConventionUpdate(oldConvention, updatedConvention, currentUser);

                // Log financial update if changed
                if (financialDataChanged) {
                    historyService.logConventionFinancialUpdate(updatedConvention,
                            oldMontantHT, oldMontantTTC, oldNbUsers,
                            updatedConvention.getMontantHT(), updatedConvention.getMontantTTC(), updatedConvention.getNbUsers());
                }

                // Check if status changed
                if (!oldStatus.equals(updatedConvention.getEtat())) {
                    historyService.logConventionStatusChange(updatedConvention, oldStatus, updatedConvention.getEtat());
                }
            }
        } catch (Exception e) {
            log.error("Failed to log convention update history: {}", e.getMessage());
            // Don't throw - we don't want history to break the main functionality
        }

        // If dates changed, update the application dates using ApplicationService
        if (datesChanged && updatedConvention.getApplication() != null) {
            applicationService.updateApplicationDatesFromConvention(
                    updatedConvention.getApplication().getId(),
                    updatedConvention.getDateDebut(),
                    updatedConvention.getDateFin()
            );
            log.info("Updated application dates due to convention date change");
        }

        // Regenerate invoices if financial data or dates/periodicity changed
        if (financialDataChanged || datesOrPeriodicityChanged) {
            log.info("Financial data or dates/periodicity changed for convention {}, regenerating invoices",
                    convention.getReferenceConvention());
            regenerateInvoicesForConvention(updatedConvention.getId());
        }

        return updatedConvention;
    }

    /**
     * Clone convention for history
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
        return clone;
    }

    /**
     * Get current user
     */
    private User getCurrentUser() {
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

        List<Convention> waitingConventions = conventionRepository.findByEtat("EN_ATTENTE");
        for (Convention convention : waitingConventions) {
            if (!today.isBefore(convention.getDateDebut())) {
                String oldStatus = convention.getEtat();
                convention.setEtat("EN_COURS");
                conventionRepository.save(convention);
                log.info("Convention {} transitioned from EN_ATTENTE to EN_COURS",
                        convention.getReferenceConvention());

                // LOG HISTORY: Status change
                try {
                    historyService.logConventionStatusChange(convention, oldStatus, "EN_COURS");
                } catch (Exception e) {
                    log.error("Failed to log status change history: {}", e.getMessage());
                }
            }
        }

        List<Convention> activeConventions = conventionRepository.findByEtat("EN_COURS");
        for (Convention convention : activeConventions) {
            if (convention.getDateFin() != null && today.isAfter(convention.getDateFin())) {
                List<Facture> invoices = factureRepository.findByConventionId(convention.getId());
                boolean allInvoicesPaid = invoices.stream()
                        .allMatch(invoice -> "PAYE".equals(invoice.getStatutPaiement()));

                if (!allInvoicesPaid) {
                    String oldStatus = convention.getEtat();
                    convention.setEtat("EN_RETARD");
                    conventionRepository.save(convention);
                    log.info("Convention {} marked as EN_RETARD", convention.getReferenceConvention());

                    // LOG HISTORY: Status change
                    try {
                        historyService.logConventionStatusChange(convention, oldStatus, "EN_RETARD");
                    } catch (Exception e) {
                        log.error("Failed to log status change history: {}", e.getMessage());
                    }
                }
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
}