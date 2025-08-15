package com.footwork.api.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.ses.SesClient;
import software.amazon.awssdk.services.ses.model.*;

import java.nio.charset.StandardCharsets;
import java.util.logging.Logger;

@Service
@Slf4j
public class EmailService {

    private static final Logger logger = Logger.getLogger(EmailService.class.getName());
    
    private final SesClient sesClient;
    
    @Value("${aws.ses.from-email:}")
    private String fromEmail;
    
    @Value("${aws.ses.region:us-east-1}")
    private String region;
    
    @Value("${app.base-url:http://localhost:8080}")
    private String baseUrl;

    public EmailService(SesClient sesClient) {
        this.sesClient = sesClient;
    }

    public void sendVerificationEmail(String toEmail, String code) {
        try {
            String subject = "Your Verification Code - Footwork";
            String htmlBody = createVerificationEmailHtml(toEmail, code);
            String textBody = createVerificationEmailText(toEmail, code);
            
            SendEmailRequest request = SendEmailRequest.builder()
                .source(fromEmail)
                .destination(Destination.builder().toAddresses(toEmail).build())
                .message(Message.builder()
                    .subject(Content.builder().data(subject).charset(StandardCharsets.UTF_8.name()).build())
                    .body(Body.builder()
                        .html(Content.builder().data(htmlBody).charset(StandardCharsets.UTF_8.name()).build())
                        .text(Content.builder().data(textBody).charset(StandardCharsets.UTF_8.name()).build())
                        .build())
                    .build())
                .build();
            
            SendEmailResponse response = sesClient.sendEmail(request);
            logger.info("Verification email sent successfully to " + toEmail + " with message ID: " + response.messageId());
            
        } catch (Exception e) {
            logger.severe("Failed to send verification email to " + toEmail + ": " + e.getMessage());
            throw new RuntimeException("Failed to send verification email", e);
        }
    }
    
    public void sendPasswordResetEmail(String toEmail, String resetCode) {
        try {
            String subject = "Password Reset Code - Footwork";
            String htmlBody = createPasswordResetEmailHtml(resetCode);
            String textBody = createPasswordResetEmailText(resetCode);
            
            SendEmailRequest request = SendEmailRequest.builder()
                .source(fromEmail)
                .destination(Destination.builder().toAddresses(toEmail).build())
                .message(Message.builder()
                    .subject(Content.builder().data(subject).charset(StandardCharsets.UTF_8.name()).build())
                    .body(Body.builder()
                        .html(Content.builder().data(htmlBody).charset(StandardCharsets.UTF_8.name()).build())
                        .text(Content.builder().data(textBody).charset(StandardCharsets.UTF_8.name()).build())
                        .build())
                    .build())
                .build();
            
            SendEmailResponse response = sesClient.sendEmail(request);
            logger.info("Password reset email sent successfully to " + toEmail + " with message ID: " + response.messageId());
            
        } catch (Exception e) {
            logger.severe("Failed to send password reset email to " + toEmail + ": " + e.getMessage());
            throw new RuntimeException("Failed to send password reset email", e);
        }
    }
    
    private String createVerificationEmailHtml(String email, String code) {
        return """
            <!DOCTYPE html>
            <html>
            <head>
                <meta charset="UTF-8">
                <meta name="viewport" content="width=device-width, initial-scale=1.0">
                <title>Your Verification Code - Footwork</title>
                <style>
                    body { font-family: Arial, sans-serif; line-height: 1.6; color: #333; }
                    .container { max-width: 600px; margin: 0 auto; padding: 20px; }
                    .header { background-color: #4CAF50; color: white; padding: 20px; text-align: center; }
                    .content { padding: 20px; background-color: #f9f9f9; }
                    .verification-code { display: inline-block; background-color: #4CAF50; color: white; padding: 20px 40px; font-size: 32px; font-weight: bold; border-radius: 10px; margin: 20px 0; letter-spacing: 5px; }
                    .footer { text-align: center; padding: 20px; color: #666; font-size: 12px; }
                </style>
            </head>
            <body>
                <div class="container">
                    <div class="header">
                        <h1>Welcome to Footwork!</h1>
                    </div>
                    <div class="content">
                        <h2>Your Verification Code</h2>
                        <p>Hi there!</p>
                        <p>Thank you for signing up for Footwork. To complete your registration, please use this verification code:</p>
                        <p style="text-align: center;">
                            <div class="verification-code">%s</div>
                        </p>
                                            <p>Enter this code in the verification form on our website.</p>
                    <p><strong>This verification code will expire in 10 minutes.</strong></p>
                    <p>If you didn't create an account with Footwork, you can safely ignore this email.</p>
                    </div>
                    <div class="footer">
                        <p>Thanks for using Footwork!</p>
                    </div>
                </div>
            </body>
            </html>
            """.formatted(code);
    }
    
    private String createVerificationEmailText(String email, String code) {
        return """
            Welcome to Footwork!
            
            Your Verification Code
            
            Hi there!
            
            Thank you for signing up for Footwork. To complete your registration, please use this verification code:
            
            %s
            
            Enter this code in the verification form on our website.
            This verification code will expire in 10 minutes.
            
            If you didn't create an account with Footwork, you can safely ignore this email.
            
            Best regards,
            The Footwork Team
            """.formatted(code);
    }
    
    private String createPasswordResetEmailHtml(String resetCode) {
        try {
            // Read the HTML template from the static file
            java.nio.file.Path htmlPath = java.nio.file.Paths.get("src/main/resources/static/password-reset-email.html");
            String htmlTemplate = java.nio.file.Files.readString(htmlPath);
            
            // Replace the placeholder code with the actual reset code
            return htmlTemplate.replace("ABC123", resetCode);
        } catch (Exception e) {
            logger.warning("Failed to read HTML template, using fallback: " + e.getMessage());
            // Fallback to inline HTML if file reading fails
            return createFallbackPasswordResetHtml(resetCode);
        }
    }
    
    private String createFallbackPasswordResetHtml(String resetCode) {
        return """
            <!DOCTYPE html>
            <html>
            <head>
                <meta charset="UTF-8">
                <meta name="viewport" content="width=device-width, initial-scale=1.0">
                <title>Password Reset Code - Footwork</title>
                <style>
                    body { font-family: Arial, sans-serif; line-height: 1.6; color: #333; }
                    .container { max-width: 600px; margin: 0 auto; padding: 20px; }
                    .header { background-color: #4CAF50; color: white; padding: 20px; text-align: center; }
                    .content { padding: 20px; background-color: #f9f9f9; }
                    .reset-code { display: inline-block; background-color: #4CAF50; color: white; padding: 20px 40px; font-size: 32px; font-weight: bold; border-radius: 10px; margin: 20px 0; letter-spacing: 5px; }
                    .footer { text-align: center; padding: 20px; color: #666; font-size: 12px; }
                    .warning { background-color: #fff3cd; border: 1px solid #ffeaa7; padding: 15px; border-radius: 5px; margin: 20px 0; }
                </style>
            </head>
            <body>
                <div class="container">
                    <div class="header">
                        <h1>Password Reset Request</h1>
                    </div>
                    <div class="content">
                        <h2>Your Password Reset Code</h2>
                        <p>You requested a password reset for your Footwork account. Use the following code to reset your password:</p>
                        <p style="text-align: center;">
                            <div class="reset-code">%s</div>
                        </p>
                        <p>Enter this code in the password reset form on our website.</p>
                        <div class="warning">
                            <p><strong>Important:</strong></p>
                            <ul>
                                <li>This code will expire in 10 minutes</li>
                                <li>If you didn't request a password reset, please ignore this email</li>
                                <li>Your password will remain unchanged until you use this code</li>
                            </ul>
                        </div>
                    </div>
                    <div class="footer">
                        <p>Thanks for using Footwork!</p>
                    </div>
                </div>
            </body>
            </html>
            """.formatted(resetCode);
    }
    
    private String createPasswordResetEmailText(String resetCode) {
        return """
            Password Reset Request - Footwork
            
            Your Password Reset Code: %s
            
            Enter this code in the password reset form on our website.
            
            Important:
            - This code will expire in 30 minutes
            - If you didn't request a password reset, please ignore this email
            - Your password will remain unchanged until you use this code
            
            Thanks for using Footwork!
            """.formatted(resetCode);
    }
}
