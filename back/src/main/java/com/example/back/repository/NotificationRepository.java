package com.example.back.repository;

import com.example.back.entity.Notification;
import com.example.back.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, Long> {

    Page<Notification> findByUserOrderByCreatedAtDesc(User user, Pageable pageable);

    long countByUserAndIsReadFalse(User user);

    List<Notification> findByReferenceIdAndReferenceType(Long referenceId, String referenceType);

    Optional<Notification> findFirstByReferenceIdAndReferenceTypeAndDaysUntilDueOrderByCreatedAtDesc(
            Long referenceId, String referenceType, Integer daysUntilDue);

    @Modifying
    @Query("UPDATE Notification n SET n.isRead = true, n.readAt = :now WHERE n.user = :user AND n.isRead = false")
    int markAllAsRead(@Param("user") User user, @Param("now") LocalDateTime now);

    List<Notification> findByIsSentFalseAndCreatedAtBefore(LocalDateTime cutoff);

    @Modifying
    @Query("DELETE FROM Notification n WHERE n.isRead = true AND n.createdAt < :cutoff")
    int deleteOldReadNotifications(@Param("cutoff") LocalDateTime cutoff);

    @Query("SELECT COUNT(n) FROM Notification n WHERE n.referenceId = :referenceId AND n.referenceType = :referenceType")
    int countByReferenceIdAndReferenceType(@Param("referenceId") Long referenceId,
                                           @Param("referenceType") String referenceType);
}