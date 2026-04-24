package com.example.back.controller;

import com.example.back.entity.ERole;
import com.example.back.entity.Notification;
import com.example.back.entity.User;
import com.example.back.repository.UserRepository;
import com.example.back.service.NotificationService;
import com.example.back.support.ControllerTestSupport;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NotificationControllerTest {

    @Mock
    private NotificationService notificationService;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private NotificationController controller;

    @AfterEach
    void tearDown() {
        ControllerTestSupport.clearAuthentication();
    }

    @Test
    void getNotifications_returnsPageMetadataAndUnreadCount() {
        User user = ControllerTestSupport.user(1L, "alice", ERole.ROLE_COMMERCIAL_METIER);
        Notification notification = new Notification();
        notification.setId(5L);

        ControllerTestSupport.authenticate(user);
        when(userRepository.findByUsername("alice")).thenReturn(Optional.of(user));
        when(notificationService.getUserNotifications(user, 0, 20)).thenReturn(new PageImpl<>(List.of(notification)));
        when(notificationService.getUnreadCount(user)).thenReturn(3L);

        ResponseEntity<?> response = controller.getNotifications(0, 20);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat((Map<String, Object>) response.getBody())
                .containsEntry("success", true)
                .containsEntry("totalElements", 1L)
                .containsEntry("unreadCount", 3L);
    }

    @Test
    void deleteNotification_delegatesToService() {
        User user = ControllerTestSupport.user(1L, "alice", ERole.ROLE_COMMERCIAL_METIER);
        ControllerTestSupport.authenticate(user);
        when(userRepository.findByUsername("alice")).thenReturn(Optional.of(user));

        ResponseEntity<?> response = controller.deleteNotification(9L);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat((Map<String, Object>) response.getBody())
                .containsEntry("success", true)
                .containsEntry("message", "Notification deleted successfully");
        verify(notificationService).deleteNotification(9L, user);
    }

    @Test
    void getUnreadCount_returnsCountOnly() {
        User user = ControllerTestSupport.user(1L, "alice", ERole.ROLE_COMMERCIAL_METIER);
        ControllerTestSupport.authenticate(user);
        when(userRepository.findByUsername("alice")).thenReturn(Optional.of(user));
        when(notificationService.getUnreadCount(user)).thenReturn(4L);

        ResponseEntity<?> response = controller.getUnreadCount();

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat((Map<String, Object>) response.getBody())
                .containsEntry("success", true)
                .containsEntry("count", 4L);
    }

    @Test
    void markAsRead_returnsNotification() {
        User user = ControllerTestSupport.user(1L, "alice", ERole.ROLE_COMMERCIAL_METIER);
        Notification notification = new Notification();
        notification.setId(6L);
        ControllerTestSupport.authenticate(user);
        when(userRepository.findByUsername("alice")).thenReturn(Optional.of(user));
        when(notificationService.markAsRead(6L, user)).thenReturn(notification);

        ResponseEntity<?> response = controller.markAsRead(6L);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat((Map<String, Object>) response.getBody())
                .containsEntry("message", "Notification marked as read")
                .containsEntry("data", notification);
    }

    @Test
    void markAllAsRead_returnsAffectedCount() {
        User user = ControllerTestSupport.user(1L, "alice", ERole.ROLE_COMMERCIAL_METIER);
        ControllerTestSupport.authenticate(user);
        when(userRepository.findByUsername("alice")).thenReturn(Optional.of(user));
        when(notificationService.markAllAsRead(user)).thenReturn(5);

        ResponseEntity<?> response = controller.markAllAsRead();

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat((Map<String, Object>) response.getBody())
                .containsEntry("message", "All notifications marked as read")
                .containsEntry("count", 5);
    }
}
