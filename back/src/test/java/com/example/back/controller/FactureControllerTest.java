package com.example.back.controller;

import com.example.back.entity.Convention;
import com.example.back.entity.ERole;
import com.example.back.entity.Facture;
import com.example.back.entity.User;
import com.example.back.payload.request.FactureRequest;
import com.example.back.payload.response.FactureResponse;
import com.example.back.repository.ConventionRepository;
import com.example.back.repository.FactureRepository;
import com.example.back.service.ConventionService;
import com.example.back.service.EmailService;
import com.example.back.service.HistoryService;
import com.example.back.service.NotificationService;
import com.example.back.service.UserContextService;
import com.example.back.service.mapper.FactureMapper;
import com.example.back.support.ControllerTestSupport;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FactureControllerTest {

    @Mock
    private EmailService emailService;
    @Mock
    private FactureRepository factureRepository;
    @Mock
    private ConventionRepository conventionRepository;
    @Mock
    private ConventionService conventionService;
    @Mock
    private FactureMapper factureMapper;
    @Mock
    private UserContextService userContextService;
    @Mock
    private HistoryService historyService;
    @Mock
    private NotificationService notificationService;

    @InjectMocks
    private FactureController controller;

    @Test
    void getFactureById_whenUserHasNoAccess_returnsForbidden() {
        User currentUser = ControllerTestSupport.user(1L, "commercial", ERole.ROLE_COMMERCIAL_METIER);
        User owner = ControllerTestSupport.user(2L, "owner", ERole.ROLE_COMMERCIAL_METIER);

        Convention convention = new Convention();
        convention.setCreatedBy(owner);

        Facture facture = new Facture();
        facture.setId(10L);
        facture.setConvention(convention);

        when(userContextService.getCurrentUser()).thenReturn(currentUser);
        when(factureRepository.findById(10L)).thenReturn(Optional.of(facture));

        ResponseEntity<?> response = controller.getFactureById(10L);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat((Map<String, Object>) response.getBody())
                .containsEntry("success", false)
                .containsEntry("message", "Access denied to this invoice");
    }

    @Test
    void generateFacture_createsInvoiceForOwnedConvention() {
        User currentUser = ControllerTestSupport.user(1L, "commercial", ERole.ROLE_COMMERCIAL_METIER);
        Convention convention = new Convention();
        convention.setId(7L);
        convention.setCreatedBy(currentUser);

        FactureRequest request = new FactureRequest();
        request.setConventionId(7L);
        request.setDateEcheance(LocalDate.now().plusDays(10));
        request.setMontantHT(new BigDecimal("1000"));
        request.setTva(new BigDecimal("19"));

        FactureResponse factureResponse = new FactureResponse();
        factureResponse.setNumeroFacture("FACT-2026-000001");

        when(userContextService.getCurrentUser()).thenReturn(currentUser);
        when(conventionRepository.findById(7L)).thenReturn(Optional.of(convention));
        when(factureRepository.count()).thenReturn(0L);
        when(factureRepository.save(any(Facture.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(factureMapper.toResponse(any(Facture.class))).thenReturn(factureResponse);

        ResponseEntity<?> response = controller.generateFacture(request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat((Map<String, Object>) response.getBody())
                .containsEntry("success", true)
                .containsEntry("message", "Invoice generated successfully")
                .containsEntry("data", factureResponse);
        verify(historyService).logFactureCreate(any(Facture.class));
        verify(conventionService).updateConventionStatusRealTime(7L);
    }
}
