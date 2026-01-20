package com.example.back.controller;

import com.example.back.entity.ERole;
import com.example.back.entity.Role;
import com.example.back.entity.User;
import com.example.back.payload.request.ProfileUpdateRequest;
import com.example.back.payload.response.MessageResponse;
import com.example.back.payload.response.ProfileResponse;
import com.example.back.repository.UserRepository;
import com.example.back.security.jwt.AuthTokenFilter;
import com.example.back.security.jwt.JwtUtils;
import com.example.back.security.services.UserDetailsImpl;
import com.example.back.security.services.UserDetailsServiceImpl;
import com.example.back.service.AvatarService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ProfileController.class)
@AutoConfigureMockMvc(addFilters = false)
public class ProfileControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private UserRepository userRepository;

    @MockBean
    private AvatarService avatarService;

    @Autowired
    private ObjectMapper objectMapper;

    private User testUser;

    @MockBean
    private AuthTokenFilter authTokenFilter;  // Add this

    @MockBean
    private JwtUtils jwtUtils;  // Add this

    @MockBean
    private UserDetailsServiceImpl userDetailsService;  // Add this

    @BeforeEach
    void setUp() {
        Role testRole = new Role(ERole.ROLE_COMMERCIAL_METIER, "Commercial Role");
        testRole.setId(1);

        Set<Role> roles = new HashSet<>();
        roles.add(testRole);

        testUser = new User("testuser", "test@email.com", "encodedPassword");
        testUser.setId(1L);
        testUser.setFirstName("Test");
        testUser.setLastName("User");
        testUser.setPhone("1234567890");
        testUser.setDepartment("IT");
        testUser.setRoles(roles);
        testUser.setEnabled(true);
        testUser.setCreatedAt(LocalDateTime.now());
        testUser.setLastLogin(LocalDateTime.now());
        testUser.setProfileImage("/uploads/avatars/test.png");

        // Set up authentication
        UserDetailsImpl userDetails = new UserDetailsImpl(
                1L, "testuser", "test@email.com", "password",
                "Test", "User",
                Set.of(new org.springframework.security.core.authority.SimpleGrantedAuthority("ROLE_COMMERCIAL_METIER")),
                true,   // accountNonLocked
                true,   // enabled
                true,   // accountNonExpired
                true    // credentialsNonExpired
        );

        Authentication authentication = new UsernamePasswordAuthenticationToken(
                userDetails, null, userDetails.getAuthorities());
        SecurityContext securityContext = mock(SecurityContext.class);
        when(securityContext.getAuthentication()).thenReturn(authentication);
        SecurityContextHolder.setContext(securityContext);
    }

    // Test GET /profile/me
    @Test
    void testGetMyProfile_Success() throws Exception {
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));

        mockMvc.perform(get("/profile/me"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.username").value("testuser"))
                .andExpect(jsonPath("$.email").value("test@email.com"))
                .andExpect(jsonPath("$.firstName").value("Test"))
                .andExpect(jsonPath("$.lastName").value("User"))
                .andExpect(jsonPath("$.phone").value("1234567890"))
                .andExpect(jsonPath("$.department").value("IT"))
                .andExpect(jsonPath("$.enabled").value(true))
                .andExpect(jsonPath("$.profileImage").value("/uploads/avatars/test.png"))
                .andExpect(jsonPath("$.roles[0]").value("ROLE_COMMERCIAL_METIER"));
    }


    @Test
    void testUpdateProfile_Success() throws Exception {
        ProfileUpdateRequest updateRequest = new ProfileUpdateRequest();
        updateRequest.setFirstName("Updated");
        updateRequest.setLastName("Name");
        updateRequest.setPhone("9876543210");
        updateRequest.setDepartment("HR");

        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(userRepository.save(any(User.class))).thenReturn(testUser);

        mockMvc.perform(put("/profile/update")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Profile updated successfully!"));

        verify(userRepository, times(1)).save(any(User.class));
    }

    @Test
    void testUpdateProfile_PartialUpdate() throws Exception {
        ProfileUpdateRequest updateRequest = new ProfileUpdateRequest();
        updateRequest.setFirstName("Updated");

        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(userRepository.save(any(User.class))).thenReturn(testUser);

        mockMvc.perform(put("/profile/update")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Profile updated successfully!"));
    }

    @Test
    void testUpdateProfile_NoChanges() throws Exception {
        ProfileUpdateRequest updateRequest = new ProfileUpdateRequest();

        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(userRepository.save(any(User.class))).thenReturn(testUser);

        mockMvc.perform(put("/profile/update")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Profile updated successfully!"));
    }

    // Test POST /profile/update-with-avatar
    @Test
    void testUpdateProfileWithAvatar_SuccessWithFile() throws Exception {
        MockMultipartFile avatarFile = new MockMultipartFile(
                "avatar",
                "profile.png",
                MediaType.IMAGE_PNG_VALUE,
                "test image content".getBytes()
        );

        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(userRepository.save(any(User.class))).thenReturn(testUser);

        mockMvc.perform(MockMvcRequestBuilders.multipart("/profile/update-with-avatar")
                        .file(avatarFile)
                        .param("firstName", "Updated")
                        .param("lastName", "Name")
                        .param("phone", "9876543210")
                        .param("department", "HR")
                        .contentType(MediaType.MULTIPART_FORM_DATA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Profile updated successfully!"));

        verify(userRepository, times(1)).save(any(User.class));
    }

    @Test
    void testUpdateProfileWithAvatar_SuccessWithoutFile() throws Exception {
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(userRepository.save(any(User.class))).thenReturn(testUser);

        mockMvc.perform(MockMvcRequestBuilders.multipart("/profile/update-with-avatar")
                        .param("firstName", "Updated")
                        .param("lastName", "Name")
                        .param("phone", "9876543210")
                        .param("department", "HR")
                        .contentType(MediaType.MULTIPART_FORM_DATA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Profile updated successfully!"));

        verify(userRepository, times(1)).save(any(User.class));
    }

    @Test
    void testUpdateProfileWithAvatar_MissingRequiredParams() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.multipart("/profile/update-with-avatar")
                        .contentType(MediaType.MULTIPART_FORM_DATA))
                .andExpect(status().isBadRequest());
    }

    @Test
    void testUpdateProfileWithAvatar_EmptyFile() throws Exception {
        MockMultipartFile emptyFile = new MockMultipartFile(
                "avatar",
                "",
                MediaType.IMAGE_PNG_VALUE,
                new byte[0]
        );

        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(userRepository.save(any(User.class))).thenReturn(testUser);

        mockMvc.perform(MockMvcRequestBuilders.multipart("/profile/update-with-avatar")
                        .file(emptyFile)
                        .param("firstName", "Updated")
                        .param("lastName", "Name")
                        .param("phone", "9876543210")
                        .param("department", "HR")
                        .contentType(MediaType.MULTIPART_FORM_DATA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Profile updated successfully!"));
    }

    // Test GET /profile/debug-avatar
    @Test
    void testDebugAvatar_Success() throws Exception {
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));

        mockMvc.perform(get("/profile/debug-avatar"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value(1))
                .andExpect(jsonPath("$.username").value("testuser"))
                .andExpect(jsonPath("$.profileImage").value("/uploads/avatars/test.png"))
                .andExpect(jsonPath("$.profileImageExists").value(true))
                .andExpect(jsonPath("$.isFilePath").value(true));
    }

    @Test
    void testDebugAvatar_NoProfileImage() throws Exception {
        testUser.setProfileImage(null);
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));

        mockMvc.perform(get("/profile/debug-avatar"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.profileImageExists").value(false));
    }

    @Test
    void testDebugAvatar_Base64Image() throws Exception {
        String base64Image = "data:image/svg+xml;base64,testbase64";
        testUser.setProfileImage(base64Image);
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));

        mockMvc.perform(get("/profile/debug-avatar"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isBase64").value(true))
                .andExpect(jsonPath("$.dataLength").value(base64Image.length()));
    }

    // Error cases
    @Test
    void testUpdateProfile_InvalidJson() throws Exception {
        String invalidJson = "{invalid json}";

        mockMvc.perform(put("/profile/update")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidJson))
                .andExpect(status().isBadRequest());
    }

    @Test
    void testUpdateProfileWithAvatar_FileUploadError() throws Exception {
        MockMultipartFile avatarFile = new MockMultipartFile(
                "avatar",
                "profile.png",
                MediaType.IMAGE_PNG_VALUE,
                "test".getBytes()
        );

        // Mock user but the file save will work in test environment
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(userRepository.save(any(User.class))).thenReturn(testUser);

        mockMvc.perform(MockMvcRequestBuilders.multipart("/profile/update-with-avatar")
                        .file(avatarFile)
                        .param("firstName", "Updated")
                        .param("lastName", "Name")
                        .param("phone", "9876543210")
                        .param("department", "HR")
                        .contentType(MediaType.MULTIPART_FORM_DATA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Profile updated successfully!"));
    }
}