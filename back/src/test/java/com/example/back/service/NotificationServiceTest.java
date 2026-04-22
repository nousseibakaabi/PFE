package com.example.back.service;

import com.example.back.entity.*;
import com.example.back.payload.response.MailResponse;
import com.example.back.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NotificationServiceTest {

    @Mock
    private NotificationRepository notificationRepository;

    @Mock
    private FactureRepository factureRepository;

    @Mock
    private ConventionRepository conventionRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private MailService mailService;

    @Mock
    private SmsService smsService;

    @Mock
    private SimpMessagingTemplate messagingTemplate;

    @InjectMocks
    private NotificationService notificationService;

    private User testUser;
    private User testChef;
    private Application testApplication;
    private Convention testConvention;
    private Facture testFacture;
    private Notification testNotification;

    @BeforeEach
    void setUp() {
        // Setup test user
        testUser = new User();
        testUser.setId(1L);
        testUser.setUsername("testuser");
        testUser.setEmail("test@example.com");
        testUser.setFirstName("Test");
        testUser.setLastName("User");
        testUser.setPhone("+21612345678");
        testUser.setNotifMode("email");

        // Setup test chef
        testChef = new User();
        testChef.setId(2L);
        testChef.setUsername("chef");
        testChef.setEmail("chef@example.com");
        testChef.setFirstName("Chef");
        testChef.setLastName("Projet");
        testChef.setPhone("+21687654321");
        testChef.setNotifMode("both");

        // Setup test application
        testApplication = new Application();
        testApplication.setId(1L);
        testApplication.setCode("APP-2024-001");
        testApplication.setName("Test Application");
        testApplication.setChefDeProjet(testChef);

        // Setup test convention
        testConvention = new Convention();
        testConvention.setId(1L);
        testConvention.setReferenceConvention("CONV-2024-001");
        testConvention.setApplication(testApplication);
        testConvention.setCreatedBy(testUser);

        // Setup test facture
        testFacture = new Facture();
        testFacture.setId(1L);
        testFacture.setNumeroFacture("FACT-2024-001");
        testFacture.setMontantTTC(new java.math.BigDecimal("11900"));
        testFacture.setDateEcheance(LocalDate.now().plusDays(3));
        testFacture.setStatutPaiement("NON_PAYE");
        testFacture.setConvention(testConvention);

        // Setup test notification
        testNotification = new Notification();
        testNotification.setId(1L);
        testNotification.setUser(testUser);
        testNotification.setTitle("Facture Due Soon");
        testNotification.setMessage("Test message");
        testNotification.setType("WARNING");
        testNotification.setNotificationType("FACTURE_DUE");
        testNotification.setReferenceId(1L);
        testNotification.setReferenceType("FACTURE");
        testNotification.setReferenceCode("FACT-2024-001");
        testNotification.setDaysUntilDue(3);
        testNotification.setIsRead(false);
        testNotification.setIsSent(false);
    }

    // ==================== CREATE NOTIFICATION TESTS ====================

    @Test
    void createFactureDueNotification_Success() {
        // Given
        // Setup testNotification with proper values
        testNotification = new Notification();
        testNotification.setId(1L);
        testNotification.setUser(testUser);
        testNotification.setTitle("Facture à échéance dans 3 jours");
        testNotification.setMessage("Test message");
        testNotification.setType("WARNING");
        testNotification.setNotificationType("FACTURE_DUE");
        testNotification.setReferenceId(1L);
        testNotification.setReferenceType("FACTURE");
        testNotification.setReferenceCode("FACT-2024-001");
        testNotification.setDaysUntilDue(3);
        testNotification.setIsRead(false);
        testNotification.setIsSent(false);
        testNotification.setCreatedAt(LocalDateTime.now());

        // Mock the repository to return empty for duplicate check
        when(notificationRepository.findFirstByReferenceIdAndReferenceTypeAndDaysUntilDueOrderByCreatedAtDesc(
                eq(1L), eq("FACTURE"), eq(3)))
                .thenReturn(Optional.empty());

        // Mock the save to return our test notification - will be called multiple times
        when(notificationRepository.save(any(Notification.class))).thenAnswer(invocation -> {
            Notification saved = invocation.getArgument(0);
            saved.setId(1L);
            saved.setCreatedAt(LocalDateTime.now());
            return saved;
        });

        doNothing().when(messagingTemplate).convertAndSendToUser(anyString(), anyString(), any());

        // Mock the mail service
        when(mailService.sendFactureDueNotification(any(User.class), any(Facture.class), anyInt()))
                .thenReturn(new MailResponse());

        // Mock the sms service (void method)
        doNothing().when(smsService).sendNotificationSms(any(Notification.class), any(Facture.class), anyInt());

        // When
        Notification result = notificationService.createFactureDueNotification(testFacture, 3);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getTitle()).isEqualTo("Facture à échéance dans 3 jours");
        assertThat(result.getType()).isEqualTo("WARNING");

        // Verify save was called multiple times (once per recipient, plus potentially in sendNotificationViaChannels)
        // Use atLeast() instead of times(1) since it will be called multiple times
        verify(notificationRepository, atLeast(1)).save(any(Notification.class));
    }

    // ==================== MARK AS READ TESTS ====================

    @Test
    void markAsRead_Success() {
        // Given
        when(notificationRepository.findById(1L)).thenReturn(Optional.of(testNotification));
        when(notificationRepository.save(any(Notification.class))).thenReturn(testNotification);

        // When
        Notification result = notificationService.markAsRead(1L, testUser);

        // Then
        assertThat(result.getIsRead()).isTrue();
        assertThat(result.getReadAt()).isNotNull();
        verify(notificationRepository).save(testNotification);
    }

    @Test
    void markAsRead_NotificationNotFound_ThrowsException() {
        // Given
        when(notificationRepository.findById(1L)).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> notificationService.markAsRead(1L, testUser))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Notification not found");
    }

    @Test
    void markAsRead_AccessDenied_ThrowsException() {
        // Given
        User otherUser = new User();
        otherUser.setId(999L);

        when(notificationRepository.findById(1L)).thenReturn(Optional.of(testNotification));

        // When & Then
        assertThatThrownBy(() -> notificationService.markAsRead(1L, otherUser))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Access denied");
    }

    // ==================== MARK ALL AS READ TESTS ====================

    @Test
    void markAllAsRead_Success() {
        // Given
        when(notificationRepository.markAllAsRead(eq(testUser), any(LocalDateTime.class)))
                .thenReturn(3);

        // When
        int result = notificationService.markAllAsRead(testUser);

        // Then
        assertThat(result).isEqualTo(3);
        verify(notificationRepository).markAllAsRead(eq(testUser), any(LocalDateTime.class));
    }

    // ==================== DELETE NOTIFICATION TESTS ====================

    @Test
    void deleteNotification_Success() {
        // Given
        when(notificationRepository.findById(1L)).thenReturn(Optional.of(testNotification));
        doNothing().when(notificationRepository).delete(testNotification);

        // When
        notificationService.deleteNotification(1L, testUser);

        // Then
        verify(notificationRepository).delete(testNotification);
    }

    @Test
    void deleteNotification_AccessDenied_ThrowsException() {
        // Given
        User otherUser = new User();
        otherUser.setId(999L);

        when(notificationRepository.findById(1L)).thenReturn(Optional.of(testNotification));

        // When & Then
        assertThatThrownBy(() -> notificationService.deleteNotification(1L, otherUser))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Access denied");
    }

    // ==================== GET NOTIFICATIONS TESTS ====================

    @Test
    void getUserNotifications_Success() {
        // Given
        List<Notification> notifications = Arrays.asList(testNotification);
        org.springframework.data.domain.Page<Notification> page =
                new org.springframework.data.domain.PageImpl<>(notifications);

        when(notificationRepository.findByUserOrderByCreatedAtDesc(eq(testUser), any()))
                .thenReturn(page);

        // When
        var result = notificationService.getUserNotifications(testUser, 0, 10);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getContent()).hasSize(1);
        verify(notificationRepository).findByUserOrderByCreatedAtDesc(eq(testUser), any());
    }

    @Test
    void getUnreadCount_Success() {
        // Given
        when(notificationRepository.countByUserAndIsReadFalse(testUser)).thenReturn(5L);

        // When
        long result = notificationService.getUnreadCount(testUser);

        // Then
        assertThat(result).isEqualTo(5L);
        verify(notificationRepository).countByUserAndIsReadFalse(testUser);
    }

    // ==================== SEND NOTIFICATION TESTS ====================

    @Test
    void sendNotificationViaChannels_EmailOnly() {
        // Given
        testUser.setNotifMode("email");
        testNotification.setUser(testUser);

        // MailResponse is returned, not void
        when(mailService.sendFactureDueNotification(any(User.class), any(Facture.class), anyInt()))
                .thenReturn(new com.example.back.payload.response.MailResponse());

        // When
        notificationService.sendNotificationViaChannels(testNotification, testFacture, 3);

        // Then
        verify(mailService).sendFactureDueNotification(eq(testUser), eq(testFacture), eq(3));
        verify(smsService, never()).sendNotificationSms(any(), any(), anyInt());
        verify(notificationRepository).save(testNotification);
    }

    @Test
    void sendNotificationViaChannels_SmsOnly() {
        // Given
        testUser.setNotifMode("sms");
        testNotification.setUser(testUser);

        // For void methods
        doNothing().when(smsService).sendNotificationSms(any(), any(), anyInt());

        // When
        notificationService.sendNotificationViaChannels(testNotification, testFacture, 3);

        // Then
        verify(smsService).sendNotificationSms(eq(testNotification), eq(testFacture), eq(3));
        verify(mailService, never()).sendFactureDueNotification(any(), any(), anyInt());
        verify(notificationRepository).save(testNotification);
    }

    @Test
    void sendNotificationViaChannels_Both() {
        // Given
        testUser.setNotifMode("both");
        testNotification.setUser(testUser);

        when(mailService.sendFactureDueNotification(any(User.class), any(Facture.class), anyInt()))
                .thenReturn(new com.example.back.payload.response.MailResponse());
        doNothing().when(smsService).sendNotificationSms(any(), any(), anyInt());

        // When
        notificationService.sendNotificationViaChannels(testNotification, testFacture, 3);

        // Then
        verify(mailService).sendFactureDueNotification(eq(testUser), eq(testFacture), eq(3));
        verify(smsService).sendNotificationSms(eq(testNotification), eq(testFacture), eq(3));
        verify(notificationRepository).save(testNotification);
    }

    // ==================== DELETE NOTIFICATIONS FOR FACTURE TESTS ====================

    @Test
    void deleteNotificationsForFacture_Success() {
        // Given
        List<Notification> notifications = Arrays.asList(testNotification);
        when(notificationRepository.findByReferenceIdAndReferenceType(1L, "FACTURE"))
                .thenReturn(notifications);
        doNothing().when(notificationRepository).deleteAll(notifications);

        // When
        notificationService.deleteNotificationsForFacture(1L);

        // Then
        verify(notificationRepository).findByReferenceIdAndReferenceType(1L, "FACTURE");
        verify(notificationRepository).deleteAll(notifications);
    }



    // ==================== CLEANUP OLD NOTIFICATIONS TESTS ====================

    @Test
    void cleanupOldNotifications_Success() {
        // Given
        when(notificationRepository.deleteOldReadNotifications(any(LocalDateTime.class)))
                .thenReturn(5);

        // When
        notificationService.cleanupOldNotifications();

        // Then
        verify(notificationRepository).deleteOldReadNotifications(any(LocalDateTime.class));
    }
}