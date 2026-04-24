package com.example.back.controller;

import com.example.back.service.ChatAIService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ChatControllerTest {

    @Mock
    private ChatAIService chatAIService;

    @InjectMocks
    private ChatController controller;

    @Test
    void askQuestion_returnsAnswerFromService() {
        when(chatAIService.askQuestion("How are you?")).thenReturn("Fine");

        ResponseEntity<?> response = controller.askQuestion(Map.of("question", "How are you?"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat((Map<String, Object>) response.getBody())
                .containsEntry("success", true)
                .containsEntry("answer", "Fine")
                .containsEntry("question", "How are you?");
    }

    @Test
    void clearCache_callsServiceAndReturnsSuccessMessage() {
        ResponseEntity<?> response = controller.clearCache();

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat((Map<String, Object>) response.getBody())
                .containsEntry("success", true)
                .containsEntry("message", "Cache vidé avec succès");
    }
}
