package com.example.back.controller;

import com.example.back.entity.Application;
import com.example.back.entity.Convention;
import com.example.back.entity.ERole;
import com.example.back.entity.Facture;
import com.example.back.entity.User;
import com.example.back.repository.FactureRepository;
import com.example.back.service.UserContextService;
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
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CalendarControllerTest {

    @Mock
    private FactureRepository factureRepository;

    @Mock
    private UserContextService userContextService;

    @InjectMocks
    private CalendarController controller;

    @Test
    void getInvoiceEvents_returnsAccessibleEventsForCurrentUser() {
        User admin = ControllerTestSupport.user(1L, "admin", ERole.ROLE_ADMIN);
        Convention convention = new Convention();
        convention.setId(9L);
        convention.setReferenceConvention("CONV-001");
        Application application = new Application();
        application.setClientName("Client A");
        convention.setApplication(application);

        Facture facture = new Facture();
        facture.setId(3L);
        facture.setNumeroFacture("FACT-001");
        facture.setConvention(convention);
        facture.setDateEcheance(LocalDate.now().plusDays(2));
        facture.setMontantTTC(new BigDecimal("1500"));
        facture.setStatutPaiement("NON_PAYE");

        when(userContextService.getCurrentUser()).thenReturn(admin);
        when(factureRepository.findAll()).thenReturn(List.of(facture));

        ResponseEntity<?> response = controller.getInvoiceEvents(null, null);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat((Map<String, Object>) response.getBody())
                .containsEntry("success", true)
                .containsEntry("count", 1)
                .containsEntry("userRole", "ADMIN");
    }

    @Test
    void getCalendarStats_whenCurrentUserFails_returnsBadRequest() {
        when(userContextService.getCurrentUser()).thenThrow(new RuntimeException("missing"));

        ResponseEntity<?> response = controller.getCalendarStats();

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat((Map<String, Object>) response.getBody())
                .containsEntry("success", false)
                .containsEntry("message", "Failed to fetch calendar stats");
    }

    @Test
    void getUpcomingInvoices_returnsOnlyAccessibleUpcomingEvents() {
        User commercial = ControllerTestSupport.user(5L, "commercial", ERole.ROLE_COMMERCIAL_METIER);
        Facture accessible = facture(10L, "FACT-010", commercial, null, LocalDate.now().plusDays(5), "NON_PAYE");
        Facture inaccessible = facture(11L, "FACT-011", ControllerTestSupport.user(6L, "other", ERole.ROLE_COMMERCIAL_METIER),
                null, LocalDate.now().plusDays(6), "NON_PAYE");

        when(userContextService.getCurrentUser()).thenReturn(commercial);
        when(factureRepository.findByDateEcheanceBetweenAndStatutPaiementNot(
                LocalDate.now(), LocalDate.now().plusMonths(1), "PAYE"))
                .thenReturn(List.of(accessible, inaccessible));

        ResponseEntity<?> response = controller.getUpcomingInvoices();

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat((Map<String, Object>) response.getBody())
                .containsEntry("success", true)
                .containsEntry("count", 1);
    }

    @Test
    void getOverdueInvoices_returnsAccessibleOverdueEvents() {
        User admin = ControllerTestSupport.user(7L, "admin", ERole.ROLE_ADMIN);
        Facture overdue = facture(12L, "FACT-012", admin, null, LocalDate.now().minusDays(2), "EN_RETARD");

        when(userContextService.getCurrentUser()).thenReturn(admin);
        when(factureRepository.findFacturesEnRetard(LocalDate.now())).thenReturn(List.of(overdue));

        ResponseEntity<?> response = controller.getOverdueInvoices();

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat((Map<String, Object>) response.getBody())
                .containsEntry("success", true)
                .containsEntry("count", 1);
    }

    @Test
    void getAllEvents_returnsCombinedAccessibleEvents() {
        User decideur = ControllerTestSupport.user(8L, "decideur", ERole.ROLE_DECIDEUR);
        LocalDate start = LocalDate.now().minusDays(1);
        LocalDate end = LocalDate.now().plusDays(7);
        Facture facture = facture(13L, "FACT-013", decideur, null, LocalDate.now().plusDays(3), "NON_PAYE");

        when(userContextService.getCurrentUser()).thenReturn(decideur);
        when(factureRepository.findByDateEcheanceBetween(start, end)).thenReturn(List.of(facture));

        ResponseEntity<?> response = controller.getAllEvents(start, end);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat((Map<String, Object>) response.getBody())
                .containsEntry("success", true)
                .containsEntry("count", 1)
                .containsEntry("userRole", "DECIDEUR");
    }

    private Facture facture(Long id, String numero, User createdBy, User chefDeProjet, LocalDate dateEcheance, String statutPaiement) {
        Convention convention = new Convention();
        convention.setId(id + 100);
        convention.setReferenceConvention("CONV-" + numero);
        convention.setCreatedBy(createdBy);

        Application application = new Application();
        application.setClientName("Client " + numero);
        application.setChefDeProjet(chefDeProjet);
        convention.setApplication(application);

        Facture facture = new Facture();
        facture.setId(id);
        facture.setNumeroFacture(numero);
        facture.setConvention(convention);
        facture.setDateEcheance(dateEcheance);
        facture.setMontantTTC(new BigDecimal("1000"));
        facture.setStatutPaiement(statutPaiement);
        return facture;
    }
}
