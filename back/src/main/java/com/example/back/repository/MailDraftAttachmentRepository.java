package com.example.back.repository;

import com.example.back.entity.MailDraftAttachment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface MailDraftAttachmentRepository extends JpaRepository<MailDraftAttachment, Long> {

}