package com.example.back.controller;

import com.example.back.entity.ERole;
import com.example.back.entity.User;
import com.example.back.payload.request.ProfileUpdateRequest;
import com.example.back.payload.response.MessageResponse;
import com.example.back.payload.response.ProfileResponse;
import com.example.back.repository.UserRepository;
import com.example.back.support.ControllerTestSupport;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProfileControllerTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private ProfileController controller;

    @AfterEach
    void tearDown() {
        ControllerTestSupport.clearAuthentication();
    }

    @Test
    void getMyProfile_returnsCurrentUserProfile() {
        User user = ControllerTestSupport.user(1L, "alice", ERole.ROLE_COMMERCIAL_METIER);
        user.setDepartment("IT");
        user.setPhone("111");

        ControllerTestSupport.authenticate(user);
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        ResponseEntity<?> response = controller.getMyProfile();

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        ProfileResponse profile = (ProfileResponse) response.getBody();
        assertThat(profile.getUsername()).isEqualTo("alice");
        assertThat(profile.getRoles()).contains("ROLE_COMMERCIAL_METIER");
    }

    @Test
    void updateNotificationMode_whenModeIsInvalid_returnsBadRequest() {
        User user = ControllerTestSupport.user(1L, "alice", ERole.ROLE_COMMERCIAL_METIER);
        ControllerTestSupport.authenticate(user);
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        ResponseEntity<?> response = controller.updateNotificationMode(Map.of("notifMode", "push"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(((MessageResponse) response.getBody()).getMessage())
                .isEqualTo("Invalid notification mode. Use 'email', 'sms', or 'both'.");
    }
}
