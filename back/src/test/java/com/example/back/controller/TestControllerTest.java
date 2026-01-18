package com.example.back.controller;

import com.example.back.security.jwt.AuthTokenFilter;
import com.example.back.security.jwt.JwtUtils;
import com.example.back.security.services.UserDetailsServiceImpl;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(TestController.class)
@AutoConfigureMockMvc(addFilters = false)
public class TestControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private JwtUtils jwtUtils;

    @MockBean
    private UserDetailsServiceImpl userDetailsService;

    @MockBean
    private AuthTokenFilter authTokenFilter;



    @Test
    void testUserAccess_WithoutAuthentication() throws Exception {
        mockMvc.perform(get("/test/user"))
                .andExpect(status().isOk())
                .andExpect(content().string("User Content."));
    }

    @Test
    @WithMockUser(username = "testuser", roles = {"COMMERCIAL_METIER"})
    void testUserAccess_WithCommercialRole() throws Exception {
        mockMvc.perform(get("/test/user"))
                .andExpect(status().isOk())
                .andExpect(content().string("User Content."));
    }

    @Test
    @WithMockUser(username = "adminuser", roles = {"ADMIN"})
    void testAdminAccess_WithAdminRole() throws Exception {
        mockMvc.perform(get("/test/admin"))
                .andExpect(status().isOk())
                .andExpect(content().string("Admin Board."));
    }

    @Test
    @WithMockUser(username = "user", roles = {"USER"})
    void testAdminAccess_WithoutAdminRole() throws Exception {
        mockMvc.perform(get("/test/admin"))
                .andExpect(status().isOk())
                .andExpect(content().string("Admin Board."));
    }

    @Test
    @WithMockUser(username = "commercial", roles = {"COMMERCIAL_METIER"})
    void testCommercialAccess() throws Exception {
        mockMvc.perform(get("/test/commercial"))
                .andExpect(status().isOk())
                .andExpect(content().string("Commercial Metier Board."));
    }

    @Test
    @WithMockUser(username = "decideur", roles = {"DECIDEUR"})
    void testDecideurAccess() throws Exception {
        mockMvc.perform(get("/test/decideur"))
                .andExpect(status().isOk())
                .andExpect(content().string("Decideur Board."));
    }

    @Test
    @WithMockUser(username = "chefprojet", roles = {"CHEF_PROJET"})
    void testChefProjetAccess() throws Exception {
        mockMvc.perform(get("/test/chef-projet"))
                .andExpect(status().isOk())
                .andExpect(content().string("Chef de Projet Board."));
    }

    @Test
    void testHealthEndpoint() throws Exception {
        mockMvc.perform(get("/test/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"))
                .andExpect(jsonPath("$.service").value("Test Controller"));
    }

    @Test
    void testPingEndpoint() throws Exception {
        mockMvc.perform(get("/test/ping"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("pong"))
                .andExpect(jsonPath("$.status").value("OK"));
    }
}