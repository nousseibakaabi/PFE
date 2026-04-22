package com.example.back.repository;

import com.example.back.entity.MailFolder;
import com.example.back.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.Optional;

@Repository
public interface MailFolderRepository extends JpaRepository<MailFolder, Long> {

    List<MailFolder> findByUserAndIsSystemFalseOrderByNameAsc(User user);

    Optional<MailFolder> findByUserAndNameAndIsSystemTrue(User user, String name);

    List<MailFolder> findByUserAndIsSystemTrueOrderByOrderIndexAsc(User user);

    List<MailFolder> findByUserAndFolderType(User user, String folderType);

    @Query("SELECT f FROM MailFolder f WHERE f.user = :user ORDER BY f.isSystem DESC, f.orderIndex ASC, f.name ASC")
    List<MailFolder> findAllByUserOrdered(@Param("user") User user);

    @Query("SELECT COUNT(f) FROM MailFolder f WHERE f.folderType = :folderType AND f.user = :user")
    long countByFolderTypeAndUser(@Param("folderType") String folderType, @Param("user") User user);

    @Query("SELECT COUNT(f) FROM MailFolder f JOIN f.mail m JOIN m.recipients r WHERE f.folderType = :folderType AND f.user = :user AND r.user = :user AND r.isRead = false")
    long countUnreadByFolderTypeAndUser(@Param("folderType") String folderType, @Param("user") User user);

    @Query("SELECT DISTINCT f.mail FROM MailFolder f WHERE f.folderType = :folderType AND f.user = :user AND f.mail IS NOT NULL ORDER BY f.mail.sentAt DESC")
    Page<com.example.back.entity.Mail> findMailsByFolderType(
            @Param("folderType") String folderType,
            @Param("user") User user,
            Pageable pageable);

    @Query("SELECT CASE WHEN COUNT(f) > 0 THEN true ELSE false END FROM MailFolder f WHERE f.mail.id = :mailId AND f.user.id = :userId AND f.folderType = :folderType")
    boolean isMailInFolder(@Param("mailId") Long mailId, @Param("userId") Long userId, @Param("folderType") String folderType);

    @Modifying
    @Transactional
    @Query("DELETE FROM MailFolder f WHERE f.mail.id = :mailId AND f.user.id = :userId AND f.folderType = :folderType")
    void removeMailFromFolder(@Param("mailId") Long mailId, @Param("userId") Long userId, @Param("folderType") String folderType);

}