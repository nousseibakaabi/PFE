package com.example.back.repository;

import com.example.back.entity.Mail;
import com.example.back.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface MailRepository extends JpaRepository<Mail, Long> {

    @Query("SELECT DISTINCT m FROM Mail m JOIN m.recipients r WHERE r.email = :email AND r.type = 'FROM' AND r.isDeleted = false ORDER BY m.sentAt DESC")
    Page<Mail> findSentByUserEmail(@Param("email") String email, Pageable pageable);

    default Page<Mail> findBySenderEmailAndIsDeletedFalseOrderBySentAtDesc(String email, Pageable pageable) {
        return findSentByUserEmail(email, pageable);
    }

    @Query("SELECT DISTINCT m FROM Mail m JOIN m.recipients r WHERE r.email = :email AND r.type != 'FROM' AND r.isDeleted = false AND r.isArchived = false ORDER BY m.sentAt DESC")
    Page<Mail> findInboxByRecipientEmail(@Param("email") String email, Pageable pageable);

    @Query("SELECT DISTINCT m FROM Mail m JOIN m.recipients r WHERE r.email = :email AND r.isStarred = true AND r.isDeleted = false ORDER BY m.sentAt DESC")
    Page<Mail> findStarredByUserEmail(@Param("email") String email, Pageable pageable);

    @Query("SELECT DISTINCT m FROM Mail m JOIN m.recipients r WHERE r.email = :email AND r.isArchived = true AND r.isDeleted = false ORDER BY m.sentAt DESC")
    Page<Mail> findArchivedByUserEmail(@Param("email") String email, Pageable pageable);

    @Query("SELECT DISTINCT m FROM Mail m JOIN m.recipients r WHERE r.email = :email AND r.isDeleted = false AND (LOWER(m.subject) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR LOWER(m.content) LIKE LOWER(CONCAT('%', :searchTerm, '%'))) ORDER BY m.sentAt DESC")
    Page<Mail> searchByUserEmail(@Param("email") String email, @Param("searchTerm") String searchTerm, Pageable pageable);

    @Query("SELECT COUNT(DISTINCT m) FROM Mail m JOIN m.recipients r WHERE r.email = :email AND r.isRead = false AND r.isDeleted = false AND r.isArchived = false AND r.type != 'FROM'")
    long countUnreadByRecipientEmail(@Param("email") String email);

    @Query("SELECT DISTINCT m FROM Mail m JOIN m.recipients r WHERE r.email = :email AND r.isDeleted = true ORDER BY m.sentAt DESC")
    Page<Mail> findTrashByUserEmail(@Param("email") String email, Pageable pageable);

    @Query("SELECT COUNT(DISTINCT m) FROM Mail m JOIN m.recipients r WHERE r.user = :user AND r.isRead = false AND r.isDeleted = false AND r.type != 'FROM'")
    long countUnreadByUser(@Param("user") User user);

    @Query("SELECT COUNT(DISTINCT m) FROM Mail m JOIN m.recipients r WHERE r.user = :user AND r.isDeleted = false AND r.type != 'FROM'")
    long countInboxByUser(@Param("user") User user);

    @Query("SELECT DISTINCT m FROM Mail m JOIN m.recipients r WHERE r.email = :groupIdentifier AND r.isDeleted = false ORDER BY m.sentAt DESC")
    Page<Mail> findMailsByGroupRecipient(@Param("groupIdentifier") String groupIdentifier, Pageable pageable);

    @Query("SELECT COUNT(DISTINCT m) FROM Mail m JOIN m.recipients r WHERE r.email = :groupIdentifier AND r.isDeleted = false")
    long countMailsByGroupRecipient(@Param("groupIdentifier") String groupIdentifier);

    @Query("SELECT COUNT(DISTINCT m) FROM Mail m JOIN m.recipients r WHERE r.email = :groupIdentifier AND r.user = :user AND r.isRead = false AND r.isDeleted = false")
    long countUnreadMailsByGroupForUser(@Param("groupIdentifier") String groupIdentifier, @Param("user") User user);

    @Query("SELECT COUNT(DISTINCT m) FROM Mail m JOIN m.recipients r WHERE r.email LIKE 'GROUP:%' AND (m.sender = :user OR r.user = :user) AND r.isDeleted = false")
    long countAllGroupMailsForUser(@Param("user") User user);

    @Query("SELECT COUNT(DISTINCT m) FROM Mail m JOIN m.recipients r WHERE r.email LIKE 'GROUP:%' AND r.user = :user AND r.isRead = false AND r.isDeleted = false")
    long countUnreadGroupMailsForUser(@Param("user") User user);
}