package com.example.back.controller;

import com.example.back.entity.ERole;
import com.example.back.entity.User;
import com.example.back.payload.request.MailGroupRequest;
import com.example.back.payload.response.MailGroupResponse;
import com.example.back.repository.UserRepository;
import com.example.back.service.MailGroupService;
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
class MailGroupControllerTest {

    @Mock
    private MailGroupService groupService;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private MailGroupController controller;

    @AfterEach
    void tearDown() {
        ControllerTestSupport.clearAuthentication();
    }

    @Test
    void getUserGroups_returnsCurrentUserGroups() {
        User user = ControllerTestSupport.user(1L, "alice", ERole.ROLE_COMMERCIAL_METIER);
        MailGroupResponse group = new MailGroupResponse();
        group.setId(4L);
        group.setName("Project Team");

        ControllerTestSupport.authenticate(user);
        when(userRepository.findByUsername("alice")).thenReturn(Optional.of(user));
        when(groupService.getUserGroups(user)).thenReturn(List.of(group));

        ResponseEntity<?> response = controller.getUserGroups();

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat((Map<String, Object>) response.getBody())
                .containsEntry("success", true)
                .containsEntry("data", List.of(group));
    }

    @Test
    void createGroup_whenServiceThrows_returnsBadRequest() {
        User user = ControllerTestSupport.user(1L, "alice", ERole.ROLE_COMMERCIAL_METIER);
        MailGroupRequest request = new MailGroupRequest();
        request.setName("Project Team");

        ControllerTestSupport.authenticate(user);
        when(userRepository.findByUsername("alice")).thenReturn(Optional.of(user));
        when(groupService.createGroup(request, user)).thenThrow(new RuntimeException("duplicate"));

        ResponseEntity<?> response = controller.createGroup(request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat((Map<String, Object>) response.getBody())
                .containsEntry("success", false)
                .containsEntry("message", "duplicate");
    }
}
