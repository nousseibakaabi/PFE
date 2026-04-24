package com.example.back.controller;

import com.example.back.payload.request.ApplicationRequest;
import com.example.back.payload.response.ApplicationResponse;
import com.example.back.repository.ApplicationRepository;
import com.example.back.repository.ConventionRepository;
import com.example.back.repository.UserRepository;
import com.example.back.service.ApplicationService;
import com.example.back.service.WorkloadService;
import com.example.back.service.mapper.ConventionMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ApplicationControllerTest {

    @Mock
    private ApplicationService applicationService;
    @Mock
    private UserRepository userRepository;
    @Mock
    private ApplicationRepository applicationRepository;
    @Mock
    private ConventionRepository conventionRepository;
    @Mock
    private ConventionMapper conventionMapper;
    @Mock
    private WorkloadService workloadService;

    @InjectMocks
    private ApplicationController controller;

    @Test
    void getAllApplications_returnsWrappedList() {
        ApplicationResponse application = new ApplicationResponse();
        application.setId(1L);
        application.setCode("APP-001");

        when(applicationService.getAllApplications()).thenReturn(List.of(application));

        ResponseEntity<?> response = controller.getAllApplications();

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat((Map<String, Object>) response.getBody())
                .containsEntry("success", true)
                .containsEntry("count", 1);
    }

    @Test
    void createApplication_whenServiceThrows_returnsBadRequest() {
        ApplicationRequest request = new ApplicationRequest();
        when(applicationService.createApplication(request)).thenThrow(new RuntimeException("invalid"));

        ResponseEntity<?> response = controller.createApplication(request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat((Map<String, Object>) response.getBody())
                .containsEntry("success", false)
                .containsEntry("message", "invalid");
    }
}
