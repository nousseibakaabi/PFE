package com.example.back.controller;

import com.example.back.service.SmsService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/sms")
@Slf4j
public class SmsController {

    @Autowired
    private SmsService smsService;

    @PostMapping("/test")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> testSms(@RequestParam String phone, @RequestParam String message) {
        try {
            log.info("📱 Test SMS requested - To: {}, Message: {}", phone, message);
            smsService.sendTestSms(phone, message);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Test SMS envoyé (vérifiez les logs)");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("❌ Test SMS failed: {}", e.getMessage());
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }

    @GetMapping("/status")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> getSmsStatus() {
        Map<String, Object> status = new HashMap<>();
        status.put("smsEnabled", smsService.isSmsEnabled());
        status.put("provider", smsService.getSmsProvider()); // À ajouter dans SmsService
        return ResponseEntity.ok(status);
    }
}