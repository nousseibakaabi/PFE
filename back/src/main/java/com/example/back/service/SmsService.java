package com.example.back.service;

import com.example.back.entity.Facture;
import com.example.back.entity.Notification;
import com.example.back.entity.User;
import com.example.back.repository.NotificationRepository;
import com.example.back.repository.UserRepository;
import com.twilio.Twilio;
import com.twilio.rest.api.v2010.account.Message;
import com.twilio.type.PhoneNumber;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Service
@Slf4j
public class SmsService {

    @Value("${sms.enabled:false}")
    private boolean smsEnabled;

    @Value("${sms.provider:mock}")
    private String smsProvider;

    @Value("${sms.twilio.account-sid:}")
    private String twilioAccountSid;

    @Value("${sms.twilio.auth-token:}")
    private String twilioAuthToken;

    @Value("${sms.twilio.phone-number:}")
    private String twilioPhoneNumber;

    @Value("${sms.sender-name:GestionApp}")
    private String senderName;

    @Autowired
    private NotificationRepository notificationRepository;

    // ============= INITIALIZATION =============

    private void initTwilio() {
        if (smsEnabled && "twilio".equals(smsProvider) &&
                !twilioAccountSid.isEmpty() && !twilioAuthToken.isEmpty()) {
            try {
                Twilio.init(twilioAccountSid, twilioAuthToken);
                log.info("✅ Twilio initialized successfully with Account SID: {}",
                        maskAccountSid(twilioAccountSid));
            } catch (Exception e) {
                log.error("❌ Failed to initialize Twilio: {}", e.getMessage());
            }
        }
    }

    // ============= PUBLIC METHODS =============

    /**
     * Send SMS for facture due notification
     */
    @Async
    public void sendFactureDueSms(String phoneNumber, Facture facture, int daysUntilDue) {
        log.info("📱 ===== SMS SENDING DEBUG =====");
        log.info("📱 Original phone number from database: '{}'", phoneNumber);
        log.info("📱 Original phone number length: {} characters",
                phoneNumber != null ? phoneNumber.length() : 0);

        if (!smsEnabled) {
            log.warn("📱 SMS is disabled. Enable with sms.enabled=true in application.properties");
            log.info("📱 Would send to: {} - Facture due in {} days", phoneNumber, daysUntilDue);
            return;
        }

        try {
            // Étape 1: Nettoyage du numéro
            log.info("📱 Step 1: Cleaning phone number...");
            String cleaned = cleanPhoneNumber(phoneNumber);
            log.info("📱 After cleaning: '{}'", cleaned);

            // Étape 2: Formatage selon les règles
            log.info("📱 Step 2: Formatting number with rules...");
            String formattedNumber = formatPhoneNumber(phoneNumber);
            log.info("📱 Final formatted number: '{}'", formattedNumber);

            // Étape 3: Validation
            log.info("📱 Step 3: Validating number format...");
            boolean isValid = isValidPhoneNumber(phoneNumber);
            log.info("📱 Is valid phone number? {}", isValid);

            if (!isValid) {
                log.error("❌ Invalid phone number format: '{}'", phoneNumber);
                return;
            }

            // Build SMS message
            String message = buildFactureDueMessage(facture, daysUntilDue);
            log.info("📱 Message to send: '{}'", message);
            log.info("📱 Message length: {} characters", message.length());

            // Send based on provider
            boolean sent = false;

            log.info("📱 Step 4: Sending via provider: {}", smsProvider);

            if ("twilio".equals(smsProvider)) {
                sent = sendViaTwilio(formattedNumber, message);
            } else if ("mock".equals(smsProvider)) {
                sent = sendViaMock(formattedNumber, message);
            } else {
                log.error("❌ Unknown SMS provider: {}", smsProvider);
                sent = sendViaMock(formattedNumber, message);
            }

            if (sent) {
                log.info("✅ SMS sent successfully to {} for facture {}",
                        maskPhoneNumber(formattedNumber), facture.getNumeroFacture());
            } else {
                log.error("❌ Failed to send SMS to {}", maskPhoneNumber(formattedNumber));
            }

            log.info("📱 ===== END SMS DEBUG =====");

        } catch (Exception e) {
            log.error("❌ Failed to send SMS to {}: {}",
                    maskPhoneNumber(phoneNumber), e.getMessage(), e);
        }
    }

    /**
     * Send SMS from notification
     */
    @Async
    public void sendNotificationSms(Notification notification, Facture facture, int daysUntilDue) {
        log.info("📱 [Notification ID: {}] Starting SMS process...", notification.getId());

        if (!smsEnabled) {
            log.warn("📱 SMS is disabled. Enable with sms.enabled=true");
            return;
        }

        User user = notification.getUser();
        String originalPhone = user.getPhone();

        log.info("📱 User: {} (ID: {})", user.getUsername(), user.getId());
        log.info("📱 Original phone in database: '{}'", originalPhone);
        log.info("📱 User notification mode: {}", user.getNotifMode());

        if (originalPhone == null || originalPhone.isEmpty()) {
            log.warn("⚠️ User {} has no phone number, cannot send SMS", user.getUsername());
            return;
        }

        try {
            // Log chaque étape du formatage
            log.info("📱 Step 1 - Raw number from DB: '{}'", originalPhone);

            String cleaned = cleanPhoneNumber(originalPhone);
            log.info("📱 Step 2 - After cleaning: '{}'", cleaned);

            String formatted = formatPhoneNumber(originalPhone);
            log.info("📱 Step 3 - After formatting: '{}'", formatted);

            log.info("📱 Step 4 - Validation result: {}", isValidPhoneNumber(originalPhone));
            log.info("📱 Step 5 - Final number to use: '{}'", formatted);

            String message = buildNotificationMessage(notification, facture, daysUntilDue);
            log.info("📱 Message content: '{}'", message);

            boolean sent = sendViaProvider(formatted, message);

            if (sent) {
                notification.setSmsSent(true);
                notification.setSmsSentAt(LocalDateTime.now());
                notification.setIsSent(notification.getEmailSent() || notification.getSmsSent());
                if (notification.getIsSent() && notification.getSentAt() == null) {
                    notification.setSentAt(LocalDateTime.now());
                }
                notificationRepository.save(notification);
                log.info("✅ SMS notification {} sent to user {}",
                        notification.getId(), user.getUsername());
            } else {
                log.error("❌ Failed to send SMS notification {}", notification.getId());
            }
        } catch (Exception e) {
            log.error("❌ Failed to send SMS notification {}: {}",
                    notification.getId(), e.getMessage(), e);
        }
    }

    /**
     * Send test SMS (for debugging)
     */
    @Async
    public void sendTestSms(String phoneNumber, String message) {
        log.info("📱 ===== TEST SMS =====");
        log.info("📱 To: {}", phoneNumber);
        log.info("📱 Message: {}", message);

        if (!smsEnabled) {
            log.warn("📱 SMS is disabled. Enable with sms.enabled=true");
            log.info("📱 TEST SMS WOULD BE SENT (mock mode)");
            return;
        }

        try {
            String formatted = formatPhoneNumber(phoneNumber);
            boolean sent = sendViaProvider(formatted, message);

            if (sent) {
                log.info("✅ Test SMS sent successfully to {}", maskPhoneNumber(formatted));
            } else {
                log.error("❌ Failed to send test SMS");
            }
        } catch (Exception e) {
            log.error("❌ Test SMS failed: {}", e.getMessage(), e);
        }
    }

    // ============= PRIVATE HELPER METHODS =============

    /**
     * Clean phone number (remove special characters)
     */
    private String cleanPhoneNumber(String phoneNumber) {
        if (phoneNumber == null) return null;
        String cleaned = phoneNumber.replaceAll("[^0-9+]", "");
        log.debug("📱 Clean: '{}' → '{}'", phoneNumber, cleaned);
        return cleaned;
    }

    /**
     * Format phone number to international format
     */
    private String formatPhoneNumber(String phoneNumber) {
        log.info("📱 ===== PHONE NUMBER FORMATTING =====");
        log.info("📱 INPUT: '{}'", phoneNumber);

        if (phoneNumber == null || phoneNumber.isEmpty()) {
            log.error("❌ Phone number is null or empty");
            throw new IllegalArgumentException("Phone number cannot be empty");
        }

        // Étape 1: Nettoyage
        String cleaned = phoneNumber.replaceAll("[^0-9+]", "");
        log.info("📱 After removing special chars: '{}'", cleaned);

        // Étape 2: Analyse du format
        log.info("📱 Number analysis:");
        log.info("  - Length: {}", cleaned.length());
        log.info("  - Starts with +? {}", cleaned.startsWith("+"));
        log.info("  - Starts with 0? {}", cleaned.startsWith("0"));
        log.info("  - All digits? {}", cleaned.matches("\\d+"));

        String result;

        // Cas 1: Numéro Tunisien avec 0 (ex: 027405659)
        if (cleaned.startsWith("0") && cleaned.length() == 10) {
            result = "+216" + cleaned.substring(1);
            log.info("📱 Tunisian number with leading 0 → +216XXXXXXXX");
        }
        // Cas 2: Numéro Tunisien sans indicatif (8 chiffres)
        else if (cleaned.matches("\\d{8}")) {
            result = "+216" + cleaned;
            log.info("📱 Tunisian number 8 digits → +216XXXXXXXX");
        }
        // Cas 3: Déjà avec +
        else if (cleaned.startsWith("+")) {
            result = cleaned;
            log.info("📱 Already has country code");
        }
        // Cas 4: Autre format
        else if (cleaned.matches("\\d+")) {
            if (cleaned.length() == 8) {
                result = "+216" + cleaned;
                log.info("📱 8 digits, assuming Tunisian → +216XXXXXXXX");
            } else if (cleaned.length() == 10 && cleaned.startsWith("0")) {
                result = "+216" + cleaned.substring(1);
                log.info("📱 10 digits with 0, Tunisian → +216XXXXXXXX");
            } else {
                result = cleaned;
                log.info("📱 Unknown format, keeping as is");
            }
        } else {
            result = cleaned;
            log.info("📱 No matching pattern, keeping cleaned version");
        }

        log.info("📱 FINAL FORMATTED NUMBER: '{}'", result);
        log.info("📱 ===== END FORMATTING =====");

        return result;
    }

    /**
     * Validate phone number
     */
    public boolean isValidPhoneNumber(String phoneNumber) {
        log.debug("📱 Validating: '{}'", phoneNumber);

        if (phoneNumber == null || phoneNumber.isEmpty()) {
            log.debug("❌ Phone number is null or empty");
            return false;
        }

        String cleaned = phoneNumber.replaceAll("[^0-9+]", "");
        log.debug("📱 Cleaned for validation: '{}'", cleaned);

        // Tunisian numbers: +216XXXXXXXX or 0XXXXXXXX or XXXXXXXX
        boolean isValid = cleaned.matches("^\\+216\\d{8}$") ||
                cleaned.matches("^0\\d{9}$") ||
                cleaned.matches("^\\d{8}$");

        log.debug("📱 Validation result: {}", isValid);

        if (!isValid) {
            log.debug("❌ Failed validation rules");
            log.debug("  - Must be +216XXXXXXXX (13 chars)");
            log.debug("  - or 0XXXXXXXX (10 chars)");
            log.debug("  - or XXXXXXXX (8 chars)");
        }

        return isValid;
    }

    /**
     * Send via provider
     */
    private boolean sendViaProvider(String phoneNumber, String message) {
        log.info("📱 Provider send to: '{}'", maskPhoneNumber(phoneNumber));
        log.info("📱 Message length: {} chars", message.length());

        if ("twilio".equals(smsProvider)) {
            log.info("📱 Using Twilio provider");
            return sendViaTwilio(phoneNumber, message);
        } else {
            log.info("📱 Using MOCK provider");
            return sendViaMock(phoneNumber, message);
        }
    }

    /**
     * Send via Twilio
     */
    private boolean sendViaTwilio(String to, String message) {
        try {
            initTwilio();

            log.info("📱 Twilio - From: '{}'", maskPhoneNumber(twilioPhoneNumber));
            log.info("📱 Twilio - To: '{}'", maskPhoneNumber(to));

            Message twilioMessage = Message.creator(
                    new PhoneNumber(to),
                    new PhoneNumber(twilioPhoneNumber),
                    message
            ).create();

            log.info("✅ Twilio response - SID: {}", twilioMessage.getSid());
            log.info("✅ Twilio status: {}", twilioMessage.getStatus());

            if (twilioMessage.getErrorCode() != null) {
                log.error("❌ Twilio error code: {}", twilioMessage.getErrorCode());
                log.error("❌ Twilio error message: {}", twilioMessage.getErrorMessage());
                return false;
            }

            return true;

        } catch (com.twilio.exception.ApiException e) {
            log.error("❌ Twilio API error - Code: {}, Message: {}",
                    e.getCode(), e.getMessage());
            return false;
        } catch (Exception e) {
            log.error("❌ Unexpected Twilio error: {}", e.getMessage(), e);
            return false;
        }
    }

    /**
     * Mock SMS sender for testing
     */
    private boolean sendViaMock(String to, String message) {
        log.info("📱 ===== MOCK SMS =====");
        log.info("📱 To: {}", to);
        log.info("📱 Message: {}", message);
        log.info("📱 ==================");
        return true;
    }

    /**
     * Build SMS message for facture due
     */
    private String buildFactureDueMessage(Facture facture, int daysUntilDue) {
        StringBuilder message = new StringBuilder();

        // Add emoji based on urgency
        if (daysUntilDue <= 0) {
            message.append("🚨 ");
        } else if (daysUntilDue <= 2) {
            message.append("⚠️ ");
        } else {
            message.append("🔔 ");
        }

        message.append("Rappel Facture: ");
        message.append(facture.getNumeroFacture());

        if (daysUntilDue > 0) {
            if (daysUntilDue == 1) {
                message.append(" sera due DEMAIN");
            } else {
                message.append(" sera due dans ").append(daysUntilDue).append(" jours");
            }
        } else if (daysUntilDue == 0) {
            message.append(" est due AUJOURD'HUI");
        } else {
            message.append(" est en RETARD de ").append(Math.abs(daysUntilDue)).append(" jours");
        }

        message.append(". Montant: ").append(facture.getMontantTTC()).append(" TND");

        if (facture.getConvention() != null) {
            message.append(". Convention: ").append(facture.getConvention().getReferenceConvention());
        }

        message.append(". Échéance: ").append(
                facture.getDateEcheance().format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))
        );

        // Keep SMS under 160 characters
        if (message.length() > 160) {
            return message.substring(0, 157) + "...";
        }

        return message.toString();
    }

    /**
     * Build SMS message from notification
     */
    private String buildNotificationMessage(Notification notification, Facture facture, int daysUntilDue) {
        StringBuilder message = new StringBuilder();

        // Add emoji based on type
        switch (notification.getType()) {
            case "DANGER":
                message.append("🚨 ");
                break;
            case "WARNING":
                message.append("⚠️ ");
                break;
            case "SUCCESS":
                message.append("✅ ");
                break;
            default:
                message.append("🔔 ");
        }

        message.append(notification.getTitle()).append(": ");
        message.append(notification.getMessage());

        // Keep SMS under 160 characters
        if (message.length() > 160) {
            return message.substring(0, 157) + "...";
        }

        return message.toString();
    }

    /**
     * Mask phone number for logs
     */
    private String maskPhoneNumber(String phoneNumber) {
        if (phoneNumber == null || phoneNumber.length() < 6) {
            return "****";
        }
        return phoneNumber.substring(0, phoneNumber.length() - 4) + "****";
    }

    /**
     * Mask Account SID for logs
     */
    private String maskAccountSid(String accountSid) {
        if (accountSid == null || accountSid.length() < 8) {
            return "****";
        }
        return accountSid.substring(0, 6) + "****" +
                accountSid.substring(accountSid.length() - 4);
    }

    /**
     * Check if SMS is enabled in configuration
     */
    public boolean isSmsEnabled() {
        return smsEnabled;
    }


    /**
     * Get SMS provider
     */
    public String getSmsProvider() {
        return smsProvider;
    }


    @Async
    public void sendDirectSms(String phoneNumber, String message) {
        log.info("📱 ===== DIRECT SMS =====");
        log.info("📱 Original phone: '{}'", phoneNumber);

        if (!smsEnabled) {
            log.warn("📱 SMS is disabled. Enable with sms.enabled=true");
            log.info("📱 Would send: {}", message);
            return;
        }

        try {
            // Format the phone number
            String formattedNumber = formatPhoneNumber(phoneNumber);
            log.info("📱 Formatted number: '{}'", formattedNumber);

            if (!isValidPhoneNumber(phoneNumber)) {
                log.error("❌ Invalid phone number format: '{}'", phoneNumber);
                return;
            }

            log.info("📱 Message: '{}'", message);
            log.info("📱 Message length: {} characters", message.length());

            // Send based on provider
            boolean sent = false;

            if ("twilio".equals(smsProvider)) {
                sent = sendViaTwilio(formattedNumber, message);
            } else if ("mock".equals(smsProvider)) {
                sent = sendViaMock(formattedNumber, message);
            } else {
                log.error("❌ Unknown SMS provider: {}", smsProvider);
                sent = sendViaMock(formattedNumber, message);
            }

            if (sent) {
                log.info("✅ SMS sent successfully to {}", maskPhoneNumber(formattedNumber));
            } else {
                log.error("❌ Failed to send SMS to {}", maskPhoneNumber(formattedNumber));
            }

        } catch (Exception e) {
            log.error("❌ Failed to send SMS: {}", e.getMessage(), e);
        }
        log.info("📱 ===== END DIRECT SMS =====");
    }
}