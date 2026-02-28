// RequestRepository.java
package com.example.back.repository;

import com.example.back.entity.Request;
import com.example.back.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface RequestRepository extends JpaRepository<Request, Long> {

    @Query("SELECT r FROM Request r WHERE r.targetUser = :user OR r.requester = :user ORDER BY r.createdAt DESC")
    List<Request> findUserRequests(@Param("user") User user);

    @Query("SELECT r FROM Request r WHERE r.status = :status ORDER BY r.createdAt DESC")
    List<Request> findByStatus(@Param("status") String status);

    @Query("SELECT r FROM Request r WHERE r.convention.id = :conventionId ORDER BY r.createdAt DESC")
    List<Request> findByConventionId(@Param("conventionId") Long conventionId);
}