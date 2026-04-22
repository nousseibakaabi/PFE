package com.example.back.controller;

import com.example.back.payload.request.ClientBilanRequest;
import com.example.back.payload.response.ClientBilanResponse;
import com.example.back.service.ClientBilanService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/client-bilan")
@RequiredArgsConstructor
public class ClientBilanController {

    private final ClientBilanService clientBilanService;

    @PostMapping("/generate")
    @PreAuthorize("hasAnyRole('ADMIN', 'COMMERCIAL_METIER', 'DECIDEUR')")
    public ResponseEntity<ClientBilanResponse> generateClientBilan(@Valid @RequestBody ClientBilanRequest request) {
        ClientBilanResponse bilan = clientBilanService.generateClientBilan(request);
        return ResponseEntity.ok(bilan);
    }

    @GetMapping("/client/{clientId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'COMMERCIAL_METIER', 'DECIDEUR')")
    public ResponseEntity<ClientBilanResponse> getClientBilan(
            @PathVariable Long clientId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {

        ClientBilanRequest request = new ClientBilanRequest();
        request.setStructureBeneficielId(clientId);
        request.setDateStart(startDate);
        request.setDateEnd(endDate);
        request.setIncludeArchived(false);

        ClientBilanResponse bilan = clientBilanService.generateClientBilan(request);
        return ResponseEntity.ok(bilan);
    }

    @GetMapping("/all")
    @PreAuthorize("hasAnyRole('ADMIN', 'COMMERCIAL_METIER', 'DECIDEUR')")
    public ResponseEntity<List<ClientBilanResponse>> generateAllClientsBilan(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {

        List<ClientBilanResponse> bilans = clientBilanService.generateAllClientsBilan(startDate, endDate);
        return ResponseEntity.ok(bilans);
    }

    @GetMapping("/paginated")
    @PreAuthorize("hasAnyRole('ADMIN', 'COMMERCIAL_METIER', 'DECIDEUR')")
    public ResponseEntity<Page<ClientBilanResponse>> getPaginatedClientBilans(
            @PageableDefault(size = 20, sort = "clientName") Pageable pageable,
            @RequestParam(required = false) String search) {

        Page<ClientBilanResponse> bilans = clientBilanService.getPaginatedClientBilans(pageable, search);
        return ResponseEntity.ok(bilans);
    }

    @GetMapping("/client/{clientId}/payment-stats")
    @PreAuthorize("hasAnyRole('ADMIN', 'COMMERCIAL_METIER', 'DECIDEUR')")
    public ResponseEntity<ClientBilanResponse.PaymentStats> getClientPaymentStats(@PathVariable Long clientId) {
        ClientBilanResponse.PaymentStats stats = clientBilanService.getClientPaymentStats(clientId);
        return ResponseEntity.ok(stats);
    }

    @GetMapping("/poor-payers")
    @PreAuthorize("hasAnyRole('ADMIN', 'COMMERCIAL_METIER')")
    public ResponseEntity<List<ClientBilanResponse>> getClientsWithPoorPayment(
            @RequestParam(defaultValue = "2") int minLatePayments,
            @RequestParam(defaultValue = "30") int minDaysLate) {

        List<ClientBilanResponse> poorPayers = clientBilanService.getClientsWithPoorPayment(minLatePayments, minDaysLate);
        return ResponseEntity.ok(poorPayers);
    }

    @GetMapping("/export-pdf/{clientId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'COMMERCIAL_METIER', 'DECIDEUR')")
    public ResponseEntity<byte[]> exportBilanToPdf(
            @PathVariable Long clientId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {

        byte[] pdfBytes = clientBilanService.exportBilanToPdf(clientId, startDate, endDate);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=client-bilan-" + clientId + ".pdf")
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdfBytes);
    }

    @GetMapping("/export-excel/{clientId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'COMMERCIAL_METIER', 'DECIDEUR')")
    public ResponseEntity<byte[]> exportBilanToExcel(
            @PathVariable Long clientId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {

        byte[] excelBytes = clientBilanService.exportBilanToExcel(clientId, startDate, endDate);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=client-bilan-" + clientId + ".xlsx")
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(excelBytes);
    }

    @GetMapping("/summary")
    @PreAuthorize("hasAnyRole('ADMIN', 'COMMERCIAL_METIER', 'DECIDEUR')")
    public ResponseEntity<Map<String, Object>> getBilanSummary() {
        List<ClientBilanResponse> bilans = clientBilanService.generateAllClientsBilan(null, null);

        Map<String, Object> summary = new HashMap<>();
        summary.put("totalClients", bilans.size());
        summary.put("clientsByRating", bilans.stream()
                .collect(Collectors.groupingBy(b -> b.getRating().getRating(), Collectors.counting())));
        summary.put("averagePaymentCompliance", bilans.stream()
                .mapToDouble(b -> b.getFinancialSummary().getPaymentComplianceRate().doubleValue())
                .average()
                .orElse(0));
        summary.put("totalContractValue", bilans.stream()
                .map(b -> b.getFinancialSummary().getTotalContractValue())
                .reduce(BigDecimal.ZERO, BigDecimal::add));
        summary.put("totalOverdue", bilans.stream()
                .map(b -> b.getFinancialSummary().getTotalOverdue())
                .reduce(BigDecimal.ZERO, BigDecimal::add));

        return ResponseEntity.ok(summary);
    }
}