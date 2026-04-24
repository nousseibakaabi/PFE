package com.example.back.controller;

import com.example.back.payload.request.ClientBilanRequest;
import com.example.back.payload.response.ClientBilanResponse;
import com.example.back.service.ClientBilanService;
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
}
