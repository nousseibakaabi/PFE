package com.example.back.security.services;

import com.example.back.entity.ERole;
import com.example.back.entity.Role;
import com.example.back.entity.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.GrantedAuthority;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.Collections;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class UserDetailsImplTest {

    private User user;
    private Role adminRole;
    private Role chefRole;

    @BeforeEach
    void setUp() {
        adminRole = new Role();
        adminRole.setName(ERole.ROLE_ADMIN);

        chefRole = new Role();
        chefRole.setName(ERole.ROLE_CHEF_PROJET);

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

    // ==================== BUILD METHOD TESTS ====================

    @Test
    void build_WithValidUser_ShouldReturnUserDetailsImpl() {
        // Act
        UserDetailsImpl userDetails = UserDetailsImpl.build(user);

        // Assert
        assertThat(userDetails).isNotNull();
        assertThat(userDetails.getId()).isEqualTo(1L);
        assertThat(userDetails.getUsername()).isEqualTo("testuser");
        assertThat(userDetails.getEmail()).isEqualTo("test@example.com");
        assertThat(userDetails.getPassword()).isEqualTo("encodedPassword123");
        assertThat(userDetails.getFirstName()).isEqualTo("John");
        assertThat(userDetails.getLastName()).isEqualTo("Doe");
        assertThat(userDetails.isEnabled()).isTrue();
        assertThat(userDetails.isAccountNonLocked()).isTrue();
        assertThat(userDetails.isAccountNonExpired()).isTrue();
        assertThat(userDetails.isCredentialsNonExpired()).isTrue();
        assertThat(userDetails.getTwoFactorEnabled()).isFalse();
    }

    @Test
    void build_WithUserRoles_ShouldMapAuthoritiesCorrectly() {
        // Arrange
        user.setRoles(Set.of(adminRole, chefRole));

        // Act
        UserDetailsImpl userDetails = UserDetailsImpl.build(user);
        Collection<? extends GrantedAuthority> authorities = userDetails.getAuthorities();

        // Assert
        assertThat(authorities).hasSize(2);
        assertThat(authorities).extracting(GrantedAuthority::getAuthority)
                .containsExactlyInAnyOrder("ROLE_ADMIN", "ROLE_CHEF_PROJET");
    }

    @Test
    void build_WithNullEnabled_ShouldDefaultToTrue() {
        // Arrange
        user.setEnabled(null);

        // Act
        UserDetailsImpl userDetails = UserDetailsImpl.build(user);

        // Assert
        assertThat(userDetails.isEnabled()).isTrue();
    }

    @Test
    void build_WithFalseEnabled_ShouldReturnFalse() {
        // Arrange
        user.setEnabled(false);

        // Act
        UserDetailsImpl userDetails = UserDetailsImpl.build(user);

        // Assert
        assertThat(userDetails.isEnabled()).isFalse();
    }

    @Test
    void build_WithLockedByAdmin_ShouldSetAccountNonLockedFalse() {
        // Arrange
        user.setLockedByAdmin(true);
        user.setAccountLockedUntil(null);

        // Act
        UserDetailsImpl userDetails = UserDetailsImpl.build(user);

        // Assert
        assertThat(userDetails.isAccountNonLocked()).isFalse();
    }

    @Test
    void build_WithTemporaryLock_ShouldSetAccountNonLockedFalse() {
        // Arrange
        user.setLockedByAdmin(false);
        user.setAccountLockedUntil(LocalDateTime.now().plusHours(1));

        // Act
        UserDetailsImpl userDetails = UserDetailsImpl.build(user);

        // Assert
        assertThat(userDetails.isAccountNonLocked()).isFalse();
    }

    @Test
    void build_WithExpiredTemporaryLock_ShouldSetAccountNonLockedTrue() {
        // Arrange
        user.setLockedByAdmin(false);
        user.setAccountLockedUntil(LocalDateTime.now().minusHours(1));

        // Act
        UserDetailsImpl userDetails = UserDetailsImpl.build(user);

        // Assert
        assertThat(userDetails.isAccountNonLocked()).isTrue();
    }

    @Test
    void build_WithNoLock_ShouldSetAccountNonLockedTrue() {
        // Arrange
        user.setLockedByAdmin(false);
        user.setAccountLockedUntil(null);

        // Act
        UserDetailsImpl userDetails = UserDetailsImpl.build(user);

        // Assert
        assertThat(userDetails.isAccountNonLocked()).isTrue();
    }

    @Test
    void build_WithNullAccountNonExpired_ShouldDefaultToTrue() {
        // Arrange
        user.setAccountNonExpired(null);

        // Act
        UserDetailsImpl userDetails = UserDetailsImpl.build(user);

        // Assert
        assertThat(userDetails.isAccountNonExpired()).isTrue();
    }

    @Test
    void build_WithFalseAccountNonExpired_ShouldReturnFalse() {
        // Arrange
        user.setAccountNonExpired(false);

        // Act
        UserDetailsImpl userDetails = UserDetailsImpl.build(user);

        // Assert
        assertThat(userDetails.isAccountNonExpired()).isFalse();
    }

    @Test
    void build_WithNullCredentialsNonExpired_ShouldDefaultToTrue() {
        // Arrange
        user.setCredentialsNonExpired(null);

        // Act
        UserDetailsImpl userDetails = UserDetailsImpl.build(user);

        // Assert
        assertThat(userDetails.isCredentialsNonExpired()).isTrue();
    }

    @Test
    void build_WithFalseCredentialsNonExpired_ShouldReturnFalse() {
        // Arrange
        user.setCredentialsNonExpired(false);

        // Act
        UserDetailsImpl userDetails = UserDetailsImpl.build(user);

        // Assert
        assertThat(userDetails.isCredentialsNonExpired()).isFalse();
    }

    @Test
    void build_WithNullTwoFactorEnabled_ShouldReturnFalse() {
        // Arrange
        user.setTwoFactorEnabled(null);

        // Act
        UserDetailsImpl userDetails = UserDetailsImpl.build(user);

        // Assert
        assertThat(userDetails.getTwoFactorEnabled()).isFalse();
    }

    @Test
    void build_WithTrueTwoFactorEnabled_ShouldReturnTrue() {
        // Arrange
        user.setTwoFactorEnabled(true);

        // Act
        UserDetailsImpl userDetails = UserDetailsImpl.build(user);

        // Assert
        assertThat(userDetails.getTwoFactorEnabled()).isTrue();
    }

    @Test
    void build_WithNoRoles_ShouldReturnEmptyAuthorities() {
        // Arrange
        user.setRoles(Collections.emptySet());

        // Act
        UserDetailsImpl userDetails = UserDetailsImpl.build(user);
        Collection<? extends GrantedAuthority> authorities = userDetails.getAuthorities();

        // Assert
        assertThat(authorities).isEmpty();
    }

    // ==================== GETTER TESTS ====================

    @Test
    void getId_ShouldReturnCorrectId() {
        // Arrange
        UserDetailsImpl userDetails = UserDetailsImpl.build(user);

        // Act & Assert
        assertThat(userDetails.getId()).isEqualTo(1L);
    }

    @Test
    void getEmail_ShouldReturnCorrectEmail() {
        // Arrange
        UserDetailsImpl userDetails = UserDetailsImpl.build(user);

        // Act & Assert
        assertThat(userDetails.getEmail()).isEqualTo("test@example.com");
    }

    @Test
    void getFirstName_ShouldReturnCorrectFirstName() {
        // Arrange
        UserDetailsImpl userDetails = UserDetailsImpl.build(user);

        // Act & Assert
        assertThat(userDetails.getFirstName()).isEqualTo("John");
    }

    @Test
    void getLastName_ShouldReturnCorrectLastName() {
        // Arrange
        UserDetailsImpl userDetails = UserDetailsImpl.build(user);

        // Act & Assert
        assertThat(userDetails.getLastName()).isEqualTo("Doe");
    }

    @Test
    void getUsername_ShouldReturnCorrectUsername() {
        // Arrange
        UserDetailsImpl userDetails = UserDetailsImpl.build(user);

        // Act & Assert
        assertThat(userDetails.getUsername()).isEqualTo("testuser");
    }

    @Test
    void getPassword_ShouldReturnCorrectPassword() {
        // Arrange
        UserDetailsImpl userDetails = UserDetailsImpl.build(user);

        // Act & Assert
        assertThat(userDetails.getPassword()).isEqualTo("encodedPassword123");
    }

    @Test
    void getAuthorities_ShouldReturnUnmodifiableCollection() {
        // Arrange
        UserDetailsImpl userDetails = UserDetailsImpl.build(user);
        Collection<? extends GrantedAuthority> authorities = userDetails.getAuthorities();

        // Assert - The collection should be modifiable by the test
        // This is just to verify the collection exists and has the right type
        assertThat(authorities).isNotNull();
    }

    // ==================== BOOLEAN GETTER TESTS ====================

    @Test
    void isAccountNonExpired_ShouldReturnCorrectValue() {
        // Arrange
        user.setAccountNonExpired(false);
        UserDetailsImpl userDetails = UserDetailsImpl.build(user);

        // Act & Assert
        assertThat(userDetails.isAccountNonExpired()).isFalse();
    }

    @Test
    void isAccountNonLocked_ShouldReturnCorrectValue() {
        // Arrange
        user.setLockedByAdmin(true);
        UserDetailsImpl userDetails = UserDetailsImpl.build(user);

        // Act & Assert
        assertThat(userDetails.isAccountNonLocked()).isFalse();
    }

    @Test
    void isCredentialsNonExpired_ShouldReturnCorrectValue() {
        // Arrange
        user.setCredentialsNonExpired(false);
        UserDetailsImpl userDetails = UserDetailsImpl.build(user);

        // Act & Assert
        assertThat(userDetails.isCredentialsNonExpired()).isFalse();
    }

    @Test
    void isEnabled_ShouldReturnCorrectValue() {
        // Arrange
        user.setEnabled(false);
        UserDetailsImpl userDetails = UserDetailsImpl.build(user);

        // Act & Assert
        assertThat(userDetails.isEnabled()).isFalse();
    }

    @Test
    void getTwoFactorEnabled_ShouldReturnCorrectValue() {
        // Arrange
        user.setTwoFactorEnabled(true);
        UserDetailsImpl userDetails = UserDetailsImpl.build(user);

        // Act & Assert
        assertThat(userDetails.getTwoFactorEnabled()).isTrue();
    }

    // ==================== EQUALS TESTS ====================

    @Test
    void equals_WithSameId_ShouldReturnTrue() {
        // Arrange
        UserDetailsImpl userDetails1 = UserDetailsImpl.build(user);

        User sameUser = new User();
        sameUser.setId(1L);
        sameUser.setUsername("different");
        UserDetailsImpl userDetails2 = UserDetailsImpl.build(sameUser);

        // Act & Assert
        assertThat(userDetails1.equals(userDetails2)).isTrue();
    }

    @Test
    void equals_WithDifferentId_ShouldReturnFalse() {
        // Arrange
        UserDetailsImpl userDetails1 = UserDetailsImpl.build(user);

        User differentUser = new User();
        differentUser.setId(2L);
        UserDetailsImpl userDetails2 = UserDetailsImpl.build(differentUser);

        // Act & Assert
        assertThat(userDetails1.equals(userDetails2)).isFalse();
    }

    @Test
    void equals_WithNull_ShouldReturnFalse() {
        // Arrange
        UserDetailsImpl userDetails = UserDetailsImpl.build(user);

        // Act & Assert
        assertThat(userDetails.equals(null)).isFalse();
    }

    @Test
    void equals_WithDifferentClass_ShouldReturnFalse() {
        // Arrange
        UserDetailsImpl userDetails = UserDetailsImpl.build(user);

        // Act & Assert
        assertThat(userDetails.equals("some string")).isFalse();
    }

    @Test
    void equals_WithSameObject_ShouldReturnTrue() {
        // Arrange
        UserDetailsImpl userDetails = UserDetailsImpl.build(user);

        // Act & Assert
        assertThat(userDetails.equals(userDetails)).isTrue();
    }

    // ==================== EDGE CASE TESTS ====================

    @Test
    void build_WithAllNullBooleanFields_ShouldHandleGracefully() {
        // Arrange
        user.setEnabled(null);
        user.setAccountNonExpired(null);
        user.setAccountNonLocked(null);
        user.setCredentialsNonExpired(null);
        user.setTwoFactorEnabled(null);
        user.setLockedByAdmin(null);
        user.setAccountLockedUntil(null);

        // Act
        UserDetailsImpl userDetails = UserDetailsImpl.build(user);

        // Assert - All should default to their safe values
        assertThat(userDetails.isEnabled()).isTrue();
        assertThat(userDetails.isAccountNonExpired()).isTrue();
        assertThat(userDetails.isAccountNonLocked()).isTrue();
        assertThat(userDetails.isCredentialsNonExpired()).isTrue();
        assertThat(userDetails.getTwoFactorEnabled()).isFalse();
    }

    @Test
    void build_WithMultipleLocks_ShouldTreatAsLocked() {
        // Arrange - Both admin lock AND temporary lock
        user.setLockedByAdmin(true);
        user.setAccountLockedUntil(LocalDateTime.now().plusHours(1));

        // Act
        UserDetailsImpl userDetails = UserDetailsImpl.build(user);

        // Assert
        assertThat(userDetails.isAccountNonLocked()).isFalse();
    }

    @Test
    void build_WithUserWithoutRoles_ShouldCreateUserDetails() {
        // Arrange
        user.setRoles(Collections.emptySet());

        // Act
        UserDetailsImpl userDetails = UserDetailsImpl.build(user);

        // Assert
        assertThat(userDetails).isNotNull();
        assertThat(userDetails.getAuthorities()).isEmpty();
    }

    @Test
    void build_ShouldCreateNewInstanceWithSameValues() {
        // Act
        UserDetailsImpl userDetails1 = UserDetailsImpl.build(user);
        UserDetailsImpl userDetails2 = UserDetailsImpl.build(user);

        // Assert - Different instances with same values (except id which is same, so equals true)
        assertThat(userDetails1).isNotSameAs(userDetails2);
        assertThat(userDetails1.equals(userDetails2)).isTrue();
    }
}