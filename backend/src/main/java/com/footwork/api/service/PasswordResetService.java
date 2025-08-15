package com.footwork.api.service;

import com.footwork.api.entity.PasswordResetCode;
import com.footwork.api.entity.UserInfo;
import com.footwork.api.repository.PasswordResetCodeRepository;
import com.footwork.api.repository.UserInfoRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import java.util.logging.Logger;

@Service
@RequiredArgsConstructor
@Slf4j
public class PasswordResetService {

    private static final Logger logger = Logger.getLogger(PasswordResetService.class.getName());
    
    private final PasswordResetCodeRepository passwordResetCodeRepository;
    private final UserInfoRepository userInfoRepository;
    private final EmailService emailService;
    private final TokenRevocationService tokenRevocationService;
    private final BCryptPasswordEncoder passwordEncoder;
    
    @Value("${app.reset.ttlMinutes:30}")
    private int ttlMinutes;
    
    @Value("${app.reset.rateLimitPerMinute:1}")
    private int rateLimitPerMinute;
    
    @Value("${app.reset.maxAttempts:5}")
    private int maxAttempts;

    /**
     * Request a password reset code
     * Always returns success to prevent email enumeration
     */
    @Transactional
    public void requestPasswordReset(String email, String ipAddress) {
        // Check rate limiting
        if (isRateLimited(email, ipAddress)) {
            logger.warning("Rate limit exceeded for password reset request from IP: " + ipAddress);
            throw new RuntimeException("Too many password reset requests. Please try again later.");
        }
        
        // Find user by email
        Optional<UserInfo> userOptional = userInfoRepository.findByEmail(email);
        if (userOptional.isEmpty()) {
            // Don't reveal if email exists, but log for security
            logger.info("Password reset requested for non-existent email: " + email);
            return;
        }
        
        UserInfo user = userOptional.get();
        // Delete any existing verification codes for this email first (like email verification service)
        logger.info("Deleting old password reset codes for email: " + email);
        passwordResetCodeRepository.deleteAllByEmail(email);
        
        // Generate new reset code
        String resetCode = generateResetCode();
        String codeHash = passwordEncoder.encode(resetCode);
        
        // Create new reset code entity
        PasswordResetCode passwordResetCode = new PasswordResetCode();
        passwordResetCode.setUserId(user.getId());
        passwordResetCode.setEmail(email);
        passwordResetCode.setCodeHash(codeHash);
        passwordResetCode.setExpiresAt(LocalDateTime.now().plusMinutes(ttlMinutes));
        passwordResetCode.setIpAddress(ipAddress);
        
        // Save the reset code
        passwordResetCodeRepository.save(passwordResetCode);
        
        // Send email with the reset code
        try {
            emailService.sendPasswordResetEmail(email, resetCode);
            logger.info("Password reset code sent successfully to: " + email);
        } catch (Exception e) {
            logger.severe("Failed to send password reset email to: " + email + " - " + e.getMessage());
            // Delete the code if email fails
            passwordResetCodeRepository.delete(passwordResetCode);
            throw new RuntimeException("Failed to send password reset email", e);
        }
    }
    
    /**
     * Reset password using the provided code
     */
    @Transactional
    public void resetPassword(String email, String code, String newPassword) {
        // Find user by email
        Optional<UserInfo> userOptional = userInfoRepository.findByEmail(email);
        if (userOptional.isEmpty()) {
            throw new RuntimeException("Invalid email or reset code");
        }
        
        UserInfo user = userOptional.get();
        
        // Find active reset codes for this email
        List<PasswordResetCode> activeCodes = passwordResetCodeRepository.findActiveCodesByEmail(
            email, LocalDateTime.now()
        );
        
        if (activeCodes.isEmpty()) {
            throw new RuntimeException("Invalid or expired reset code");
        }
        
        // Find the code that matches
        PasswordResetCode resetCode = null;
        for (PasswordResetCode activeCode : activeCodes) {
            if (passwordEncoder.matches(code, activeCode.getCodeHash())) {
                resetCode = activeCode;
                break;
            }
        }
        
        if (resetCode == null) {
            throw new RuntimeException("Invalid reset code");
        }
        
        // Check if code is consumed
        if (resetCode.getConsumed()) {
            throw new RuntimeException("Reset code has already been used");
        }
        
        // Check attempts
        if (resetCode.getAttempts() >= maxAttempts) {
            throw new RuntimeException("Too many failed attempts. Please request a new reset code.");
        }
        
        // Increment attempts
        passwordResetCodeRepository.incrementAttempts(resetCode.getId());
        
        // Update user password
        String hashedNewPassword = passwordEncoder.encode(newPassword);
        user.setPassword(hashedNewPassword);
        userInfoRepository.save(user);
        
        // Mark code as consumed
        passwordResetCodeRepository.markAsConsumed(resetCode.getId());
        
        // Revoke all existing sessions for this user
        tokenRevocationService.revokeAllTokensForUser(user.getEmail());
        
        logger.info("Password reset successful for user: " + email);
    }
    
    /**
     * Check if request is rate limited
     */
    private boolean isRateLimited(String email, String ipAddress) {
        LocalDateTime oneMinuteAgo = LocalDateTime.now().minusMinutes(1);
        
        long emailCount = passwordResetCodeRepository.countRecentCodesByEmail(email, oneMinuteAgo);
        long ipCount = passwordResetCodeRepository.countRecentCodesByIpAddress(ipAddress, oneMinuteAgo);
        
        return emailCount >= rateLimitPerMinute || ipCount >= rateLimitPerMinute;
    }
    

    
    /**
     * Generate a random 6-digit alphanumeric reset code
     */
    private String generateResetCode() {
        String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
        Random random = new Random();
        StringBuilder code = new StringBuilder();
        
        for (int i = 0; i < 6; i++) {
            code.append(chars.charAt(random.nextInt(chars.length())));
        }
        
        return code.toString();
    }
    
    /**
     * Clean up expired codes (can be called by a scheduled task)
     */
    @Transactional
    public void cleanupExpiredCodes() {
        passwordResetCodeRepository.deleteExpiredCodes(LocalDateTime.now());
        logger.info("Expired password reset codes cleaned up");
    }
    
    /**
     * Clean up old codes by age (for better database hygiene)
     * Removes codes older than 24 hours regardless of expiration
     */
    @Transactional
    public void cleanupOldCodes() {
        LocalDateTime twentyFourHoursAgo = LocalDateTime.now().minusHours(24);
        passwordResetCodeRepository.deleteOldCodes(twentyFourHoursAgo);
        logger.info("Old password reset codes (24+ hours) cleaned up");
    }
}
