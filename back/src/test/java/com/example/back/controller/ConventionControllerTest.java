package com.example.back.controller;

import com.example.back.entity.ERole;
import com.example.back.entity.User;
import com.example.back.payload.request.ConventionRequest;
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
import java.util.Map;
import java.util.Optional;

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
}
