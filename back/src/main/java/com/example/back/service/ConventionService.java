package com.example.back.service;

import com.example.back.entity.Convention;
import com.example.back.entity.Facture;
import com.example.back.repository.ConventionRepository;
import com.example.back.repository.FactureRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.Period;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;

@Service
@Slf4j
public class ConventionService {

    @Autowired
    private ConventionRepository conventionRepository;

    @Autowired
    private FactureRepository factureRepository;



    /**
     * Generate invoices automatically when a convention is created
     */
    @Transactional
    public void generateInvoicesForConvention(Convention convention) {
        if (convention.getFactures() != null && !convention.getFactures().isEmpty()) {
            return; // Invoices already generated
        }

        log.info("Generating invoices for convention: {}", convention.getReferenceConvention());

        // Validate required fields
        if (convention.getDateDebut() == null || convention.getDateFin() == null ||
                convention.getMontantTotal() == null || convention.getPeriodicite() == null) {
            throw new IllegalArgumentException("Missing required fields for invoice generation");
        }

        // Calculate number of invoices based on periodicity
        int numberOfInvoices = calculateNumberOfInvoices(convention);

        // FIX: For ANNUEL periodicity, if duration is exactly 1 year, create only 1 invoice
        if ("ANNUEL".equalsIgnoreCase(convention.getPeriodicite())) {
            long years = ChronoUnit.YEARS.between(convention.getDateDebut(), convention.getDateFin());
            if (years == 1) {
                numberOfInvoices = 1;
            }
        }

        BigDecimal invoiceAmount = convention.getMontantTotal()
                .divide(BigDecimal.valueOf(numberOfInvoices), 2, RoundingMode.HALF_UP);

        // Generate invoices
        LocalDate currentDate = convention.getDateDebut();

        for (int i = 1; i <= numberOfInvoices; i++) {
            Facture facture = new Facture();
            facture.setNumeroFacture(generateInvoiceNumber(convention, i));
            facture.setConvention(convention);
            facture.setDateFacturation(currentDate);
            facture.setDateEcheance(calculateDueDate(currentDate, convention.getPeriodicite()));
            facture.setMontantHT(invoiceAmount);
            facture.setTva(BigDecimal.valueOf(19.00));
            facture.setStatutPaiement("NON_PAYE");
            facture.setNotes(String.format("Facture %d/%d pour la convention %s",
                    i, numberOfInvoices, convention.getReferenceConvention()));

            // Save invoice
            factureRepository.save(facture);

            // Move to next period
            currentDate = getNextPeriodDate(currentDate, convention.getPeriodicite());
        }

        log.info("Generated {} invoices for convention {}", numberOfInvoices, convention.getReferenceConvention());
    }

    private int calculateNumberOfInvoices(Convention convention) {
        LocalDate start = convention.getDateDebut();
        LocalDate end = convention.getDateFin();

        // FIX: Use inclusive calculation
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
                return 1; // Single invoice for other periodicities
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
                return invoiceDate.plusMonths(1); // Default to 1 month
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

    /**
     * REAL-TIME: Update convention status immediately
     * Call this whenever invoices change or time passes
     */
    @Transactional
    public void updateConventionStatusRealTime(Long conventionId) {
        Optional<Convention> conventionOpt = conventionRepository.findById(conventionId);

        if (conventionOpt.isEmpty()) {
            log.warn("Convention not found for real-time status update: {}", conventionId);
            return;
        }

        Convention convention = conventionOpt.get();
        String oldStatus = convention.getEtat();

        // Update status based on new logic
        convention.updateStatus();

        if (!oldStatus.equals(convention.getEtat())) {
            conventionRepository.save(convention);
            log.info("REAL-TIME: Convention {} status changed from {} to {}",
                    convention.getReferenceConvention(), oldStatus, convention.getEtat());
        }
    }

    /**
     * Check date transitions (EN_ATTENTE → EN_COURS) every day
     */
    @Scheduled(cron = "0 0 6 * * *") // Run at 6 AM every day
    @Transactional
    public void checkDateTransitions() {
        log.info("Checking date transitions for conventions...");
        LocalDate today = LocalDate.now();

        // Find all EN_ATTENTE conventions
        List<Convention> waitingConventions = conventionRepository.findByEtat("EN_ATTENTE");

        for (Convention convention : waitingConventions) {
            // If today is on or after start date, transition to EN_COURS
            if (!today.isBefore(convention.getDateDebut())) {
                convention.setEtat("EN_COURS");
                conventionRepository.save(convention);
                log.info("Convention {} transitioned from EN_ATTENTE to EN_COURS (start date: {})",
                        convention.getReferenceConvention(), convention.getDateDebut());
            }
        }

        // Check for conventions that should be EN_RETARD (end date passed with unpaid invoices)
        List<Convention> activeConventions = conventionRepository.findByEtat("EN_COURS");

        for (Convention convention : activeConventions) {
            if (convention.getDateFin() != null && today.isAfter(convention.getDateFin())) {
                // Check if not all invoices are paid
                List<Facture> invoices = factureRepository.findByConventionId(convention.getId());
                boolean allInvoicesPaid = invoices.stream()
                        .allMatch(invoice -> "PAYE".equals(invoice.getStatutPaiement()));

                if (!allInvoicesPaid) {
                    convention.setEtat("EN_RETARD");
                    conventionRepository.save(convention);
                    log.info("Convention {} marked as EN_RETARD (end date passed, unpaid invoices)",
                            convention.getReferenceConvention());
                }
            }
        }
    }

    /**
     * Check for conventions that should be TERMINE (all invoices paid)
     */
    @Scheduled(fixedRate = 300000) // Every 5 minutes
    @Transactional
    public void checkForTerminedConventions() {
        log.debug("Checking for conventions with all invoices paid...");

        List<Convention> activeConventions = conventionRepository.findByArchivedFalse();

        for (Convention convention : activeConventions) {
            // Skip if already TERMINE or ARCHIVE
            if ("TERMINE".equals(convention.getEtat()) ||
                    "ARCHIVE".equals(convention.getEtat())) {
                continue;
            }

            List<Facture> invoices = factureRepository.findByConventionId(convention.getId());

            if (!invoices.isEmpty()) {
                boolean allInvoicesPaid = invoices.stream()
                        .allMatch(invoice -> "PAYE".equals(invoice.getStatutPaiement()));

                if (allInvoicesPaid) {
                    convention.setEtat("TERMINE");
                    conventionRepository.save(convention);
                    log.info("Convention {} marked as TERMINE (all invoices paid)",
                            convention.getReferenceConvention());
                }
            }
        }
    }

    /**
     * REAL-TIME: Update convention status based on invoice statuses
     */
    @Transactional
    public void updateConventionStatusBasedOnInvoicesRealTime(Convention convention) {
        List<Facture> invoices = factureRepository.findByConventionId(convention.getId());

        if (invoices.isEmpty()) {
            return;
        }

        LocalDate today = LocalDate.now();

        // Check for overdue invoices
        boolean hasOverdueInvoices = invoices.stream()
                .anyMatch(invoice ->
                        "NON_PAYE".equals(invoice.getStatutPaiement()) &&
                                invoice.getDateEcheance().isBefore(today) &&
                                !"EN_RETARD".equals(invoice.getStatutPaiement())
                );

        if (hasOverdueInvoices && !"EN_RETARD".equals(convention.getEtat())) {
            convention.setEtat("EN_RETARD");
            log.info("REAL-TIME: Convention {} marked as EN_RETARD (overdue invoices found)",
                    convention.getReferenceConvention());

            // Also mark invoices as EN_RETARD
            for (Facture invoice : invoices) {
                if ("NON_PAYE".equals(invoice.getStatutPaiement()) &&
                        invoice.getDateEcheance().isBefore(today)) {
                    invoice.setStatutPaiement("EN_RETARD");
                    factureRepository.save(invoice);
                }
            }
        }

        // Check if all invoices are paid
        boolean allInvoicesPaid = invoices.stream()
                .allMatch(invoice -> "PAYE".equals(invoice.getStatutPaiement()));

        if (allInvoicesPaid && !"TERMINE".equals(convention.getEtat())) {
            convention.setEtat("TERMINE");
            log.info("REAL-TIME: Convention {} marked as TERMINE (all invoices paid)",
                    convention.getReferenceConvention());
        }
    }

    /**
     * REAL-TIME: Check and update TERMINE status
     */
    @Transactional
    public void checkAndUpdateTermineStatusRealTime(Convention convention) {
        List<Facture> invoices = factureRepository.findByConventionId(convention.getId());

        if (invoices.isEmpty()) {
            return;
        }

        boolean allInvoicesPaid = invoices.stream()
                .allMatch(invoice -> "PAYE".equals(invoice.getStatutPaiement()));

        if (allInvoicesPaid && !"TERMINE".equals(convention.getEtat())) {
            convention.setEtat("TERMINE");
            conventionRepository.save(convention);
            log.info("REAL-TIME: Convention {} marked as TERMINE (all invoices paid)",
                    convention.getReferenceConvention());
        }
    }

    /**
     * REAL-TIME: Check date transitions (EN_ATTENTE -> EN_COURS)
     */
    @Transactional
    public void checkDateTransitionsRealTime() {
        LocalDate today = LocalDate.now();

        // Find conventions that should transition from EN_ATTENTE to EN_COURS
        List<Convention> waitingConventions = conventionRepository.findByEtat("EN_ATTENTE");

        for (Convention convention : waitingConventions) {
            if (today.isAfter(convention.getDateDebut().minusDays(1))) { // Starting today or started
                convention.setEtat("EN_COURS");
                conventionRepository.save(convention);
                log.info("REAL-TIME: Convention {} transitioned from EN_ATTENTE to EN_COURS",
                        convention.getReferenceConvention());
            }
        }

        // Find conventions that should transition to TERMINE based on end date
        List<Convention> activeConventions = conventionRepository.findByEtat("EN_COURS");

        for (Convention convention : activeConventions) {
            if (convention.getDateFin() != null &&
                    today.isAfter(convention.getDateFin()) &&
                    !"TERMINE".equals(convention.getEtat())) {

                // Check if all invoices are paid
                List<Facture> invoices = factureRepository.findByConventionId(convention.getId());
                boolean allInvoicesPaid = invoices.stream()
                        .allMatch(invoice -> "PAYE".equals(invoice.getStatutPaiement()));

                if (allInvoicesPaid) {
                    convention.setEtat("TERMINE");
                    conventionRepository.save(convention);
                    log.info("REAL-TIME: Convention {} marked as TERMINE (end date reached, all invoices paid)",
                            convention.getReferenceConvention());
                }
            }
        }
    }

    /**
     * REAL-TIME: Trigger status update for ALL conventions
     * Useful for admin actions or system startup
     */
    @Transactional
    public void updateAllConventionsStatusRealTime() {
        log.info("REAL-TIME: Updating status for all conventions...");

        List<Convention> allConventions = conventionRepository.findByArchivedFalse();

        for (Convention convention : allConventions) {
            updateConventionStatusRealTime(convention.getId());
        }

        log.info("REAL-TIME: Updated {} conventions", allConventions.size());
    }

    // Remove or modify old scheduled tasks for more frequent updates
    @Scheduled(fixedRate = 300000) // Every 5 minutes (300,000 ms)
    @Transactional
    public void checkStatusUpdatesFrequently() {
        log.debug("Frequent status check running...");
        checkDateTransitionsRealTime();
    }

    @Scheduled(fixedRate = 600000) // Every 10 minutes (600,000 ms)
    @Transactional
    public void checkOverdueInvoicesFrequently() {
        log.debug("Frequent overdue invoice check running...");

        LocalDate today = LocalDate.now();
        List<Facture> overdueInvoices = factureRepository.findOverdueInvoices(today);

        for (Facture invoice : overdueInvoices) {
            if ("NON_PAYE".equals(invoice.getStatutPaiement())) {
                invoice.setStatutPaiement("EN_RETARD");
                factureRepository.save(invoice);

                // Update convention status
                Convention convention = invoice.getConvention();
                if (convention != null && !"EN_RETARD".equals(convention.getEtat())) {
                    convention.setEtat("EN_RETARD");
                    conventionRepository.save(convention);
                }
            }
        }

        if (!overdueInvoices.isEmpty()) {
            log.info("Frequent check: Updated {} overdue invoices", overdueInvoices.size());
        }
    }

    // Keep daily scheduled tasks for cleanup
    @Scheduled(cron = "0 0 3 * * *") // 3 AM daily for comprehensive cleanup
    @Transactional
    public void dailyComprehensiveStatusUpdate() {
        log.info("Daily comprehensive status update running...");
        updateAllConventionsStatusRealTime();
    }
}