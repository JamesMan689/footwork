package com.footwork.api.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.logging.Logger;

@Service
@RequiredArgsConstructor
@Slf4j
public class PasswordResetCleanupService {

    private static final Logger logger = Logger.getLogger(PasswordResetCleanupService.class.getName());
    
    private final PasswordResetService passwordResetService;

    /**
     * Clean up expired password reset codes every 5 minutes
     * This prevents the database from accumulating expired codes
     * More frequent cleanup for better database hygiene
     */
    @Scheduled(fixedRate = 5 * 60 * 1000) // Every 5 minutes
    public void cleanupExpiredCodes() {
        try {
            logger.info("Starting scheduled cleanup of expired password reset codes");
            passwordResetService.cleanupExpiredCodes();
            logger.info("Completed scheduled cleanup of expired password reset codes");
        } catch (Exception e) {
            logger.severe("Error during scheduled cleanup of expired password reset codes: " + e.getMessage());
        }
    }
    
    /**
     * Clean up old codes by age every 2 hours
     * Removes codes older than 24 hours for better database hygiene
     */
    @Scheduled(fixedRate = 2 * 60 * 60 * 1000) // Every 2 hours
    public void cleanupOldCodes() {
        try {
            logger.info("Starting scheduled cleanup of old password reset codes");
            passwordResetService.cleanupOldCodes();
            logger.info("Completed scheduled cleanup of old password reset codes");
        } catch (Exception e) {
            logger.severe("Error during scheduled cleanup of old password reset codes: " + e.getMessage());
        }
    }
}
