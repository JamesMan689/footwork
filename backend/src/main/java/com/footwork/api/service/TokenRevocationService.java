package com.footwork.api.service;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.logging.Logger;
import java.util.Set;
import java.util.HashSet;
import java.util.Map;

@Service
public class TokenRevocationService {
    
    private static final Logger logger = Logger.getLogger(TokenRevocationService.class.getName());
    private final ConcurrentMap<String, Long> revokedTokens = new ConcurrentHashMap<>();
    
    // Track which user each token belongs to for better revocation management
    private final ConcurrentMap<String, String> tokenUserMapping = new ConcurrentHashMap<>();
    
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
    
    public void revokeTokenForUser(String token, String userEmail) {
        if (token == null || token.trim().isEmpty()) {
            logger.warning("Attempted to revoke null or empty token");
            return;
        }
        
        // Store the user-token mapping
        tokenUserMapping.put(token, userEmail);
        
        // Revoke the token
        Long previousRevocation = revokedTokens.put(token, System.currentTimeMillis());
        if (previousRevocation != null) {
            logger.info("Token was already revoked at: " + previousRevocation);
        } else {
            logger.info("Token revoked successfully for user: " + userEmail + " at: " + System.currentTimeMillis());
        }
    }
    
    /**
     * Track a token for a user without revoking it
     * This is used when generating new tokens to maintain the user-token mapping
     */
    public void trackTokenForUser(String token, String userEmail) {
        if (token == null || token.trim().isEmpty()) {
            logger.warning("Attempted to track null or empty token");
            return;
        }
        
        // Store the user-token mapping without revoking
        tokenUserMapping.put(token, userEmail);
        logger.info("Token tracked for user: " + userEmail);
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
    
    /**
     * Revoke all tokens for a specific user
     * This is useful for force logout or when user credentials are compromised
     */
    public void revokeAllTokensForUser(String userEmail) {
        if (userEmail == null || userEmail.trim().isEmpty()) {
            logger.warning("Attempted to revoke tokens for null or empty email");
            return;
        }
        
        int revokedCount = 0;
        
        // Find all tokens belonging to this user and revoke them
        for (Map.Entry<String, String> entry : tokenUserMapping.entrySet()) {
            if (userEmail.equals(entry.getValue())) {
                String token = entry.getKey();
                if (!revokedTokens.containsKey(token)) {
                    revokedTokens.put(token, System.currentTimeMillis());
                    revokedCount++;
                }
            }
        }
        
        logger.info("Revoked " + revokedCount + " tokens for user: " + userEmail);
    }

    /**
     * Revoke all tokens for a user during logout
     * This ensures that when a user logs out, ALL their tokens are invalidated
     */
    public void revokeAllUserTokensOnLogout(String userEmail) {
        if (userEmail == null || userEmail.trim().isEmpty()) {
            logger.warning("Attempted to revoke tokens for null or empty email during logout");
            return;
        }
        
        int revokedCount = 0;
        
        // Find all tokens belonging to this user and revoke them
        for (Map.Entry<String, String> entry : tokenUserMapping.entrySet()) {
            if (userEmail.equals(entry.getValue())) {
                String token = entry.getKey();
                if (!revokedTokens.containsKey(token)) {
                    revokedTokens.put(token, System.currentTimeMillis());
                    revokedCount++;
                    logger.info("Revoked token during logout for user: " + userEmail);
                }
            }
        }
        
        logger.info("Logout completed - revoked " + revokedCount + " tokens for user: " + userEmail);
    }
    
    /**
     * Get all revoked tokens for a specific user
     */
    public Set<String> getRevokedTokensForUser(String userEmail) {
        Set<String> userRevokedTokens = new HashSet<>();
        
        for (Map.Entry<String, String> entry : tokenUserMapping.entrySet()) {
            if (userEmail.equals(entry.getValue()) && revokedTokens.containsKey(entry.getKey())) {
                userRevokedTokens.add(entry.getKey());
            }
        }
        
        return userRevokedTokens;
    }

    /**
     * Get the count of active (non-revoked) tokens for a specific user
     */
    public int getActiveTokenCountForUser(String userEmail) {
        if (userEmail == null || userEmail.trim().isEmpty()) {
            return 0;
        }
        
        int activeCount = 0;
        
        for (Map.Entry<String, String> entry : tokenUserMapping.entrySet()) {
            if (userEmail.equals(entry.getValue()) && !revokedTokens.containsKey(entry.getKey())) {
                activeCount++;
            }
        }
        
        return activeCount;
    }

    /**
     * Get the count of revoked tokens for a specific user
     */
    public int getRevokedTokenCountForUser(String userEmail) {
        if (userEmail == null || userEmail.trim().isEmpty()) {
            return 0;
        }
        
        int revokedCount = 0;
        
        for (Map.Entry<String, String> entry : tokenUserMapping.entrySet()) {
            if (userEmail.equals(entry.getValue()) && revokedTokens.containsKey(entry.getKey())) {
                revokedCount++;
            }
        }
        
        return revokedCount;
    }
    
    // Clean up old revoked tokens (for memory management)
    public void cleanupOldRevokedTokens() {
        long currentTime = System.currentTimeMillis();
        long maxAge = 7 * 24 * 60 * 60 * 1000L; // 7 days in milliseconds
        
        int beforeSize = revokedTokens.size();
        int beforeMappingSize = tokenUserMapping.size();
        
        // Remove old revoked tokens
        revokedTokens.entrySet().removeIf(entry -> 
            (currentTime - entry.getValue()) > maxAge
        );
        
        // Clean up corresponding user mappings
        tokenUserMapping.entrySet().removeIf(entry -> 
            !revokedTokens.containsKey(entry.getKey())
        );
        
        int afterSize = revokedTokens.size();
        int afterMappingSize = tokenUserMapping.size();
        
        if (beforeSize != afterSize || beforeMappingSize != afterMappingSize) {
            logger.info("Cleaned up " + (beforeSize - afterSize) + " old revoked tokens and " + 
                       (beforeMappingSize - afterMappingSize) + " user mappings");
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