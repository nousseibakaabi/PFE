package com.example.back.controller;

import com.example.back.entity.ERole;
import com.example.back.entity.User;
import com.example.back.payload.request.ChangePasswordRequest;
import com.example.back.payload.request.ForgotPasswordRequest;
import com.example.back.service.PasswordService;
import com.example.back.service.TokenService;
import com.example.back.support.ControllerTestSupport;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PasswordControllerTest {

    @Mock
    private PasswordService passwordService;

    @Mock
    private TokenService tokenService;

    @InjectMocks
    private PasswordController controller;

    @AfterEach
    void tearDown() {
        ControllerTestSupport.clearAuthentication();
    }

    @Test
    void forgotPassword_whenEmailDoesNotExist_returnsBadRequest() {
        ForgotPasswordRequest request = new ForgotPasswordRequest();
        request.setEmail("missing@example.com");

        when(passwordService.emailExists("missing@example.com")).thenReturn(false);
        when(tokenService.generateErrorResponse("No account found with this email address."))
                .thenReturn(Map.of("success", false, "message", "No account found with this email address."));

        ResponseEntity<?> response = controller.forgotPassword(request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat((Map<String, Object>) response.getBody())
                .containsEntry("success", false)
                .containsEntry("message", "No account found with this email address.");
    }

    @Test
    void changePassword_whenConfirmationDiffers_returnsBadRequest() {
        User user = ControllerTestSupport.user(1L, "alice", ERole.ROLE_COMMERCIAL_METIER);
        ControllerTestSupport.authenticate(user);

        ChangePasswordRequest request = new ChangePasswordRequest();
        ReflectionTestUtils.setField(request, "currentPassword", "old");
        ReflectionTestUtils.setField(request, "newPassword", "new-one");
        ReflectionTestUtils.setField(request, "confirmPassword", "new-two");

        when(tokenService.generateErrorResponse("New password and confirmation do not match"))
                .thenReturn(Map.of("success", false, "message", "New password and confirmation do not match"));

        ResponseEntity<?> response = controller.changePassword(request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat((Map<String, Object>) response.getBody())
                .containsEntry("success", false)
                .containsEntry("message", "New password and confirmation do not match");
        verifyNoInteractions(passwordService);
    }
}
