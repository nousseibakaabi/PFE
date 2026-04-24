package com.example.back.controller;

import com.example.back.entity.ERole;
import com.example.back.entity.Role;
import com.example.back.entity.User;
import com.example.back.payload.request.SignupRequest;
import com.example.back.payload.response.MessageResponse;
import com.example.back.repository.RoleRepository;
import com.example.back.repository.UserRepository;
import com.example.back.service.AvatarService;
import com.example.back.service.EmailService;
import com.example.back.service.HistoryService;
import com.example.back.support.ControllerTestSupport;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.time.LocalDateTime;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminControllerTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private RoleRepository roleRepository;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private AvatarService avatarService;
    @Mock
    private HistoryService historyService;
    @Mock
    private EmailService emailService;

    @InjectMocks
    private AdminController controller;

    @AfterEach
    void tearDown() {
        ControllerTestSupport.clearAuthentication();
    }

    @Test
    void getAllUsers_excludesAdminAndSystemUsers() {
        User admin = ControllerTestSupport.user(1L, "admin", ERole.ROLE_ADMIN);
        User system = ControllerTestSupport.user(2L, "system", ERole.ROLE_COMMERCIAL_METIER);
        User regular = ControllerTestSupport.user(3L, "regular", ERole.ROLE_COMMERCIAL_METIER);

        when(userRepository.findAll()).thenReturn(List.of(admin, system, regular));

        ResponseEntity<List<User>> response = controller.getAllUsers();

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).extracting(User::getUsername).containsExactly("regular");
    }

    @Test
    void lockUser_updatesStateAndSendsNotification() throws Exception {
        User admin = ControllerTestSupport.user(1L, "admin", ERole.ROLE_ADMIN);
        User regular = ControllerTestSupport.user(2L, "regular", ERole.ROLE_COMMERCIAL_METIER);
        ControllerTestSupport.authenticate(admin);

        when(userRepository.findByUsername("admin")).thenReturn(Optional.of(admin));
        when(userRepository.findById(2L)).thenReturn(Optional.of(regular));

        ResponseEntity<?> response = controller.lockUser(2L);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(((MessageResponse) response.getBody()).getMessage())
                .isEqualTo("User locked successfully and email notification sent");
        assertThat(regular.getLockedByAdmin()).isTrue();
        assertThat(regular.getAccountNonLocked()).isFalse();
        verify(historyService).logUserLock(regular, admin);
        verify(emailService).sendAccountLockedByAdminEmail(regular.getEmail(), regular.getUsername());
    }

    @Test
    void unlockUser_whenUserDoesNotExist_returnsBadRequest() {
        User admin = ControllerTestSupport.user(1L, "admin", ERole.ROLE_ADMIN);
        ControllerTestSupport.authenticate(admin);

        when(userRepository.findByUsername("admin")).thenReturn(Optional.of(admin));
        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        ResponseEntity<?> response = controller.unlockUser(99L);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat((Map<String, Object>) response.getBody())
                .containsEntry("success", false)
                .containsEntry("message", "User not found");
    }

    @Test
    void addUser_whenUsernameExists_returnsBadRequest() {
        SignupRequest request = new SignupRequest();
        request.setUsername("existing");
        request.setEmail("new@example.com");
        request.setPassword("password123");

        when(userRepository.existsByUsername("existing")).thenReturn(true);

        ResponseEntity<?> response = controller.addUser(request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat((Map<String, Object>) response.getBody())
                .containsEntry("success", false)
                .containsEntry("message", "Username is already taken!");
    }

    @Test
    void updateUserDepartment_logsDepartmentChange() {
        User admin = ControllerTestSupport.user(1L, "admin", ERole.ROLE_ADMIN);
        User regular = ControllerTestSupport.user(2L, "regular", ERole.ROLE_COMMERCIAL_METIER);
        regular.setDepartment("IT");
        ControllerTestSupport.authenticate(admin);

        when(userRepository.findByUsername("admin")).thenReturn(Optional.of(admin));
        when(userRepository.findById(2L)).thenReturn(Optional.of(regular));

        ResponseEntity<?> response = controller.updateUserDepartment(2L, Map.of("department", "Sales"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(((MessageResponse) response.getBody()).getMessage()).isEqualTo("User department updated successfully");
        verify(historyService).logUserDepartmentChange(regular, admin, "IT", "Sales");
    }

    @Test
    void getAvailableRoles_returnsThreeVisibleRoles() {
        ResponseEntity<?> response = controller.getAvailableRoles();

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        Map<String, Object> body = (Map<String, Object>) response.getBody();
        assertThat(body).containsEntry("success", true);
        assertThat((List<?>) body.get("roles")).hasSize(3);
    }

    @Test
    void getLockedUsers_returnsUsersLockedByAdminOrTimeWindow() {
        User regular = ControllerTestSupport.user(2L, "regular", ERole.ROLE_COMMERCIAL_METIER);
        User locked = ControllerTestSupport.user(3L, "locked", ERole.ROLE_COMMERCIAL_METIER);
        locked.setLockedByAdmin(true);
        User tempLocked = ControllerTestSupport.user(4L, "temp", ERole.ROLE_COMMERCIAL_METIER);
        tempLocked.setAccountLockedUntil(LocalDateTime.now().plusMinutes(10));

        when(userRepository.findAll()).thenReturn(List.of(regular, locked, tempLocked));

        ResponseEntity<List<User>> response = controller.getLockedUsers();

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).extracting(User::getUsername).containsExactlyInAnyOrder("locked", "temp");
    }

    @Test
    void updateUserRoles_updatesAssignedRoles() {
        User admin = ControllerTestSupport.user(1L, "admin", ERole.ROLE_ADMIN);
        User regular = ControllerTestSupport.user(2L, "regular", ERole.ROLE_COMMERCIAL_METIER);
        ControllerTestSupport.authenticate(admin);
        Role decideurRole = new Role();
        decideurRole.setName(ERole.ROLE_DECIDEUR);

        when(userRepository.findByUsername("admin")).thenReturn(Optional.of(admin));
        when(userRepository.findById(2L)).thenReturn(Optional.of(regular));
        when(roleRepository.findByName(ERole.ROLE_DECIDEUR)).thenReturn(Optional.of(decideurRole));

        ResponseEntity<?> response = controller.updateUserRoles(2L, Map.of("roles", List.of("ROLE_DECIDEUR")));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(((MessageResponse) response.getBody()).getMessage()).isEqualTo("User roles updated successfully");
    }
}
