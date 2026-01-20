package com.example.back.service;

import jakarta.mail.internet.InternetAddress;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;

import java.io.UnsupportedEncodingException;
import java.time.LocalDateTime;

@Service
public class EmailService {

    @Autowired
    private JavaMailSender mailSender;

    @Value("${spring.mail.username}")
    private String fromEmail;

    @Value("${frontend.url:http://localhost:4200}")
    private String frontendUrl;

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
    public void sendPasswordResetEmail(String to, String resetToken, String username) throws MessagingException {
        String resetLink = frontendUrl + "/reset-password?token=" + resetToken;

        String subject = "Password Reset Request";
        String htmlContent = """
            <!DOCTYPE html>
            <html>
            <head>
                <style>
                    body { font-family: Arial, sans-serif; line-height: 1.6; color: #333; }
                    .container { max-width: 600px; margin: 0 auto; padding: 20px; }
                    .header { background-color: #4eb8dd; color: white; padding: 20px; text-align: center; border-radius: 5px 5px 0 0; }
                    .content { background-color: #f9f9f9; padding: 30px; border-radius: 0 0 5px 5px; }
                    .button { display: inline-block; background-color: #4eb8dd; color: white; padding: 12px 24px; text-decoration: none; border-radius: 4px; margin: 20px 0; }
                    .footer { margin-top: 20px; padding-top: 20px; border-top: 1px solid #eee; color: #777; font-size: 12px; }
                </style>
            </head>
            <body>
                <div class="container">
                    <div class="header">
                        <h2>Password Reset Request</h2>
                    </div>
                    <div class="content">
                        <p>Hello %s,</p>
                        <p>You have requested to reset your password. Click the button below to create a new password:</p>
                        <p style="text-align: center;">
                            <a href="%s" class="button">Reset Password</a>
                        </p>
                        <p>If the button doesn't work, copy and paste this link into your browser:</p>
                        <p><code>%s</code></p>
                        <p>This link will expire in 24 hours.</p>
                    </div>
                    <div class="footer">
                        <p>This is an automated message, please do not reply to this email.</p>
                    </div>
                </div>
            </body>
            </html>
            """.formatted(username, resetLink, resetLink);

        MimeMessage message = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

        try {
            helper.setFrom(new InternetAddress(fromEmail, "CNI Administrator"));
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        }
        helper.setTo(to);
        helper.setSubject(subject);
        helper.setText(htmlContent, true);

        mailSender.send(message);
    }

    // Send password reset confirmation email
    public void sendPasswordResetConfirmation(String to, String username) throws MessagingException {
        String subject = "Password Reset Successful";
        String htmlContent = """
            <!DOCTYPE html>
            <html>
            <head>
                <style>
                    body { font-family: Arial, sans-serif; line-height: 1.6; color: #333; }
                    .container { max-width: 600px; margin: 0 auto; padding: 20px; }
                    .header { background-color: #28a745; color: white; padding: 20px; text-align: center; border-radius: 5px 5px 0 0; }
                    .content { background-color: #f9f9f9; padding: 30px; border-radius: 0 0 5px 5px; }
                    .footer { margin-top: 20px; padding-top: 20px; border-top: 1px solid #eee; color: #777; font-size: 12px; }
                </style>
            </head>
            <body>
                <div class="container">
                    <div class="header">
                        <h2>Password Reset Successful</h2>
                    </div>
                    <div class="content">
                        <p>Hello %s,</p>
                        <p>Your password has been successfully reset.</p>
                        <p>If you did not make this change, please contact our support team immediately.</p>
                        <p>For security reasons, we recommend that you:</p>
                        <ul>
                            <li>Use a strong, unique password</li>
                            <li>Enable two-factor authentication if available</li>
                            <li>Avoid using the same password for multiple accounts</li>
                        </ul>
                    </div>
                    <div class="footer">
                        <p>This is an automated message, please do not reply to this email.</p>
                    </div>
                </div>
            </body>
            </html>
            """.formatted(username);

        MimeMessage message = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

        try {
            helper.setFrom(new InternetAddress(fromEmail, "CNI Administrator"));
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        }        helper.setTo(to);
        helper.setSubject(subject);
        helper.setText(htmlContent, true);

        mailSender.send(message);
    }

    // Send email when account is locked by admin
    public void sendAccountLockedByAdminEmail(String to, String username) throws MessagingException {
        String subject = "Account Locked by Administrator";
        String htmlContent = """
        <!DOCTYPE html>
        <html>
        <head>
            <style>
                body { font-family: Arial, sans-serif; line-height: 1.6; color: #333; }
                .container { max-width: 600px; margin: 0 auto; padding: 20px; }
                .header { background-color: #dc3545; color: white; padding: 20px; text-align: center; border-radius: 5px 5px 0 0; }
                .content { background-color: #f9f9f9; padding: 30px; border-radius: 0 0 5px 5px; }
                .footer { margin-top: 20px; padding-top: 20px; border-top: 1px solid #eee; color: #777; font-size: 12px; }
            </style>
        </head>
        <body>
            <div class="container">
                <div class="header">
                    <h2>Account Locked</h2>
                </div>
                <div class="content">
                    <p>Hello %s,</p>
                    <p>Your account has been <strong>locked by an administrator</strong>.</p>
                    <p><strong>Action:</strong> Administrative lock</p>
                    <p><strong>Reason:</strong> Administrative action</p>
                    <p>Please contact the system administrator to unlock your account and discuss the reason for this action.</p>
                    <p>If you believe this is an error, please contact support immediately.</p>
                </div>
                <div class="footer">
                    <p>This is an automated security notification.</p>
                </div>
            </div>
        </body>
        </html>
        """.formatted(username, LocalDateTime.now().toString());

        MimeMessage message = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

        try {
            helper.setFrom(new InternetAddress(fromEmail, "CNI Administrator"));
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        }
        helper.setTo(to);
        helper.setSubject(subject);
        helper.setText(htmlContent, true);

        mailSender.send(message);
    }

    // Send email when account is unlocked by admin
    public void sendAccountUnlockedByAdminEmail(String to, String username) throws MessagingException {
        String subject = "Account Unlocked";
        String htmlContent = """
        <!DOCTYPE html>
        <html>
        <head>
            <style>
                body { font-family: Arial, sans-serif; line-height: 1.6; color: #333; }
                .container { max-width: 600px; margin: 0 auto; padding: 20px; }
                .header { background-color: #28a745; color: white; padding: 20px; text-align: center; border-radius: 5px 5px 0 0; }
                .content { background-color: #f9f9f9; padding: 30px; border-radius: 0 0 5px 5px; }
                .footer { margin-top: 20px; padding-top: 20px; border-top: 1px solid #eee; color: #777; font-size: 12px; }
            </style>
        </head>
        <body>
            <div class="container">
                <div class="header">
                    <h2>Account Unlocked</h2>
                </div>
                <div class="content">
                    <p>Hello %s,</p>
                    <p>Your account has been <strong>unlocked by an administrator</strong>.</p>
                    <p>You can now login to your account using your credentials.</p>
                    <p><strong>Action:</strong> Account unlocked by administrator</p>
                    <p>If you did not request this unlock or have any concerns, please contact our support team immediately.</p>
                </div>
                <div class="footer">
                    <p>This is an automated notification from the system administrator.</p>
                </div>
            </div>
        </body>
        </html>
        """.formatted(username, LocalDateTime.now().toString());

        MimeMessage message = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

        try {
            helper.setFrom(new InternetAddress(fromEmail, "CNI Administrator"));
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        }
        helper.setTo(to);
        helper.setSubject(subject);
        helper.setText(htmlContent, true);

        mailSender.send(message);
    }

    // Send email when account is temporarily locked due to failed attempts
    public void sendAccountTemporarilyLockedEmail(String to, String username, long minutesRemaining) throws MessagingException {
        String subject = "Account Temporarily Locked";
        String htmlContent = """
        <!DOCTYPE html>
        <html>
        <head>
            <style>
                body { font-family: Arial, sans-serif; line-height: 1.6; color: #333; }
                .container { max-width: 600px; margin: 0 auto; padding: 20px; }
                .header { background-color: #ffc107; color: #333; padding: 20px; text-align: center; border-radius: 5px 5px 0 0; }
                .content { background-color: #f9f9f9; padding: 30px; border-radius: 0 0 5px 5px; }
                .footer { margin-top: 20px; padding-top: 20px; border-top: 1px solid #eee; color: #777; font-size: 12px; }
            </style>
        </head>
        <body>
            <div class="container">
                <div class="header">
                    <h2>Account Temporarily Locked</h2>
                </div>
                <div class="content">
                    <p>Hello %s,</p>
                    <p>Your account has been <strong>temporarily locked</strong> due to multiple failed login attempts.</p>
                    <p><strong>Lock duration:</strong> %d minute(s)</p>
                    <p><strong>Reason:</strong> Security - Too many failed login attempts</p>
                    <p>Your account will be automatically unlocked after the lock period expires.</p>
                    <p>For security reasons:</p>
                    <ul>
                        <li>Ensure you're using the correct password</li>
                        <li>Consider resetting your password if you've forgotten it</li>
                        <li>Contact support if you suspect unauthorized access attempts</li>
                    </ul>
                </div>
                <div class="footer">
                    <p>This is an automated security notification.</p>
                </div>
            </div>
        </body>
        </html>
        """.formatted(username, minutesRemaining);

        MimeMessage message = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

        try {
            helper.setFrom(new InternetAddress(fromEmail, "CNI Administrator"));
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        }
        helper.setTo(to);
        helper.setSubject(subject);
        helper.setText(htmlContent, true);

        mailSender.send(message);
    }
}