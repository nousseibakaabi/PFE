package com.example.back.controller;

import com.example.back.entity.ERole;
import com.example.back.entity.User;
import com.example.back.payload.request.LoginRequest;
import com.example.back.payload.response.MessageResponse;
import com.example.back.repository.UserRepository;
import com.example.back.security.jwt.JwtUtils;
import com.example.back.service.EmailService;
import com.example.back.service.HistoryService;
import com.example.back.service.LoginAttemptService;
import com.example.back.service.TwoFactorService;
import com.example.back.support.ControllerTestSupport;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthControllerTest {

    @Mock
    private AuthenticationManager authenticationManager;

    @Mock
    private UserRepository userRepository;

    @Mock
    private JwtUtils jwtUtils;

    @Mock
    private LoginAttemptService loginAttemptService;

    @Mock
    private EmailService emailService;

    @Mock
    private HistoryService historyService;

    @Mock
    private TwoFactorService twoFactorService;

    private AuthController controller;

    @BeforeEach
    void setUp() {
        controller = new AuthController(
                authenticationManager,
                userRepository,
                jwtUtils,
                loginAttemptService,
                emailService,
                historyService,
                twoFactorService
        );
    }

    @AfterEach
    void tearDown() {
        ControllerTestSupport.clearAuthentication();
    }

    @Test
    void authenticateUser_whenUserIsAdminLocked_returnsUnauthorized() {
        User lockedUser = ControllerTestSupport.user(1L, "locked", ERole.ROLE_COMMERCIAL_METIER);
        lockedUser.setLockedByAdmin(true);

        LoginRequest request = new LoginRequest();
        ReflectionTestUtils.setField(request, "usernameOrEmail", "locked");
        request.setPassword("secret");

        when(userRepository.findByUsernameOrEmail("locked")).thenReturn(Optional.of(lockedUser));

        ResponseEntity<?> response = controller.authenticateUser(request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat((Map<String, Object>) response.getBody())
                .containsEntry("error", "AccountLocked")
                .containsEntry("lockType", "ADMIN");
        verifyNoInteractions(authenticationManager);
    }

    @Test
    void verifyTwoFactor_whenSessionIsUnknown_returnsBadRequest() {
        ResponseEntity<?> response = controller.verifyTwoFactor(Map.of("tempToken", "missing", "code", "123456"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(((MessageResponse) response.getBody()).getMessage()).isEqualTo("Session invalide ou expirée");
    }

    @Test
    void logoutUser_whenAuthenticated_logsHistory() {
        User user = ControllerTestSupport.user(2L, "alice", ERole.ROLE_COMMERCIAL_METIER);
        ControllerTestSupport.authenticate(user);
        when(userRepository.findByUsername("alice")).thenReturn(Optional.of(user));

        ResponseEntity<?> response = controller.logoutUser();

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(((MessageResponse) response.getBody()).getMessage()).isEqualTo("Déconnexion réussie");
        verify(historyService).logUserLogout(user);
    }
}
