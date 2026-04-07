package com.example.back.service;

import jakarta.mail.internet.InternetAddress;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;

import java.io.UnsupportedEncodingException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Service
public class EmailService {

    @Autowired
    private JavaMailSender mailSender;

    @Value("${spring.mail.username}")
    private String fromEmail;

    @Value("${frontend.url:http://localhost:4200}")
    private String frontendUrl;

    private final String PRIMARY_COLOR = "#6EB9D5";
    private final String DARK_BLUE = "#4A8AA5";
    private final String SUCCESS_COLOR = "#10b981";
    private final String WARNING_COLOR = "#f59e0b";
    private final String ERROR_COLOR = "#ef4444";

    // Send simple text email
    public void sendSimpleMessage(String to, String subject, String text) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(fromEmail);
        message.setTo(to);
        message.setSubject(subject);
        message.setText(text);
        mailSender.send(message);
    }

    // Send HTML email for password reset
// Send HTML email for password reset
// Send HTML email for password reset
    public void sendPasswordResetEmail(String to, String resetToken, String username) throws MessagingException {
        String resetLink = frontendUrl + "/reset-password?token=" + resetToken;

        String subject = "🔐 Réinitialisation de votre mot de passe";
        String htmlContent = """
        <!DOCTYPE html>
        <html>
        <head>
            <meta charset="UTF-8">
            <style>
                body { font-family: 'Segoe UI', Arial, sans-serif; line-height: 1.6; color: #333; margin: 0; padding: 0; }
                .container { max-width: 600px; margin: 20px auto; background: #ffffff; border-radius: 24px; overflow: hidden; box-shadow: 0 10px 30px rgba(0,0,0,0.08); }
                .header { background: linear-gradient(135deg, """ + PRIMARY_COLOR +
                """ 
                        0%, """ + DARK_BLUE +
                """ 
                100%); padding: 30px; text-align: center; }
                .icon { width: 80px; height: 80px; background: rgba(255,255,255,0.2); border-radius: 50%; margin: 0 auto 15px; display: flex; align-items: center; justify-content: center; backdrop-filter: blur(5px); }
                .icon svg { width: 40px; height: 40px; fill: white; }
                .content { padding: 40px; background: #ffffff; }
                .button { display: inline-block; background: """ + PRIMARY_COLOR +
                """
                ; color: white; padding: 14px 32px; text-decoration: none; border-radius: 50px; font-weight: 600; margin: 20px 0; box-shadow: 0 4px 15px rgba(110, 185, 213, 0.3); transition: all 0.3s ease; }
                .button:hover { background: """ + DARK_BLUE +
                """
                ; transform: translateY(-2px); box-shadow: 0 8px 25px rgba(110, 185, 213, 0.4); }
                .info-box { background: #f8fafc; border-radius: 16px; padding: 20px; margin: 20px 0; border: 1px solid #e2e8f0; }
                .footer { padding: 30px; text-align: center; background: #f8fafc; color: #64748b; font-size: 13px; border-top: 1px solid #e2e8f0; }
                .signature { margin-top: 20px; padding-top: 20px; border-top: 2px dashed """ + PRIMARY_COLOR +
                """
                ; }
                .emoji { font-size: 20px; }
            </style>
        </head>
        <body>
            <div class="container">
                <div class="header">
                    <div class="icon">
                        <svg viewBox="0 0 24 24" fill="white">
                            <path d="M12 2C8.13 2 5 5.13 5 9v3c0 .5.3.9.7 1.2l-1.4 1.4C3.5 13.9 3 13 3 12V9c0-5 4-9 9-9s9 4 9 9v3c0 .5-.3.9-.7 1.2l-1.4-1.4c.4-.3.7-.7.7-1.2V9c0-3.87-3.13-7-7-7z"/>
                            <path d="M12 22c-2.2 0-4-1.8-4-4v-1h8v1c0 2.2-1.8 4-4 4z"/>
                            <circle cx="12" cy="10" r="2"/>
                        </svg>
                    </div>
                    <h1 style="color: white; margin: 0; font-size: 28px;">Réinitialisation du mot de passe</h1>
                </div>
                <div class="content">
                    <p style="font-size: 18px; color: """ + PRIMARY_COLOR +
                """
                        ;">Bonjour """ + username +
                    """
                    ,</p>
                    <p>Nous avons reçu une demande de réinitialisation de votre mot de passe.</p>
                    
                    <div class="info-box">
                        <p style="margin: 0;"><span class="emoji">⏰</span> Ce lien expire dans <strong>24 heures</strong></p>
                        <p style="margin: 10px 0 0;"><span class="emoji">🔒</span> Pour votre sécurité, ne partagez jamais ce lien</p>
                    </div>
                    
                    <div style="text-align: center;">
                        <a href=\"""" + resetLink +
                    """
                    \" class="button">🔑 Réinitialiser mon mot de passe</a>
                    </div>
                    
                    <p style="color: #64748b; font-size: 14px; text-align: center;">Ou copiez ce lien :</p>
                    <p style="background: #f1f5f9; padding: 12px; border-radius: 8px; word-break: break-all; font-size: 12px;">""" + resetLink +
                    """
                    </p>
                    
                    <div class="signature">
                        <p style="margin: 10px 0 0;">À très vite sur CNI !</p>
                        <p style="margin: 20px 0 0; font-weight: 600; color: """ + PRIMARY_COLOR +
                    """
                    ;">L'équipe CNI</p>
                    </div>
                </div>
                <div class="footer">
                    <p>© 2026 CNI. Tous droits réservés.</p>
                    <p style="margin: 5px 0 0;">Cet email a été envoyé à 
                    """
                + to +
                """
                </p>
                </div>
            </div>
        </body>
        </html>
        """;

        sendHtmlEmail(to, subject, htmlContent);
    }
    // Send password reset confirmation email
    public void sendPasswordResetConfirmation(String to, String username) throws MessagingException {
        String subject = "✓ Mot de passe modifié avec succès";
        String htmlContent = """
            <!DOCTYPE html>
            <html>
            <head>
                <meta charset="UTF-8">
                <style>
                    body { font-family: 'Segoe UI', Arial, sans-serif; line-height: 1.6; color: #333; margin: 0; padding: 0; }
                    .container { max-width: 600px; margin: 20px auto; background: #ffffff; border-radius: 24px; overflow: hidden; box-shadow: 0 10px 30px rgba(0,0,0,0.08); }
                    .header { background: linear-gradient(135deg, %s 0%, %s 100%); padding: 30px; text-align: center; }
                    .icon { width: 80px; height: 80px; background: rgba(255,255,255,0.2); border-radius: 50%; margin: 0 auto 15px; display: flex; align-items: center; justify-content: center; }
                    .icon svg { width: 40px; height: 40px; fill: white; }
                    .content { padding: 40px; background: #ffffff; }
                    .success-badge { background: %s; color: white; padding: 12px 24px; border-radius: 50px; display: inline-block; font-weight: 600; margin-bottom: 20px; }
                    .tips { background: #f8fafc; border-radius: 16px; padding: 20px; margin: 20px 0; }
                    .footer { padding: 30px; text-align: center; background: #f8fafc; color: #64748b; font-size: 13px; border-top: 1px solid #e2e8f0; }
                </style>
            </head>
            <body>
                <div class="container">
                    <div class="header">
                        <div class="icon">
                            <svg viewBox="0 0 24 24" fill="white">
                                <path d="M9 16.17L4.83 12l-1.42 1.41L9 19 21 7l-1.41-1.41L9 16.17z"/>
                            </svg>
                        </div>
                        <h1 style="color: white; margin: 0; font-size: 28px;">Mot de passe modifié !</h1>
                    </div>
                    <div class="content">
                        <div style="text-align: center;">
                            <div class="success-badge">✓ Opération réussie</div>
                        </div>
                        
                        <p style="font-size: 18px; color: %s;">Bonjour %s,</p>
                        <p>Votre mot de passe a été modifié avec succès. Votre compte est maintenant sécurisé avec votre nouveau mot de passe.</p>
                        
                        <div class="tips">
                            <h3 style="color: %s; margin-top: 0;">💡 Petits rappels de sécurité :</h3>
                            <ul style="padding-left: 20px;">
                                <li>Utilisez un mot de passe unique et fort</li>
                                <li>Ne le partagez jamais avec personne</li>
                                <li>Changez-le régulièrement</li>
                                <li>Activez la double authentification si disponible</li>
                            </ul>
                        </div>
                        
                        <p style="text-align: center; margin: 30px 0 0;">
                            <span class="emoji" style="font-size: 24px;">🔐</span>
                        </p>
                        
                        <p style="margin: 20px 0 0;">Si vous n'êtes pas à l'origine de ce changement, contactez-nous immédiatement !</p>
                        
                        <p style="margin: 30px 0 0; font-weight: 600; color: %s;">L'équipe CNI</p>
                    </div>
                    <div class="footer">
                        <p>© 2026 CNI. Tous droits réservés.</p>
                        <p>Email envoyé à %s</p>
                    </div>
                </div>
            </body>
            </html>
            """.formatted(
                SUCCESS_COLOR, "#0f9e6a",
                SUCCESS_COLOR,
                SUCCESS_COLOR, username,
                SUCCESS_COLOR,
                PRIMARY_COLOR, to
        );

        sendHtmlEmail(to, subject, htmlContent);
    }

    // Send email when account is locked by admin
    public void sendAccountLockedByAdminEmail(String to, String username) throws MessagingException {
        String subject = "🔒 Compte verrouillé par l'administrateur";
        String htmlContent = """
            <!DOCTYPE html>
            <html>
            <head>
                <meta charset="UTF-8">
                <style>
                    body { font-family: 'Segoe UI', Arial, sans-serif; line-height: 1.6; color: #333; margin: 0; padding: 0; }
                    .container { max-width: 600px; margin: 20px auto; background: #ffffff; border-radius: 24px; overflow: hidden; box-shadow: 0 10px 30px rgba(0,0,0,0.08); }
                    .header { background: linear-gradient(135deg, %s 0%, %s 100%); padding: 30px; text-align: center; }
                    .icon { width: 80px; height: 80px; background: rgba(255,255,255,0.2); border-radius: 50%; margin: 0 auto 15px; display: flex; align-items: center; justify-content: center; }
                    .icon svg { width: 40px; height: 40px; fill: white; }
                    .content { padding: 40px; background: #ffffff; }
                    .warning-box { background: #fff3f3; border-left: 4px solid %s; padding: 20px; margin: 20px 0; border-radius: 8px; }
                    .footer { padding: 30px; text-align: center; background: #f8fafc; color: #64748b; font-size: 13px; border-top: 1px solid #e2e8f0; }
                </style>
            </head>
            <body>
                <div class="container">
                    <div class="header">
                        <div class="icon">
                            <svg viewBox="0 0 24 24" fill="white">
                                <path d="M18 8h-1V6c0-2.76-2.24-5-5-5S7 3.24 7 6v2H6c-1.1 0-2 .9-2 2v10c0 1.1.9 2 2 2h12c1.1 0 2-.9 2-2V10c0-1.1-.9-2-2-2zm-6 9c-1.1 0-2-.9-2-2s.9-2 2-2 2 .9 2 2-.9 2-2 2zm3.1-9H8.9V6c0-1.71 1.39-3.1 3.1-3.1 1.71 0 3.1 1.39 3.1 3.1v2z"/>
                            </svg>
                        </div>
                        <h1 style="color: white; margin: 0; font-size: 28px;">Compte verrouillé</h1>
                    </div>
                    <div class="content">
                        <p style="font-size: 18px; color: %s;">Bonjour %s,</p>
                        
                        <div class="warning-box">
                            <p style="margin: 0; font-weight: 600; color: %s;">⚠️ Action administrative</p>
                            <p style="margin: 10px 0 0;">Votre compte a été verrouillé par un administrateur.</p>
                        </div>
                        
                        <p><strong>📋 Détails :</strong></p>
                        <ul>
                            <li><strong>Date :</strong> %s</li>
                            <li><strong>Type :</strong> Verrouillage administratif</li>
                            <li><strong>Statut :</strong> Compte inaccessible</li>
                        </ul>
                        
                        <div style="background: #f8fafc; border-radius: 12px; padding: 15px; margin: 20px 0;">
                            <p style="margin: 0;"><span class="emoji">🔍</span> <strong>Que faire ?</strong></p>
                            <p style="margin: 10px 0 0;">Contactez l'équipe administrative pour plus d'informations.</p>
                        </div>
                        
                        <p style="margin: 30px 0 0;">Si vous pensez qu'il s'agit d'une erreur, répondez à cet email.</p>
                        
                        <p style="margin: 20px 0 0; font-weight: 600; color: %s;">L'équipe CNI</p>
                    </div>
                    <div class="footer">
                        <p>© 2026 CNI. Tous droits réservés.</p>
                        <p>Notification de sécurité envoyée à %s</p>
                    </div>
                </div>
            </body>
            </html>
            """.formatted(
                ERROR_COLOR, "#b91c1c",
                ERROR_COLOR,
                ERROR_COLOR, username,
                ERROR_COLOR,
                LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")),
                PRIMARY_COLOR, to
        );

        sendHtmlEmail(to, subject, htmlContent);
    }

    // Send email when account is unlocked by admin
    public void sendAccountUnlockedByAdminEmail(String to, String username) throws MessagingException {
        String subject = "🔓 Compte déverrouillé";
        String htmlContent = """
            <!DOCTYPE html>
            <html>
            <head>
                <meta charset="UTF-8">
                <style>
                    body { font-family: 'Segoe UI', Arial, sans-serif; line-height: 1.6; color: #333; margin: 0; padding: 0; }
                    .container { max-width: 600px; margin: 20px auto; background: #ffffff; border-radius: 24px; overflow: hidden; box-shadow: 0 10px 30px rgba(0,0,0,0.08); }
                    .header { background: linear-gradient(135deg, %s 0%, %s 100%); padding: 30px; text-align: center; }
                    .icon { width: 80px; height: 80px; background: rgba(255,255,255,0.2); border-radius: 50%; margin: 0 auto 15px; display: flex; align-items: center; justify-content: center; }
                    .icon svg { width: 40px; height: 40px; fill: white; }
                    .content { padding: 40px; background: #ffffff; }
                    .success-box { background: #f0fdf4; border-left: 4px solid %s; padding: 20px; margin: 20px 0; border-radius: 8px; }
                    .footer { padding: 30px; text-align: center; background: #f8fafc; color: #64748b; font-size: 13px; border-top: 1px solid #e2e8f0; }
                </style>
            </head>
            <body>
                <div class="container">
                    <div class="header">
                        <div class="icon">
                            <svg viewBox="0 0 24 24" fill="white">
                                <path d="M18 8h-1V6c0-2.76-2.24-5-5-5S7 3.24 7 6v2H6c-1.1 0-2 .9-2 2v10c0 1.1.9 2 2 2h12c1.1 0 2-.9 2-2V10c0-1.1-.9-2-2-2zm-6 9c-1.1 0-2-.9-2-2s.9-2 2-2 2 .9 2 2-.9 2-2 2zm3.1-9H8.9V6c0-1.71 1.39-3.1 3.1-3.1 1.71 0 3.1 1.39 3.1 3.1v2z"/>
                                <path d="M9 16.17L4.83 12l-1.42 1.41L9 19 21 7l-1.41-1.41L9 16.17z" transform="translate(0, 5)"/>
                            </svg>
                        </div>
                        <h1 style="color: white; margin: 0; font-size: 28px;">Compte déverrouillé</h1>
                    </div>
                    <div class="content">
                        <p style="font-size: 18px; color: %s;">Bonjour %s,</p>
                        
                        <div class="success-box">
                            <p style="margin: 0; font-weight: 600; color: %s;">✅ Bonne nouvelle !</p>
                            <p style="margin: 10px 0 0;">Votre compte a été déverrouillé par un administrateur.</p>
                        </div>
                        
                        <p><strong>📋 Détails :</strong></p>
                        <ul>
                            <li><strong>Date :</strong> %s</li>
                            <li><strong>Action :</strong> Réactivation du compte</li>
                            <li><strong>Statut :</strong> Compte actif</li>
                        </ul>
                        
                        <div style="background: #f8fafc; border-radius: 12px; padding: 20px; margin: 20px 0; text-align: center;">
                            <p style="margin: 0; font-size: 18px;">🎉 Vous pouvez maintenant vous connecter !</p>
                        </div>
                        
                        <p style="margin: 20px 0 0; font-weight: 600; color: %s;">L'équipe CNI</p>
                    </div>
                    <div class="footer">
                        <p>© 2026 CNI. Tous droits réservés.</p>
                        <p>Notification envoyée à %s</p>
                    </div>
                </div>
            </body>
            </html>
            """.formatted(
                SUCCESS_COLOR, "#0f9e6a",
                SUCCESS_COLOR,
                SUCCESS_COLOR, username,
                SUCCESS_COLOR,
                LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")),
                PRIMARY_COLOR, to
        );

        sendHtmlEmail(to, subject, htmlContent);
    }

    // Send email when account is temporarily locked due to failed attempts
    public void sendAccountTemporarilyLockedEmail(String to, String username, long minutesRemaining) throws MessagingException {
        String subject = "⏳ Compte temporairement verrouillé";
        String htmlContent = """
            <!DOCTYPE html>
            <html>
            <head>
                <meta charset="UTF-8">
                <style>
                    body { font-family: 'Segoe UI', Arial, sans-serif; line-height: 1.6; color: #333; margin: 0; padding: 0; }
                    .container { max-width: 600px; margin: 20px auto; background: #ffffff; border-radius: 24px; overflow: hidden; box-shadow: 0 10px 30px rgba(0,0,0,0.08); }
                    .header { background: linear-gradient(135deg, %s 0%, %s 100%); padding: 30px; text-align: center; }
                    .icon { width: 80px; height: 80px; background: rgba(255,255,255,0.2); border-radius: 50%; margin: 0 auto 15px; display: flex; align-items: center; justify-content: center; }
                    .icon svg { width: 40px; height: 40px; fill: white; }
                    .content { padding: 40px; background: #ffffff; }
                    .timer-box { background: #fff7ed; border-left: 4px solid %s; padding: 20px; margin: 20px 0; border-radius: 8px; }
                    .timer { font-size: 36px; font-weight: bold; color: %s; text-align: center; margin: 20px 0; }
                    .footer { padding: 30px; text-align: center; background: #f8fafc; color: #64748b; font-size: 13px; border-top: 1px solid #e2e8f0; }
                </style>
            </head>
            <body>
                <div class="container">
                    <div class="header">
                        <div class="icon">
                            <svg viewBox="0 0 24 24" fill="white">
                                <circle cx="12" cy="12" r="10"/>
                                <polyline points="12 6 12 12 16 14"/>
                                <path d="M12 2v2M12 20v2M4 12H2M22 12h-2M6.4 6.4L4.2 4.2M17.6 17.6l2.2 2.2"/>
                            </svg>
                        </div>
                        <h1 style="color: white; margin: 0; font-size: 28px;">Verrouillage temporaire</h1>
                    </div>
                    <div class="content">
                        <p style="font-size: 18px; color: %s;">Bonjour %s,</p>
                        
                        <div class="timer-box">
                            <p style="margin: 0; font-weight: 600; color: %s;">⚠️ Trop de tentatives échouées</p>
                            <p style="margin: 10px 0 0;">Pour votre sécurité, votre compte est temporairement verrouillé.</p>
                            
                            <div class="timer">
                                ⏱️ %d minutes
                            </div>
                            
                            <p style="text-align: center; margin: 0;">Temps restant avant déverrouillage automatique</p>
                        </div>
                        
                        <p><strong>💡 Conseils :</strong></p>
                        <ul>
                            <li>Attendez la fin du délai de %d minutes</li>
                            <li>Utilisez "Mot de passe oublié" si nécessaire</li>
                            <li>Vérifiez vos identifiants</li>
                        </ul>
                        
                        <p style="margin: 20px 0 0;">Besoin d'aide ? Contactez notre support.</p>
                        
                        <p style="margin: 20px 0 0; font-weight: 600; color: %s;">L'équipe CNI</p>
                    </div>
                    <div class="footer">
                        <p>© 2026 CNI. Tous droits réservés.</p>
                        <p>Notification de sécurité envoyée à %s</p>
                    </div>
                </div>
            </body>
            </html>
            """.formatted(
                WARNING_COLOR, "#d97706",
                WARNING_COLOR, WARNING_COLOR,
                WARNING_COLOR, username,
                WARNING_COLOR,
                minutesRemaining,
                minutesRemaining,
                PRIMARY_COLOR, to
        );

        sendHtmlEmail(to, subject, htmlContent);
    }

    // Helper method to send HTML emails
    private void sendHtmlEmail(String to, String subject, String htmlContent) throws MessagingException {
        MimeMessage message = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

        try {
            helper.setFrom(new InternetAddress(fromEmail, "CNI 💙"));
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        }
        helper.setTo(to);
        helper.setSubject("✨ " + subject);
        helper.setText(htmlContent, true);

        mailSender.send(message);
    }



    // Add to EmailService.java

    public void sendEmailWithAttachment(String to, String subject, String htmlContent,
                                        String pdfBase64, String pdfFileName) throws MessagingException {
        MimeMessage message = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

        try {
            helper.setFrom(new InternetAddress(fromEmail, "CNI 💙"));
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        }
        helper.setTo(to);
        helper.setSubject(subject);
        helper.setText(htmlContent, true);

        // Attach PDF if provided
        if (pdfBase64 != null && !pdfBase64.isEmpty()) {
            byte[] pdfBytes = java.util.Base64.getDecoder().decode(pdfBase64);
            helper.addAttachment(pdfFileName, new ByteArrayResource(pdfBytes), "application/pdf");
        }

        mailSender.send(message);
    }
}