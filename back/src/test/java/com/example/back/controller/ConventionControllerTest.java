package com.example.back.controller;

import com.example.back.entity.Application;
import com.example.back.entity.Convention;
import com.example.back.entity.ERole;
import com.example.back.entity.OldConvention;
import com.example.back.entity.Structure;
import com.example.back.entity.User;
import com.example.back.payload.request.ArchiveConventionRequest;
import com.example.back.payload.request.ConventionRequest;
import com.example.back.payload.request.RenewalRequestDTO;
import com.example.back.payload.response.ConventionResponse;
import com.example.back.repository.ApplicationRepository;
import com.example.back.repository.ConventionRepository;
import com.example.back.repository.FactureRepository;
import com.example.back.repository.OldConventionRepository;
import com.example.back.repository.OldFactureRepository;
import com.example.back.repository.UserRepository;
import com.example.back.service.ApplicationService;
import com.example.back.service.ConventionService;
import com.example.back.service.HistoryService;
import com.example.back.service.mapper.ConventionMapper;
import com.example.back.support.ControllerTestSupport;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ConventionControllerTest {

    @Mock
    private ConventionRepository conventionRepository;
    @Mock
    private ApplicationRepository applicationRepository;
    @Mock
    private FactureRepository factureRepository;
    @Mock
    private ConventionService conventionService;
    @Mock
    private ConventionMapper conventionMapper;
    @Mock
    private UserRepository userRepository;
    @Mock
    private ApplicationService applicationService;
    @Mock
    private HistoryService historyService;
    @Mock
    private OldFactureRepository oldFactureRepository;
    @Mock
    private OldConventionRepository oldConventionRepository;

    @InjectMocks
    private ConventionController controller;

    @AfterEach
    void tearDown() {
        ControllerTestSupport.clearAuthentication();
    }

    @Test
    void calculateTTC_wrapsServiceResponse() {
        when(conventionService.calculateTTCResponse(new BigDecimal("100"), new BigDecimal("19")))
                .thenReturn(Map.of("montantTTC", new BigDecimal("119")));

        ResponseEntity<?> response = controller.calculateTTC(new BigDecimal("100"), new BigDecimal("19"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat((Map<String, Object>) response.getBody())
                .containsEntry("success", true)
                .containsEntry("data", Map.of("montantTTC", new BigDecimal("119")));
    }

    @Test
    void createConvention_whenReferenceAlreadyExists_returnsBadRequest() {
        User user = ControllerTestSupport.user(1L, "commercial", ERole.ROLE_COMMERCIAL_METIER);
        ControllerTestSupport.authenticate(user);

        ConventionRequest request = new ConventionRequest();
        request.setReferenceConvention("CONV-2026-001");

        when(userRepository.findByUsername("commercial")).thenReturn(Optional.of(user));
        when(conventionRepository.existsByReferenceConvention("CONV-2026-001")).thenReturn(true);

        ResponseEntity<?> response = controller.createConvention(request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat((Map<String, Object>) response.getBody())
                .containsEntry("success", false)
                .containsEntry("message", "Convention with this reference already exists");
    }

    @Test
    void readEndpoints_returnExpectedPayloads() {
        Convention convention = new Convention();
        convention.setId(5L);
        ConventionResponse conventionResponse = new ConventionResponse();
        conventionResponse.setId(5L);
        Structure structure = new Structure();
        structure.setId(1L);
        structure.setName("Client");

        when(conventionService.determineNbUsersResponse(1L, 2L)).thenReturn(Map.of("nbUsers", 2L));
        when(conventionRepository.findById(5L)).thenReturn(Optional.of(convention));
        when(conventionMapper.toResponse(convention)).thenReturn(conventionResponse);
        when(conventionRepository.findByArchivedTrue()).thenReturn(List.of(convention));
        when(conventionRepository.findByArchivedFalse()).thenReturn(List.of());
        when(conventionRepository.findByApplicationId(8L)).thenReturn(List.of(convention));
        when(conventionRepository.findByEtat("EN_COURS")).thenReturn(List.of(convention));
        when(applicationService.getOrCreateStructureForApplication(8L)).thenReturn(structure);
        when(conventionService.generateSuggestedReference()).thenReturn("CONV-2026-999");

        assertThat(controller.determineNbUsers(1L, 2L).getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(controller.getConventionById(5L).getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(controller.getArchivedConventions().getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(controller.getConventionStats().getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(controller.searchConventions(null, null, null, null, null).getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(controller.getConventionsByStructure(1L).getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(controller.getConventionsByApplication(8L).getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(controller.getConventionsByZone(1L).getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(controller.getConventionsByEtat("EN_COURS").getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(controller.getExpiringSoonConventions().getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(controller.getClientStructureFromApplication(8L).getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(controller.generateReferenceSuggestion().getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    void currentUserDependentEndpoints_returnBadRequestWhenUserCannotBeResolved() {
        User authUser = ControllerTestSupport.user(1L, "commercial", ERole.ROLE_COMMERCIAL_METIER);
        ControllerTestSupport.authenticate(authUser);
        when(userRepository.findByUsername("commercial")).thenReturn(Optional.empty());

        ConventionRequest request = new ConventionRequest();
        request.setReferenceConvention("CONV-2026-001");
        ArchiveConventionRequest archiveRequest = new ArchiveConventionRequest();
        RenewalRequestDTO renewalRequestDTO = new RenewalRequestDTO();

        List<ResponseEntity<?>> responses = List.of(
                controller.createConvention(request),
                controller.archiveConvention(1L, archiveRequest),
                controller.restoreConvention(1L),
                controller.getActiveConventions(),
                controller.getAllConventions(false),
                controller.renewConvention(1L, renewalRequestDTO)
        );

        assertThat(responses).allSatisfy(response -> assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST));
    }

    @Test
    void remainingEndpoints_areCovered() {
        when(conventionRepository.findById(99L)).thenReturn(Optional.empty());
        when(oldConventionRepository.findById(77L)).thenReturn(Optional.empty());
        org.mockito.Mockito.doNothing().when(conventionService).syncAllApplicationDates(5L);

        assertThat(controller.updateConvention(1L, new ConventionRequest()).getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(controller.deleteConvention(99L).getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(controller.syncApplicationDates(5L).getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(controller.getPreviousVersions(99L, 0, 10).getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(controller.getOldConventionById(77L).getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }
}
