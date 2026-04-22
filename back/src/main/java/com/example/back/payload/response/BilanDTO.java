package com.example.back.payload.response;

import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Data
public class BilanDTO {
    private String title;
    private String periodType; // "MONTH", "YEAR", "ALL_TIME"
    private int year;
    private int month;
    private LocalDate startDate;
    private LocalDate endDate;
    private BilanSummary summary = new BilanSummary();
    private List<BilanItem> items = new ArrayList<>();

    @Data
    public static class BilanSummary {
        // Convention totals
        private int totalConventions = 0;
        private int activeConventions = 0;
        private int terminatedConventions = 0;
        private int archivedConventions = 0;

        // Financial totals
        private BigDecimal totalMontantHT = BigDecimal.ZERO;
        private BigDecimal totalMontantTTC = BigDecimal.ZERO;
        private BigDecimal totalTVA = BigDecimal.ZERO;

        // Payment status
        private BigDecimal totalPaid = BigDecimal.ZERO;
        private BigDecimal totalUnpaid = BigDecimal.ZERO;
        private BigDecimal totalOverdue = BigDecimal.ZERO;

        // Invoice stats
        private int totalInvoices = 0;
        private int paidInvoices = 0;
        private int unpaidInvoices = 0;
        private int overdueInvoices = 0;

        // Payment rate
        private double paymentRate = 0.0;
    }

    @Data
    public static class BilanItem {
        private Long id;
        private String reference;
        private String libelle;
        private String type; // "CONVENTION" or "OLD_CONVENTION"
        private String etat;
        private LocalDate startDate;
        private LocalDate endDate;
        private BigDecimal montantHT;
        private BigDecimal montantTTC;
        private BigDecimal tva;
        private Long nbUsers;
        private String periodicite;
        private Integer renewalVersion;

        // Invoice summary for this convention
        private List<InvoiceBilanItem> invoices = new ArrayList<>();
        private InvoiceSummary invoiceSummary = new InvoiceSummary();

        // Structure info
        private String structureResponsable;
        private String structureBeneficiel;
        private String applicationName;
    }

    @Data
    public static class InvoiceBilanItem {
        private Long id;
        private String numeroFacture;
        private LocalDate dateFacturation;
        private LocalDate dateEcheance;
        private BigDecimal montantHT;
        private BigDecimal montantTTC;
        private String statutPaiement;
        private LocalDate datePaiement;
        private String referencePaiement;
        private boolean isOverdue;
        private String paiementType; // AVANCE, RETARD, PONCTUEL, EN_ATTENTE, EN_RETARD
        private Integer joursRetard;
    }

    @Data
    public static class InvoiceSummary {
        private int total = 0;
        private int paid = 0;
        private int unpaid = 0;
        private int overdue = 0;
        private BigDecimal totalAmount = BigDecimal.ZERO;
        private BigDecimal paidAmount = BigDecimal.ZERO;
        private BigDecimal unpaidAmount = BigDecimal.ZERO;
        private double paymentRate = 0.0;
    }
}