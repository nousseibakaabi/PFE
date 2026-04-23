package com.example.back.service;

import com.example.back.entity.*;
import com.example.back.payload.request.MailRequest;
import com.example.back.payload.response.MailResponse;
import com.example.back.repository.*;
import com.example.back.service.mapper.MailMapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;


import java.time.LocalDateTime;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MailServiceTest {

    @Mock
    private MailRepository mailRepository;

    @Mock
    private MailRecipientRepository recipientRepository;

    @Mock
    private MailDraftRepository draftRepository;

    @Mock
    private UserRepository userRepository;


    @Mock
    private ObjectMapper objectMapper;

    @Mock
    private MailMapper mailMapper;

    @Mock
    private MailGroupService groupService;

    @Mock
    private MailFolderService folderService;

    @Mock
    private MailGroupRepository groupRepository;

    @InjectMocks
    private MailService mailService;

    private User testUser;
    private User testRecipient;
    private Mail testMail;
    private MailRecipient mailRecipient;
    private MailDraft testDraft;

    @BeforeEach
    void setUp() {
        // Setup test user
        testUser = new User();
        testUser.setId(1L);
        testUser.setUsername("sender");
        testUser.setEmail("sender@example.com");
        testUser.setFirstName("Sender");
        testUser.setLastName("User");

        // Setup test recipient
        testRecipient = new User();
        testRecipient.setId(2L);
        testRecipient.setUsername("recipient");
        testRecipient.setEmail("recipient@example.com");
        testRecipient.setFirstName("Recipient");
        testRecipient.setLastName("User");

        // Setup test mail
        testMail = new Mail();
        testMail.setId(1L);
        testMail.setSubject("Test Subject");
        testMail.setContent("Test Content");
        testMail.setSender(testUser);
        testMail.setSenderEmail(testUser.getEmail());
        testMail.setSenderName(testUser.getFirstName() + " " + testUser.getLastName());
        testMail.setImportance("NORMAL");
        testMail.setHasAttachments(false);
        testMail.setSentAt(LocalDateTime.now());

        // Setup test mail recipient
        mailRecipient = new MailRecipient();
        mailRecipient.setId(1L);
        mailRecipient.setMail(testMail);
        mailRecipient.setEmail(mailRecipient.getEmail());
        mailRecipient.setName(testRecipient.getFirstName() + " " + testRecipient.getLastName());
        mailRecipient.setUser(testRecipient);
        mailRecipient.setType("TO");
        mailRecipient.setIsRead(false);
        mailRecipient.setIsStarred(false);
        mailRecipient.setIsArchived(false);
        mailRecipient.setIsDeleted(false);

        testMail.setRecipients(Arrays.asList(mailRecipient));

        // Setup test draft
        testDraft = new MailDraft();
        testDraft.setId(1L);
        testDraft.setUser(testUser);
        testDraft.setSubject("Draft Subject");
        testDraft.setContent("Draft Content");
        testDraft.setLastSavedAt(LocalDateTime.now());
        testDraft.setIsSending(false);
    }

    // ==================== SEND MAIL TESTS ====================

    @Test
    void sendMail_Success() throws Exception {
        // Given
        MailRequest request = new MailRequest();
        request.setSubject("Test Subject");
        request.setContent("Test Content");
        request.setTo(Arrays.asList("recipient@example.com"));
        request.setImportance("NORMAL");

        when(mailRepository.save(any(Mail.class))).thenReturn(testMail);
        when(userRepository.findByEmail("recipient@example.com")).thenReturn(Optional.of(testRecipient));
        when(recipientRepository.saveAll(anyList())).thenReturn(Arrays.asList(mailRecipient));
        when(mailMapper.toResponse(any(Mail.class), anyString())).thenReturn(new MailResponse());

        // When
        MailResponse result = mailService.sendMail(request, testUser, null);

        // Then
        assertThat(result).isNotNull();
        verify(mailRepository).save(any(Mail.class));
        verify(recipientRepository).saveAll(anyList());
    }

    @Test
    void sendMail_WithGroupRecipient_Success() throws Exception {
        // Given
        MailRequest request = new MailRequest();
        request.setSubject("Test Subject");
        request.setContent("Test Content");
        request.setTo(Arrays.asList("GROUP:ADMIN"));
        request.setImportance("NORMAL");

        MailGroup group = new MailGroup();
        group.setId(1L);
        group.setName("ADMIN");
        group.setMembers(Arrays.asList(testRecipient));

        when(mailRepository.save(any(Mail.class))).thenReturn(testMail);
        when(groupRepository.findByNameAndIsSystemTrue("ADMIN")).thenReturn(Optional.of(group));
        when(userRepository.findByEmail("recipient@example.com")).thenReturn(Optional.of(testRecipient));
        when(recipientRepository.saveAll(anyList())).thenReturn(Arrays.asList(mailRecipient));
        when(mailMapper.toResponse(any(Mail.class), anyString())).thenReturn(new MailResponse());

        // When
        MailResponse result = mailService.sendMail(request, testUser, null);

        // Then
        assertThat(result).isNotNull();
        verify(mailRepository).save(any(Mail.class));
    }

    @Test
    void sendMail_WithMultipleRecipients_Success() throws Exception {
        // Given
        User secondRecipient = new User();
        secondRecipient.setId(3L);
        secondRecipient.setEmail("second@example.com");

        MailRequest request = new MailRequest();
        request.setSubject("Test Subject");
        request.setContent("Test Content");
        request.setTo(Arrays.asList("recipient@example.com", "second@example.com"));
        request.setCc(Arrays.asList("cc@example.com"));
        request.setBcc(Arrays.asList("bcc@example.com"));

        when(mailRepository.save(any(Mail.class))).thenReturn(testMail);
        when(userRepository.findByEmail("recipient@example.com")).thenReturn(Optional.of(testRecipient));
        when(userRepository.findByEmail("second@example.com")).thenReturn(Optional.of(secondRecipient));
        when(userRepository.findByEmail("cc@example.com")).thenReturn(Optional.empty());
        when(userRepository.findByEmail("bcc@example.com")).thenReturn(Optional.empty());
        when(recipientRepository.saveAll(anyList())).thenReturn(new ArrayList<>());
        when(mailMapper.toResponse(any(Mail.class), anyString())).thenReturn(new MailResponse());

        // When
        MailResponse result = mailService.sendMail(request, testUser, null);

        // Then
        assertThat(result).isNotNull();
        verify(mailRepository).save(any(Mail.class));
        verify(recipientRepository).saveAll(anyList());
    }

    // ==================== FACTURE NOTIFICATION TESTS ====================

    @Test
    void sendFactureDueNotification_Success()  {
        // Given
        Facture facture = new Facture();
        facture.setId(1L);
        facture.setNumeroFacture("FACT-001");
        facture.setMontantTTC(new java.math.BigDecimal("1000"));
        facture.setDateEcheance(LocalDateTime.now().plusDays(5).toLocalDate());

        Convention convention = new Convention();
        convention.setReferenceConvention("CONV-001");
        convention.setLibelle("Test Convention");
        facture.setConvention(convention);

        User systemUser = new User();
        systemUser.setId(999L);
        systemUser.setUsername("system");
        systemUser.setEmail("system@example.com");

        when(userRepository.findByUsername("system")).thenReturn(Optional.of(systemUser));
        when(mailRepository.save(any(Mail.class))).thenReturn(testMail);
        when(mailMapper.toResponse(any(Mail.class), anyString())).thenReturn(new MailResponse());

        // When
        MailResponse result = mailService.sendFactureDueNotification(testRecipient, facture, 5);

        // Then
        assertThat(result).isNotNull();
        verify(mailRepository).save(any(Mail.class));
    }

    // ==================== DRAFT TESTS ====================

    @Test
    void saveDraft_Success() throws Exception {
        // Given
        com.example.back.payload.request.MailDraftRequest request =
                new com.example.back.payload.request.MailDraftRequest();
        request.setSubject("Draft Subject");
        request.setContent("Draft Content");
        request.setTo(Arrays.asList("recipient@example.com"));

        when(draftRepository.save(any(MailDraft.class))).thenReturn(testDraft);
        when(objectMapper.writeValueAsString(any())).thenReturn("[]");

        // When
        MailDraft result = mailService.saveDraft(request, testUser, null);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getSubject()).isEqualTo("Draft Subject");
        verify(draftRepository).save(any(MailDraft.class));
    }

    @Test
    void getDraftById_Success() {
        // Given
        when(draftRepository.findById(1L)).thenReturn(Optional.of(testDraft));

        // When
        MailDraft result = mailService.getDraftById(1L, testUser);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(1L);
    }

    @Test
    void getDraftById_AccessDenied_ThrowsException() {
        // Given
        User otherUser = new User();
        otherUser.setId(999L);

        when(draftRepository.findById(1L)).thenReturn(Optional.of(testDraft));

        // When & Then
        assertThatThrownBy(() -> mailService.getDraftById(1L, otherUser))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Access denied");
    }

    @Test
    void deleteDraft_Success() {
        // Given
        when(draftRepository.findById(1L)).thenReturn(Optional.of(testDraft));
        doNothing().when(draftRepository).delete(testDraft);

        // When
        mailService.deleteDraft(1L, testUser);

        // Then
        verify(draftRepository).delete(testDraft);
    }

    // ==================== MAIL RETRIEVAL TESTS ====================

    @Test
    void getInbox_Success() {
        // Given
        Page<Mail> mailPage = new PageImpl<>(Arrays.asList(testMail));
        MailResponse response = new MailResponse();

        when(mailRepository.findInboxByRecipientEmail(eq(testUser.getEmail()), any()))
                .thenReturn(mailPage);
        when(mailMapper.toResponse(any(Mail.class), anyString())).thenReturn(response);

        // When
        Page<MailResponse> result = mailService.getInbox(testUser.getEmail(), 0, 10);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getContent()).hasSize(1);
    }

    @Test
    void getSent_Success() {
        // Given
        Page<Mail> mailPage = new PageImpl<>(Arrays.asList(testMail));
        MailResponse response = new MailResponse();

        when(mailRepository.findBySenderEmailAndIsDeletedFalseOrderBySentAtDesc(eq(testUser.getEmail()), any()))
                .thenReturn(mailPage);
        when(mailMapper.toResponse(any(Mail.class), anyString())).thenReturn(response);

        // When
        Page<MailResponse> result = mailService.getSent(testUser.getEmail(), 0, 10);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getContent()).hasSize(1);
    }

    @Test
    void getStarred_Success() {
        // Given
        Page<Mail> mailPage = new PageImpl<>(Arrays.asList(testMail));
        MailResponse response = new MailResponse();

        when(mailRepository.findStarredByUserEmail(eq(testUser.getEmail()), any()))
                .thenReturn(mailPage);
        when(mailMapper.toResponse(any(Mail.class), anyString())).thenReturn(response);

        // When
        Page<MailResponse> result = mailService.getStarred(testUser.getEmail(), 0, 10);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getContent()).hasSize(1);
    }


    @Test
    void getMailById_AccessDenied_ThrowsException() {
        // Given
        User otherUser = new User();
        otherUser.setId(999L);
        otherUser.setEmail("other@example.com");

        when(mailRepository.findById(1L)).thenReturn(Optional.of(testMail));
        when(userRepository.findByEmail("other@example.com")).thenReturn(Optional.of(otherUser));

        // When & Then
        assertThatThrownBy(() -> mailService.getMailById(1L, "other@example.com"))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Access denied");
    }

    // ==================== MAIL ACTION TESTS ====================

    @Test
    void searchMails_Success() {
        // Given
        Page<Mail> mailPage = new PageImpl<>(Arrays.asList(testMail));
        MailResponse response = new MailResponse();

        when(mailRepository.searchByUserEmail(eq(testUser.getEmail()), anyString(), any()))
                .thenReturn(mailPage);
        when(mailMapper.toResponse(any(Mail.class), anyString())).thenReturn(response);

        // When
        Page<MailResponse> result = mailService.searchMails(testUser.getEmail(), "test", 0, 10);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getContent()).hasSize(1);
    }

    @Test
    void getStats_Success() {
        // Given
        when(userRepository.findByEmail(testUser.getEmail())).thenReturn(Optional.of(testUser));
        when(mailRepository.findInboxByRecipientEmail(eq(testUser.getEmail()), any()))
                .thenReturn(new PageImpl<>(Arrays.asList(testMail)));
        when(mailRepository.countUnreadByRecipientEmail(testUser.getEmail())).thenReturn(5L);
        when(mailRepository.findBySenderEmailAndIsDeletedFalseOrderBySentAtDesc(eq(testUser.getEmail()), any()))
                .thenReturn(new PageImpl<>(Arrays.asList(testMail)));
        when(mailRepository.findStarredByUserEmail(eq(testUser.getEmail()), any()))
                .thenReturn(new PageImpl<>(Arrays.asList(testMail)));
        when(mailRepository.findArchivedByUserEmail(eq(testUser.getEmail()), any()))
                .thenReturn(new PageImpl<>(Arrays.asList(testMail)));
        when(mailRepository.findTrashByUserEmail(eq(testUser.getEmail()), any()))
                .thenReturn(new PageImpl<>(Arrays.asList(testMail)));
        when(draftRepository.countByUser(testUser)).thenReturn(2L);
        when(groupRepository.countByIsSystemTrue()).thenReturn(4L);
        when(groupRepository.countCustomGroupsForUser(testUser)).thenReturn(1L);
        when(mailRepository.countAllGroupMailsForUser(testUser)).thenReturn(10L);
        when(mailRepository.countUnreadGroupMailsForUser(testUser)).thenReturn(3L);
        when(groupService.getGroupMailStats(testUser)).thenReturn(new ArrayList<>());

        // When
        com.example.back.payload.response.MailStatsResponse result = mailService.getStats(testUser.getEmail());

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getInboxCount()).isEqualTo(1);
        assertThat(result.getUnreadCount()).isEqualTo(5);
        assertThat(result.getDraftCount()).isEqualTo(2);
    }
}