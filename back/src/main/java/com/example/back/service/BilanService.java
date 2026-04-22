// BilanService.java
package com.example.back.service;

import com.example.back.entity.*;
import com.example.back.payload.response.BilanDTO;
import com.example.back.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class BilanService {

    private final ConventionRepository conventionRepository;
    private final FactureRepository factureRepository;
    private final OldConventionRepository oldConventionRepository;
    private final OldFactureRepository oldFactureRepository;

    /**
     * Get bilan for all factures only
     */
    public BilanDTO getFacturesBilan(LocalDate startDate, LocalDate endDate) {
        log.info("Generating factures bilan from {} to {}", startDate, endDate);

        BilanDTO bilan = new BilanDTO();
        bilan.setTitle("Bilan des Factures");
        bilan.setStartDate(startDate);
        bilan.setEndDate(endDate);
        bilan.setPeriodType(determinePeriodType(startDate, endDate));

        List<Facture> factures = factureRepository.findByDateFacturationBetween(startDate, endDate);

        for (Facture facture : factures) {
            BilanDTO.BilanItem item = convertFactureToBilanItem(facture);
            bilan.getItems().add(item);
            updateSummaryWithFacture(bilan.getSummary(), facture);
        }

        calculateSummaryRates(bilan.getSummary());

        return bilan;
    }

    /**
     * Get bilan for all conventions only
     */
    public BilanDTO getConventionsBilan(LocalDate startDate, LocalDate endDate, boolean includeOldVersions) {
        log.info("Generating conventions bilan from {} to {}, includeOld: {}", startDate, endDate, includeOldVersions);

        BilanDTO bilan = new BilanDTO();
        bilan.setTitle("Bilan des Conventions");
        bilan.setStartDate(startDate);
        bilan.setEndDate(endDate);
        bilan.setPeriodType(determinePeriodType(startDate, endDate));

        List<Convention> conventions = conventionRepository.findByDateDebutBetween(startDate, endDate);

        for (Convention convention : conventions) {
            BilanDTO.BilanItem item = convertConventionToBilanItem(convention);

            // Add current invoices
            List<Facture> factures = factureRepository.findByConventionId(convention.getId());
            for (Facture facture : factures) {
                BilanDTO.InvoiceBilanItem invoiceItem = convertFactureToInvoiceItem(facture);
                item.getInvoices().add(invoiceItem);
                updateInvoiceSummary(item.getInvoiceSummary(), facture);
            }

            bilan.getItems().add(item);
            updateSummaryWithConvention(bilan.getSummary(), convention, item.getInvoiceSummary());
        }

        if (includeOldVersions) {
            List<OldConvention> oldConventions = oldConventionRepository.findAll();
            for (OldConvention oldConv : oldConventions) {
                if (isWithinDateRange(oldConv.getDateDebut(), startDate, endDate)) {
                    BilanDTO.BilanItem item = convertOldConventionToBilanItem(oldConv);

                    List<OldFacture> oldFactures = oldFactureRepository.findByOldConvention(oldConv);
                    for (OldFacture oldFacture : oldFactures) {
                        BilanDTO.InvoiceBilanItem invoiceItem = convertOldFactureToInvoiceItem(oldFacture);
                        item.getInvoices().add(invoiceItem);
                        updateInvoiceSummaryWithOldFacture(item.getInvoiceSummary(), oldFacture);
                    }

                    bilan.getItems().add(item);
                    updateSummaryWithOldConvention(bilan.getSummary(), oldConv, item.getInvoiceSummary());
                }
            }
        }

        calculateSummaryRates(bilan.getSummary());

        return bilan;
    }

    /**
     * Get combined bilan (conventions + factures)
     */
    public BilanDTO getCombinedBilan(LocalDate startDate, LocalDate endDate, boolean includeOldVersions) {
        log.info("Generating combined bilan from {} to {}, includeOld: {}", startDate, endDate, includeOldVersions);

        BilanDTO bilan = new BilanDTO();
        bilan.setTitle("Bilan Global (Conventions + Factures)");
        bilan.setStartDate(startDate);
        bilan.setEndDate(endDate);
        bilan.setPeriodType(determinePeriodType(startDate, endDate));

        // Add conventions
        List<Convention> conventions = conventionRepository.findByDateDebutBetween(startDate, endDate);
        for (Convention convention : conventions) {
            BilanDTO.BilanItem item = convertConventionToBilanItem(convention);

            List<Facture> factures = factureRepository.findByConventionId(convention.getId());
            for (Facture facture : factures) {
                BilanDTO.InvoiceBilanItem invoiceItem = convertFactureToInvoiceItem(facture);
                item.getInvoices().add(invoiceItem);
                updateInvoiceSummary(item.getInvoiceSummary(), facture);
            }

            bilan.getItems().add(item);
            updateSummaryWithConvention(bilan.getSummary(), convention, item.getInvoiceSummary());
        }

        // Add old versions if requested
        if (includeOldVersions) {
            List<OldConvention> oldConventions = oldConventionRepository.findAll();
            for (OldConvention oldConv : oldConventions) {
                if (isWithinDateRange(oldConv.getDateDebut(), startDate, endDate)) {
                    BilanDTO.BilanItem item = convertOldConventionToBilanItem(oldConv);

                    List<OldFacture> oldFactures = oldFactureRepository.findByOldConvention(oldConv);
                    for (OldFacture oldFacture : oldFactures) {
                        BilanDTO.InvoiceBilanItem invoiceItem = convertOldFactureToInvoiceItem(oldFacture);
                        item.getInvoices().add(invoiceItem);
                        updateInvoiceSummaryWithOldFacture(item.getInvoiceSummary(), oldFacture);
                    }

                    bilan.getItems().add(item);
                    updateSummaryWithOldConvention(bilan.getSummary(), oldConv, item.getInvoiceSummary());
                }
            }
        }

        calculateSummaryRates(bilan.getSummary());

        return bilan;
    }

    /**
     * Get bilan for a specific convention (includes its factures)
     */
    public BilanDTO getConventionBilan(Long conventionId) {
        log.info("Generating bilan for convention ID: {}", conventionId);

        Convention convention = conventionRepository.findById(conventionId)
                .orElseThrow(() -> new RuntimeException("Convention not found"));

        BilanDTO bilan = new BilanDTO();
        bilan.setTitle("Bilan de la Convention: " + convention.getReferenceConvention());
        bilan.setStartDate(convention.getDateDebut());
        bilan.setEndDate(convention.getDateFin());
        bilan.setPeriodType("SPECIFIC");

        BilanDTO.BilanItem item = convertConventionToBilanItem(convention);

        // Add current invoices
        List<Facture> factures = factureRepository.findByConventionId(conventionId);
        for (Facture facture : factures) {
            BilanDTO.InvoiceBilanItem invoiceItem = convertFactureToInvoiceItem(facture);
            item.getInvoices().add(invoiceItem);
            updateInvoiceSummary(item.getInvoiceSummary(), facture);
        }

        // Add old versions' invoices if any
        List<OldConvention> oldVersions = oldConventionRepository.findByCurrentConventionOrderByRenewalVersionDesc(convention);
        for (OldConvention oldConv : oldVersions) {
            List<OldFacture> oldFactures = oldFactureRepository.findByOldConvention(oldConv);
            for (OldFacture oldFacture : oldFactures) {
                BilanDTO.InvoiceBilanItem invoiceItem = convertOldFactureToInvoiceItem(oldFacture);
                item.getInvoices().add(invoiceItem);
                updateInvoiceSummaryWithOldFacture(item.getInvoiceSummary(), oldFacture);
            }
        }

        bilan.getItems().add(item);
        updateSummaryWithConvention(bilan.getSummary(), convention, item.getInvoiceSummary());
        calculateSummaryRates(bilan.getSummary());

        return bilan;
    }

    /**
     * Get bilan by month
     */
    public BilanDTO getBilanByMonth(int year, int month, String type, boolean includeOldVersions) {
        YearMonth yearMonth = YearMonth.of(year, month);
        LocalDate startDate = yearMonth.atDay(1);
        LocalDate endDate = yearMonth.atEndOfMonth();

        BilanDTO bilan;
        switch (type) {
            case "factures":
                bilan = getFacturesBilan(startDate, endDate);
                break;
            case "conventions":
                bilan = getConventionsBilan(startDate, endDate, includeOldVersions);
                break;
            default:
                bilan = getCombinedBilan(startDate, endDate, includeOldVersions);
        }

        bilan.setYear(year);
        bilan.setMonth(month);
        return bilan;
    }

    /**
     * Get bilan by year
     */
    public BilanDTO getBilanByYear(int year, String type, boolean includeOldVersions) {
        LocalDate startDate = LocalDate.of(year, 1, 1);
        LocalDate endDate = LocalDate.of(year, 12, 31);

        BilanDTO bilan;
        switch (type) {
            case "factures":
                bilan = getFacturesBilan(startDate, endDate);
                break;
            case "conventions":
                bilan = getConventionsBilan(startDate, endDate, includeOldVersions);
                break;
            default:
                bilan = getCombinedBilan(startDate, endDate, includeOldVersions);
        }

        bilan.setYear(year);
        return bilan;
    }

    // ============= CONVERSION METHODS =============

    private BilanDTO.BilanItem convertConventionToBilanItem(Convention convention) {
        BilanDTO.BilanItem item = new BilanDTO.BilanItem();
        item.setId(convention.getId());
        item.setReference(convention.getReferenceConvention());
        item.setLibelle(convention.getLibelle());
        item.setType("CONVENTION");
        item.setEtat(convention.getEtat());
        item.setStartDate(convention.getDateDebut());
        item.setEndDate(convention.getDateFin());
        item.setMontantHT(convention.getMontantHT());
        item.setMontantTTC(convention.getMontantTTC());
        item.setTva(convention.getTva());
        item.setNbUsers(convention.getNbUsers());
        item.setPeriodicite(convention.getPeriodicite());
        item.setRenewalVersion(convention.getRenewalVersion());

        if (convention.getStructureResponsable() != null) {
            item.setStructureResponsable(convention.getStructureResponsable().getName());
        }
        if (convention.getStructureBeneficiel() != null) {
            item.setStructureBeneficiel(convention.getStructureBeneficiel().getName());
        }
        if (convention.getApplication() != null) {
            item.setApplicationName(convention.getApplication().getName());
        }

        return item;
    }

    private BilanDTO.BilanItem convertOldConventionToBilanItem(OldConvention oldConv) {
        BilanDTO.BilanItem item = new BilanDTO.BilanItem();
        item.setId(oldConv.getId());
        item.setReference(oldConv.getReferenceConvention());
        item.setLibelle(oldConv.getLibelle());
        item.setType("OLD_CONVENTION");
        item.setEtat(oldConv.getEtat());
        item.setStartDate(oldConv.getDateDebut());
        item.setEndDate(oldConv.getDateFin());
        item.setMontantHT(oldConv.getMontantHT());
        item.setMontantTTC(oldConv.getMontantTTC());
        item.setTva(oldConv.getTva());
        item.setNbUsers(oldConv.getNbUsers());
        item.setPeriodicite(oldConv.getPeriodicite());
        item.setRenewalVersion(oldConv.getRenewalVersion());

        if (oldConv.getStructureResponsable() != null) {
            item.setStructureResponsable(oldConv.getStructureResponsable().getName());
        }
        if (oldConv.getStructureBeneficiel() != null) {
            item.setStructureBeneficiel(oldConv.getStructureBeneficiel().getName());
        }
        if (oldConv.getApplication() != null) {
            item.setApplicationName(oldConv.getApplication().getName());
        }

        return item;
    }

    private BilanDTO.BilanItem convertFactureToBilanItem(Facture facture) {
        BilanDTO.BilanItem item = new BilanDTO.BilanItem();
        item.setId(facture.getId());
        item.setReference(facture.getNumeroFacture());
        item.setLibelle("Facture " + facture.getNumeroFacture());
        item.setType("FACTURE");
        item.setEtat(facture.getStatutPaiement());
        item.setStartDate(facture.getDateFacturation());
        item.setEndDate(facture.getDateEcheance());
        item.setMontantHT(facture.getMontantHT());
        item.setMontantTTC(facture.getMontantTTC());
        item.setTva(facture.getTva());

        if (facture.getConvention() != null) {
            item.setReference(facture.getConvention().getReferenceConvention() + " - " + facture.getNumeroFacture());
            if (facture.getConvention().getStructureResponsable() != null) {
                item.setStructureResponsable(facture.getConvention().getStructureResponsable().getName());
            }
            if (facture.getConvention().getApplication() != null) {
                item.setApplicationName(facture.getConvention().getApplication().getName());
            }
        }

        return item;
    }

    private BilanDTO.InvoiceBilanItem convertFactureToInvoiceItem(Facture facture) {
        BilanDTO.InvoiceBilanItem item = new BilanDTO.InvoiceBilanItem();
        item.setId(facture.getId());
        item.setNumeroFacture(facture.getNumeroFacture());
        item.setDateFacturation(facture.getDateFacturation());
        item.setDateEcheance(facture.getDateEcheance());
        item.setMontantHT(facture.getMontantHT());
        item.setMontantTTC(facture.getMontantTTC());
        item.setStatutPaiement(facture.getStatutPaiement());
        item.setDatePaiement(facture.getDatePaiement());
        item.setReferencePaiement(facture.getReferencePaiement());
        item.setOverdue(facture.isEnRetard());
        item.setJoursRetard(facture.getJoursRetard());

        Map<String, Object> details = facture.getPaiementDetails();
        if (!details.isEmpty()) {
            item.setPaiementType((String) details.get("type"));
        }

        return item;
    }

    private BilanDTO.InvoiceBilanItem convertOldFactureToInvoiceItem(OldFacture oldFacture) {
        BilanDTO.InvoiceBilanItem item = new BilanDTO.InvoiceBilanItem();
        item.setId(oldFacture.getId());
        item.setNumeroFacture(oldFacture.getNumeroFacture());
        item.setDateFacturation(oldFacture.getDateFacturation());
        item.setDateEcheance(oldFacture.getDateEcheance());
        item.setMontantHT(oldFacture.getMontantHT());
        item.setMontantTTC(oldFacture.getMontantTTC());
        item.setStatutPaiement(oldFacture.getStatutPaiement());
        item.setDatePaiement(oldFacture.getDatePaiement());
        item.setReferencePaiement(oldFacture.getReferencePaiement());

        if (oldFacture.getDateEcheance() != null &&
                !"PAYE".equals(oldFacture.getStatutPaiement()) &&
                oldFacture.getDateEcheance().isBefore(LocalDate.now())) {
            item.setOverdue(true);
        }

        return item;
    }

    // ============= SUMMARY UPDATE METHODS =============

    private void updateSummaryWithConvention(BilanDTO.BilanSummary summary, Convention convention, BilanDTO.InvoiceSummary invoiceSummary) {
        summary.setTotalConventions(summary.getTotalConventions() + 1);

        switch (convention.getEtat()) {
            case "EN COURS":
                summary.setActiveConventions(summary.getActiveConventions() + 1);
                break;
            case "TERMINE":
                summary.setTerminatedConventions(summary.getTerminatedConventions() + 1);
                break;
            case "ARCHIVE":
                summary.setArchivedConventions(summary.getArchivedConventions() + 1);
                break;
        }

        if (convention.getMontantHT() != null) {
            summary.setTotalMontantHT(summary.getTotalMontantHT().add(convention.getMontantHT()));
        }
        if (convention.getMontantTTC() != null) {
            summary.setTotalMontantTTC(summary.getTotalMontantTTC().add(convention.getMontantTTC()));
        }

        // TVA = TTC - HT
        if (convention.getMontantTTC() != null && convention.getMontantHT() != null) {
            summary.setTotalTVA(summary.getTotalTVA().add(
                    convention.getMontantTTC().subtract(convention.getMontantHT())
            ));
        }

        // Invoice stats
        summary.setTotalInvoices(summary.getTotalInvoices() + invoiceSummary.getTotal());
        summary.setPaidInvoices(summary.getPaidInvoices() + invoiceSummary.getPaid());
        summary.setUnpaidInvoices(summary.getUnpaidInvoices() + invoiceSummary.getUnpaid());
        summary.setOverdueInvoices(summary.getOverdueInvoices() + invoiceSummary.getOverdue());

        summary.setTotalPaid(summary.getTotalPaid().add(invoiceSummary.getPaidAmount()));
        summary.setTotalUnpaid(summary.getTotalUnpaid().add(invoiceSummary.getUnpaidAmount()));
    }

    private void updateSummaryWithOldConvention(BilanDTO.BilanSummary summary, OldConvention oldConv, BilanDTO.InvoiceSummary invoiceSummary) {
        summary.setTotalConventions(summary.getTotalConventions() + 1);

        if (oldConv.getMontantHT() != null) {
            summary.setTotalMontantHT(summary.getTotalMontantHT().add(oldConv.getMontantHT()));
        }
        if (oldConv.getMontantTTC() != null) {
            summary.setTotalMontantTTC(summary.getTotalMontantTTC().add(oldConv.getMontantTTC()));
        }

        summary.setTotalInvoices(summary.getTotalInvoices() + invoiceSummary.getTotal());
        summary.setPaidInvoices(summary.getPaidInvoices() + invoiceSummary.getPaid());
        summary.setUnpaidInvoices(summary.getUnpaidInvoices() + invoiceSummary.getUnpaid());
        summary.setOverdueInvoices(summary.getOverdueInvoices() + invoiceSummary.getOverdue());

        summary.setTotalPaid(summary.getTotalPaid().add(invoiceSummary.getPaidAmount()));
        summary.setTotalUnpaid(summary.getTotalUnpaid().add(invoiceSummary.getUnpaidAmount()));
    }

    private void updateSummaryWithFacture(BilanDTO.BilanSummary summary, Facture facture) {
        summary.setTotalInvoices(summary.getTotalInvoices() + 1);

        if ("PAYE".equals(facture.getStatutPaiement())) {
            summary.setPaidInvoices(summary.getPaidInvoices() + 1);
            if (facture.getMontantTTC() != null) {
                summary.setTotalPaid(summary.getTotalPaid().add(facture.getMontantTTC()));
            }
        } else {
            summary.setUnpaidInvoices(summary.getUnpaidInvoices() + 1);
            if (facture.getMontantTTC() != null) {
                summary.setTotalUnpaid(summary.getTotalUnpaid().add(facture.getMontantTTC()));
            }
            if (facture.isEnRetard()) {
                summary.setOverdueInvoices(summary.getOverdueInvoices() + 1);
                summary.setTotalOverdue(summary.getTotalOverdue().add(facture.getMontantTTC()));
            }
        }

        if (facture.getMontantTTC() != null) {
            summary.setTotalMontantTTC(summary.getTotalMontantTTC().add(facture.getMontantTTC()));
        }
        if (facture.getMontantHT() != null) {
            summary.setTotalMontantHT(summary.getTotalMontantHT().add(facture.getMontantHT()));
        }
        if (facture.getTva() != null && facture.getMontantHT() != null) {
            BigDecimal tvaAmount = facture.getMontantHT()
                    .multiply(facture.getTva())
                    .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
            summary.setTotalTVA(summary.getTotalTVA().add(tvaAmount));
        }
    }

    private void updateInvoiceSummary(BilanDTO.InvoiceSummary summary, Facture facture) {
        summary.setTotal(summary.getTotal() + 1);

        if (facture.getMontantTTC() != null) {
            summary.setTotalAmount(summary.getTotalAmount().add(facture.getMontantTTC()));
        }

        if ("PAYE".equals(facture.getStatutPaiement())) {
            summary.setPaid(summary.getPaid() + 1);
            if (facture.getMontantTTC() != null) {
                summary.setPaidAmount(summary.getPaidAmount().add(facture.getMontantTTC()));
            }
        } else {
            summary.setUnpaid(summary.getUnpaid() + 1);
            if (facture.getMontantTTC() != null) {
                summary.setUnpaidAmount(summary.getUnpaidAmount().add(facture.getMontantTTC()));
            }
            if (facture.isEnRetard()) {
                summary.setOverdue(summary.getOverdue() + 1);
            }
        }
    }

    private void updateInvoiceSummaryWithOldFacture(BilanDTO.InvoiceSummary summary, OldFacture oldFacture) {
        summary.setTotal(summary.getTotal() + 1);

        if (oldFacture.getMontantTTC() != null) {
            summary.setTotalAmount(summary.getTotalAmount().add(oldFacture.getMontantTTC()));
        }

        if ("PAYE".equals(oldFacture.getStatutPaiement())) {
            summary.setPaid(summary.getPaid() + 1);
            if (oldFacture.getMontantTTC() != null) {
                summary.setPaidAmount(summary.getPaidAmount().add(oldFacture.getMontantTTC()));
            }
        } else {
            summary.setUnpaid(summary.getUnpaid() + 1);
            if (oldFacture.getMontantTTC() != null) {
                summary.setUnpaidAmount(summary.getUnpaidAmount().add(oldFacture.getMontantTTC()));
            }
        }
    }

    private void calculateSummaryRates(BilanDTO.BilanSummary summary) {
        if (summary.getTotalMontantTTC().compareTo(BigDecimal.ZERO) > 0) {
            summary.setPaymentRate(
                    summary.getTotalPaid()
                            .multiply(BigDecimal.valueOf(100))
                            .divide(summary.getTotalMontantTTC(), 2, RoundingMode.HALF_UP)
                            .doubleValue()
            );
        }

        // Calculate invoice payment rate
        if (summary.getTotalInvoices() > 0) {
            // Already have invoice payment rate from invoice summary
        }
    }

    private String determinePeriodType(LocalDate startDate, LocalDate endDate) {
        if (startDate == null || endDate == null) return "ALL_TIME";

        if (startDate.getDayOfMonth() == 1 &&
                endDate.equals(startDate.withDayOfMonth(startDate.lengthOfMonth()))) {
            return "MONTH";
        }

        if (startDate.getDayOfYear() == 1 && endDate.getDayOfYear() == 365) {
            return "YEAR";
        }

        return "CUSTOM";
    }

    private boolean isWithinDateRange(LocalDate date, LocalDate start, LocalDate end) {
        if (date == null) return false;
        return !date.isBefore(start) && !date.isAfter(end);
    }
}