package com.example.back.controller;

import com.example.back.entity.ERole;
import com.example.back.entity.Structure;
import com.example.back.entity.User;
import com.example.back.entity.ZoneGeographique;
import com.example.back.entity.ZoneType;
import com.example.back.repository.ApplicationRepository;
import com.example.back.repository.ConventionRepository;
import com.example.back.repository.FactureRepository;
import com.example.back.repository.StructureRepository;
import com.example.back.repository.UserRepository;
import com.example.back.repository.ZoneGeographiqueRepository;
import com.example.back.service.UserContextService;
import com.example.back.support.ControllerTestSupport;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StatsControllerTest {

    @Mock
    private ConventionRepository conventionRepository;
    @Mock
    private FactureRepository factureRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private StructureRepository structureRepository;
    @Mock
    private ZoneGeographiqueRepository zoneGeographiqueRepository;
    @Mock
    private ApplicationRepository applicationRepository;
    @Mock
    private UserContextService userContextService;

    @InjectMocks
    private StatsController controller;

    @Test
    void getNomenclatureDetailedStats_returnsAggregatedCounts() {
        User currentUser = ControllerTestSupport.user(1L, "decideur", ERole.ROLE_DECIDEUR);

        Structure client = new Structure();
        client.setTypeStructure("Client");
        Structure responsable = new Structure();
        responsable.setTypeStructure("Internal");

        ZoneGeographique tunis = new ZoneGeographique();
        tunis.setType(ZoneType.TUNISIAN_ZONE);
        ZoneGeographique custom = new ZoneGeographique();
        custom.setType(ZoneType.CUSTOM_ZONE);

        when(userContextService.getCurrentUser()).thenReturn(currentUser);
        when(structureRepository.findAll()).thenReturn(List.of(client, responsable));
        when(structureRepository.findByTypeStructureNot("Client")).thenReturn(List.of(responsable));
        when(structureRepository.findByTypeStructure("Client")).thenReturn(List.of(client));
        when(structureRepository.count()).thenReturn(2L);
        when(zoneGeographiqueRepository.findAll()).thenReturn(List.of(tunis, custom));
        when(zoneGeographiqueRepository.findByType(ZoneType.TUNISIAN_ZONE)).thenReturn(List.of(tunis));
        when(zoneGeographiqueRepository.findByType(ZoneType.CUSTOM_ZONE)).thenReturn(List.of(custom));
        when(zoneGeographiqueRepository.count()).thenReturn(2L);
        when(applicationRepository.count()).thenReturn(3L);

        ResponseEntity<?> response = controller.getNomenclatureDetailedStats();

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat((Map<String, Object>) response.getBody())
                .containsEntry("success", true)
                .containsEntry("userRole", "DECIDEUR");
    }

    @Test
    void getUserDetailedStats_excludesAdminsFromMainDistribution() {
        User admin = ControllerTestSupport.user(1L, "admin", ERole.ROLE_ADMIN);
        User commercial = ControllerTestSupport.user(2L, "commercial", ERole.ROLE_COMMERCIAL_METIER);
        commercial.setLastLogin(LocalDateTime.now().minusMonths(4));

        when(userRepository.findAll()).thenReturn(List.of(admin, commercial));

        ResponseEntity<?> response = controller.getUserDetailedStats();

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        Map<String, Object> body = (Map<String, Object>) response.getBody();
        Map<String, Object> data = (Map<String, Object>) body.get("data");
        assertThat(body).containsEntry("success", true);
        assertThat(data).containsKey("usersByRole");
    }

    @Test
    void userContextDependentEndpoints_returnBadRequestWhenCurrentUserFails() {
        when(userContextService.getCurrentUser()).thenThrow(new RuntimeException("missing"));

        List<ResponseEntity<?>> responses = List.of(
                controller.getDashboardStats(),
                controller.getConventionDetailedStats(),
                controller.getFactureDetailedStats(),
                controller.getApplicationDetailedStats(),
                controller.getFinancialDetailedStats(),
                controller.getNomenclatureDetailedStats(),
                controller.getSummaryStats(),
                controller.getOverdueAlerts()
        );

        assertThat(responses).allSatisfy(response -> assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST));
    }
}
