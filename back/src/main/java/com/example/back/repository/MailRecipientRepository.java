package com.example.back.repository;

import com.example.back.entity.MailRecipient;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;


@Repository
public interface MailRecipientRepository extends JpaRepository<MailRecipient, Long> {

    @Modifying
    @Query("UPDATE MailRecipient r SET r.isArchived = :archived WHERE r.email = :email AND r.mail.id = :mailId")
    void setArchived(@Param("email") String email, @Param("mailId") Long mailId, @Param("archived") boolean archived);
}