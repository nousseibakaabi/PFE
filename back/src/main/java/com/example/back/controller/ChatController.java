package com.example.back.controller;

import com.example.back.service.ChatAIService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

import static org.springframework.http.ResponseEntity.*;

@RestController
@RequestMapping("/api/chat")
@Slf4j
public class ChatController {

    private final ChatAIService chatAIService;

    public ChatController(ChatAIService chatAIService) {
        this.chatAIService = chatAIService;
    }

    @GetMapping("/cache/stats")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> getCacheStats() {
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("data", chatAIService.getCacheStats());
        response.put("geminiAvailable", chatAIService.isGeminiAvailable());
        return ok(response);
    }

    @DeleteMapping("/cache/clear")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> clearCache() {
        chatAIService.clearCache();
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "Cache vidé avec succès");
        return ok(response);
    }

    @DeleteMapping("/cache/question")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> clearCacheForQuestion(@RequestBody Map<String, String> request) {
        String question = request.get("question");
        chatAIService.clearCacheForQuestion(question);
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "Cache vidé pour la question: " + question);
        return ok(response);
    }

    @PostMapping("/ask")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> askQuestion(@RequestBody Map<String, String> request) {
        ResponseEntity<?> result;
        try {
            String question = request.get("question");
            log.info("📨 Nouvelle question: {}", question);

            String answer = chatAIService.askQuestion(question);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("answer", answer);
            response.put("question", question);

            result = ok(response);

        } catch (Exception e) {
            log.error("❌ Erreur: {}", e.getMessage());
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("error", e.getMessage());
            errorResponse.put("answer", "❌ L'IA n'a pas pu traiter votre question: " + e.getMessage());
            result = ok(errorResponse);
        }
        return result;
    }


}