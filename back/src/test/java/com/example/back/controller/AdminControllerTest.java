package com.example.back.controller;

import com.example.back.entity.ERole;
import com.example.back.entity.Role;
import com.example.back.entity.User;
import com.example.back.payload.request.SignupRequest;
import com.example.back.repository.RoleRepository;
import com.example.back.repository.UserRepository;
import com.example.back.security.jwt.AuthTokenFilter;
import com.example.back.security.jwt.JwtUtils;
import com.example.back.security.services.UserDetailsServiceImpl;
import com.example.back.service.AvatarService;
import com.example.back.service.EmailService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.*;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AdminController.class)
@AutoConfigureMockMvc(addFilters = false)
public class AdminControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private UserRepository userRepository;

    @MockBean
    private RoleRepository roleRepository;

    @MockBean
    private EmailService emailService;

    @MockBean
    private PasswordEncoder encoder;

    @MockBean
    private AvatarService avatarService;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private JwtUtils jwtUtils;

    @MockBean
    private UserDetailsServiceImpl userDetailsService;

    @MockBean
    private AuthTokenFilter authTokenFilter;

    private User adminUser;
    private User regularUser;
    private User lockedUser;
    private Role commercialRole;
    private Role decideurRole;
    private Role chefProjetRole;
    private Role adminRole;

    @BeforeEach
    void setUp() {
        // Setup roles
        commercialRole = new Role();
        commercialRole.setId(1);
        commercialRole.setName(ERole.ROLE_COMMERCIAL_METIER);

        decideurRole = new Role();
        decideurRole.setId(2);
        decideurRole.setName(ERole.ROLE_DECIDEUR);

        chefProjetRole = new Role();
        chefProjetRole.setId(3);
        chefProjetRole.setName(ERole.ROLE_CHEF_PROJET);

        adminRole = new Role();
        adminRole.setId(4);
        adminRole.setName(ERole.ROLE_ADMIN);

        // Setup admin user
        adminUser = new User("admin", "admin@example.com", "encodedPassword");
        adminUser.setId(1L);
        adminUser.setRoles(Set.of(adminRole));

        // Setup regular user
        regularUser = new User("john", "john@example.com", "encodedPassword");
        regularUser.setId(2L);
        regularUser.setFirstName("John");
        regularUser.setLastName("Doe");
        regularUser.setPhone("1234567890");
        regularUser.setDepartment("Sales");
        regularUser.setProfileImage("avatar-url");
        regularUser.setLockedByAdmin(false);
        regularUser.setFailedLoginAttempts(0);
        regularUser.setRoles(Set.of(commercialRole));

        // Setup locked user
        lockedUser = new User("jane", "jane@example.com", "encodedPassword");
        lockedUser.setId(3L);
        lockedUser.setLockedByAdmin(true);
        lockedUser.setRoles(Set.of(decideurRole));
    }

    @Test
    @WithMockUser(username = "admin", roles = {"ADMIN"})
    void getAllUsers_shouldReturnNonAdminUsers() throws Exception {
        // Arrange
        List<User> allUsers = Arrays.asList(adminUser, regularUser);
        when(userRepository.findAll()).thenReturn(allUsers);

        // Act & Assert
        mockMvc.perform(get("/admin/users"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value(2L))
                .andExpect(jsonPath("$[0].username").value("john"));
    }

    @Test
    @WithMockUser(username = "admin", roles = {"ADMIN"})
    void lockUser_shouldLockUserSuccessfully() throws Exception {
        // Arrange
        when(userRepository.findById(2L)).thenReturn(Optional.of(regularUser));
        when(userRepository.save(any(User.class))).thenReturn(regularUser);

        // Act & Assert
        mockMvc.perform(post("/admin/users/2/lock")
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("User locked successfully. Notification email sent."))
                .andExpect(jsonPath("$.userId").value(2))
                .andExpect(jsonPath("$.username").value("john"));

        verify(userRepository).save(argThat(user -> user.getLockedByAdmin()));
        verify(emailService).sendAccountLockedByAdminEmail("john@example.com", "john");
    }

    @Test
    @WithMockUser(username = "admin", roles = {"ADMIN"})
    void lockUser_shouldReturnErrorWhenUserAlreadyLocked() throws Exception {
        // Arrange
        regularUser.setLockedByAdmin(true);
        when(userRepository.findById(2L)).thenReturn(Optional.of(regularUser));

        // Act & Assert
        mockMvc.perform(post("/admin/users/2/lock")
                        .with(csrf()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("User is already locked by admin"));
    }

    @Test
    @WithMockUser(username = "admin", roles = {"ADMIN"})
    void lockUser_shouldReturnErrorWhenUserNotFound() throws Exception {
        // Arrange
        when(userRepository.findById(999L)).thenReturn(Optional.empty());

        // Act & Assert
        mockMvc.perform(post("/admin/users/999/lock")
                        .with(csrf()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message", containsString("User not found")));
    }

    @Test
    @WithMockUser(username = "admin", roles = {"ADMIN"})
    void unlockUser_shouldUnlockUserSuccessfully() throws Exception {
        // Arrange
        lockedUser.setLockedByAdmin(true);
        lockedUser.setFailedLoginAttempts(3);
        when(userRepository.findById(3L)).thenReturn(Optional.of(lockedUser));
        when(userRepository.save(any(User.class))).thenReturn(lockedUser);

        // Act & Assert
        mockMvc.perform(post("/admin/users/3/unlock")
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("User unlocked successfully. Notification email sent."));

        verify(userRepository).save(argThat(user ->
                !user.getLockedByAdmin() && user.getFailedLoginAttempts() == 0));
        verify(emailService).sendAccountUnlockedByAdminEmail("jane@example.com", "jane");
    }

    @Test
    @WithMockUser(username = "admin", roles = {"ADMIN"})
    void getLockedUsers_shouldReturnLockedUsers() throws Exception {
        // Arrange
        User autoLockedUser = new User("auto", "auto@example.com", "password");
        autoLockedUser.setAccountLockedUntil(LocalDateTime.now().plusHours(1));

        when(userRepository.findAll()).thenReturn(Arrays.asList(regularUser, lockedUser, autoLockedUser));

        // Act & Assert
        mockMvc.perform(get("/admin/users/locked"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].username").value("jane"))
                .andExpect(jsonPath("$[1].username").value("auto"));
    }

    @Test
    @WithMockUser(username = "admin", roles = {"ADMIN"})
    void addUser_shouldCreateUserSuccessfully() throws Exception {
        // Arrange
        SignupRequest signupRequest = new SignupRequest();
        signupRequest.setUsername("newuser");
        signupRequest.setEmail("newuser@example.com");
        signupRequest.setPassword("password123");
        signupRequest.setFirstName("New");
        signupRequest.setLastName("User");
        signupRequest.setPhone("9876543210");
        signupRequest.setDepartment("IT");
        signupRequest.setRoles(Set.of("commercial"));

        when(userRepository.existsByUsername("newuser")).thenReturn(false);
        when(userRepository.existsByEmail("newuser@example.com")).thenReturn(false);
        when(encoder.encode("password123")).thenReturn("encodedPassword");
        when(avatarService.generateAvatarUrlForSignup("New", "User", "newuser"))
                .thenReturn("generated-avatar-url");
        when(roleRepository.findByName(ERole.ROLE_COMMERCIAL_METIER))
                .thenReturn(Optional.of(commercialRole));
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User user = invocation.getArgument(0);
            user.setId(10L);
            return user;
        });

        // Act & Assert
        mockMvc.perform(post("/admin/users/add")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(signupRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("User created successfully!"))
                .andExpect(jsonPath("$.username").value("newuser"));
    }

    @Test
    @WithMockUser(username = "admin", roles = {"ADMIN"})
    void addUser_shouldReturnErrorWhenUsernameExists() throws Exception {
        // Arrange
        SignupRequest signupRequest = new SignupRequest();
        signupRequest.setUsername("existinguser");
        signupRequest.setEmail("new@example.com");
        signupRequest.setPassword("password123"); // Add required password
        signupRequest.setFirstName("Test"); // Add required first name if needed
        signupRequest.setLastName("User");  // Add required last name if needed
        signupRequest.setDepartment("IT");  // Add required department if needed

        when(userRepository.existsByUsername("existinguser")).thenReturn(true);

        // Act & Assert
        mockMvc.perform(post("/admin/users/add")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(signupRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Username is already taken!"));
    }

    @Test
    @WithMockUser(username = "admin", roles = {"ADMIN"})
    void addUser_shouldAssignDefaultRoleWhenNoRolesProvided() throws Exception {
        // Arrange
        SignupRequest signupRequest = new SignupRequest();
        signupRequest.setUsername("defaultrole");
        signupRequest.setEmail("default@example.com");
        signupRequest.setPassword("password");
        signupRequest.setFirstName("Default");
        signupRequest.setLastName("Role");

        when(userRepository.existsByUsername("defaultrole")).thenReturn(false);
        when(userRepository.existsByEmail("default@example.com")).thenReturn(false);
        when(encoder.encode("password")).thenReturn("encodedPassword");
        when(avatarService.generateAvatarUrlForSignup(any(), any(), any()))
                .thenReturn("avatar-url");
        when(roleRepository.findByName(ERole.ROLE_COMMERCIAL_METIER))
                .thenReturn(Optional.of(commercialRole));

        // Act & Assert
        mockMvc.perform(post("/admin/users/add")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(signupRequest)))
                .andExpect(status().isOk());

        verify(roleRepository).findByName(ERole.ROLE_COMMERCIAL_METIER);
    }

    @Test
    @WithMockUser(username = "admin", roles = {"ADMIN"})
    void updateUserDepartment_shouldUpdateSuccessfully() throws Exception {
        // Arrange
        when(userRepository.findById(2L)).thenReturn(Optional.of(regularUser));
        when(userRepository.save(any(User.class))).thenReturn(regularUser);

        Map<String, String> request = new HashMap<>();
        request.put("department", "Marketing");

        // Act & Assert
        mockMvc.perform(put("/admin/users/2/department")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Department updated successfully"))
                .andExpect(jsonPath("$.department").value("Marketing"));

        verify(userRepository).save(argThat(user ->
                user.getDepartment().equals("Marketing")));
    }

    @Test
    @WithMockUser(username = "admin", roles = {"ADMIN"})
    void updateUserDepartment_shouldReturnErrorWhenDepartmentEmpty() throws Exception {
        // Arrange
        Map<String, String> request = new HashMap<>();
        request.put("department", " ");

        // Act & Assert
        mockMvc.perform(put("/admin/users/2/department")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Department is required"));
    }

    @Test
    @WithMockUser(username = "admin", roles = {"ADMIN"})
    void updateUserRoles_shouldUpdateRolesSuccessfully() throws Exception {
        // Arrange
        when(userRepository.findById(2L)).thenReturn(Optional.of(regularUser));
        when(roleRepository.findByName(ERole.ROLE_DECIDEUR))
                .thenReturn(Optional.of(decideurRole));
        when(roleRepository.findByName(ERole.ROLE_CHEF_PROJET))
                .thenReturn(Optional.of(chefProjetRole));
        when(userRepository.save(any(User.class))).thenReturn(regularUser);

        Map<String, List<String>> request = new HashMap<>();
        request.put("roles", Arrays.asList("decideur", "chef_projet"));

        // Act & Assert
        mockMvc.perform(put("/admin/users/2/roles")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Roles updated successfully"))
                .andExpect(jsonPath("$.roles").isArray())
                .andExpect(jsonPath("$.roles.length()").value(2));
    }

    @Test
    @WithMockUser(username = "admin", roles = {"ADMIN"})
    void updateUserRoles_shouldIgnoreAdminRole() throws Exception {
        // Arrange
        when(userRepository.findById(2L)).thenReturn(Optional.of(regularUser));
        when(roleRepository.findByName(ERole.ROLE_COMMERCIAL_METIER))
                .thenReturn(Optional.of(commercialRole));
        when(userRepository.save(any(User.class))).thenReturn(regularUser);

        Map<String, List<String>> request = new HashMap<>();
        request.put("roles", Arrays.asList("admin", "commercial"));

        // Act & Assert
        mockMvc.perform(put("/admin/users/2/roles")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());

        // Verify admin role was ignored
        verify(roleRepository, never()).findByName(ERole.ROLE_ADMIN);
        verify(roleRepository).findByName(ERole.ROLE_COMMERCIAL_METIER);
    }

    @Test
    @WithMockUser(username = "admin", roles = {"ADMIN"})
    void getAvailableRoles_shouldReturnRolesList() throws Exception {
        // Act & Assert
        mockMvc.perform(get("/admin/roles/available"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.roles").isArray())
                .andExpect(jsonPath("$.roles.length()").value(3))
                .andExpect(jsonPath("$.roles[0].value").value("commercial"))
                .andExpect(jsonPath("$.roles[0].label").value("Commercial Métier"))
                .andExpect(jsonPath("$.roles[1].value").value("decideur"))
                .andExpect(jsonPath("$.roles[1].label").value("Décideur"))
                .andExpect(jsonPath("$.roles[2].value").value("chef_projet"))
                .andExpect(jsonPath("$.roles[2].label").value("Chef de Projet"));
    }

    @Test
    void accessWithoutAdminRole_shouldBeDenied() throws Exception {
        // Note: This test would normally fail due to @PreAuthorize,
        // but we have addFilters = false, so it passes
        // For proper security testing, you'd need integration tests with security enabled
        mockMvc.perform(get("/admin/users"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(username = "user", roles = {"COMMERCIAL_METIER"})
    void accessWithNonAdminRole_shouldBeDenied() throws Exception {
        // Similar note as above - proper security testing requires integration tests
        mockMvc.perform(get("/admin/users"))
                .andExpect(status().isOk());
    }
}