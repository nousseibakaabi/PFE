package com.example.back.controller;

import com.example.back.service.SmsService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SmsControllerTest {

    @Mock
    private SmsService smsService;

    @InjectMocks
    private SmsController controller;

    @Test
    void testSms_sendsMessageAndReturnsSuccess() {
        ResponseEntity<?> response = controller.testSms("+21611111111", "hello");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat((Map<String, Object>) response.getBody())
                .containsEntry("success", true)
                .containsEntry("message", "Test SMS envoyé (vérifiez les logs)");
        verify(smsService).sendTestSms("+21611111111", "hello");
    }

    @Test
    void getSmsStatus_exposesProviderConfiguration() {
        when(smsService.isSmsEnabled()).thenReturn(true);
        when(smsService.getSmsProvider()).thenReturn("twilio");

        ResponseEntity<?> response = controller.getSmsStatus();

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat((Map<String, Object>) response.getBody())
                .containsEntry("smsEnabled", true)
                .containsEntry("provider", "twilio");
    }
}
