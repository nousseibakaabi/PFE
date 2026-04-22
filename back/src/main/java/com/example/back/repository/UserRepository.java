package com.example.back.repository;

import com.example.back.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByUsername(String username);
    Optional<User> findByEmail(String email);
    Boolean existsByUsername(String username);
    Boolean existsByEmail(String email);

    @Query("SELECT u FROM User u WHERE u.username = :identifier OR u.email = :identifier")
    Optional<User> findByUsernameOrEmail(@Param("identifier") String identifier);


    List<User> findByAccountLockedUntilBeforeAndLockedByAdminFalse(LocalDateTime dateTime);

    Boolean existsByPhone(String phone);

    @Query("SELECT u FROM User u WHERE LOWER(u.email) LIKE LOWER(:searchTerm) OR LOWER(u.firstName) LIKE LOWER(:searchTerm) OR LOWER(u.lastName) LIKE LOWER(:searchTerm)")
    List<User> searchUsers(@Param("searchTerm") String searchTerm);

    @Query("SELECT u FROM User u JOIN u.roles r WHERE r.name = :roleName")
    List<User> findByRoleName(@Param("roleName") String roleName);


}