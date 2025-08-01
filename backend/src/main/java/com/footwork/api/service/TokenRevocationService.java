package com.footwork.api.service;

import org.springframework.stereotype.Service;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@Service
public class TokenRevocationService {
    
    private final ConcurrentMap<String, Long> revokedTokens = new ConcurrentHashMap<>();
    
    public void revokeToken(String token) {
        revokedTokens.put(token, System.currentTimeMillis());
    }
    
    public boolean isTokenRevoked(String token) {
        return revokedTokens.containsKey(token);
    }
    
    // Clean up old revoked tokens (optional - for memory management)
    public void cleanupOldRevokedTokens() {
        long currentTime = System.currentTimeMillis();
        long maxAge = 7 * 24 * 60 * 60 * 1000L; // 7 days in milliseconds
        
        revokedTokens.entrySet().removeIf(entry -> 
            (currentTime - entry.getValue()) > maxAge
        );
    }
} 