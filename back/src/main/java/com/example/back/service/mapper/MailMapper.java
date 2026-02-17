// MailMapper.java
package com.example.back.service.mapper;

import com.example.back.entity.Mail;
import com.example.back.entity.MailAttachment;
import com.example.back.entity.MailRecipient;
import com.example.back.payload.response.MailResponse;
import org.springframework.stereotype.Service;

import java.util.stream.Collectors;

@Service
public class MailMapper {

    public MailResponse toResponse(Mail mail, String currentUserEmail) {
        MailResponse response = new MailResponse();

        response.setId(mail.getId());
        response.setSubject(mail.getSubject());
        response.setContent(mail.getContent());
        response.setSenderEmail(mail.getSenderEmail());
        response.setSenderName(mail.getSenderName());
        response.setSenderId(mail.getSender().getId());
        response.setSentAt(mail.getSentAt());
        response.setHasAttachments(mail.getHasAttachments());
        response.setImportance(mail.getImportance());
        response.setParentMailId(mail.getParentMail() != null ? mail.getParentMail().getId() : null);
        response.setReplyCount(mail.getReplies() != null ? mail.getReplies().size() : 0);

        // Find recipient info for current user
        mail.getRecipients().stream()
                .filter(r -> r.getEmail().equals(currentUserEmail))
                .findFirst()
                .ifPresent(recipient -> {
                    response.setIsRead(recipient.getIsRead());
                    response.setIsStarred(recipient.getIsStarred());
                    response.setIsArchived(recipient.getIsArchived());
                    response.setReadAt(recipient.getReadAt());
                });

        // Map recipients
        response.setRecipients(mail.getRecipients().stream()
                .filter(r -> !r.getType().equals("FROM")) // Don't show sender as recipient
                .map(this::toRecipientResponse)
                .collect(Collectors.toList()));

        // Map attachments
        if (mail.getAttachments() != null) {
            response.setAttachments(mail.getAttachments().stream()
                    .map(this::toAttachmentResponse)
                    .collect(Collectors.toList()));
        }


        response.setGroupMail(false);
        for (MailRecipient recipient : mail.getRecipients()) {
            if (recipient.getEmail() != null && recipient.getEmail().startsWith("GROUP:")) {
                response.setGroupMail(true);
                response.setGroupName(recipient.getEmail().substring(6));
                break;
            }
        }

        return response;
    }

    private MailResponse.RecipientResponse toRecipientResponse(MailRecipient recipient) {
        MailResponse.RecipientResponse response = new MailResponse.RecipientResponse();
        response.setEmail(recipient.getEmail());
        response.setName(recipient.getName());
        response.setType(recipient.getType());
        response.setIsRead(recipient.getIsRead());
        response.setReadAt(recipient.getReadAt());
        return response;
    }

    private MailResponse.AttachmentResponse toAttachmentResponse(MailAttachment attachment) {
        MailResponse.AttachmentResponse response = new MailResponse.AttachmentResponse();
        response.setId(attachment.getId());
        response.setFileName(attachment.getFileName());
        response.setFileType(attachment.getFileType());
        response.setFileSize(attachment.getFileSize());
        response.setDownloadUrl("/api/mails/attachments/" + attachment.getId());
        return response;
    }
}