package com.example.back.controller;

import com.example.back.entity.Application;
import com.example.back.entity.ERole;
import com.example.back.entity.User;
import com.example.back.payload.request.CreateReassignmentRequestDTO;
import com.example.back.repository.ApplicationRepository;
import com.example.back.repository.ConventionRepository;
import com.example.back.repository.UserRepository;
import com.example.back.service.RequestService;
import com.example.back.service.WorkloadService;
import com.example.back.service.mapper.RequestMapper;
import com.example.back.support.ControllerTestSupport;
import org.junit.jupiter.api.AfterEach;
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
class RequestControllerTest {

    @Mock
    private RequestService requestService;
    @Mock
    private UserRepository userRepository;
    @Mock
    private WorkloadService workloadService;
    @Mock
    private ApplicationRepository applicationRepository;
    @Mock
    private ConventionRepository conventionRepository;
    @Mock
    private RequestMapper requestMapper;

    @InjectMocks
    private RequestController controller;

    @AfterEach
    void tearDown() {
        ControllerTestSupport.clearAuthentication();
    }

    @Test
    void getAvailableChefs_excludesCurrentUserAndAddsWorkloadInfo() {
        User currentUser = ControllerTestSupport.user(1L, "chef1", ERole.ROLE_CHEF_PROJET);
        User otherChef = ControllerTestSupport.user(2L, "chef2", ERole.ROLE_CHEF_PROJET);

        WorkloadService.AssignmentCheck check = new WorkloadService.AssignmentCheck();
        WorkloadService.WorkloadAnalysis analysis = new WorkloadService.WorkloadAnalysis();
        analysis.setCurrentWorkload(20.0);
        analysis.setProjectedWorkload(35.0);
        check.setAnalysis(analysis);
        check.setCanAssign(true);

        ControllerTestSupport.authenticate(currentUser);
        when(userRepository.findByUsername("chef1")).thenReturn(Optional.of(currentUser));
        when(userRepository.findByRoleName("ROLE_CHEF_PROJET")).thenReturn(List.of(currentUser, otherChef));
        when(workloadService.checkAssignment(2L, 10L)).thenReturn(check);

        ResponseEntity<?> response = controller.getAvailableChefs(10L);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        Map<String, Object> body = (Map<String, Object>) response.getBody();
        assertThat(body).containsEntry("success", true);
        assertThat((List<?>) body.get("data")).hasSize(1);
    }

    @Test
    void createReassignmentRequest_whenCurrentUserIsNotCreator_returnsForbidden() {
        User currentUser = ControllerTestSupport.user(1L, "chef1", ERole.ROLE_CHEF_PROJET);
        User creator = ControllerTestSupport.user(2L, "creator", ERole.ROLE_CHEF_PROJET);
        Application application = new Application();
        application.setId(7L);
        application.setCreatedBy(creator);

        CreateReassignmentRequestDTO request = new CreateReassignmentRequestDTO();
        request.setApplicationId(7L);
        request.setRecommendedChefId(3L);
        request.setReason("Too much work");

        ControllerTestSupport.authenticate(currentUser);
        when(userRepository.findByUsername("chef1")).thenReturn(Optional.of(currentUser));
        when(applicationRepository.findById(7L)).thenReturn(Optional.of(application));

        ResponseEntity<?> response = controller.createReassignmentRequest(request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat((Map<String, Object>) response.getBody())
                .containsEntry("success", false)
                .containsEntry("message", "Vous n'êtes pas le créateur de cette application");
    }
}
