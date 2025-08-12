package com.footwork.api.service;

import com.footwork.api.entity.EmailVerification;
import com.footwork.api.entity.UserInfo;
import com.footwork.api.repository.EmailVerificationRepository;
import com.footwork.api.repository.UserInfoRepository;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.logging.Logger;
import java.time.LocalDate;

@Service
public class EmailVerificationService {

    private static final Logger logger = Logger.getLogger(EmailVerificationService.class.getName());
    
    private final EmailVerificationRepository emailVerificationRepository;
    private final UserInfoRepository userInfoRepository;
    private final EmailService emailService;
    
    private static final int CODE_LENGTH = 6;
    private static final int CODE_EXPIRY_MINUTES = 10;

    public EmailVerificationService(EmailVerificationRepository emailVerificationRepository,
                                   UserInfoRepository userInfoRepository,
                                   EmailService emailService) {
        this.emailVerificationRepository = emailVerificationRepository;
        this.userInfoRepository = userInfoRepository;
        this.emailService = emailService;
    }

    @Transactional
    public void sendVerificationEmail(String email) {
        logger.info("=== SEND VERIFICATION EMAIL STARTED ===");
        logger.info("Email: " + email);
        
        // Check if user exists
        Optional<UserInfo> userOptional = userInfoRepository.findByEmail(email);
        if (userOptional.isEmpty()) {
            logger.warning("User not found with email: " + email);
            throw new RuntimeException("User not found with email: " + email);
        }
        
        UserInfo user = userOptional.get();
        logger.info("User found: " + user.getName());
        
        // Check if email is already verified
        if (user.isEmailVerified()) {
            logger.warning("Email already verified for: " + email);
            throw new RuntimeException("Email is already verified");
        }
        
        // Delete any existing verification codes for this email first
        logger.info("Deleting old verification codes for email: " + email);
        emailVerificationRepository.deleteAllByEmail(email);
        
        // Generate and save verification code
        String code = generateVerificationCode();
        LocalDateTime expiresAt = LocalDateTime.now().plusMinutes(CODE_EXPIRY_MINUTES);
        
        logger.info("Generated code: " + code + " for email: " + email);
        logger.info("Expires at: " + expiresAt);
        
        EmailVerification verification = new EmailVerification();
        verification.setCode(code);
        verification.setEmail(email);
        verification.setExpiresAt(expiresAt);
        verification.setUsed(false);
        
        EmailVerification savedVerification = emailVerificationRepository.save(verification);
        logger.info("Saved verification record with ID: " + savedVerification.getId());
        
        // Send verification email
        try {
            logger.info("About to send email via EmailService...");
            emailService.sendVerificationEmail(email, code);
            logger.info("=== VERIFICATION EMAIL SENT SUCCESSFULLY ===");
        } catch (Exception e) {
            logger.severe("Failed to send verification email: " + e.getMessage());
            // Delete the code if email sending fails
            emailVerificationRepository.delete(verification);
            throw new RuntimeException("Failed to send verification email", e);
        }
    }

    @Transactional
    public boolean verifyEmail(String code, String email) {
        LocalDateTime now = LocalDateTime.now();
        
        // Find valid verification code
        Optional<EmailVerification> verificationOptional = emailVerificationRepository
            .findByCodeAndEmailAndUsedFalseAndExpiresAtAfter(code, email, now);
        
        if (verificationOptional.isEmpty()) {
            logger.warning("Invalid or expired verification code: " + code + " for email: " + email);
            return false;
        }
        
        EmailVerification verification = verificationOptional.get();
        
        // Mark code as used
        verification.setUsed(true);
        emailVerificationRepository.save(verification);
        
        // Mark all other codes for this email as used
        emailVerificationRepository.markAllTokensAsUsedForEmail(email);
        
        // Update user as verified
        Optional<UserInfo> userOptional = userInfoRepository.findByEmail(email);
        if (userOptional.isPresent()) {
            UserInfo user = userOptional.get();
            user.setEmailVerified(true);
            user.setEmailVerifiedAt(LocalDate.now());
            userInfoRepository.save(user);
            
            logger.info("Email verified successfully for: " + email);
            return true;
        }
        
        logger.warning("User not found for verified email: " + email);
        return false;
    }

    @Transactional
    public void resendVerificationEmail(String email) {
        // Delete any existing unused tokens for this email
        emailVerificationRepository.markAllTokensAsUsedForEmail(email);
        
        // Send new verification email
        sendVerificationEmail(email);
    }

    private String generateVerificationCode() {
        SecureRandom random = new SecureRandom();
        StringBuilder code = new StringBuilder(CODE_LENGTH);
        
        for (int i = 0; i < CODE_LENGTH; i++) {
            code.append(random.nextInt(10)); // Only digits 0-9
        }
        
        return code.toString();
    }

    @Scheduled(cron = "0 */5 * * * ?") // Run every 5 minutes
    @Transactional
    public void cleanupExpiredCodes() {
        LocalDateTime now = LocalDateTime.now();
        int deletedCount = emailVerificationRepository.deleteExpiredCodes(now);
        if (deletedCount > 0) {
            logger.info("Cleaned up " + deletedCount + " expired email verification codes");
        }
    }
}
