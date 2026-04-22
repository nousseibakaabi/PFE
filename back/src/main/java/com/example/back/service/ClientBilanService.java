package com.example.back.service;

import com.example.back.entity.*;
import com.example.back.payload.request.ClientBilanRequest;
import com.example.back.payload.response.ClientBilanResponse;
import com.example.back.repository.*;
import com.itextpdf.kernel.font.PdfFont;
import com.itextpdf.kernel.font.PdfFontFactory;
import com.itextpdf.kernel.geom.PageSize;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.kernel.pdf.canvas.draw.SolidLine;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.element.Table;
import com.itextpdf.layout.element.Cell;
import com.itextpdf.layout.element.LineSeparator;
import com.itextpdf.layout.properties.TextAlignment;
import com.itextpdf.layout.properties.UnitValue;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ClientBilanService {

    private final StructureRepository structureRepository;
    private final ConventionRepository conventionRepository;
    private final FactureRepository factureRepository;
    private final ApplicationRepository applicationRepository;
    private final HistoryService historyService;

    // ============ MAIN PUBLIC METHODS ============

    @Transactional
    public ClientBilanResponse generateClientBilan(ClientBilanRequest request) {
        log.info("Generating bilan for client ID: {}", request.getStructureBeneficielId());

        Structure client = structureRepository.findById(request.getStructureBeneficielId())
                .orElseThrow(() -> new RuntimeException("Client not found"));

        if (!"Client".equalsIgnoreCase(client.getTypeStructure())) {
            throw new RuntimeException("Structure with ID " + request.getStructureBeneficielId() +
                    " is not a client (typeStructure = " + client.getTypeStructure() + ")");
        }

        List<Convention> conventions;
        if (request.getIncludeArchived()) {
            conventions = conventionRepository.findByStructureBeneficiel(client);
        } else {
            conventions = conventionRepository.findByStructureBeneficielAndArchivedFalse(client);
        }

        if (request.getDateStart() != null) {
            conventions = conventions.stream()
                    .filter(c -> c.getDateDebut() == null || !c.getDateDebut().isBefore(request.getDateStart()))
                    .collect(Collectors.toList());
        }
        if (request.getDateEnd() != null) {
            conventions = conventions.stream()
                    .filter(c -> c.getDateFin() == null || !c.getDateFin().isAfter(request.getDateEnd()))
                    .collect(Collectors.toList());
        }

        ClientBilanResponse response = new ClientBilanResponse();

        response.setClientId(client.getId());
        response.setClientCode(client.getCode());
        response.setClientName(client.getName());
        response.setClientEmail(client.getEmail());
        response.setClientPhone(client.getPhone());
        response.setClientType(client.getTypeStructure());
        response.setBilanStartDate(request.getDateStart());
        response.setBilanEndDate(request.getDateEnd());
        response.setGeneratedAt(LocalDate.now());
        response.setSummary(buildSummaryStats(conventions));

        List<ClientBilanResponse.ConventionBilan> conventionBilans = new ArrayList<>();
        for (Convention convention : conventions) {
            conventionBilans.add(buildConventionBilan(convention));
        }
        response.setConventions(conventionBilans);
        response.setPaymentStats(buildPaymentStats(conventions));
        response.setFinancialSummary(buildFinancialSummary(conventions));

        ClientBilanResponse.ClientRating rating = calculateClientRating(response);
        response.setRating(rating);
        response.setRecommendations(generateRecommendations(rating, response));

        try {
            logBilanGeneration(client);
        } catch (Exception e) {
            log.error("Failed to log bilan generation: {}", e.getMessage());
        }

        return response;
    }

    @Transactional
    public List<ClientBilanResponse> generateAllClientsBilan(LocalDate startDate, LocalDate endDate) {
        List<Structure> clients = structureRepository.findByTypeStructure("Client");
        log.info("Found {} clients to generate bilans for", clients.size());

        List<ClientBilanResponse> bilans = new ArrayList<>();

        for (Structure client : clients) {
            ClientBilanRequest request = new ClientBilanRequest();
            request.setStructureBeneficielId(client.getId());
            request.setDateStart(startDate);
            request.setDateEnd(endDate);
            request.setIncludeArchived(false);

            try {
                bilans.add(generateClientBilan(request));
            } catch (Exception e) {
                log.error("Failed to generate bilan for client {} ({}): {}",
                        client.getId(), client.getName(), e.getMessage());
            }
        }

        bilans.sort((a, b) -> Integer.compare(b.getRating().getOverallScore(), a.getRating().getOverallScore()));
        return bilans;
    }

    @Transactional
    public Page<ClientBilanResponse> getPaginatedClientBilans(Pageable pageable, String searchTerm) {
        List<Structure> clients = structureRepository.findByTypeStructure("Client");

        if (searchTerm != null && !searchTerm.isEmpty()) {
            String searchLower = searchTerm.toLowerCase();
            clients = clients.stream()
                    .filter(c -> c.getName().toLowerCase().contains(searchLower) ||
                            c.getCode().toLowerCase().contains(searchLower) ||
                            (c.getEmail() != null && c.getEmail().toLowerCase().contains(searchLower)))
                    .collect(Collectors.toList());
        }

        List<ClientBilanResponse> bilans = new ArrayList<>();
        for (Structure client : clients) {
            ClientBilanRequest request = new ClientBilanRequest();
            request.setStructureBeneficielId(client.getId());
            request.setIncludeArchived(false);

            try {
                bilans.add(generateClientBilan(request));
            } catch (Exception e) {
                log.error("Failed to generate bilan for client {}: {}", client.getId(), e.getMessage());
            }
        }

        bilans.sort((a, b) -> Integer.compare(b.getRating().getOverallScore(), a.getRating().getOverallScore()));

        int start = (int) pageable.getOffset();
        int end = Math.min((start + pageable.getPageSize()), bilans.size());

        if (start > bilans.size()) {
            return new PageImpl<>(new ArrayList<>(), pageable, bilans.size());
        }

        return new PageImpl<>(bilans.subList(start, end), pageable, bilans.size());
    }

    @Transactional
    public ClientBilanResponse.PaymentStats getClientPaymentStats(Long clientId) {
        Structure client = structureRepository.findById(clientId)
                .orElseThrow(() -> new RuntimeException("Client not found"));

        if (!"Client".equalsIgnoreCase(client.getTypeStructure())) {
            throw new RuntimeException("Structure is not a client");
        }

        ClientBilanRequest request = new ClientBilanRequest();
        request.setStructureBeneficielId(clientId);
        request.setIncludeArchived(true);

        ClientBilanResponse bilan = generateClientBilan(request);
        return bilan.getPaymentStats();
    }

    @Transactional
    public List<ClientBilanResponse> getClientsWithPoorPayment(int minLatePayments, int minDaysLate) {
        List<Structure> clients = structureRepository.findByTypeStructure("Client");
        List<ClientBilanResponse> poorPayers = new ArrayList<>();

        for (Structure client : clients) {
            ClientBilanRequest request = new ClientBilanRequest();
            request.setStructureBeneficielId(client.getId());
            request.setIncludeArchived(true);

            ClientBilanResponse bilan = generateClientBilan(request);
            ClientBilanResponse.PaymentStats stats = bilan.getPaymentStats();

            if (stats.getLatePayments() >= minLatePayments) {
                boolean hasLongLate = false;
                for (ClientBilanResponse.ConventionBilan conv : bilan.getConventions()) {
                    if (conv.getLatePaymentDetails() != null &&
                            conv.getLatePaymentDetails().getMaxDaysLate() >= minDaysLate) {
                        hasLongLate = true;
                        break;
                    }
                }

                if (hasLongLate) {
                    poorPayers.add(bilan);
                }
            }
        }

        poorPayers.sort((a, b) -> Integer.compare(b.getPaymentStats().getLatePayments(),
                a.getPaymentStats().getLatePayments()));

        return poorPayers;
    }

    public byte[] exportBilanToPdf(Long clientId, LocalDate startDate, LocalDate endDate) {
        Structure client = structureRepository.findById(clientId)
                .orElseThrow(() -> new RuntimeException("Client not found"));

        if (!"Client".equalsIgnoreCase(client.getTypeStructure())) {
            throw new RuntimeException("Structure is not a client");
        }

        ClientBilanRequest request = new ClientBilanRequest();
        request.setStructureBeneficielId(clientId);
        request.setDateStart(startDate);
        request.setDateEnd(endDate);

        ClientBilanResponse bilan = generateClientBilan(request);
        return generatePdfFromBilan(bilan, client);
    }

    public byte[] exportBilanToExcel(Long clientId, LocalDate startDate, LocalDate endDate) {
        Structure client = structureRepository.findById(clientId)
                .orElseThrow(() -> new RuntimeException("Client not found"));

        if (!"Client".equalsIgnoreCase(client.getTypeStructure())) {
            throw new RuntimeException("Structure is not a client");
        }

        ClientBilanRequest request = new ClientBilanRequest();
        request.setStructureBeneficielId(clientId);
        request.setDateStart(startDate);
        request.setDateEnd(endDate);

        ClientBilanResponse bilan = generateClientBilan(request);
        return generateExcelFromBilan(bilan, client);
    }

    // ============ PRIVATE HELPER METHODS ============

    private ClientBilanResponse.SummaryStats buildSummaryStats(List<Convention> conventions) {
        ClientBilanResponse.SummaryStats stats = new ClientBilanResponse.SummaryStats();
        stats.setTotalConventions(conventions.size());
        stats.setActiveConventions((int) conventions.stream().filter(c -> "EN COURS".equals(c.getEtat())).count());
        stats.setTerminatedConventions((int) conventions.stream().filter(c -> "TERMINE".equals(c.getEtat())).count());
        stats.setArchivedConventions((int) conventions.stream().filter(c -> Boolean.TRUE.equals(c.getArchived())).count());

        Set<Long> appIds = conventions.stream()
                .map(Convention::getApplication)
                .filter(Objects::nonNull)
                .map(Application::getId)
                .collect(Collectors.toSet());
        stats.setTotalApplications(appIds.size());

        List<String> appNames = conventions.stream()
                .map(Convention::getApplication)
                .filter(Objects::nonNull)
                .map(Application::getName)
                .distinct()
                .collect(Collectors.toList());
        stats.setApplicationNames(appNames);

        return stats;
    }

    private ClientBilanResponse.ConventionBilan buildConventionBilan(Convention convention) {
        ClientBilanResponse.ConventionBilan bilan = new ClientBilanResponse.ConventionBilan();
        bilan.setConventionId(convention.getId());
        bilan.setReferenceConvention(convention.getReferenceConvention());
        bilan.setLibelle(convention.getLibelle());
        bilan.setDateDebut(convention.getDateDebut());
        bilan.setDateFin(convention.getDateFin());
        bilan.setEtat(convention.getEtat());
        bilan.setMontantHT(convention.getMontantHT());
        bilan.setMontantTTC(convention.getMontantTTC());
        bilan.setNbUsers(convention.getNbUsers());
        bilan.setPeriodicite(convention.getPeriodicite());

        if (convention.getApplication() != null) {
            bilan.setApplicationName(convention.getApplication().getName());
            bilan.setApplicationCode(convention.getApplication().getCode());
        }

        List<Facture> factures = factureRepository.findByConventionId(convention.getId());
        bilan.setInvoiceStats(buildInvoiceStats(factures, convention.getMontantTTC()));
        bilan.setPaymentHistory(buildPaymentHistory(factures));
        bilan.setLatePaymentDetails(buildLatePaymentDetails(factures, convention));

        return bilan;
    }

    private ClientBilanResponse.InvoiceStats buildInvoiceStats(List<Facture> factures, BigDecimal totalContractValue) {
        ClientBilanResponse.InvoiceStats stats = new ClientBilanResponse.InvoiceStats();
        stats.setTotalInvoices(factures.size());

        long paid = factures.stream().filter(f -> "PAYE".equals(f.getStatutPaiement())).count();
        long unpaid = factures.stream().filter(f -> "NON_PAYE".equals(f.getStatutPaiement())).count();
        long late = factures.stream().filter(Facture::isEnRetard).count();

        long paidOnTime = 0;
        long paidLate = 0;

        for (Facture f : factures) {
            if ("PAYE".equals(f.getStatutPaiement()) && f.getDatePaiement() != null && f.getDateEcheance() != null) {
                if (!f.getDatePaiement().isAfter(f.getDateEcheance())) {
                    paidOnTime++;
                } else {
                    paidLate++;
                }
            }
        }

        stats.setPaidInvoices((int) paid);
        stats.setUnpaidInvoices((int) unpaid);
        stats.setLateInvoices((int) late);
        stats.setPaidOnTimeInvoices((int) paidOnTime);
        stats.setPaidLateInvoices((int) paidLate);

        BigDecimal totalAmount = factures.stream().map(Facture::getMontantTTC).filter(Objects::nonNull).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal paidAmount = factures.stream().filter(f -> "PAYE".equals(f.getStatutPaiement())).map(Facture::getMontantTTC).filter(Objects::nonNull).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal unpaidAmount = factures.stream().filter(f -> !"PAYE".equals(f.getStatutPaiement())).map(Facture::getMontantTTC).filter(Objects::nonNull).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal lateAmount = factures.stream().filter(Facture::isEnRetard).map(Facture::getMontantTTC).filter(Objects::nonNull).reduce(BigDecimal.ZERO, BigDecimal::add);

        stats.setTotalAmount(totalAmount);
        stats.setPaidAmount(paidAmount);
        stats.setUnpaidAmount(unpaidAmount);
        stats.setLateAmount(lateAmount);

        if (totalContractValue != null && totalContractValue.compareTo(BigDecimal.ZERO) > 0) {
            stats.setPaymentRate(paidAmount.multiply(BigDecimal.valueOf(100)).divide(totalContractValue, 2, RoundingMode.HALF_UP));
        } else if (totalAmount.compareTo(BigDecimal.ZERO) > 0) {
            stats.setPaymentRate(paidAmount.multiply(BigDecimal.valueOf(100)).divide(totalAmount, 2, RoundingMode.HALF_UP));
        } else {
            stats.setPaymentRate(BigDecimal.ZERO);
        }

        if (paid > 0) {
            stats.setOnTimePaymentRate(BigDecimal.valueOf(paidOnTime).multiply(BigDecimal.valueOf(100)).divide(BigDecimal.valueOf(paid), 2, RoundingMode.HALF_UP));
        } else {
            stats.setOnTimePaymentRate(BigDecimal.ZERO);
        }

        return stats;
    }

    private List<ClientBilanResponse.PaymentRecord> buildPaymentHistory(List<Facture> factures) {
        List<ClientBilanResponse.PaymentRecord> records = new ArrayList<>();

        for (Facture facture : factures) {
            ClientBilanResponse.PaymentRecord record = new ClientBilanResponse.PaymentRecord();
            record.setInvoiceId(facture.getId());
            record.setInvoiceNumber(facture.getNumeroFacture());
            record.setInvoiceDate(facture.getDateFacturation());
            record.setDueDate(facture.getDateEcheance());
            record.setPaymentDate(facture.getDatePaiement());
            record.setAmount(facture.getMontantTTC());
            record.setPaymentStatus(facture.getStatutPaiement());
            record.setPaymentReference(facture.getReferencePaiement());

            if (facture.getDatePaiement() != null && facture.getDateEcheance() != null) {
                long daysLate = ChronoUnit.DAYS.between(facture.getDateEcheance(), facture.getDatePaiement());
                record.setDaysLate((int) daysLate);
                if (daysLate < 0) record.setPaymentTiming("ADVANCE");
                else if (daysLate == 0) record.setPaymentTiming("ON_TIME");
                else record.setPaymentTiming("LATE");
            } else if ("PAYE".equals(facture.getStatutPaiement())) {
                record.setPaymentTiming("ON_TIME");
                record.setDaysLate(0);
            } else {
                record.setPaymentTiming("PENDING");
            }
            records.add(record);
        }

        records.sort((a, b) -> {
            if (a.getPaymentDate() == null && b.getPaymentDate() == null) return 0;
            if (a.getPaymentDate() == null) return 1;
            if (b.getPaymentDate() == null) return -1;
            return b.getPaymentDate().compareTo(a.getPaymentDate());
        });

        return records;
    }

    private ClientBilanResponse.LatePaymentDetails buildLatePaymentDetails(List<Facture> factures, Convention convention) {
        ClientBilanResponse.LatePaymentDetails details = new ClientBilanResponse.LatePaymentDetails();

        List<Facture> paidLate = factures.stream()
                .filter(f -> "PAYE".equals(f.getStatutPaiement()))
                .filter(f -> f.getDatePaiement() != null && f.getDateEcheance() != null)
                .filter(f -> f.getDatePaiement().isAfter(f.getDateEcheance()))
                .collect(Collectors.toList());

        details.setTotalLatePayments(paidLate.size());

        if (!paidLate.isEmpty()) {
            double avgDays = paidLate.stream().mapToLong(f -> ChronoUnit.DAYS.between(f.getDateEcheance(), f.getDatePaiement())).average().orElse(0);
            details.setAverageDaysLate((int) Math.round(avgDays));
            int maxDays = paidLate.stream().mapToInt(f -> (int) ChronoUnit.DAYS.between(f.getDateEcheance(), f.getDatePaiement())).max().orElse(0);
            details.setMaxDaysLate(maxDays);
            int minDays = paidLate.stream().mapToInt(f -> (int) ChronoUnit.DAYS.between(f.getDateEcheance(), f.getDatePaiement())).min().orElse(0);
            details.setMinDaysLate(minDays);

            List<ClientBilanResponse.LatePaymentRecord> worst = paidLate.stream()
                    .sorted((a, b) -> Long.compare(ChronoUnit.DAYS.between(b.getDateEcheance(), b.getDatePaiement()), ChronoUnit.DAYS.between(a.getDateEcheance(), a.getDatePaiement())))
                    .limit(5)
                    .map(f -> {
                        ClientBilanResponse.LatePaymentRecord record = new ClientBilanResponse.LatePaymentRecord();
                        record.setInvoiceNumber(f.getNumeroFacture());
                        record.setDueDate(f.getDateEcheance());
                        record.setPaymentDate(f.getDatePaiement());
                        record.setDaysLate((int) ChronoUnit.DAYS.between(f.getDateEcheance(), f.getDatePaiement()));
                        record.setAmount(f.getMontantTTC());
                        record.setConventionReference(convention.getReferenceConvention());
                        return record;
                    })
                    .collect(Collectors.toList());
            details.setWorstLatePayments(worst);

            Map<String, Integer> lateByPeriodicite = new HashMap<>();
            for (Facture f : paidLate) {
                String periodicite = convention.getPeriodicite() != null ? convention.getPeriodicite() : "UNKNOWN";
                lateByPeriodicite.merge(periodicite, 1, Integer::sum);
            }
            details.setLateByPeriodicite(lateByPeriodicite);
        } else {
            details.setAverageDaysLate(0);
            details.setMaxDaysLate(0);
            details.setMinDaysLate(0);
            details.setWorstLatePayments(new ArrayList<>());
            details.setLateByPeriodicite(new HashMap<>());
        }

        return details;
    }

    private ClientBilanResponse.PaymentStats buildPaymentStats(List<Convention> conventions) {
        ClientBilanResponse.PaymentStats stats = new ClientBilanResponse.PaymentStats();
        int totalPayments = 0, onTime = 0, late = 0, advance = 0;

        for (Convention convention : conventions) {
            List<Facture> factures = factureRepository.findByConventionId(convention.getId());
            for (Facture facture : factures) {
                if ("PAYE".equals(facture.getStatutPaiement())) {
                    totalPayments++;
                    if (facture.getDatePaiement() != null && facture.getDateEcheance() != null) {
                        long daysDiff = ChronoUnit.DAYS.between(facture.getDateEcheance(), facture.getDatePaiement());
                        if (daysDiff < 0) advance++;
                        else if (daysDiff == 0) onTime++;
                        else late++;
                    } else {
                        onTime++;
                    }
                }
            }
        }

        stats.setTotalPayments(totalPayments);
        stats.setOnTimePayments(onTime);
        stats.setLatePayments(late);
        stats.setAdvancePayments(advance);

        if (totalPayments > 0) {
            stats.setOnTimePercentage(BigDecimal.valueOf(onTime).multiply(BigDecimal.valueOf(100)).divide(BigDecimal.valueOf(totalPayments), 2, RoundingMode.HALF_UP));
            stats.setLatePercentage(BigDecimal.valueOf(late).multiply(BigDecimal.valueOf(100)).divide(BigDecimal.valueOf(totalPayments), 2, RoundingMode.HALF_UP));
            stats.setAdvancePercentage(BigDecimal.valueOf(advance).multiply(BigDecimal.valueOf(100)).divide(BigDecimal.valueOf(totalPayments), 2, RoundingMode.HALF_UP));
        } else {
            stats.setOnTimePercentage(BigDecimal.ZERO);
            stats.setLatePercentage(BigDecimal.ZERO);
            stats.setAdvancePercentage(BigDecimal.ZERO);
        }

        if (totalPayments == 0) {
            stats.setPaymentBehavior("NO_PAYMENTS");
            stats.setBehaviorDescription("Aucun paiement enregistré");
        } else if (late == 0 && advance >= 0) {
            stats.setPaymentBehavior("EXCELLENT");
            stats.setBehaviorDescription("Excellent - Tous les paiements sont à temps ou en avance");
        } else if (late <= totalPayments * 0.1) {
            stats.setPaymentBehavior("GOOD");
            stats.setBehaviorDescription("Bon - Moins de 10% de retards");
        } else if (late <= totalPayments * 0.25) {
            stats.setPaymentBehavior("AVERAGE");
            stats.setBehaviorDescription("Moyen - Entre 10% et 25% de retards");
        } else if (late <= totalPayments * 0.5) {
            stats.setPaymentBehavior("POOR");
            stats.setBehaviorDescription("Médiocre - Entre 25% et 50% de retards");
        } else {
            stats.setPaymentBehavior("VERY_POOR");
            stats.setBehaviorDescription("Très mauvais - Plus de 50% de retards");
        }

        return stats;
    }

    private ClientBilanResponse.FinancialSummary buildFinancialSummary(List<Convention> conventions) {
        ClientBilanResponse.FinancialSummary summary = new ClientBilanResponse.FinancialSummary();
        BigDecimal totalContractValue = BigDecimal.ZERO, totalPaid = BigDecimal.ZERO, totalUnpaid = BigDecimal.ZERO, totalOverdue = BigDecimal.ZERO;
        Map<Integer, BigDecimal> yearlyTotal = new TreeMap<>(), yearlyPaid = new TreeMap<>(), yearlyUnpaid = new TreeMap<>();

        for (Convention convention : conventions) {
            BigDecimal contractValue = convention.getMontantTTC() != null ? convention.getMontantTTC() : BigDecimal.ZERO;
            totalContractValue = totalContractValue.add(contractValue);

            List<Facture> factures = factureRepository.findByConventionId(convention.getId());
            BigDecimal paidAmount = factures.stream().filter(f -> "PAYE".equals(f.getStatutPaiement())).map(Facture::getMontantTTC).filter(Objects::nonNull).reduce(BigDecimal.ZERO, BigDecimal::add);
            BigDecimal unpaidAmount = factures.stream().filter(f -> !"PAYE".equals(f.getStatutPaiement())).map(Facture::getMontantTTC).filter(Objects::nonNull).reduce(BigDecimal.ZERO, BigDecimal::add);
            BigDecimal overdueAmount = factures.stream().filter(Facture::isEnRetard).map(Facture::getMontantTTC).filter(Objects::nonNull).reduce(BigDecimal.ZERO, BigDecimal::add);

            totalPaid = totalPaid.add(paidAmount);
            totalUnpaid = totalUnpaid.add(unpaidAmount);
            totalOverdue = totalOverdue.add(overdueAmount);

            if (convention.getDateDebut() != null) {
                int year = convention.getDateDebut().getYear();
                yearlyTotal.merge(year, contractValue, BigDecimal::add);
                yearlyPaid.merge(year, paidAmount, BigDecimal::add);
                yearlyUnpaid.merge(year, unpaidAmount, BigDecimal::add);
            }
        }

        summary.setTotalContractValue(totalContractValue);
        summary.setTotalPaid(totalPaid);
        summary.setTotalUnpaid(totalUnpaid);
        summary.setTotalOverdue(totalOverdue);

        if (totalContractValue.compareTo(BigDecimal.ZERO) > 0) {
            summary.setPaymentComplianceRate(totalPaid.multiply(BigDecimal.valueOf(100)).divide(totalContractValue, 2, RoundingMode.HALF_UP));
        } else {
            summary.setPaymentComplianceRate(BigDecimal.ZERO);
        }

        summary.setYearlyTotal(yearlyTotal);
        summary.setYearlyPaid(yearlyPaid);
        summary.setYearlyUnpaid(yearlyUnpaid);

        return summary;
    }

    private ClientBilanResponse.ClientRating calculateClientRating(ClientBilanResponse response) {
        ClientBilanResponse.ClientRating rating = new ClientBilanResponse.ClientRating();
        ClientBilanResponse.PaymentStats paymentStats = response.getPaymentStats();

        int paymentScore = 0;
        if (paymentStats.getTotalPayments() > 0) {
            double onTimePct = paymentStats.getOnTimePercentage().doubleValue();
            paymentScore += (int) (onTimePct * 0.2);
            if (paymentStats.getLatePayments() == 0) paymentScore += 10;
            else if (paymentStats.getLatePercentage().doubleValue() <= 10) paymentScore += 5;
            if (paymentStats.getAdvancePercentage().doubleValue() >= 20) paymentScore += 5;
        } else {
            paymentScore = 20;
        }
        paymentScore = Math.min(paymentScore, 40);

        int complianceScore = 0;
        ClientBilanResponse.FinancialSummary financial = response.getFinancialSummary();
        if (financial.getTotalContractValue().compareTo(BigDecimal.ZERO) > 0) {
            complianceScore = (int) (financial.getPaymentComplianceRate().doubleValue() * 0.3);
        } else {
            complianceScore = 15;
        }
        complianceScore = Math.min(complianceScore, 30);

        int activityScore = 0;
        ClientBilanResponse.SummaryStats summary = response.getSummary();
        if (summary.getTotalConventions() >= 5) activityScore = 30;
        else if (summary.getTotalConventions() >= 3) activityScore = 20;
        else if (summary.getTotalConventions() >= 1) activityScore = 10;
        if (summary.getActiveConventions() > 0) activityScore += Math.min(10, summary.getActiveConventions() * 2);
        activityScore = Math.min(activityScore, 30);

        int totalScore = paymentScore + complianceScore + activityScore;
        rating.setOverallScore(totalScore);

        if (totalScore >= 90) { rating.setRating("A"); rating.setRatingLabel("Excellent"); }
        else if (totalScore >= 75) { rating.setRating("B"); rating.setRatingLabel("Bon"); }
        else if (totalScore >= 60) { rating.setRating("C"); rating.setRatingLabel("Moyen"); }
        else if (totalScore >= 40) { rating.setRating("D"); rating.setRatingLabel("Médiocre"); }
        else { rating.setRating("F"); rating.setRatingLabel("Critique"); }

        rating.setPaymentScore(paymentScore);
        rating.setContractComplianceScore(complianceScore);
        rating.setActivityScore(activityScore);

        List<String> strengths = new ArrayList<>(), weaknesses = new ArrayList<>();

        if (paymentStats.getLatePayments() == 0 && paymentStats.getTotalPayments() > 0) strengths.add("Excellent historique de paiement - Aucun retard");
        else if (paymentStats.getLatePercentage().doubleValue() <= 10) strengths.add("Très bon historique de paiement - Moins de 10% de retards");
        else if (paymentStats.getLatePercentage().doubleValue() > 50) weaknesses.add("Mauvais historique de paiement - Plus de 50% de paiements en retard");
        else if (paymentStats.getLatePercentage().doubleValue() > 25) weaknesses.add("Historique de paiement à améliorer - " + String.format("%.1f", paymentStats.getLatePercentage()) + "% de retards");

        if (paymentStats.getAdvancePercentage().doubleValue() >= 20) strengths.add("Paiements fréquents en avance (" + String.format("%.0f", paymentStats.getAdvancePercentage()) + "%)");

        if (summary.getTotalConventions() >= 5) strengths.add("Client fidèle - " + summary.getTotalConventions() + " conventions signées");
        else if (summary.getTotalConventions() == 0) weaknesses.add("Aucune convention active - Client inactif");
        if (summary.getActiveConventions() > 0) strengths.add(summary.getActiveConventions() + " convention(s) actuellement en cours");

        if (financial.getTotalOverdue().compareTo(BigDecimal.ZERO) > 0) weaknesses.add("Montant impayé important: " + String.format("%.2f", financial.getTotalOverdue()) + " TND en souffrance");

        if (financial.getPaymentComplianceRate().doubleValue() >= 95) strengths.add("Excellent taux de conformité de paiement (" + String.format("%.1f", financial.getPaymentComplianceRate()) + "%)");
        else if (financial.getPaymentComplianceRate().doubleValue() < 70 && financial.getTotalContractValue().compareTo(BigDecimal.ZERO) > 0) weaknesses.add("Faible taux de conformité de paiement (" + String.format("%.1f", financial.getPaymentComplianceRate()) + "%)");

        rating.setStrengths(strengths);
        rating.setWeaknesses(weaknesses);

        return rating;
    }

    private List<String> generateRecommendations(ClientBilanResponse.ClientRating rating, ClientBilanResponse response) {
        List<String> recommendations = new ArrayList<>();

        switch (rating.getRating()) {
            case "A":
                recommendations.add("✓ Excellent client - Maintenir la relation privilégiée");
                recommendations.add("✓ Proposer des renouvellements anticipés");
                recommendations.add("✓ Envisager des conditions commerciales préférentielles");
                break;
            case "B":
                recommendations.add("✓ Bon client - Programmer des rendez-vous de suivi réguliers");
                recommendations.add("✓ Surveiller les échéances de paiement");
                break;
            case "C":
                recommendations.add("⚠️ Client moyen - Renforcer le suivi des paiements");
                recommendations.add("⚠️ Mettre en place des relances automatiques");
                recommendations.add("⚠️ Réviser les conditions de paiement si nécessaire");
                break;
            case "D":
                recommendations.add("⚠️ Client à risque - Contacter immédiatement pour les impayés");
                recommendations.add("⚠️ Suspendre les nouveaux services jusqu'à régularisation");
                recommendations.add("⚠️ Envisager un échéancier de paiement");
                break;
            case "F":
                recommendations.add("🔴 Client critique - Action immédiate requise");
                recommendations.add("🔴 Bloquer l'accès aux services");
                recommendations.add("🔴 Engager les procédures de recouvrement");
                recommendations.add("🔴 Alerter la direction");
                break;
        }

        ClientBilanResponse.PaymentStats paymentStats = response.getPaymentStats();
        if (paymentStats.getLatePayments() > 0) {
            recommendations.add("Mettre en place des rappels de paiement 5 jours avant échéance");
            if (paymentStats.getLatePayments() > 3) recommendations.add("Demander un rendez-vous avec la direction financière du client");
        }

        ClientBilanResponse.FinancialSummary financial = response.getFinancialSummary();
        if (financial.getTotalOverdue().compareTo(BigDecimal.valueOf(10000)) > 0) recommendations.add("Envoyer une mise en demeure pour les impayés > 10,000 TND");

        if (response.getSummary().getTotalConventions() == 0) {
            recommendations.add("Prospect - Planifier une rencontre commerciale");
            recommendations.add("Présenter l'offre de services");
        }

        return recommendations;
    }

    private void logBilanGeneration(Structure client) {
        try {
            if (historyService != null) {
                historyService.logBilanGeneration(client, LocalDateTime.now());
            }
            log.info("Bilan generated for client: {} ({})", client.getName(), client.getCode());
        } catch (Exception e) {
            log.error("Failed to log bilan generation: {}", e.getMessage());
        }
    }

    // ============ PDF GENERATION ============

    private byte[] generatePdfFromBilan(ClientBilanResponse bilan, Structure client) {
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            PdfWriter writer = new PdfWriter(baos);
            PdfDocument pdfDoc = new PdfDocument(writer);
            Document document = new Document(pdfDoc, PageSize.A4);
            document.setMargins(50, 50, 50, 50);

            PdfFont boldFont = PdfFontFactory.createFont();
            PdfFont regularFont = PdfFontFactory.createFont();

            // Header
            Paragraph title = new Paragraph("BILAN FINANCIER CLIENT")
                    .setFont(boldFont).setFontSize(20).setTextAlignment(TextAlignment.CENTER).setMarginBottom(20);
            document.add(title);

            Table infoTable = new Table(UnitValue.createPercentArray(new float[]{30, 70}));
            infoTable.setWidth(UnitValue.createPercentValue(100)).setMarginBottom(15);
            addPdfInfoRow(infoTable, "Client:", client.getName(), boldFont, regularFont);
            addPdfInfoRow(infoTable, "Code:", client.getCode(), boldFont, regularFont);
            addPdfInfoRow(infoTable, "Email:", client.getEmail() != null ? client.getEmail() : "Non renseigné", boldFont, regularFont);
            addPdfInfoRow(infoTable, "Téléphone:", client.getPhone() != null ? client.getPhone() : "Non renseigné", boldFont, regularFont);
            addPdfInfoRow(infoTable, "Type:", client.getTypeStructure() != null ? client.getTypeStructure() : "Client", boldFont, regularFont);
            document.add(infoTable);

            String periodText = "Période d'analyse: ";
            if (bilan.getBilanStartDate() != null && bilan.getBilanEndDate() != null) periodText += formatDate(bilan.getBilanStartDate()) + " - " + formatDate(bilan.getBilanEndDate());
            else if (bilan.getBilanStartDate() != null) periodText += "Depuis le " + formatDate(bilan.getBilanStartDate());
            else if (bilan.getBilanEndDate() != null) periodText += "Jusqu'au " + formatDate(bilan.getBilanEndDate());
            else periodText += "Toute la période";
            document.add(new Paragraph(periodText).setFont(regularFont).setFontSize(10).setTextAlignment(TextAlignment.CENTER).setMarginBottom(20));
            document.add(new LineSeparator(new SolidLine()));
            // Summary Section
            document.add(new Paragraph("1. RÉSUMÉ GÉNÉRAL").setFont(boldFont).setFontSize(14).setMarginTop(15).setMarginBottom(10));
            Table summaryTable = new Table(UnitValue.createPercentArray(new float[]{50, 50})).setWidth(UnitValue.createPercentValue(100));
            addPdfSummaryRow(summaryTable, "Total conventions:", String.valueOf(bilan.getSummary().getTotalConventions()), boldFont, regularFont);
            addPdfSummaryRow(summaryTable, "Conventions actives:", String.valueOf(bilan.getSummary().getActiveConventions()), boldFont, regularFont);
            addPdfSummaryRow(summaryTable, "Conventions terminées:", String.valueOf(bilan.getSummary().getTerminatedConventions()), boldFont, regularFont);
            addPdfSummaryRow(summaryTable, "Conventions archivées:", String.valueOf(bilan.getSummary().getArchivedConventions()), boldFont, regularFont);
            addPdfSummaryRow(summaryTable, "Applications concernées:", String.valueOf(bilan.getSummary().getTotalApplications()), boldFont, regularFont);
            document.add(summaryTable);

            // Payment Stats Section
            document.add(new Paragraph("2. STATISTIQUES DE PAIEMENT").setFont(boldFont).setFontSize(14).setMarginTop(15).setMarginBottom(10));
            Table paymentTable = new Table(UnitValue.createPercentArray(new float[]{50, 50})).setWidth(UnitValue.createPercentValue(100));
            addPdfSummaryRow(paymentTable, "Total paiements:", String.valueOf(bilan.getPaymentStats().getTotalPayments()), boldFont, regularFont);
            addPdfSummaryRow(paymentTable, "Paiements à temps:", bilan.getPaymentStats().getOnTimePayments() + " (" + bilan.getPaymentStats().getOnTimePercentage() + "%)", boldFont, regularFont);
            addPdfSummaryRow(paymentTable, "Paiements en retard:", bilan.getPaymentStats().getLatePayments() + " (" + bilan.getPaymentStats().getLatePercentage() + "%)", boldFont, regularFont);
            addPdfSummaryRow(paymentTable, "Paiements en avance:", bilan.getPaymentStats().getAdvancePayments() + " (" + bilan.getPaymentStats().getAdvancePercentage() + "%)", boldFont, regularFont);
            addPdfSummaryRow(paymentTable, "Comportement:", bilan.getPaymentStats().getBehaviorDescription(), boldFont, regularFont);
            document.add(paymentTable);

            // Financial Summary Section
            document.add(new Paragraph("3. RÉSUMÉ FINANCIER").setFont(boldFont).setFontSize(14).setMarginTop(15).setMarginBottom(10));
            Table financialTable = new Table(UnitValue.createPercentArray(new float[]{50, 50})).setWidth(UnitValue.createPercentValue(100));
            addPdfSummaryRow(financialTable, "Valeur totale des contrats:", formatCurrency(bilan.getFinancialSummary().getTotalContractValue()), boldFont, regularFont);
            addPdfSummaryRow(financialTable, "Total payé:", formatCurrency(bilan.getFinancialSummary().getTotalPaid()), boldFont, regularFont);
            addPdfSummaryRow(financialTable, "Total impayé:", formatCurrency(bilan.getFinancialSummary().getTotalUnpaid()), boldFont, regularFont);
            addPdfSummaryRow(financialTable, "Total en souffrance:", formatCurrency(bilan.getFinancialSummary().getTotalOverdue()), boldFont, regularFont);
            addPdfSummaryRow(financialTable, "Taux de conformité:", bilan.getFinancialSummary().getPaymentComplianceRate() + "%", boldFont, regularFont);
            document.add(financialTable);

            // Rating Section
            document.add(new Paragraph("4. ÉVALUATION ET RECOMMANDATIONS").setFont(boldFont).setFontSize(14).setMarginTop(15).setMarginBottom(10));
            Table ratingTable = new Table(UnitValue.createPercentArray(new float[]{40, 60})).setWidth(UnitValue.createPercentValue(100));
            addPdfSummaryRow(ratingTable, "Score global:", bilan.getRating().getOverallScore() + "/100", boldFont, regularFont);
            addPdfSummaryRow(ratingTable, "Note:", bilan.getRating().getRating() + " - " + bilan.getRating().getRatingLabel(), boldFont, regularFont);
            addPdfSummaryRow(ratingTable, "Score paiement:", bilan.getRating().getPaymentScore() + "/40", boldFont, regularFont);
            addPdfSummaryRow(ratingTable, "Score conformité:", bilan.getRating().getContractComplianceScore() + "/30", boldFont, regularFont);
            addPdfSummaryRow(ratingTable, "Score activité:", bilan.getRating().getActivityScore() + "/30", boldFont, regularFont);
            document.add(ratingTable);

            // Footer
            document.add(new Paragraph("Document généré automatiquement le " + LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy à HH:mm:ss")))
                    .setFontSize(8).setTextAlignment(TextAlignment.CENTER).setMarginTop(30));

            document.close();
            return baos.toByteArray();
        } catch (Exception e) {
            log.error("Error generating PDF: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to generate PDF: " + e.getMessage());
        }
    }

    private void addPdfInfoRow(Table table, String label, String value, PdfFont boldFont, PdfFont regularFont) {
        table.addCell(new Cell().add(new Paragraph(label).setFont(boldFont)));
        table.addCell(new Cell().add(new Paragraph(value).setFont(regularFont)));
    }

    private void addPdfSummaryRow(Table table, String label, String value, PdfFont boldFont, PdfFont regularFont) {
        table.addCell(new Cell().add(new Paragraph(label).setFont(boldFont)));
        table.addCell(new Cell().add(new Paragraph(value).setFont(regularFont)));
    }

    // ============ EXCEL GENERATION ============

    private byte[] generateExcelFromBilan(ClientBilanResponse bilan, Structure client) {
        try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            CellStyle headerStyle = createExcelHeaderStyle(workbook);
            CellStyle titleStyle = createExcelTitleStyle(workbook);
            CellStyle boldStyle = createExcelBoldStyle(workbook);
            CellStyle currencyStyle = createExcelCurrencyStyle(workbook);
            CellStyle dateStyle = createExcelDateStyle(workbook);

            // Sheet 1: Client Info
            Sheet infoSheet = workbook.createSheet("Informations Client");
            int rowNum = 0;
            Row titleRow = infoSheet.createRow(rowNum++);
            org.apache.poi.ss.usermodel.Cell titleCell = titleRow.createCell(0);
            titleCell.setCellValue("BILAN FINANCIER CLIENT");
            titleCell.setCellStyle(titleStyle);
            infoSheet.addMergedRegion(new CellRangeAddress(0, 0, 0, 2));
            rowNum++;

            Row headerRow = infoSheet.createRow(rowNum++);
            org.apache.poi.ss.usermodel.Cell headerCell = headerRow.createCell(0);
            headerCell.setCellValue("INFORMATIONS CLIENT");
            headerCell.setCellStyle(headerStyle);
            infoSheet.addMergedRegion(new CellRangeAddress(rowNum - 1, rowNum - 1, 0, 2));

            addExcelRow(infoSheet, rowNum++, "Nom du client:", client.getName(), boldStyle);
            addExcelRow(infoSheet, rowNum++, "Code client:", client.getCode(), boldStyle);
            addExcelRow(infoSheet, rowNum++, "Email:", client.getEmail() != null ? client.getEmail() : "Non renseigné", boldStyle);
            addExcelRow(infoSheet, rowNum++, "Téléphone:", client.getPhone() != null ? client.getPhone() : "Non renseigné", boldStyle);
            addExcelRow(infoSheet, rowNum++, "Type:", client.getTypeStructure() != null ? client.getTypeStructure() : "Client", boldStyle);
            rowNum++;

            Row periodHeaderRow = infoSheet.createRow(rowNum++);
            org.apache.poi.ss.usermodel.Cell periodHeaderCell = periodHeaderRow.createCell(0);
            periodHeaderCell.setCellValue("PÉRIODE D'ANALYSE");
            periodHeaderCell.setCellStyle(headerStyle);
            infoSheet.addMergedRegion(new CellRangeAddress(rowNum - 1, rowNum - 1, 0, 2));

            String periodText = "";
            if (bilan.getBilanStartDate() != null && bilan.getBilanEndDate() != null) periodText = formatDate(bilan.getBilanStartDate()) + " - " + formatDate(bilan.getBilanEndDate());
            else if (bilan.getBilanStartDate() != null) periodText = "Depuis le " + formatDate(bilan.getBilanStartDate());
            else if (bilan.getBilanEndDate() != null) periodText = "Jusqu'au " + formatDate(bilan.getBilanEndDate());
            else periodText = "Toute la période";
            addExcelRow(infoSheet, rowNum++, "Période:", periodText, boldStyle);
            addExcelRow(infoSheet, rowNum++, "Généré le:", formatDate(bilan.getGeneratedAt()), boldStyle);
            rowNum++;

            Row summaryHeaderRow = infoSheet.createRow(rowNum++);
            org.apache.poi.ss.usermodel.Cell summaryHeaderCell = summaryHeaderRow.createCell(0);
            summaryHeaderCell.setCellValue("RÉSUMÉ STATISTIQUES");
            summaryHeaderCell.setCellStyle(headerStyle);
            infoSheet.addMergedRegion(new CellRangeAddress(rowNum - 1, rowNum - 1, 0, 2));

            addExcelRow(infoSheet, rowNum++, "Total conventions:", String.valueOf(bilan.getSummary().getTotalConventions()), boldStyle);
            addExcelRow(infoSheet, rowNum++, "Conventions actives:", String.valueOf(bilan.getSummary().getActiveConventions()), boldStyle);
            addExcelRow(infoSheet, rowNum++, "Conventions terminées:", String.valueOf(bilan.getSummary().getTerminatedConventions()), boldStyle);
            addExcelRow(infoSheet, rowNum++, "Conventions archivées:", String.valueOf(bilan.getSummary().getArchivedConventions()), boldStyle);
            addExcelRow(infoSheet, rowNum++, "Applications concernées:", String.valueOf(bilan.getSummary().getTotalApplications()), boldStyle);

            // Sheet 2: Conventions
            Sheet convSheet = workbook.createSheet("Conventions");
            rowNum = 0;
            org.apache.poi.ss.usermodel.Row convHeaderRow = convSheet.createRow(rowNum++);
            String[] convHeaders = {"Référence", "Libellé", "Application", "Date Début", "Date Fin", "Statut", "Montant TTC", "Périodicité", "Factures", "Montant Payé"};
            for (int i = 0; i < convHeaders.length; i++) {
                org.apache.poi.ss.usermodel.Cell cell = convHeaderRow.createCell(i);
                cell.setCellValue(convHeaders[i]);
                cell.setCellStyle(headerStyle);
            }
            for (ClientBilanResponse.ConventionBilan conv : bilan.getConventions()) {
                org.apache.poi.ss.usermodel.Row row = convSheet.createRow(rowNum++);
                row.createCell(0).setCellValue(conv.getReferenceConvention());
                row.createCell(1).setCellValue(conv.getLibelle());
                row.createCell(2).setCellValue(conv.getApplicationName() != null ? conv.getApplicationName() : "N/A");
                if (conv.getDateDebut() != null) row.createCell(3).setCellValue(java.sql.Date.valueOf(conv.getDateDebut()));
                if (conv.getDateFin() != null) row.createCell(4).setCellValue(java.sql.Date.valueOf(conv.getDateFin()));
                row.createCell(5).setCellValue(conv.getEtat());
                if (conv.getMontantTTC() != null) row.createCell(6).setCellValue(conv.getMontantTTC().doubleValue());
                row.createCell(7).setCellValue(conv.getPeriodicite() != null ? conv.getPeriodicite() : "N/A");
                if (conv.getInvoiceStats() != null) {
                    row.createCell(8).setCellValue(conv.getInvoiceStats().getPaidInvoices() + "/" + conv.getInvoiceStats().getTotalInvoices());
                    if (conv.getInvoiceStats().getPaidAmount() != null) row.createCell(9).setCellValue(conv.getInvoiceStats().getPaidAmount().doubleValue());
                }
            }

            // Sheet 3: Financial Summary
            Sheet financialSheet = workbook.createSheet("Résumé Financier");
            rowNum = 0;
            Row financialTitleRow = financialSheet.createRow(rowNum++);
            org.apache.poi.ss.usermodel.Cell financialTitleCell = financialTitleRow.createCell(0);
            financialTitleCell.setCellValue("RÉSUMÉ FINANCIER");
            financialTitleCell.setCellStyle(headerStyle);
            financialSheet.addMergedRegion(new CellRangeAddress(rowNum - 1, rowNum - 1, 0, 1));
            rowNum++;

            addExcelRow(financialSheet, rowNum++, "Valeur totale des contrats:", formatCurrency(bilan.getFinancialSummary().getTotalContractValue()), boldStyle);
            addExcelRow(financialSheet, rowNum++, "Total payé:", formatCurrency(bilan.getFinancialSummary().getTotalPaid()), boldStyle);
            addExcelRow(financialSheet, rowNum++, "Total impayé:", formatCurrency(bilan.getFinancialSummary().getTotalUnpaid()), boldStyle);
            addExcelRow(financialSheet, rowNum++, "Total en souffrance:", formatCurrency(bilan.getFinancialSummary().getTotalOverdue()), boldStyle);
            addExcelRow(financialSheet, rowNum++, "Taux de conformité:", bilan.getFinancialSummary().getPaymentComplianceRate() + "%", boldStyle);
            rowNum++;

            Row paymentHeaderRow2 = financialSheet.createRow(rowNum++);
            org.apache.poi.ss.usermodel.Cell paymentHeaderCell2 = paymentHeaderRow2.createCell(0);
            paymentHeaderCell2.setCellValue("STATISTIQUES DE PAIEMENT");
            paymentHeaderCell2.setCellStyle(headerStyle);
            financialSheet.addMergedRegion(new CellRangeAddress(rowNum - 1, rowNum - 1, 0, 1));

            addExcelRow(financialSheet, rowNum++, "Total paiements:", String.valueOf(bilan.getPaymentStats().getTotalPayments()), boldStyle);
            addExcelRow(financialSheet, rowNum++, "Paiements à temps:", bilan.getPaymentStats().getOnTimePayments() + " (" + bilan.getPaymentStats().getOnTimePercentage() + "%)", boldStyle);
            addExcelRow(financialSheet, rowNum++, "Paiements en retard:", bilan.getPaymentStats().getLatePayments() + " (" + bilan.getPaymentStats().getLatePercentage() + "%)", boldStyle);
            addExcelRow(financialSheet, rowNum++, "Paiements en avance:", bilan.getPaymentStats().getAdvancePayments() + " (" + bilan.getPaymentStats().getAdvancePercentage() + "%)", boldStyle);
            addExcelRow(financialSheet, rowNum++, "Comportement:", bilan.getPaymentStats().getBehaviorDescription(), boldStyle);

            // Sheet 4: Rating
            Sheet ratingSheet = workbook.createSheet("Évaluation");
            rowNum = 0;
            Row ratingTitleRow = ratingSheet.createRow(rowNum++);
            org.apache.poi.ss.usermodel.Cell ratingTitleCell = ratingTitleRow.createCell(0);
            ratingTitleCell.setCellValue("NOTE GLOBALE");
            ratingTitleCell.setCellStyle(headerStyle);
            ratingSheet.addMergedRegion(new CellRangeAddress(rowNum - 1, rowNum - 1, 0, 1));

            addExcelRow(ratingSheet, rowNum++, "Score:", bilan.getRating().getOverallScore() + "/100", boldStyle);
            addExcelRow(ratingSheet, rowNum++, "Note:", bilan.getRating().getRating() + " - " + bilan.getRating().getRatingLabel(), boldStyle);
            rowNum++;

            Row scoresHeaderRow = ratingSheet.createRow(rowNum++);
            org.apache.poi.ss.usermodel.Cell scoresHeaderCell = scoresHeaderRow.createCell(0);
            scoresHeaderCell.setCellValue("DÉTAIL DES SCORES");
            scoresHeaderCell.setCellStyle(headerStyle);
            ratingSheet.addMergedRegion(new CellRangeAddress(rowNum - 1, rowNum - 1, 0, 1));

            addExcelRow(ratingSheet, rowNum++, "Score paiement:", bilan.getRating().getPaymentScore() + "/40", boldStyle);
            addExcelRow(ratingSheet, rowNum++, "Score conformité:", bilan.getRating().getContractComplianceScore() + "/30", boldStyle);
            addExcelRow(ratingSheet, rowNum++, "Score activité:", bilan.getRating().getActivityScore() + "/30", boldStyle);
            rowNum++;

            Row strengthsHeaderRow = ratingSheet.createRow(rowNum++);
            org.apache.poi.ss.usermodel.Cell strengthsHeaderCell = strengthsHeaderRow.createCell(0);
            strengthsHeaderCell.setCellValue("POINTS FORTS");
            strengthsHeaderCell.setCellStyle(headerStyle);
            ratingSheet.addMergedRegion(new CellRangeAddress(rowNum - 1, rowNum - 1, 0, 1));

            if (bilan.getRating().getStrengths() != null && !bilan.getRating().getStrengths().isEmpty()) {
                for (String strength : bilan.getRating().getStrengths()) addExcelRow(ratingSheet, rowNum++, "✓", strength, boldStyle);
            } else {
                addExcelRow(ratingSheet, rowNum++, "✓", "Aucun point fort identifié", boldStyle);
            }
            rowNum++;

            Row weaknessesHeaderRow = ratingSheet.createRow(rowNum++);
            org.apache.poi.ss.usermodel.Cell weaknessesHeaderCell = weaknessesHeaderRow.createCell(0);
            weaknessesHeaderCell.setCellValue("POINTS À AMÉLIORER");
            weaknessesHeaderCell.setCellStyle(headerStyle);
            ratingSheet.addMergedRegion(new CellRangeAddress(rowNum - 1, rowNum - 1, 0, 1));

            if (bilan.getRating().getWeaknesses() != null && !bilan.getRating().getWeaknesses().isEmpty()) {
                for (String weakness : bilan.getRating().getWeaknesses()) addExcelRow(ratingSheet, rowNum++, "⚠", weakness, boldStyle);
            } else {
                addExcelRow(ratingSheet, rowNum++, "⚠", "Aucun point faible identifié", boldStyle);
            }
            rowNum++;

            Row recHeaderRow = ratingSheet.createRow(rowNum++);
            org.apache.poi.ss.usermodel.Cell recHeaderCell = recHeaderRow.createCell(0);
            recHeaderCell.setCellValue("RECOMMANDATIONS");
            recHeaderCell.setCellStyle(headerStyle);
            ratingSheet.addMergedRegion(new CellRangeAddress(rowNum - 1, rowNum - 1, 0, 1));

            if (bilan.getRecommendations() != null && !bilan.getRecommendations().isEmpty()) {
                for (String recommendation : bilan.getRecommendations()) addExcelRow(ratingSheet, rowNum++, "→", recommendation, boldStyle);
            } else {
                addExcelRow(ratingSheet, rowNum++, "→", "Aucune recommandation spécifique", boldStyle);
            }

            for (int i = 0; i < 3; i++) infoSheet.autoSizeColumn(i);
            for (int i = 0; i < convHeaders.length; i++) convSheet.autoSizeColumn(i);
            for (int i = 0; i < 2; i++) financialSheet.autoSizeColumn(i);
            for (int i = 0; i < 2; i++) ratingSheet.autoSizeColumn(i);

            workbook.write(baos);
            return baos.toByteArray();
        } catch (Exception e) {
            log.error("Error generating Excel: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to generate Excel: " + e.getMessage());
        }
    }

    private CellStyle createExcelHeaderStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        Font font = workbook.createFont();
        font.setBold(true);
        font.setColor(IndexedColors.WHITE.getIndex());
        style.setFont(font);
        style.setFillForegroundColor(IndexedColors.DARK_BLUE.getIndex());
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderTop(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);
        return style;
    }

    private CellStyle createExcelTitleStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        Font font = workbook.createFont();
        font.setBold(true);
        font.setFontHeightInPoints((short) 14);
        style.setFont(font);
        return style;
    }

    private CellStyle createExcelBoldStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        Font font = workbook.createFont();
        font.setBold(true);
        style.setFont(font);
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderTop(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);
        return style;
    }

    private CellStyle createExcelCurrencyStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        CreationHelper createHelper = workbook.getCreationHelper();
        style.setDataFormat(createHelper.createDataFormat().getFormat("#,##0.00"));
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderTop(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);
        return style;
    }

    private CellStyle createExcelDateStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        CreationHelper createHelper = workbook.getCreationHelper();
        style.setDataFormat(createHelper.createDataFormat().getFormat("dd/MM/yyyy"));
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderTop(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);
        return style;
    }

    private void addExcelRow(Sheet sheet, int rowNum, String label, String value, CellStyle boldStyle) {
        Row row = sheet.createRow(rowNum);
        org.apache.poi.ss.usermodel.Cell labelCell = row.createCell(0);
        labelCell.setCellValue(label);
        labelCell.setCellStyle(boldStyle);
        org.apache.poi.ss.usermodel.Cell valueCell = row.createCell(1);
        valueCell.setCellValue(value);
    }

    private String formatDate(LocalDate date) {
        if (date == null) return "N/A";
        return date.format(DateTimeFormatter.ofPattern("dd/MM/yyyy"));
    }

    private String formatCurrency(BigDecimal amount) {
        if (amount == null) return "0,00 TND";
        return String.format("%,.2f TND", amount);
    }
}