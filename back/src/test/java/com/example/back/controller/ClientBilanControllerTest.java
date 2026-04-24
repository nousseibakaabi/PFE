package com.example.back.controller;

import com.example.back.payload.request.ClientBilanRequest;
import com.example.back.payload.response.ClientBilanResponse;
import com.example.back.service.ClientBilanService;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ClientBilanControllerTest {

    @Mock
    private ClientBilanService clientBilanService;

    @InjectMocks
    private ClientBilanController controller;

    @Test
    void generateClientBilan_returnsServiceResult() {
        ClientBilanRequest request = new ClientBilanRequest();
        ClientBilanResponse responseBody = new ClientBilanResponse();
        responseBody.setClientId(12L);
        responseBody.setClientName("Client One");

        when(clientBilanService.generateClientBilan(request)).thenReturn(responseBody);

        ResponseEntity<ClientBilanResponse> response = controller.generateClientBilan(request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isSameAs(responseBody);
    }

    @Test
    void getBilanSummary_aggregatesReturnedBilans() {
        ClientBilanResponse.FinancialSummary financialSummary = new ClientBilanResponse.FinancialSummary();
        financialSummary.setPaymentComplianceRate(new BigDecimal("90"));
        financialSummary.setTotalContractValue(new BigDecimal("1000"));
        financialSummary.setTotalOverdue(new BigDecimal("100"));

        ClientBilanResponse.ClientRating rating = new ClientBilanResponse.ClientRating();
        rating.setRating("A");

        ClientBilanResponse bilan = new ClientBilanResponse();
        bilan.setFinancialSummary(financialSummary);
        bilan.setRating(rating);

        when(clientBilanService.generateAllClientsBilan(null, null)).thenReturn(List.of(bilan));

        ResponseEntity<Map<String, Object>> response = controller.getBilanSummary();

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody())
                .containsEntry("totalClients", 1)
                .containsEntry("totalContractValue", new BigDecimal("1000"))
                .containsEntry("totalOverdue", new BigDecimal("100"));
    }

    @Test
    void getClientBilan_buildsRequestFromPathAndDates() {
        ClientBilanResponse responseBody = new ClientBilanResponse();
        when(clientBilanService.generateClientBilan(org.mockito.ArgumentMatchers.any(ClientBilanRequest.class)))
                .thenReturn(responseBody);

        ResponseEntity<ClientBilanResponse> response = controller.getClientBilan(
                5L,
                LocalDate.of(2026, 1, 1),
                LocalDate.of(2026, 2, 1)
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isSameAs(responseBody);
    }

    @Test
    void generateAllClientsBilan_returnsList() {
        ClientBilanResponse item = new ClientBilanResponse();
        when(clientBilanService.generateAllClientsBilan(null, null)).thenReturn(List.of(item));

        ResponseEntity<List<ClientBilanResponse>> response = controller.generateAllClientsBilan(null, null);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).containsExactly(item);
    }

    @Test
    void getPaginatedClientBilans_returnsPage() {
        PageImpl<ClientBilanResponse> page = new PageImpl<>(List.of(new ClientBilanResponse()), PageRequest.of(0, 20), 1);
        when(clientBilanService.getPaginatedClientBilans(PageRequest.of(0, 20), "q")).thenReturn(page);

        ResponseEntity<org.springframework.data.domain.Page<ClientBilanResponse>> response =
                controller.getPaginatedClientBilans(PageRequest.of(0, 20), "q");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isSameAs(page);
    }

    @Test
    void getClientPaymentStats_returnsStats() {
        ClientBilanResponse.PaymentStats stats = new ClientBilanResponse.PaymentStats();
        when(clientBilanService.getClientPaymentStats(4L)).thenReturn(stats);

        ResponseEntity<ClientBilanResponse.PaymentStats> response = controller.getClientPaymentStats(4L);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isSameAs(stats);
    }

    @Test
    void getClientsWithPoorPayment_returnsList() {
        when(clientBilanService.getClientsWithPoorPayment(3, 60)).thenReturn(List.of(new ClientBilanResponse()));

        ResponseEntity<List<ClientBilanResponse>> response = controller.getClientsWithPoorPayment(3, 60);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).hasSize(1);
    }

    @Test
    void exportBilanToPdf_setsAttachmentHeaders() {
        when(clientBilanService.exportBilanToPdf(7L, null, null)).thenReturn(new byte[]{1, 2});

        ResponseEntity<byte[]> response = controller.exportBilanToPdf(7L, null, null);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getHeaders().getFirst(HttpHeaders.CONTENT_DISPOSITION))
                .isEqualTo("attachment; filename=client-bilan-7.pdf");
        assertThat(response.getBody()).containsExactly(1, 2);
    }

    @Test
    void exportBilanToExcel_setsAttachmentHeaders() {
        when(clientBilanService.exportBilanToExcel(8L, null, null)).thenReturn(new byte[]{3, 4});

        ResponseEntity<byte[]> response = controller.exportBilanToExcel(8L, null, null);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getHeaders().getFirst(HttpHeaders.CONTENT_DISPOSITION))
                .isEqualTo("attachment; filename=client-bilan-8.xlsx");
        assertThat(response.getBody()).containsExactly(3, 4);
    }
}
