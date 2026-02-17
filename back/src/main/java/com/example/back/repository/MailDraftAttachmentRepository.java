package com.example.back.repository;

import com.example.back.entity.MailDraftAttachment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MailDraftAttachmentRepository extends JpaRepository<MailDraftAttachment, Long> {
    List<MailDraftAttachment> findByDraftId(Long draftId);
    void deleteByDraftId(Long draftId);
}