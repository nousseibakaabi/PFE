package com.example.back.service;

import jakarta.mail.internet.InternetAddress;
import org.apache.commons.codec.binary.Base64;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.ClassPathResource;
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



    private String PRIMARY_COLOR = "#6EB9D5";
    private String DARK_BLUE = "#2C5F8A";
    private final String SUCCESS_COLOR = "#10b981";
    private final String WARNING_COLOR = "#f59e0b";
    private final String ERROR_COLOR = "#ef4444";



    private String getLogoBase64() {
        try {
            ClassPathResource resource = new ClassPathResource("images/logo.png");
            System.out.println("Logo file exists: " + resource.exists());
            System.out.println("Logo file path: " + resource.getPath());

            byte[] fileBytes = resource.getInputStream().readAllBytes();
            System.out.println("Logo file size: " + fileBytes.length + " bytes");

            String base64 = Base64.encodeBase64String(fileBytes);
            System.out.println("Base64 length: " + base64.length());

            return base64;
        } catch (Exception e) {
            System.err.println("ERROR loading logo: " + e.getMessage());
            e.printStackTrace();
            return ""; // Return empty if logo not found
        }
    }

    public void sendPasswordResetEmail(String to, String resetToken, String username) throws MessagingException {
        String resetLink = frontendUrl + "/reset-password?token=" + resetToken;
        String subject = "🔐 Réinitialisation de votre mot de passe";

        String htmlContent = String.format("""
    <!DOCTYPE html>
    <html>
    <head>
        <meta charset="UTF-8">
        <meta name="viewport" content="width=device-width, initial-scale=1.0">
        <title>Réinitialisation mot de passe</title>
        <style>
            * { margin: 0; padding: 0; box-sizing: border-box; }
            body { 
                font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, 'Helvetica Neue', Arial, sans-serif;
                line-height: 1.6; 
                background: #f0f4f8;
                margin: 0; 
                padding: 20px; 
            }
            .container { 
                max-width: 550px; 
                margin: 0 auto; 
                background: #ffffff;
                border-radius: 20px;
                overflow: hidden;
                box-shadow: 0 10px 25px -5px rgba(0,0,0,0.1);
            }
            /* Small blue gradient bar */
            .gradient-bar {
                background: linear-gradient(135deg, %s 0%%, %s 100%%);
                padding: 20px;
                text-align: center;
            }
            .gradient-bar h2 {
                color: white;
                font-size: 18px;
                font-weight: 600;
                margin: 0;
                letter-spacing: 0.5px;
            }
            /* Content area */
            .content {
                padding: 35px 30px;
            }
            /* Message text */
            .message-text {
                color: #4a5568;
                font-size: 15px;
                line-height: 1.6;
                margin-bottom: 30px;
            }
            .message-text .greeting {
                font-weight: 600;
                color: %s;
                font-size: 16px;
                margin-bottom: 12px;
            }
            /* Transparent button with blue border */
            .button-container {
                text-align: center;
                margin: 30px 0;
            }
            .btn-outline {
                display: inline-block;
                background: transparent;
                color: %s !important;
                padding: 12px 32px;
                text-decoration: none;
                border-radius: 50px;
                font-weight: 600;
                font-size: 15px;
                border: 2px solid %s;
                transition: all 0.3s ease;
                cursor: pointer;
            }
            /* Info text as simple list */
            .info-list {
                margin: 30px 0 25px 0;
                padding: 0;
                list-style: none;
            }
            .info-list li {
                display: flex;
                align-items: center;
                gap: 12px;
                padding: 10px 0;
                color: #4a5568;
                font-size: 14px;
                border-bottom: 1px solid #eef2f6;
            }
            .info-list li:last-child {
                border-bottom: none;
            }
            .info-emoji {
                font-size: 20px;
                min-width: 32px;
            }
            .info-text {
                flex: 1;
            }
            .info-text strong {
                color: %s;
            }
            /* Alternative link */
            .alt-link {
                background: #f8fafc;
                border-radius: 12px;
                padding: 15px;
                margin: 25px 0;
                border: 1px solid #e2e8f0;
            }
            .alt-label {
                color: #64748b;
                font-size: 12px;
                text-align: center;
                margin-bottom: 10px;
                font-weight: 500;
            }
            .alt-url {
                background: white;
                padding: 10px 12px;
                border-radius: 8px;
                word-break: break-all;
                font-size: 11px;
                font-family: 'Courier New', monospace;
                color: %s;
                border: 1px solid #e2e8f0;
                text-align: center;
            }
            /* Footer */
            .footer {
                padding: 20px 30px;
                text-align: center;
                background: #f8fafc;
                border-top: 1px solid #e2e8f0;
            }
            .footer p {
                color: #94a3b8;
                font-size: 11px;
                margin: 5px 0;
            }
            .auto-message {
                margin-top: 8px;
                font-size: 10px;
                color: #cbd5e1;
            }
            @media (max-width: 500px) {
                .content { padding: 25px 20px; }
            }
        </style>
    </head>
    <body>
        <div class="container">
            <!-- Small blue gradient bar -->
            <div class="gradient-bar">
                <h2>🔐 RÉINITIALISATION DU MOT DE PASSE</h2>
            </div>
            
            <!-- Content -->
            <div class="content">
                <!-- Message text -->
                <div class="message-text">
                    <div class="greeting">Bonjour %s,</div>
                    <p style="margin: 0;">Nous avons reçu une demande de réinitialisation de votre mot de passe. Cliquez sur le bouton ci-dessous pour en créer un nouveau :</p>
                </div>
                
                <!-- Transparent button with blue border -->
                <div class="button-container">
                    <a href="%s" class="btn-outline">🔑 Réinitialiser mon mot de passe</a>
                </div>
                
                <!-- Alternative link -->
                <div class="alt-link">
                    <div class="alt-label">⚠️ Si le bouton ne fonctionne pas, copiez ce lien :</div>
                    <div class="alt-url">%s</div>
                </div>
                
                <!-- Info as text list under the link -->
           <p style="margin: 20px 0 15px 0; font-size: 12px; color: #64748b; text-align: center;">
                                                            ⏰ 24h • 🔒 Ne pas partager • 🛡️ SSL
                                                        </p>
            </div>
            
            <!-- Footer -->
            <div class="footer">
                <p>© 2026 CNI. Tous droits réservés.</p>
                <div class="auto-message">Cet email est automatique - merci de ne pas répondre</div>
            </div>
        </div>
    </body>
    </html>
    """,
                PRIMARY_COLOR, DARK_BLUE,  // gradient bar colors
                PRIMARY_COLOR,              // greeting color
                PRIMARY_COLOR,              // button text color
                PRIMARY_COLOR,              // button border color
                PRIMARY_COLOR,              // strong text color in info list
                PRIMARY_COLOR,              // alt url color
                username,                   // greeting username
                resetLink,                  // button link
                resetLink                   // fallback link
        );

        sendHtmlEmail(to, subject, htmlContent);
    }

    public void sendPasswordResetConfirmation(String to, String username) throws MessagingException {
        String subject = "✓ Mot de passe modifié avec succès";

        String htmlContent = String.format("""
    <!DOCTYPE html>
    <html>
    <head>
        <meta charset="UTF-8">
        <meta name="viewport" content="width=device-width, initial-scale=1.0">
        <title>Mot de passe modifié</title>
        <style>
            * { margin: 0; padding: 0; box-sizing: border-box; }
            body { 
                font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, 'Helvetica Neue', Arial, sans-serif;
                line-height: 1.6; 
                background: #f0f4f8;
                margin: 0; 
                padding: 20px; 
            }
            .container { 
                max-width: 550px; 
                margin: 0 auto; 
                background: #ffffff;
                border-radius: 20px;
                overflow: hidden;
                box-shadow: 0 10px 25px -5px rgba(0,0,0,0.1);
            }
            /* Small green gradient bar */
            .gradient-bar {
                background: linear-gradient(135deg, %s 0%%, %s 100%%);
                padding: 20px;
                text-align: center;
            }
            .gradient-bar h2 {
                color: white;
                font-size: 18px;
                font-weight: 600;
                margin: 0;
                letter-spacing: 0.5px;
            }
            /* Content area */
            .content {
                padding: 35px 30px;
            }
            /* Message text */
            .message-text {
                color: #4a5568;
                font-size: 15px;
                line-height: 1.6;
                margin-bottom: 25px;
            }
            .message-text .greeting {
                font-weight: 600;
                color: %s;
                font-size: 16px;
                margin-bottom: 12px;
            }
            /* Success badge */
            .success-badge {
                text-align: center;
                margin: 20px 0;
            }
            .badge {
                display: inline-block;
                background: %s;
                color: white;
                padding: 8px 20px;
                border-radius: 50px;
                font-size: 13px;
                font-weight: 600;
            }
            /* Security tips as compact list */
            .tips-list {
                margin: 25px 0;
                padding: 0;
                list-style: none;
            }
            .tips-list li {
                display: flex;
                align-items: center;
                gap: 10px;
                padding: 8px 0;
                font-size: 13px;
                color: #4a5568;
                border-bottom: 1px solid #eef2f6;
            }
            .tips-list li:last-child {
                border-bottom: none;
            }
            .tips-list .emoji {
                font-size: 16px;
                min-width: 28px;
            }
            /* Footer */
            .footer {
                padding: 20px 30px;
                text-align: center;
                background: #f8fafc;
                border-top: 1px solid #e2e8f0;
            }
            .footer p {
                color: #94a3b8;
                font-size: 11px;
                margin: 5px 0;
            }
            .auto-message {
                margin-top: 8px;
                font-size: 10px;
                color: #cbd5e1;
            }
            @media (max-width: 500px) {
                .content { padding: 25px 20px; }
            }
        </style>
    </head>
    <body>
        <div class="container">
            <!-- Green gradient bar -->
            <div class="gradient-bar">
                <h2>✓ MOT DE PASSE MODIFIÉ</h2>
            </div>
            
            <!-- Content -->
            <div class="content">
                <!-- Success badge -->
                <div class="success-badge">
                    <div class="badge">✓ Opération réussie</div>
                </div>
                
                <!-- Message text -->
                <div class="message-text">
                    <div class="greeting">Bonjour %s,</div>
                    <p style="margin: 0;">Votre mot de passe a été modifié avec succès. Votre compte est maintenant sécurisé avec votre nouveau mot de passe.</p>
                </div>
                
                <!-- Security tips as compact list -->
                <ul class="tips-list">
                    <li>
                        <span class="emoji">🔐</span>
                        <span>Utilisez un mot de passe <strong style="color: %s;">unique et fort</strong></span>
                    </li>
                    <li>
                        <span class="emoji">🚫</span>
                        <span>Ne le <strong style="color: %s;">partagez jamais</strong> avec personne</span>
                    </li>
                    <li>
                        <span class="emoji">🔄</span>
                        <span>Changez-le <strong style="color: %s;">régulièrement</strong></span>
                    </li>
                    <li>
                        <span class="emoji">🔑</span>
                        <span>Activez la <strong style="color: %s;">double authentification</strong></span>
                    </li>
                </ul>
                
                <!-- Warning message -->
                <p style="margin: 20px 0 0; font-size: 12px; color: #ef4444; text-align: center;">
                    ⚠️ Si vous n'êtes pas à l'origine de ce changement, contactez-nous immédiatement !
                </p>
            </div>
            
            <!-- Footer -->
            <div class="footer">
                <p>© 2026 CNI. Tous droits réservés.</p>
                <div class="auto-message">Cet email est automatique - merci de ne pas répondre</div>
            </div>
        </div>
    </body>
    </html>
    """,
                SUCCESS_COLOR, "#0f9e6a",  // gradient bar colors
                SUCCESS_COLOR,              // greeting color
                SUCCESS_COLOR,              // badge color
                username,                   // greeting username
                SUCCESS_COLOR,              // strong text color (unique et fort)
                SUCCESS_COLOR,              // strong text color (partagez jamais)
                SUCCESS_COLOR,              // strong text color (régulièrement)
                SUCCESS_COLOR,              // strong text color (double authentification)
                PRIMARY_COLOR,              // team name color
                to                          // email recipient in footer
        );

        sendHtmlEmail(to, subject, htmlContent);
    }

    public void sendAccountLockedByAdminEmail(String to, String username) throws MessagingException {
        String subject = "🔒 Compte verrouillé par l'administrateur";

        String htmlContent = String.format("""
    <!DOCTYPE html>
    <html>
    <head>
        <meta charset="UTF-8">
        <meta name="viewport" content="width=device-width, initial-scale=1.0">
        <title>Compte verrouillé</title>
        <style>
            * { margin: 0; padding: 0; box-sizing: border-box; }
            body { 
                font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, 'Helvetica Neue', Arial, sans-serif;
                line-height: 1.6; 
                background: #f0f4f8;
                margin: 0; 
                padding: 20px; 
            }
            .container { 
                max-width: 550px; 
                margin: 0 auto; 
                background: #ffffff;
                border-radius: 20px;
                overflow: hidden;
                box-shadow: 0 10px 25px -5px rgba(0,0,0,0.1);
            }
            /* Red gradient bar */
            .gradient-bar {
                background: linear-gradient(135deg, %s 0%%, %s 100%%);
                padding: 20px;
                text-align: center;
            }
            .gradient-bar h2 {
                color: white;
                font-size: 18px;
                font-weight: 600;
                margin: 0;
                letter-spacing: 0.5px;
            }
            /* Content area */
            .content {
                padding: 35px 30px;
            }
            /* Message text */
            .message-text {
                color: #4a5568;
                font-size: 15px;
                line-height: 1.6;
                margin-bottom: 25px;
            }
            .message-text .greeting {
                font-weight: 600;
                color: %s;
                font-size: 16px;
                margin-bottom: 12px;
            }
            /* Warning badge */
            .warning-badge {
                text-align: center;
                margin: 20px 0;
            }
            .badge {
                display: inline-block;
                background: %s;
                color: white;
                padding: 8px 20px;
                border-radius: 50px;
                font-size: 13px;
                font-weight: 600;
            }
            /* Details list */
            .details-list {
                margin: 25px 0;
                padding: 0;
                list-style: none;
                background: #fef2f2;
                border-radius: 12px;
                padding: 15px 20px;
            }
            .details-list li {
                display: flex;
                align-items: center;
                gap: 10px;
                padding: 8px 0;
                font-size: 13px;
                color: #4a5568;
                border-bottom: 1px solid #fecaca;
            }
            .details-list li:last-child {
                border-bottom: none;
            }
            .details-list .emoji {
                font-size: 16px;
                min-width: 28px;
            }
            /* Action box */
            .action-box {
                background: #f8fafc;
                border-radius: 12px;
                padding: 15px;
                margin: 20px 0;
                text-align: center;
            }
            .action-box p {
                margin: 5px 0;
                font-size: 13px;
                color: #4a5568;
            }
            /* Footer */
            .footer {
                padding: 20px 30px;
                text-align: center;
                background: #f8fafc;
                border-top: 1px solid #e2e8f0;
            }
            .footer p {
                color: #94a3b8;
                font-size: 11px;
                margin: 5px 0;
            }
            .auto-message {
                margin-top: 8px;
                font-size: 10px;
                color: #cbd5e1;
            }
            @media (max-width: 500px) {
                .content { padding: 25px 20px; }
            }
        </style>
    </head>
    <body>
        <div class="container">
            <!-- Red gradient bar -->
            <div class="gradient-bar">
                <h2>🔒 COMPTE VERROUILLÉ</h2>
            </div>
            
            <!-- Content -->
            <div class="content">
                <!-- Warning badge -->
                <div class="warning-badge">
                    <div class="badge">⚠️ Action administrative</div>
                </div>
                
                <!-- Message text -->
                <div class="message-text">
                    <div class="greeting">Bonjour %s,</div>
                    <p style="margin: 0;">Votre compte a été verrouillé par un administrateur.</p>
                </div>
                
                <!-- Details list -->
                <ul class="details-list">
                    <li>
                        <span class="emoji">📅</span>
                        <span><strong>Date :</strong> %s</span>
                    </li>
                    <li>
                        <span class="emoji">🏷️</span>
                        <span><strong>Type :</strong> Verrouillage administratif</span>
                    </li>
                    <li>
                        <span class="emoji">🔒</span>
                        <span><strong>Statut :</strong> Compte inaccessible</span>
                    </li>
                </ul>
                
                <!-- Action box -->
                <div class="action-box">
                    <p>🔍 <strong>Que faire ?</strong></p>
                    <p>Contactez l'équipe administrative pour plus d'informations.</p>
                </div>
                
                <!-- Warning message -->
                <p style="margin: 20px 0 0; font-size: 12px; color: #ef4444; text-align: center;">
                    ⚠️ Si vous pensez qu'il s'agit d'une erreur, répondez à cet email.
                </p>
                
             
            </div>
            
            <!-- Footer -->
            <div class="footer">
                <p>© 2026 CNI. Tous droits réservés.</p>
                <div class="auto-message">Cet email est automatique - merci de ne pas répondre</div>
            </div>
        </div>
    </body>
    </html>
    """,
                ERROR_COLOR, "#b91c1c",      // gradient bar colors
                ERROR_COLOR,                 // greeting color
                ERROR_COLOR,                 // badge color
                username,                    // greeting username
                LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")), // date
                PRIMARY_COLOR,               // team name color
                to                           // email recipient in footer
        );

        sendHtmlEmail(to, subject, htmlContent);
    }

    public void sendAccountUnlockedByAdminEmail(String to, String username) throws MessagingException {
        String subject = "🔓 Compte déverrouillé";

        String htmlContent = String.format("""
    <!DOCTYPE html>
    <html>
    <head>
        <meta charset="UTF-8">
        <meta name="viewport" content="width=device-width, initial-scale=1.0">
        <title>Compte déverrouillé</title>
        <style>
            * { margin: 0; padding: 0; box-sizing: border-box; }
            body { 
                font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, 'Helvetica Neue', Arial, sans-serif;
                line-height: 1.6; 
                background: #f0f4f8;
                margin: 0; 
                padding: 20px; 
            }
            .container { 
                max-width: 550px; 
                margin: 0 auto; 
                background: #ffffff;
                border-radius: 20px;
                overflow: hidden;
                box-shadow: 0 10px 25px -5px rgba(0,0,0,0.1);
            }
            .gradient-bar {
                background: linear-gradient(135deg, %s 0%%, %s 100%%);
                padding: 20px;
                text-align: center;
            }
            .gradient-bar h2 {
                color: white;
                font-size: 18px;
                font-weight: 600;
                margin: 0;
                letter-spacing: 0.5px;
            }
            .content {
                padding: 35px 30px;
            }
            .message-text {
                color: #4a5568;
                font-size: 15px;
                line-height: 1.6;
                margin-bottom: 25px;
            }
            .message-text .greeting {
                font-weight: 600;
                color: %s;
                font-size: 16px;
                margin-bottom: 12px;
            }
            .success-badge {
                text-align: center;
                margin: 20px 0;
            }
            .badge {
                display: inline-block;
                background: %s;
                color: white;
                padding: 8px 20px;
                border-radius: 50px;
                font-size: 13px;
                font-weight: 600;
            }
            .details-list {
                margin: 25px 0;
                padding: 0;
                list-style: none;
                background: #f0fdf4;
                border-radius: 12px;
                padding: 15px 20px;
            }
            .details-list li {
                display: flex;
                align-items: center;
                gap: 10px;
                padding: 8px 0;
                font-size: 13px;
                color: #4a5568;
                border-bottom: 1px solid #dcfce7;
            }
            .details-list li:last-child {
                border-bottom: none;
            }
            .details-list .emoji {
                font-size: 16px;
                min-width: 28px;
            }
            .footer {
                padding: 20px 30px;
                text-align: center;
                background: #f8fafc;
                border-top: 1px solid #e2e8f0;
            }
            .footer p {
                color: #94a3b8;
                font-size: 11px;
                margin: 5px 0;
            }
            .auto-message {
                margin-top: 8px;
                font-size: 10px;
                color: #cbd5e1;
            }
            @media (max-width: 500px) {
                .content { padding: 25px 20px; }
            }
        </style>
    </head>
    <body>
        <div class="container">
            <div class="gradient-bar">
                <h2>🔓 COMPTE DÉVERROUILLÉ</h2>
            </div>
            
            <div class="content">
                <div class="success-badge">
                    <div class="badge">✅ Compte réactivé</div>
                </div>
                
                <div class="message-text">
                    <div class="greeting">Bonjour %s,</div>
                    <p style="margin: 0;">Votre compte a été déverrouillé par un administrateur.</p>
                </div>
                
                <ul class="details-list">
                    <li>
                        <span class="emoji">📅</span>
                        <span><strong>Date :</strong> %s</span>
                    </li>
                    <li>
                        <span class="emoji">🔓</span>
                        <span><strong>Action :</strong> Réactivation du compte</span>
                    </li>
                    <li>
                        <span class="emoji">✅</span>
                        <span><strong>Statut :</strong> Compte actif</span>
                    </li>
                </ul>
                
                <p style="margin: 20px 0 0; font-size: 12px; text-align: center;">
                    🎉 Vous pouvez maintenant vous connecter !
                </p>
               
            </div>
            
            <div class="footer">
                <p>© 2026 CNI. Tous droits réservés.</p>
                <div class="auto-message">Cet email est automatique - merci de ne pas répondre</div>
            </div>
        </div>
    </body>
    </html>
    """,
                SUCCESS_COLOR, "#0f9e6a",  // gradient bar colors
                SUCCESS_COLOR,              // greeting color
                SUCCESS_COLOR,              // badge color
                username,                   // greeting username
                LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")), // date
                PRIMARY_COLOR,              // team name color
                to                          // email recipient in footer
        );

        sendHtmlEmail(to, subject, htmlContent);
    }

    // Send email when account is temporarily locked due to failed attempts
    public void sendAccountTemporarilyLockedEmail(String to, String username, long minutesRemaining) throws MessagingException {
        System.out.println("=== SENDING TEMPORARY LOCK EMAIL ===");
        System.out.println("To: " + to);
        System.out.println("Username: " + username);
        System.out.println("Minutes remaining: " + minutesRemaining);

        String subject = "⏳ Compte temporairement verrouillé";

        String htmlContent = String.format("""
    <!DOCTYPE html>
    <html>
    <head>
        <meta charset="UTF-8">
        <meta name="viewport" content="width=device-width, initial-scale=1.0">
        <title>Compte temporairement verrouillé</title>
        <style>
            * { margin: 0; padding: 0; box-sizing: border-box; }
            body { 
                font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, 'Helvetica Neue', Arial, sans-serif;
                line-height: 1.6; 
                background: #f0f4f8;
                margin: 0; 
                padding: 20px; 
            }
            .container { 
                max-width: 550px; 
                margin: 0 auto; 
                background: #ffffff;
                border-radius: 20px;
                overflow: hidden;
                box-shadow: 0 10px 25px -5px rgba(0,0,0,0.1);
            }
            .gradient-bar {
                background: linear-gradient(135deg, %s 0%%, %s 100%%);
                padding: 20px;
                text-align: center;
            }
            .gradient-bar h2 {
                color: white;
                font-size: 18px;
                font-weight: 600;
                margin: 0;
                letter-spacing: 0.5px;
            }
            .content {
                padding: 35px 30px;
            }
            .message-text {
                color: #4a5568;
                font-size: 15px;
                line-height: 1.6;
                margin-bottom: 25px;
            }
            .message-text .greeting {
                font-weight: 600;
                color: %s;
                font-size: 16px;
                margin-bottom: 12px;
            }
            .warning-badge {
                text-align: center;
                margin: 20px 0;
            }
            .badge {
                display: inline-block;
                background: %s;
                color: white;
                padding: 8px 20px;
                border-radius: 50px;
                font-size: 13px;
                font-weight: 600;
            }
            .timer-display {
                background: #fff7ed;
                border-radius: 16px;
                padding: 20px;
                margin: 25px 0;
                text-align: center;
                border: 1px solid #fed7aa;
            }
            .timer {
                font-size: 42px;
                font-weight: bold;
                color: %s;
                margin: 15px 0;
                letter-spacing: 2px;
            }
            .timer-label {
                font-size: 12px;
                color: #64748b;
            }
            .tips-list {
                margin: 25px 0;
                padding: 0;
                list-style: none;
                background: #f8fafc;
                border-radius: 12px;
                padding: 15px 20px;
            }
            .tips-list li {
                display: flex;
                align-items: center;
                gap: 10px;
                padding: 8px 0;
                font-size: 13px;
                color: #4a5568;
                border-bottom: 1px solid #e2e8f0;
            }
            .tips-list li:last-child {
                border-bottom: none;
            }
            .tips-list .emoji {
                font-size: 16px;
                min-width: 28px;
            }
            .footer {
                padding: 20px 30px;
                text-align: center;
                background: #f8fafc;
                border-top: 1px solid #e2e8f0;
            }
            .footer p {
                color: #94a3b8;
                font-size: 11px;
                margin: 5px 0;
            }
            .auto-message {
                margin-top: 8px;
                font-size: 10px;
                color: #cbd5e1;
            }
            @media (max-width: 500px) {
                .content { padding: 25px 20px; }
                .timer { font-size: 32px; }
            }
        </style>
    </head>
    <body>
        <div class="container">
            <div class="gradient-bar">
                <h2>⏳ VERROUILLAGE TEMPORAIRE</h2>
            </div>
            
            <div class="content">
                <div class="warning-badge">
                    <div class="badge">⚠️ Trop de tentatives échouées</div>
                </div>
                
                <div class="message-text">
                    <div class="greeting">Bonjour %s,</div>
                    <p style="margin: 0;">Pour votre sécurité, votre compte est temporairement verrouillé.</p>
                </div>
                
                <div class="timer-display">
                    <div class="timer">⏱️ %d</div>
                    <div class="timer-label">minutes restantes avant déverrouillage automatique</div>
                </div>
                
                <ul class="tips-list">
                    <li>
                        <span class="emoji">⏰</span>
                        <span>Attendez la fin du délai de <strong>%d minutes</strong></span>
                    </li>
                    <li>
                        <span class="emoji">🔑</span>
                        <span>Utilisez <strong>"Mot de passe oublié"</strong> si nécessaire</span>
                    </li>
                    <li>
                        <span class="emoji">✅</span>
                        <span><strong>Vérifiez vos identifiants</strong> avant de réessayer</span>
                    </li>
                </ul>
                
                <p style="margin: 20px 0 0; font-size: 13px; color: #4a5568; text-align: center;">
                    💬 Besoin d'aide ? Contactez notre support
                </p>
                
               
            </div>
            
            <div class="footer">
                <p>© 2026 CNI. Tous droits réservés.</p>
                <div class="auto-message">Cet email est automatique - merci de ne pas répondre</div>
            </div>
        </div>
    </body>
    </html>
    """,
                WARNING_COLOR, "#d97706",    // gradient bar colors
                WARNING_COLOR,               // greeting color
                WARNING_COLOR,               // badge color
                WARNING_COLOR,               // timer color
                username,                    // greeting username
                minutesRemaining,            // timer minutes
                minutesRemaining,            // tips minutes
                PRIMARY_COLOR,               // team name color
                to                           // email recipient in footer
        );

        System.out.println("HTML content length: " + htmlContent.length());
        System.out.println("About to send email...");
        sendHtmlEmail(to, subject, htmlContent);
        System.out.println("Email sent successfully!");
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