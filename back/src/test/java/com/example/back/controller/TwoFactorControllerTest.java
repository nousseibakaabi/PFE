package com.example.back.controller;

import com.example.back.entity.ERole;
import com.example.back.entity.User;
import com.example.back.payload.request.TwoFactorRequest;
import com.example.back.payload.response.MessageResponse;
import com.example.back.repository.UserRepository;
import com.example.back.service.TwoFactorService;
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
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TwoFactorControllerTest {

    @Mock
    private TwoFactorService twoFactorService;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private TwoFactorController controller;

    @AfterEach
    void tearDown() {
        ControllerTestSupport.clearAuthentication();
    }

    @Test
    void setupTwoFactor_returnsGeneratedSecretAndQrCode() {
        User user = ControllerTestSupport.user(1L, "alice", ERole.ROLE_COMMERCIAL_METIER);
        ControllerTestSupport.authenticate(user);
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(twoFactorService.generateSecret(user))
                .thenReturn(new TwoFactorService.TwoFactorSetup("secret", "qr-url", List.of("A", "B")));

        ResponseEntity<?> response = controller.setupTwoFactor();

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat((Map<String, Object>) response.getBody())
                .containsEntry("success", true)
                .containsEntry("secret", "secret")
                .containsEntry("qrCodeUrl", "qr-url");
    }

    @Test
    void verifyAndEnable_whenCodeIsNotNumeric_returnsBadRequest() {
        User user = ControllerTestSupport.user(1L, "alice", ERole.ROLE_COMMERCIAL_METIER);
        user.setTwoFactorSecret("secret");
        ControllerTestSupport.authenticate(user);
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        TwoFactorRequest request = new TwoFactorRequest();
        request.setCode("abc");

        ResponseEntity<?> response = controller.verifyAndEnable(request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(((MessageResponse) response.getBody()).getMessage()).isEqualTo("Invalid code format");
    }

    @Test
    void disableTwoFactor_whenCodeIsValid_disablesFeature() {
        User user = ControllerTestSupport.user(1L, "alice", ERole.ROLE_COMMERCIAL_METIER);
        user.setTwoFactorSecret("secret");
        ControllerTestSupport.authenticate(user);
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(twoFactorService.verifyCode("secret", 123456)).thenReturn(true);

        TwoFactorRequest request = new TwoFactorRequest();
        request.setCode("123456");

        ResponseEntity<?> response = controller.disableTwoFactor(request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(((MessageResponse) response.getBody()).getMessage()).isEqualTo("2FA désactivée avec succès !");
        verify(twoFactorService).disableTwoFactor(user);
    }

    @Test
    void getTwoFactorStatus_returnsFlag() {
        User user = ControllerTestSupport.user(1L, "alice", ERole.ROLE_COMMERCIAL_METIER);
        ControllerTestSupport.authenticate(user);
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(twoFactorService.isTwoFactorEnabled(user)).thenReturn(true);

        ResponseEntity<?> response = controller.getTwoFactorStatus();

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat((Map<String, Object>) response.getBody()).containsEntry("enabled", true);
    }
}
