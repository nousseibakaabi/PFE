package com.example.back.config;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
public class SecurityConfigTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void testPublicEndpoints_NoAuthRequired() throws Exception {
        // Test actual public endpoints from your application
        mockMvc.perform(get("/test/all"))
                .andExpect(status().isOk());
    }

    @Test
    void testProtectedEndpoints_WithoutAuth() throws Exception {
        // These should return 401 (Unauthorized)
        mockMvc.perform(get("/profile/me"))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(get("/test/user"))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(get("/test/admin"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(username = "commercial", roles = {"COMMERCIAL_METIER"})
    void testCommercialMetierEndpoints() throws Exception {
        mockMvc.perform(get("/test/user"))
                .andExpect(status().isOk());

        // Commercial should NOT be able to access admin endpoint
        mockMvc.perform(get("/test/admin"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "admin", roles = {"ADMIN"})
    void testAdminEndpoints() throws Exception {
        mockMvc.perform(get("/test/admin"))
                .andExpect(status().isOk());

        mockMvc.perform(get("/test/user"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(username = "decideur", roles = {"DECIDEUR"})
    void testDecideurEndpoints() throws Exception {
        mockMvc.perform(get("/test/user"))
                .andExpect(status().isOk());

        mockMvc.perform(get("/test/admin"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "chef", roles = {"CHEF_PROJET"})
    void testChefProjetEndpoints() throws Exception {
        mockMvc.perform(get("/test/user"))
                .andExpect(status().isOk());

        mockMvc.perform(get("/test/admin"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "user", roles = {"USER"})
    void testGenericUserRole_ShouldFail() throws Exception {
        // USER role doesn't have access according to your expression
        mockMvc.perform(get("/test/user"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "commercial", roles = {"COMMERCIAL_METIER"})
    void testCommercialTryingToAccessAdminEndpoint() throws Exception {
        mockMvc.perform(get("/test/admin"))
                .andExpect(status().isForbidden());
    }
}