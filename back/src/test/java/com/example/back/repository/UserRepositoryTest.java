// src/test/java/com/example/back/repository/UserRepositoryTest.java
package com.example.back.repository;

import com.example.back.entity.ERole;
import com.example.back.entity.Role;
import com.example.back.entity.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
public class UserRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    private User testUser;
    private Role testRole;

    @BeforeEach
    void setUp() {
        // Create and persist role
        testRole = new Role(ERole.ROLE_COMMERCIAL_METIER, "Commercial Role");
        entityManager.persist(testRole);
        entityManager.flush();

        // Create user
        testUser = new User("testuser", "test@email.com", "password");
        testUser.setFirstName("Test");
        testUser.setLastName("User");
        testUser.getRoles().add(testRole);

        entityManager.persist(testUser);
        entityManager.flush();
    }

    @Test
    void testFindByUsername() {
        // Act
        Optional<User> found = userRepository.findByUsername("testuser");

        // Assert
        assertThat(found).isPresent();
        assertThat(found.get().getUsername()).isEqualTo("testuser");
        assertThat(found.get().getEmail()).isEqualTo("test@email.com");
    }

    @Test
    void testFindByEmail() {
        // Act
        Optional<User> found = userRepository.findByEmail("test@email.com");

        // Assert
        assertThat(found).isPresent();
        assertThat(found.get().getEmail()).isEqualTo("test@email.com");
        assertThat(found.get().getUsername()).isEqualTo("testuser");
    }

    @Test
    void testFindByUsernameOrEmail_WithUsername() {
        // Act
        Optional<User> found = userRepository.findByUsernameOrEmail("testuser");

        // Assert
        assertThat(found).isPresent();
        assertThat(found.get().getUsername()).isEqualTo("testuser");
    }

    @Test
    void testFindByUsernameOrEmail_WithEmail() {
        // Act
        Optional<User> found = userRepository.findByUsernameOrEmail("test@email.com");

        // Assert
        assertThat(found).isPresent();
        assertThat(found.get().getEmail()).isEqualTo("test@email.com");
    }

    @Test
    void testExistsByUsername() {
        // Act
        boolean exists = userRepository.existsByUsername("testuser");

        // Assert
        assertThat(exists).isTrue();
    }

    @Test
    void testExistsByUsername_NotFound() {
        // Act
        boolean exists = userRepository.existsByUsername("nonexistent");

        // Assert
        assertThat(exists).isFalse();
    }

    @Test
    void testExistsByEmail() {
        // Act
        boolean exists = userRepository.existsByEmail("test@email.com");

        // Assert
        assertThat(exists).isTrue();
    }

    @Test
    void testExistsByEmail_NotFound() {
        // Act
        boolean exists = userRepository.existsByEmail("nonexistent@email.com");

        // Assert
        assertThat(exists).isFalse();
    }

    @Test
    void testSaveUser() {
        // Arrange
        User newUser = new User("newuser", "new@email.com", "password");
        newUser.setFirstName("New");
        newUser.setLastName("User");
        newUser.getRoles().add(testRole);

        // Act
        User saved = userRepository.save(newUser);

        // Assert
        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getUsername()).isEqualTo("newuser");
        assertThat(saved.getEmail()).isEqualTo("new@email.com");
        assertThat(saved.getCreatedAt()).isNotNull();
    }
}