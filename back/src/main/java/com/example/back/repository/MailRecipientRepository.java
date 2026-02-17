// MailRecipientRepository.java
package com.example.back.repository;

import com.example.back.entity.MailRecipient;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MailRecipientRepository extends JpaRepository<MailRecipient, Long> {

    List<MailRecipient> findByMailId(Long mailId);

    List<MailRecipient> findByEmailAndIsDeletedFalseOrderByMailSentAtDesc(String email);

    @Modifying
    @Query("UPDATE MailRecipient r SET r.isRead = true, r.readAt = CURRENT_TIMESTAMP WHERE r.email = :email AND r.mail.id = :mailId")
    void markAsRead(@Param("email") String email, @Param("mailId") Long mailId);

    @Modifying
    @Query("UPDATE MailRecipient r SET r.isStarred = :starred WHERE r.email = :email AND r.mail.id = :mailId")
    void setStarred(@Param("email") String email, @Param("mailId") Long mailId, @Param("starred") boolean starred);

    @Modifying
    @Query("UPDATE MailRecipient r SET r.isArchived = :archived WHERE r.email = :email AND r.mail.id = :mailId")
    void setArchived(@Param("email") String email, @Param("mailId") Long mailId, @Param("archived") boolean archived);

    @Modifying
    @Query("UPDATE MailRecipient r SET r.isDeleted = true WHERE r.email = :email AND r.mail.id = :mailId")
    void moveToTrash(@Param("email") String email, @Param("mailId") Long mailId);
}