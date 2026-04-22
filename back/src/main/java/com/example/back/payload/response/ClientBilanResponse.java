package com.example.back.payload.response;

import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@Data
public class ClientBilanResponse {

    private Long clientId;
    private String clientCode;
    private String clientName;
    private String clientEmail;
    private String clientPhone;
    private String clientType;
    private LocalDate bilanStartDate;
    private LocalDate bilanEndDate;
    private LocalDate generatedAt;
    private SummaryStats summary;
    private List<ConventionBilan> conventions;
    private PaymentStats paymentStats;
    private FinancialSummary financialSummary;
    private ClientRating rating;
    private List<String> recommendations;

    @Data
    public static class SummaryStats {
        private int totalConventions;
        private int activeConventions;
        private int terminatedConventions;
        private int archivedConventions;
        private int totalApplications;
        private List<String> applicationNames;
    }

    @Data
    public static class ConventionBilan {
        private Long conventionId;
        private String referenceConvention;
        private String libelle;
        private String applicationName;
        private String applicationCode;
        private LocalDate dateDebut;
        private LocalDate dateFin;
        private String etat;
        private BigDecimal montantHT;
        private BigDecimal montantTTC;
        private Long nbUsers;
        private String periodicite;
        private InvoiceStats invoiceStats;
        private List<PaymentRecord> paymentHistory;
        private LatePaymentDetails latePaymentDetails;
    }

    @Data
    public static class InvoiceStats {
        private int totalInvoices;
        private int paidInvoices;
        private int unpaidInvoices;
        private int lateInvoices;
        private int paidOnTimeInvoices;
        private int paidLateInvoices;
        private BigDecimal totalAmount;
        private BigDecimal paidAmount;
        private BigDecimal unpaidAmount;
        private BigDecimal lateAmount;
        private BigDecimal paymentRate;
        private BigDecimal onTimePaymentRate;
    }

    @Data
    public static class PaymentRecord {
        private Long invoiceId;
        private String invoiceNumber;
        private LocalDate invoiceDate;
        private LocalDate dueDate;
        private LocalDate paymentDate;
        private BigDecimal amount;
        private String paymentStatus;
        private String paymentReference;
        private Integer daysLate;
        private String paymentTiming;
    }

    @Data
    public static class LatePaymentDetails {
        private int totalLatePayments;
        private int averageDaysLate;
        private int maxDaysLate;
        private int minDaysLate;
        private List<LatePaymentRecord> worstLatePayments;
        private Map<String, Integer> lateByPeriodicite;
    }

    @Data
    public static class LatePaymentRecord {
        private String invoiceNumber;
        private LocalDate dueDate;
        private LocalDate paymentDate;
        private int daysLate;
        private BigDecimal amount;
        private String conventionReference;
    }

    @Data
    public static class PaymentStats {
        private int totalPayments;
        private int onTimePayments;
        private int latePayments;
        private int advancePayments;
        private BigDecimal onTimePercentage;
        private BigDecimal latePercentage;
        private BigDecimal advancePercentage;
        private String paymentBehavior;
        private String behaviorDescription;
    }

    @Data
    public static class FinancialSummary {
        private BigDecimal totalContractValue;
        private BigDecimal totalPaid;
        private BigDecimal totalUnpaid;
        private BigDecimal totalOverdue;
        private BigDecimal paymentComplianceRate;
        private Map<Integer, BigDecimal> yearlyTotal;
        private Map<Integer, BigDecimal> yearlyPaid;
        private Map<Integer, BigDecimal> yearlyUnpaid;
    }

    @Data
    public static class ClientRating {
        private int overallScore;
        private String rating;
        private String ratingLabel;
        private int paymentScore;
        private int contractComplianceScore;
        private int activityScore;
        private List<String> strengths;
        private List<String> weaknesses;
    }
}