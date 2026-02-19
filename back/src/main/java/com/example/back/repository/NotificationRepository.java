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

    // Find all notifications for a user
    Page<Notification> findByUserOrderByCreatedAtDesc(User user, Pageable pageable);

    // Find unread notifications for a user
    List<Notification> findByUserAndIsReadFalseOrderByCreatedAtDesc(User user);

    // Count unread notifications for a user
    long countByUserAndIsReadFalse(User user);

    // Find notifications by type
    List<Notification> findByUserAndTypeOrderByCreatedAtDesc(User user, String type);

    // Find notifications by reference
    List<Notification> findByReferenceIdAndReferenceType(Long referenceId, String referenceType);

    // Find latest notification for a specific facture and days until due
    Optional<Notification> findFirstByReferenceIdAndReferenceTypeAndDaysUntilDueOrderByCreatedAtDesc(
            Long referenceId, String referenceType, Integer daysUntilDue);

    // Mark all notifications as read for a user
    @Modifying
    @Query("UPDATE Notification n SET n.isRead = true, n.readAt = :now WHERE n.user = :user AND n.isRead = false")
    int markAllAsRead(@Param("user") User user, @Param("now") LocalDateTime now);

    // Find notifications that need to be sent (not sent yet)
    List<Notification> findByIsSentFalseAndCreatedAtBefore(LocalDateTime cutoff);

    // Find notifications for factures due within a range
    @Query("SELECT n FROM Notification n WHERE n.referenceType = 'FACTURE' " +
            "AND n.daysUntilDue BETWEEN :minDays AND :maxDays " +
            "AND n.isRead = false ORDER BY n.createdAt DESC")
    List<Notification> findActiveFactureNotifications(@Param("minDays") Integer minDays,
                                                      @Param("maxDays") Integer maxDays);

    // Delete old read notifications
    @Modifying
    @Query("DELETE FROM Notification n WHERE n.isRead = true AND n.createdAt < :cutoff")
    int deleteOldReadNotifications(@Param("cutoff") LocalDateTime cutoff);

    // Dans NotificationRepository.java
    @Query("SELECT COUNT(n) FROM Notification n WHERE n.referenceId = :referenceId AND n.referenceType = :referenceType")
    int countByReferenceIdAndReferenceType(@Param("referenceId") Long referenceId,
                                           @Param("referenceType") String referenceType);
}