package com.example.back.controller;

import com.example.back.entity.ERole;
import com.example.back.entity.User;
import com.example.back.repository.UserRepository;
import com.example.back.support.ControllerTestSupport;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserControllerTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private UserController controller;

    @Test
    void getAllUsers_returnsMappedUsers() {
        User user = ControllerTestSupport.user(1L, "alice", ERole.ROLE_COMMERCIAL_METIER);
        when(userRepository.findAll()).thenReturn(List.of(user));

        ResponseEntity<?> response = controller.getAllUsers();

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat((Map<String, Object>) response.getBody())
                .containsEntry("success", true)
                .containsEntry("count", 1);
    }

    @Test
    void getUsersByRole_filtersOnRoleName() {
        User commercial = ControllerTestSupport.user(1L, "alice", ERole.ROLE_COMMERCIAL_METIER);
        User decideur = ControllerTestSupport.user(2L, "bob", ERole.ROLE_DECIDEUR);
        when(userRepository.findAll()).thenReturn(List.of(commercial, decideur));

        ResponseEntity<?> response = controller.getUsersByRole("commercial_metier");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat((Map<String, Object>) response.getBody())
                .containsEntry("success", true)
                .containsEntry("count", 1);
    }
}
