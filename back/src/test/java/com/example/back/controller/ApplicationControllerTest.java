package com.example.back.controller;

import com.example.back.entity.Structure;
import com.example.back.entity.User;
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
import java.util.Optional;

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

    @Test
    void readEndpoints_wrapServiceResponses() {
        ApplicationResponse application = new ApplicationResponse();
        application.setId(2L);
        application.setCode("APP-002");
        Structure structure = new Structure();
        structure.setId(8L);
        structure.setName("Client");
        WorkloadService.AssignmentCheck check = new WorkloadService.AssignmentCheck();
        WorkloadService.WorkloadDashboard dashboard = new WorkloadService.WorkloadDashboard();

        when(applicationService.getApplicationById(2L)).thenReturn(application);
        when(applicationService.getApplicationsByChefDeProjet(9L)).thenReturn(List.of(application));
        when(applicationService.searchApplications("APP", "Name", "Client", 9L, "ACTIVE")).thenReturn(List.of(application));
        when(applicationService.getApplicationDashboard()).thenReturn(Map.of("total", 3));
        when(applicationService.getOrCreateStructureForApplication(2L)).thenReturn(structure);
        when(applicationService.getUnassignedApplications()).thenReturn(List.of(application));
        when(applicationService.generateSuggestedApplicationCode()).thenReturn("APP-2026-123");
        when(applicationService.getApplicationDateSummary(2L)).thenReturn(Map.of("daysRemaining", 5));
        when(applicationService.getApplicationsWithoutConventions()).thenReturn(List.of(application));
        when(workloadService.checkAssignment(1L, 2L)).thenReturn(check);
        when(workloadService.getWorkloadDashboard()).thenReturn(dashboard);

        assertThat(((Map<String, Object>) controller.getApplicationById(2L).getBody())).containsEntry("data", application);
        assertThat(((Map<String, Object>) controller.getApplicationsByChefDeProjet(9L).getBody())).containsEntry("count", 1);
        assertThat(((Map<String, Object>) controller.searchApplications("APP", "Name", "Client", 9L, "ACTIVE").getBody())).containsEntry("count", 1);
        assertThat(((Map<String, Object>) controller.getApplicationDashboard().getBody())).containsEntry("data", Map.of("total", 3));
        assertThat(((Map<String, Object>) controller.getClientStructureForApplication(2L).getBody())).containsEntry("data", structure);
        assertThat(((Map<String, Object>) controller.getUnassignedApplications().getBody())).containsEntry("count", 1);
        assertThat(((Map<String, Object>) controller.generateApplicationCode().getBody())).containsEntry("suggestedCode", "APP-2026-123");
        assertThat(((Map<String, Object>) controller.getApplicationDateSummary(2L).getBody())).containsEntry("data", Map.of("daysRemaining", 5));
        assertThat(((Map<String, Object>) controller.getApplicationsWithoutConventions().getBody())).containsEntry("count", 1);
        assertThat(((Map<String, Object>) controller.checkAssignment(1L, 2L).getBody())).containsEntry("data", check);
        assertThat(((Map<String, Object>) controller.getWorkloadDashboard().getBody())).containsEntry("data", dashboard);
    }

    @Test
    void writeEndpoints_wrapServiceResponses() {
        ApplicationRequest request = new ApplicationRequest();
        ApplicationResponse application = new ApplicationResponse();
        application.setId(3L);
        application.setCode("APP-003");
        application.setTerminatedBy("admin");
        application.setDaysRemainingAtTermination(2L);
        application.setTerminatedEarly(true);
        WorkloadService.AssignmentResult assignmentResult = WorkloadService.AssignmentResult.success(
                new WorkloadService.AssignmentCheck(), "assigned");

        when(applicationService.updateApplication(3L, request)).thenReturn(application);
        when(applicationService.manuallyTerminateApplication(3L, "done")).thenReturn(application);
        when(workloadService.assignApplication(1L, 3L, true)).thenReturn(assignmentResult);

        controller.calculateApplicationStatus(3L);
        assertThat(((Map<String, Object>) controller.updateApplication(3L, request).getBody())).containsEntry("data", application);
        assertThat(((Map<String, Object>) controller.assignWithWorkloadCheck(1L, 3L, true).getBody()))
                .containsEntry("success", true)
                .containsEntry("message", "assigned");
        assertThat(((Map<String, Object>) controller.manuallyTerminateApplication(3L, Map.of("reason", "done")).getBody()))
                .containsEntry("success", true)
                .containsEntry("data", application);
    }

    @Test
    void repositoryBackedEndpoints_returnExpectedPayloads() {
        User chef = new User();
        chef.setId(4L);
        when(userRepository.findById(4L)).thenReturn(Optional.of(chef));
        when(applicationRepository.findByChefDeProjet(chef)).thenReturn(List.of());
        when(applicationService.getArchivedApplicationsForCurrentUser()).thenReturn(List.of(new ApplicationResponse()));
        when(applicationRepository.existsByCode("APP-1")).thenReturn(true);

        assertThat(((Map<String, Object>) controller.getConventionsByChefDeProjet(4L).getBody()))
                .containsEntry("success", true);
        assertThat(((Map<String, Object>) controller.getArchivedApplications().getBody())).containsEntry("count", 1);
        assertThat(((Map<String, Object>) controller.checkApplicationCodeExists("APP-1").getBody()))
                .containsEntry("exists", true)
                .containsEntry("code", "APP-1");
    }
}
