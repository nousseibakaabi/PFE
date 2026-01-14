package com.example.back.controller;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/test")
public class TestController {

    @GetMapping("/all")
    public Map<String, String> allAccess() {
        Map<String, String> response = new HashMap<>();
        response.put("message", "Public Content.");
        response.put("status", "SUCCESS");
        return response;
    }

    @GetMapping("/user")
    @PreAuthorize("hasRole('COMMERCIAL_METIER') or hasRole('ADMIN') or hasRole('DECIDEUR') or hasRole('CHEF_PROJET')")
    public String userAccess() {
        return "User Content.";
    }

    @GetMapping("/admin")
    @PreAuthorize("hasRole('ADMIN')")
    public String adminAccess() {
        return "Admin Board.";
    }

    @GetMapping("/commercial")
    @PreAuthorize("hasRole('COMMERCIAL_METIER')")
    public String commercialAccess() {
        return "Commercial Metier Board.";
    }

    @GetMapping("/decideur")
    @PreAuthorize("hasRole('DECIDEUR')")
    public String decideurAccess() {
        return "Decideur Board.";
    }

    @GetMapping("/chef-projet")
    @PreAuthorize("hasRole('CHEF_PROJET')")
    public String chefProjetAccess() {
        return "Chef de Projet Board.";
    }

    // Add health endpoint
    @GetMapping("/health")
    public Map<String, Object> health() {
        Map<String, Object> response = new HashMap<>();
        response.put("status", "UP");
        response.put("service", "Test Controller");
        response.put("timestamp", System.currentTimeMillis());
        return response;
    }

    // Add ping endpoint
    @GetMapping("/ping")
    public Map<String, String> ping() {
        Map<String, String> response = new HashMap<>();
        response.put("message", "pong");
        response.put("status", "OK");
        return response;
    }
}