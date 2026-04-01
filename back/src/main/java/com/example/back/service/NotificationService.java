package com.example.back.service;

import com.example.back.entity.*;
import com.example.back.repository.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Slf4j
public class NotificationService {

    @Autowired
    private NotificationRepository notificationRepository;

    @Autowired
    private FactureRepository factureRepository;

    @Autowired
    private ConventionRepository conventionRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private MailService mailService;

    @Autowired
    private SmsService smsService;


    @Autowired
    private SimpMessagingTemplate messagingTemplate; // For WebSocket real-time updates

    // ============= NOTIFICATION CREATION =============

    /**
     * Create a facture due notification
     */
    @Transactional
    public Notification createFactureDueNotification(Facture facture, int daysUntilDue) {
        log.info("Creating due notification for facture {} ({} days until due)",
                facture.getNumeroFacture(), daysUntilDue);

        // Vérifier si on a déjà créé CETTE notification AUJOURD'HUI
        Optional<Notification> existing = notificationRepository
                .findFirstByReferenceIdAndReferenceTypeAndDaysUntilDueOrderByCreatedAtDesc(
                        facture.getId(), "FACTURE", daysUntilDue);

        if (existing.isPresent() &&
                existing.get().getCreatedAt().toLocalDate().equals(LocalDate.now())) {
            log.info("Notification for facture {} ({} days) already created today",
                    facture.getNumeroFacture(), daysUntilDue);
            return existing.get();  // ← Ne crée pas de doublon
        }

        // Get the users who should receive this notification
        List<User> recipients = getFactureRecipients(facture);

        if (recipients.isEmpty()) {
            log.warn("No recipients found for facture {} notification", facture.getNumeroFacture());
            return null;
        }

        // Create notification for each recipient
        Notification firstNotification = null;

        for (User user : recipients) {
            Notification notification = new Notification();
            notification.setUser(user);

            // Set title based on days until due
            String title;
            String type;
            if (daysUntilDue > 0) {
                title = daysUntilDue == 1 ? "Facture à échéance demain" : "Facture à échéance dans " + daysUntilDue + " jours";
                type = "WARNING";
            } else if (daysUntilDue == 0) {
                title = "Facture à échéance aujourd'hui";
                type = "DANGER";
            } else {
                title = "Facture en retard";
                type = "DANGER";
            }

            notification.setTitle(title);
            notification.setType(type);
            notification.setNotificationType("FACTURE_DUE");

            // Create detailed message
            String message = createFactureDueMessage(facture, daysUntilDue);
            notification.setMessage(message);

            notification.setReferenceId(facture.getId());
            notification.setReferenceType("FACTURE");
            notification.setReferenceCode(facture.getNumeroFacture());
            notification.setDaysUntilDue(daysUntilDue);

            notification.setIsRead(false);
            notification.setIsSent(false);
            notification.setEmailSent(false);
            notification.setSmsSent(false);

            Notification saved = notificationRepository.save(notification);

            if (firstNotification == null) {
                firstNotification = saved;
            }

            log.info("Created notification ID {} for user {}", saved.getId(), user.getUsername());

            // Send real-time notification via WebSocket
            sendRealtimeNotification(saved);

            // Send email/SMS based on user preferences
            sendNotificationViaChannels(saved, facture, daysUntilDue);
        }

        return firstNotification;
    }

    /**
     * Get recipients for a facture notification
     */
    private List<User> getFactureRecipients(Facture facture) {
        Set<User> recipients = new HashSet<>();

        Convention convention = facture.getConvention();
        if (convention == null) {
            return new ArrayList<>();
        }

        // Add Chef de Projet from the application
        Application application = convention.getApplication();
        if (application != null && application.getChefDeProjet() != null) {
            recipients.add(application.getChefDeProjet());
        }

        // Add Commercial Metier who created the convention
        if (convention.getCreatedBy() != null) {
            recipients.add(convention.getCreatedBy());
        }

        log.info("Found {} recipients for facture {}: {}",
                recipients.size(), facture.getNumeroFacture(),
                recipients.stream().map(User::getUsername).collect(Collectors.toList()));

        return new ArrayList<>(recipients);
    }

    /**
     * Create the notification message for a facture
     */
    private String createFactureDueMessage(Facture facture, int daysUntilDue) {
        Convention convention = facture.getConvention();
        Application application = convention != null ? convention.getApplication() : null;

        StringBuilder message = new StringBuilder();
        message.append("Facture ").append(facture.getNumeroFacture());

        if (convention != null) {
            message.append(" pour la convention ").append(convention.getReferenceConvention());

            if (application != null) {
                message.append(" (").append(application.getCode()).append(")");
            }
        }

        if (daysUntilDue > 0) {
            message.append(" sera échue dans ").append(daysUntilDue);
            message.append(daysUntilDue == 1 ? " jour" : " jours");
        } else if (daysUntilDue == 0) {
            message.append(" est échue aujourd'hui");
        } else {
            message.append(" est en retard de ").append(Math.abs(daysUntilDue));
            message.append(Math.abs(daysUntilDue) == 1 ? " jour" : " jours");
        }

        message.append(". Montant: ").append(facture.getMontantTTC()).append(" TND");
        message.append(". Date d'échéance: ").append(
                facture.getDateEcheance().format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))
        );

        return message.toString();
    }

    /**
     * Send notification via user's preferred channels (email/SMS)
     */

    public void sendNotificationViaChannels(Notification notification, Facture facture, int daysUntilDue) {
        User user = notification.getUser();
        String notifMode = user.getNotifMode();

        // Collecter tous les SMS à envoyer pour éviter les conflits
        List<User> smsRecipients = new ArrayList<>();

        if (notifMode.equals("sms") || notifMode.equals("both")) {
            smsRecipients.add(user);
        }

        // Envoyer les emails immédiatement
        if (notifMode.equals("email") || notifMode.equals("both")) {
            try {
                mailService.sendFactureDueNotification(user, facture, daysUntilDue);
                notification.setEmailSent(true);
                notification.setEmailSentAt(LocalDateTime.now());
                log.info("✅ Email sent to {}", user.getEmail());
            } catch (Exception e) {
                log.error("❌ Failed to send email", e);
            }
        }

        for (User smsUser : smsRecipients) {
            try {
                Thread.sleep(9000);
                smsService.sendNotificationSms(notification, facture, daysUntilDue);
                log.info("✅ SMS sent to {}", smsUser.getPhone());
            } catch (Exception e) {
                log.error("❌ Failed to send SMS", e);
            }
        }

        notificationRepository.save(notification);
    }

    /**
     * Send real-time notification via WebSocket
     */
    private void sendRealtimeNotification(Notification notification) {
        try {
            // Create a DTO for frontend
            Map<String, Object> notificationDTO = new HashMap<>();
            notificationDTO.put("id", notification.getId());
            notificationDTO.put("title", notification.getTitle());
            notificationDTO.put("message", notification.getMessage());
            notificationDTO.put("type", notification.getType());
            notificationDTO.put("notificationType", notification.getNotificationType());
            notificationDTO.put("referenceId", notification.getReferenceId());
            notificationDTO.put("referenceCode", notification.getReferenceCode());
            notificationDTO.put("daysUntilDue", notification.getDaysUntilDue());
            notificationDTO.put("createdAt", notification.getCreatedAt().toString());
            notificationDTO.put("isRead", notification.getIsRead());

            // Send to specific user
            messagingTemplate.convertAndSendToUser(
                    notification.getUser().getUsername(),
                    "/queue/notifications",
                    notificationDTO
            );

            log.info("Real-time notification sent to user {}", notification.getUser().getUsername());
        } catch (Exception e) {
            log.error("Failed to send real-time notification: {}", e.getMessage());
        }
    }

    // ============= SCHEDULED TASKS =============

    /**
     * Check for unpaid invoices approaching due date
     * Runs daily at 8 AM
     */
    @Scheduled(cron = "0 * * * * *")  // Toutes les minutes
    @Transactional
    public void checkUnpaidInvoices() {
        LocalDateTime start = LocalDateTime.now();
        log.info("🕐 [SCHEDULER] Starting unpaid invoices check at {}", start);

        LocalDate today = LocalDate.now();

        // Get all unpaid invoices using individual calls
        List<Facture> unpaidInvoices = new ArrayList<>();
        unpaidInvoices.addAll(factureRepository.findByStatutPaiement("NON_PAYE"));
        unpaidInvoices.addAll(factureRepository.findByStatutPaiement("EN_RETARD"));

        if (unpaidInvoices.isEmpty()) {
            log.info("📭 No unpaid invoices found");
            return;
        }

        log.info("📊 Found {} unpaid invoices to check", unpaidInvoices.size());

        int notificationsCreated = 0;
        int notificationsSkipped = 0;

        for (Facture facture : unpaidInvoices) {
            long daysUntilDue = ChronoUnit.DAYS.between(today, facture.getDateEcheance());

            // Log pour debug
            log.debug("Facture {}: échéance {}, jours restants: {}",
                    facture.getNumeroFacture(), facture.getDateEcheance(), daysUntilDue);

            // Appel à processInvoiceDueDate qui retourne void maintenant
            int beforeCount = notificationRepository.countByReferenceIdAndReferenceType(
                    facture.getId(), "FACTURE"
            );

            processInvoiceDueDate(facture, today);

            int afterCount = notificationRepository.countByReferenceIdAndReferenceType(
                    facture.getId(), "FACTURE"
            );

            if (afterCount > beforeCount) {
                notificationsCreated++;
            } else {
                notificationsSkipped++;
            }
        }

        LocalDateTime end = LocalDateTime.now();
        long duration = ChronoUnit.MILLIS.between(start, end);

        log.info("✅ [SCHEDULER] Completed in {}ms - Created: {}, Skipped: {}",
                duration, notificationsCreated, notificationsSkipped);
    }
    /**
     * Process a single invoice for due date notifications
     */
    private void processInvoiceDueDate(Facture facture, LocalDate today) {
        if (facture.getDateEcheance() == null) {
            return;
        }

        long daysUntilDue = java.time.temporal.ChronoUnit.DAYS.between(today, facture.getDateEcheance());

        // Check if invoice is due within 5 days or overdue
        if (daysUntilDue <= 5 && daysUntilDue >= 0) {
            // Invoice due in 0-5 days
            int days = (int) daysUntilDue;
            log.info("Invoice {} due in {} days", facture.getNumeroFacture(), days);

            // Create notification for each day (5,4,3,2,1,0)
            createFactureDueNotification(facture, days);

        } else if (daysUntilDue < 0) {
            // Invoice is overdue
            int daysOverdue = (int) Math.abs(daysUntilDue);
            log.info("Invoice {} is overdue by {} days", facture.getNumeroFacture(), daysOverdue);

            // Create overdue notification (send once per day for first 5 days of overdue)
            if (daysOverdue <= 5) {
                createFactureDueNotification(facture, (int) daysUntilDue); // Negative value indicates overdue
            }
        }
    }
    /**
     * Hourly check to ensure notifications are sent (retry failed sends)
     */
    @Scheduled(cron = "0 0 * * * *") // Every hour
    @Transactional
    public void retryFailedNotifications() {
        LocalDateTime oneHourAgo = LocalDateTime.now().minusHours(1);
        List<Notification> unsent = notificationRepository.findByIsSentFalseAndCreatedAtBefore(oneHourAgo);

        for (Notification notification : unsent) {
            if (notification.isFactureNotification()) {
                Optional<Facture> facture = factureRepository.findById(notification.getReferenceId());
                if (facture.isPresent() && notification.getDaysUntilDue() != null) {
                    sendNotificationViaChannels(notification, facture.get(), notification.getDaysUntilDue());
                }
            }
        }
    }

    // ============= NOTIFICATION MANAGEMENT =============

    /**
     * Get user's notifications
     */
    public Page<Notification> getUserNotifications(User user, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        return notificationRepository.findByUserOrderByCreatedAtDesc(user, pageable);
    }

    /**
     * Get unread notifications count
     */
    public long getUnreadCount(User user) {
        return notificationRepository.countByUserAndIsReadFalse(user);
    }

    /**
     * Mark notification as read
     */
    @Transactional
    public Notification markAsRead(Long notificationId, User user) {
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new RuntimeException("Notification not found"));

        if (!notification.getUser().getId().equals(user.getId())) {
            throw new RuntimeException("Access denied");
        }

        notification.markAsRead();
        return notificationRepository.save(notification);
    }

    /**
     * Mark all notifications as read for a user
     */
    @Transactional
    public int markAllAsRead(User user) {
        return notificationRepository.markAllAsRead(user, LocalDateTime.now());
    }

    /**
     * Delete notification
     */
    @Transactional
    public void deleteNotification(Long notificationId, User user) {
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new RuntimeException("Notification not found"));

        if (!notification.getUser().getId().equals(user.getId())) {
            throw new RuntimeException("Access denied");
        }

        notificationRepository.delete(notification);
    }

    /**
     * Clean up old read notifications (run weekly)
     */
    @Scheduled(cron = "0 0 2 * * SUN") // Every Sunday at 2 AM
    @Transactional
    public void cleanupOldNotifications() {
        LocalDateTime oneMonthAgo = LocalDateTime.now().minusMonths(1);
        int deleted = notificationRepository.deleteOldReadNotifications(oneMonthAgo);
        log.info("Cleaned up {} old read notifications", deleted);
    }


    @Transactional
    public void deleteNotificationsForFacture(Long factureId) {
        List<Notification> notifications = notificationRepository
                .findByReferenceIdAndReferenceType(factureId, "FACTURE");

        if (!notifications.isEmpty()) {
            notificationRepository.deleteAll(notifications);
            log.info("Deleted {} notifications for facture {}",
                    notifications.size(), factureId);
        }
    }
}