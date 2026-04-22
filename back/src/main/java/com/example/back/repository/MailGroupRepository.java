package com.example.back.repository;

import com.example.back.entity.ERole;
import com.example.back.entity.MailGroup;
import com.example.back.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface MailGroupRepository extends JpaRepository<MailGroup, Long> {

    List<MailGroup> findByIsSystemTrue();

    Optional<MailGroup> findByNameAndIsSystemTrue(String name);

    Optional<MailGroup> findByNameAndOwner(String name, User owner);

    @Query("SELECT DISTINCT g FROM MailGroup g LEFT JOIN g.members m WHERE g.owner = :user OR m = :user")
    List<MailGroup> findGroupsForUser(@Param("user") User user);

    @Query("SELECT DISTINCT u FROM User u JOIN u.roles r WHERE r.name = :roleName")
    List<User> findUsersByRole(@Param("roleName") ERole roleName);

    long countByIsSystemTrue();

    @Query("SELECT COUNT(g) FROM MailGroup g WHERE g.isSystem = false AND (g.owner = :user OR :user MEMBER OF g.members)")
    long countCustomGroupsForUser(@Param("user") User user);

    @Query("SELECT g FROM MailGroup g WHERE g.isSystem = true OR :user MEMBER OF g.members OR g.owner = :user")
    List<MailGroup> findGroupsForUserWithAccess(@Param("user") User user);

}