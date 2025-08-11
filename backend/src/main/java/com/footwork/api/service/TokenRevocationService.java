package com.footwork.api.service;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.logging.Logger;

@Service
public class TokenRevocationService {
    
    private static final Logger logger = Logger.getLogger(TokenRevocationService.class.getName());
    private final ConcurrentMap<String, Long> revokedTokens = new ConcurrentHashMap<>();
    
    public void revokeToken(String token) {
        if (token == null || token.trim().isEmpty()) {
            logger.warning("Attempted to revoke null or empty token");
            return;
        }
        
        Long previousRevocation = revokedTokens.put(token, System.currentTimeMillis());
        if (previousRevocation != null) {
            logger.info("Token was already revoked at: " + previousRevocation);
        } else {
            logger.info("Token revoked successfully at: " + System.currentTimeMillis());
        }
    }
    
    public boolean isTokenRevoked(String token) {
        if (token == null || token.trim().isEmpty()) {
            return true; // Consider null/empty tokens as revoked
        }
        return revokedTokens.containsKey(token);
    }
    
    public Long getTokenRevocationTime(String token) {
        return revokedTokens.get(token);
    }
    
    public int getRevokedTokenCount() {
        return revokedTokens.size();
    }
    
    // Revoke all tokens for a specific user (when email changes)
    public void revokeAllTokensForUser(String email) {
        // Note: In a production system, you might want to track user-token mappings
        // For now, we'll log this action. In a real implementation, you'd want to:
        // 1. Store user-token mappings in the database
        // 2. Revoke all active tokens for that user
        // 3. Force logout on all devices
        
        logger.info("Email changed for user: " + email + ". All existing tokens should be invalidated.");
        logger.info("Note: Implement user-token mapping in production for proper token revocation.");
    }
    
    // Clean up old revoked tokens (for memory management)
    public void cleanupOldRevokedTokens() {
        long currentTime = System.currentTimeMillis();
        long maxAge = 7 * 24 * 60 * 60 * 1000L; // 7 days in milliseconds
        
        int beforeSize = revokedTokens.size();
        revokedTokens.entrySet().removeIf(entry -> 
            (currentTime - entry.getValue()) > maxAge
        );
        int afterSize = revokedTokens.size();
        
        if (beforeSize != afterSize) {
            logger.info("Cleaned up " + (beforeSize - afterSize) + " old revoked tokens");
        }
    }
    
    // Automatically clean up old revoked tokens every hour
    @Scheduled(fixedRate = 60 * 60 * 1000) // 1 hour in milliseconds
    public void scheduledCleanup() {
        logger.info("Running scheduled cleanup of revoked tokens. Current count: " + revokedTokens.size());
        cleanupOldRevokedTokens();
        logger.info("Scheduled cleanup completed. Current count: " + revokedTokens.size());
    }
} 