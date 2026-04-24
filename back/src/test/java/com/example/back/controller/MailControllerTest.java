package com.example.back.controller;

import com.example.back.entity.ERole;
import com.example.back.entity.MailFolder;
import com.example.back.entity.User;
import com.example.back.payload.response.MailFolderResponse;
import com.example.back.payload.response.MailResponse;
import com.example.back.repository.MailAttachmentRepository;
import com.example.back.repository.MailFolderRepository;
import com.example.back.repository.UserRepository;
import com.example.back.service.MailFolderService;
import com.example.back.service.MailService;
import com.example.back.support.ControllerTestSupport;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MailControllerTest {

    @Mock
    private MailAttachmentRepository attachmentRepository;
    @Mock
    private MailService mailService;
    @Mock
    private UserRepository userRepository;
    @Mock
    private MailFolderRepository folderRepository;
    @Mock
    private MailFolderService folderService;

    private MailController controller;

    @BeforeEach
    void setUp() {
        controller = new MailController(
                attachmentRepository,
                mailService,
                userRepository,
                folderRepository,
                new ObjectMapper(),
                folderService
        );
    }

    @AfterEach
    void tearDown() {
        ControllerTestSupport.clearAuthentication();
    }

    @Test
    void getInbox_returnsPagedMailsForCurrentUser() {
        User user = ControllerTestSupport.user(1L, "alice", ERole.ROLE_COMMERCIAL_METIER);
        ControllerTestSupport.authenticate(user);

        MailResponse mail = new MailResponse();
        mail.setId(5L);
        mail.setSubject("Hello");

        when(userRepository.findByUsername("alice")).thenReturn(Optional.of(user));
        when(mailService.getInbox("alice@example.com", 0, 20)).thenReturn(new PageImpl<>(List.of(mail)));

        ResponseEntity<?> response = controller.getInbox(0, 20);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat((Map<String, Object>) response.getBody())
                .containsEntry("success", true)
                .containsEntry("totalElements", 1L)
                .containsEntry("currentPage", 0);
    }

    @Test
    void createFolder_savesFolderForCurrentUser() {
        User user = ControllerTestSupport.user(1L, "alice", ERole.ROLE_COMMERCIAL_METIER);
        ControllerTestSupport.authenticate(user);

        MailFolder folder = new MailFolder();
        folder.setId(9L);
        folder.setName("Finance");
        folder.setColor("#123456");
        folder.setIsSystem(false);

        when(userRepository.findByUsername("alice")).thenReturn(Optional.of(user));
        when(folderRepository.save(org.mockito.ArgumentMatchers.any(MailFolder.class))).thenReturn(folder);

        ResponseEntity<?> response = controller.createFolder(Map.of("name", "Finance", "color", "#123456"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        Map<String, Object> body = (Map<String, Object>) response.getBody();
        assertThat(body).containsEntry("success", true).containsEntry("message", "Folder created successfully");
        assertThat(body.get("data")).isInstanceOf(MailFolderResponse.class);
    }
}
