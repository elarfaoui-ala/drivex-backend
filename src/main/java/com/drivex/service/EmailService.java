package com.drivex.service;

import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailService {

    private final JavaMailSender mailSender;

    @Value("${app.mail.from}")
    private String fromAddress;

    @Async
    public void sendPasswordResetEmail(String to, String resetLink) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(fromAddress);
            helper.setTo(to);
            helper.setSubject("DriveX — Password Reset");

            String html = """
                <!DOCTYPE html>
                <html>
                <head><meta charset="UTF-8"></head>
                <body style="font-family: Arial, sans-serif; padding: 24px;">
                    <h2>Password Reset</h2>
                    <p>Click the button below to reset your DriveX password. This link expires in 1 hour.</p>
                    <a href="%s"
                       style="display: inline-block; padding: 12px 24px; margin: 16px 0;
                              background-color: #2563eb; color: white; text-decoration: none;
                              border-radius: 6px; font-size: 16px;">
                        Reset Password
                    </a>
                    <p style="color: #6b7280; font-size: 14px;">
                        If you didn't request this, you can safely ignore this email.
                    </p>
                    <hr style="margin-top: 24px;">
                    <p style="color: #9ca3af; font-size: 12px;">DriveX Delivery Team</p>
                </body>
                </html>
                """.formatted(resetLink);

            helper.setText(html, true);
            mailSender.send(message);
            log.info("Password reset email sent to {}", to);
        } catch (Exception e) {
            log.error("Failed to send password reset email to {}. Check SMTP_HOST/SMTP_USERNAME/SMTP_PASSWORD env vars. Error: {}", to, e.getMessage());
        }
    }
}
