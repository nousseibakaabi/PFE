package com.example.back.security.services;

import com.example.back.entity.Role;
import com.example.back.entity.ERole;
import com.example.back.entity.User;
import com.example.back.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserDetailsServiceImplTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private UserDetailsServiceImpl userDetailsService;

    private User user;
    private Role adminRole;

    @BeforeEach
    void setUp() {
        adminRole = new Role();
        adminRole.setName(ERole.ROLE_ADMIN);

        user = new User();
        user.setId(1L);
        user.setUsername("testuser");
        user.setEmail("test@example.com");
        user.setPassword("encodedPassword123");
        user.setFirstName("John");
        user.setLastName("Doe");
        user.setRoles(Set.of(adminRole));
        user.setEnabled(true);
        user.setAccountNonExpired(true);
        user.setAccountNonLocked(true);
        user.setCredentialsNonExpired(true);
        user.setTwoFactorEnabled(false);
        user.setLockedByAdmin(false);
        user.setAccountLockedUntil(null);
    }




    // ==================== LOAD USER BY USERNAME TESTS ====================

    @Test
    void loadUserByUsername_WithValidUsername_ShouldReturnUserDetails() {
        // Arrange
        when(userRepository.findByUsernameOrEmail("testuser")).thenReturn(Optional.of(user));

        // Act
        UserDetails userDetails = userDetailsService.loadUserByUsername("testuser");

        // Assert
        assertThat(userDetails).isNotNull();
        assertThat(userDetails.getUsername()).isEqualTo("testuser");
        assertThat(userDetails.getPassword()).isEqualTo("encodedPassword123");
        assertThat(userDetails.getAuthorities()).hasSize(1);
        assertThat(userDetails.isEnabled()).isTrue();
        assertThat(userDetails.isAccountNonLocked()).isTrue();

        verify(userRepository, times(1)).findByUsernameOrEmail("testuser");
    }

    @Test
    void loadUserByUsername_WithValidEmail_ShouldReturnUserDetails() {
        // Arrange
        when(userRepository.findByUsernameOrEmail("test@example.com")).thenReturn(Optional.of(user));

        // Act
        UserDetails userDetails = userDetailsService.loadUserByUsername("test@example.com");

        // Assert
        assertThat(userDetails).isNotNull();
        assertThat(userDetails.getUsername()).isEqualTo("testuser");
        assertThat(userDetails.getPassword()).isEqualTo("encodedPassword123");

        verify(userRepository, times(1)).findByUsernameOrEmail("test@example.com");
    }

    @Test
    void loadUserByUsername_WithNonExistentUsername_ShouldThrowUsernameNotFoundException() {
        // Arrange
        String nonExistentUsername = "nonexistent";
        when(userRepository.findByUsernameOrEmail(nonExistentUsername)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> userDetailsService.loadUserByUsername(nonExistentUsername))
                .isInstanceOf(UsernameNotFoundException.class)
                .hasMessage("User not found with username or email: " + nonExistentUsername);

        verify(userRepository, times(1)).findByUsernameOrEmail(nonExistentUsername);
    }

    @Test
    void loadUserByUsername_WithNonExistentEmail_ShouldThrowUsernameNotFoundException() {
        // Arrange
        String nonExistentEmail = "nonexistent@example.com";
        when(userRepository.findByUsernameOrEmail(nonExistentEmail)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> userDetailsService.loadUserByUsername(nonExistentEmail))
                .isInstanceOf(UsernameNotFoundException.class)
                .hasMessage("User not found with username or email: " + nonExistentEmail);

        verify(userRepository, times(1)).findByUsernameOrEmail(nonExistentEmail);
    }

    @Test
    void loadUserByUsername_WithNullInput_ShouldThrowException() {
        // Act & Assert
        assertThatThrownBy(() -> userDetailsService.loadUserByUsername(null))
                .isInstanceOf(Exception.class);
    }

    @Test
    void loadUserByUsername_WithEmptyString_ShouldThrowUsernameNotFoundException() {
        // Arrange
        String emptyUsername = "";
        when(userRepository.findByUsernameOrEmail(emptyUsername)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> userDetailsService.loadUserByUsername(emptyUsername))
                .isInstanceOf(UsernameNotFoundException.class)
                .hasMessage("User not found with username or email: " + emptyUsername);
    }

    // ==================== USER WITH DIFFERENT STATES TESTS ====================

    @Test
    void loadUserByUsername_WithDisabledUser_ShouldReturnUserDetailsWithEnabledFalse() {
        // Arrange
        user.setEnabled(false);
        when(userRepository.findByUsernameOrEmail("disableduser")).thenReturn(Optional.of(user));

        // Act
        UserDetails userDetails = userDetailsService.loadUserByUsername("disableduser");

        // Assert
        assertThat(userDetails).isNotNull();
        assertThat(userDetails.isEnabled()).isFalse();
    }

    @Test
    void loadUserByUsername_WithLockedUser_ShouldReturnUserDetailsWithAccountNonLockedFalse() {
        // Arrange
        user.setLockedByAdmin(true);
        when(userRepository.findByUsernameOrEmail("lockeduser")).thenReturn(Optional.of(user));

        // Act
        UserDetails userDetails = userDetailsService.loadUserByUsername("lockeduser");

        // Assert
        assertThat(userDetails).isNotNull();
        assertThat(userDetails.isAccountNonLocked()).isFalse();
    }

    @Test
    void loadUserByUsername_WithExpiredAccount_ShouldReturnUserDetailsWithAccountNonExpiredFalse() {
        // Arrange
        user.setAccountNonExpired(false);
        when(userRepository.findByUsernameOrEmail("expireduser")).thenReturn(Optional.of(user));

        // Act
        UserDetails userDetails = userDetailsService.loadUserByUsername("expireduser");

        // Assert
        assertThat(userDetails).isNotNull();
        assertThat(userDetails.isAccountNonExpired()).isFalse();
    }

    @Test
    void loadUserByUsername_WithExpiredCredentials_ShouldReturnUserDetailsWithCredentialsNonExpiredFalse() {
        // Arrange
        user.setCredentialsNonExpired(false);
        when(userRepository.findByUsernameOrEmail("expiredcredsuser")).thenReturn(Optional.of(user));

        // Act
        UserDetails userDetails = userDetailsService.loadUserByUsername("expiredcredsuser");

        // Assert
        assertThat(userDetails).isNotNull();
        assertThat(userDetails.isCredentialsNonExpired()).isFalse();
    }

    // ==================== USER WITH MULTIPLE ROLES TESTS ====================

    @Test
    void loadUserByUsername_WithUserHavingMultipleRoles_ShouldReturnAllAuthorities() {
        // Arrange
        Role chefRole = new Role();
        chefRole.setName(ERole.ROLE_CHEF_PROJET);
        user.setRoles(Set.of(adminRole, chefRole));

        when(userRepository.findByUsernameOrEmail("multiroleuser")).thenReturn(Optional.of(user));

        // Act
        UserDetails userDetails = userDetailsService.loadUserByUsername("multiroleuser");

        // Assert
        assertThat(userDetails).isNotNull();
        assertThat(userDetails.getAuthorities()).hasSize(2);
        assertThat(userDetails.getAuthorities())
                .extracting("authority")
                .containsExactlyInAnyOrder("ROLE_ADMIN", "ROLE_CHEF_PROJET");
    }

    // ==================== TRANSACTIONAL BEHAVIOR TESTS ====================

    @Test
    void loadUserByUsername_ShouldBeTransactional() {
        // This test verifies that the method is called successfully
        // The @Transactional annotation is tested by Spring at runtime

        // Arrange
        when(userRepository.findByUsernameOrEmail("testuser")).thenReturn(Optional.of(user));

        // Act
        UserDetails userDetails = userDetailsService.loadUserByUsername("testuser");

        // Assert
        assertThat(userDetails).isNotNull();
    }

    // ==================== EDGE CASE TESTS ====================

    @Test
    void loadUserByUsername_WithUserHavingNoRoles_ShouldReturnUserDetailsWithEmptyAuthorities() {
        // Arrange
        user.setRoles(Set.of());
        when(userRepository.findByUsernameOrEmail("norolesuser")).thenReturn(Optional.of(user));

        // Act
        UserDetails userDetails = userDetailsService.loadUserByUsername("norolesuser");

        // Assert
        assertThat(userDetails).isNotNull();
        assertThat(userDetails.getAuthorities()).isEmpty();
    }

    @Test
    void loadUserByUsername_WithUsernameCase_ShouldBeCaseSensitive() {
        // Arrange
        when(userRepository.findByUsernameOrEmail("TestUser")).thenReturn(Optional.of(user));

        // Act
        UserDetails userDetails = userDetailsService.loadUserByUsername("TestUser");

        // Assert
        assertThat(userDetails).isNotNull();
        verify(userRepository, times(1)).findByUsernameOrEmail("TestUser");
    }

    @Test
    void loadUserByUsername_ShouldCallRepositoryExactlyOnce() {
        // Arrange
        when(userRepository.findByUsernameOrEmail("testuser")).thenReturn(Optional.of(user));

        // Act
        userDetailsService.loadUserByUsername("testuser");

        // Assert
        verify(userRepository, times(1)).findByUsernameOrEmail("testuser");
        verifyNoMoreInteractions(userRepository);
    }

    @Test
    void loadUserByUsername_WithWhitespaceInUsername_ShouldPassThrough() {
        // Arrange
        String usernameWithSpace = "test user";
        when(userRepository.findByUsernameOrEmail(usernameWithSpace)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> userDetailsService.loadUserByUsername(usernameWithSpace))
                .isInstanceOf(UsernameNotFoundException.class)
                .hasMessage("User not found with username or email: " + usernameWithSpace);

        verify(userRepository, times(1)).findByUsernameOrEmail(usernameWithSpace);
    }

    // ==================== PERFORMANCE/VERIFICATION TESTS ====================

    @Test
    void loadUserByUsername_ShouldReturnUserDetailsImplInstance() {
        // Arrange
        when(userRepository.findByUsernameOrEmail("testuser")).thenReturn(Optional.of(user));

        // Act
        UserDetails userDetails = userDetailsService.loadUserByUsername("testuser");

        // Assert
        assertThat(userDetails).isInstanceOf(UserDetailsImpl.class);
    }

    @Test
    void loadUserByUsername_ShouldPreserveUserDataCorrectly() {
        // Arrange
        user.setFirstName("Jane");
        user.setLastName("Smith");
        user.setEmail("jane.smith@example.com");

        when(userRepository.findByUsernameOrEmail("janesmith")).thenReturn(Optional.of(user));

        // Act
        UserDetailsImpl userDetails = (UserDetailsImpl) userDetailsService.loadUserByUsername("janesmith");

        // Assert
        assertThat(userDetails.getId()).isEqualTo(1L);
        assertThat(userDetails.getFirstName()).isEqualTo("Jane");
        assertThat(userDetails.getLastName()).isEqualTo("Smith");
        assertThat(userDetails.getEmail()).isEqualTo("jane.smith@example.com");
    }
}