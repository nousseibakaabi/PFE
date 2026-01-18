package com.example.back.controller;

import com.example.back.security.jwt.AuthTokenFilter;
import com.example.back.security.jwt.JwtUtils;
import com.example.back.security.services.UserDetailsServiceImpl;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AvatarController.class)
@AutoConfigureMockMvc(addFilters = false)
public class AvatarControllerTest {

    @Autowired
    private MockMvc mockMvc;

    // Add these mock beans
    @MockBean
    private JwtUtils jwtUtils;

    @MockBean
    private UserDetailsServiceImpl userDetailsService;

    @MockBean
    private AuthTokenFilter authTokenFilter;

    @Test
    void testUploadAvatar_Success() throws Exception {
        // Arrange
        MockMultipartFile avatarFile = new MockMultipartFile(
                "avatar",
                "profile.png",
                MediaType.IMAGE_PNG_VALUE,
                "test image content".getBytes()
        );

        // Act & Assert - This will work but file won't actually be saved in test
        mockMvc.perform(MockMvcRequestBuilders.multipart("/profile/upload-avatar")
                        .file(avatarFile)
                        .contentType(MediaType.MULTIPART_FORM_DATA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.profileImage").isString());
    }

    @Test
    void testUploadAvatar_NoFile() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.multipart("/profile/upload-avatar")
                        .contentType(MediaType.MULTIPART_FORM_DATA))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("")); // Empty response body
    }

    @Test
    void testUploadAvatar_ValidFilenameEmptyContent() throws Exception {
        MockMultipartFile emptyFile = new MockMultipartFile(
                "avatar",
                "empty.png", // Add a filename
                MediaType.IMAGE_PNG_VALUE,
                new byte[0]
        );

        mockMvc.perform(MockMvcRequestBuilders.multipart("/profile/upload-avatar")
                        .file(emptyFile)
                        .contentType(MediaType.MULTIPART_FORM_DATA))
                .andExpect(status().isOk()) // Should work with valid filename
                .andExpect(jsonPath("$.profileImage").isString());
    }

    @Test
    void testUploadAvatar_TextFile() throws Exception {
        // Arrange - Upload a text file
        MockMultipartFile textFile = new MockMultipartFile(
                "avatar",
                "document.txt",
                MediaType.TEXT_PLAIN_VALUE,
                "This is a text file".getBytes()
        );

        // Act & Assert
        mockMvc.perform(MockMvcRequestBuilders.multipart("/profile/upload-avatar")
                        .file(textFile)
                        .contentType(MediaType.MULTIPART_FORM_DATA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.profileImage").isString());
    }
}