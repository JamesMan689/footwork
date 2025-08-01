package com.footwork.api.controller;

import com.footwork.api.entity.AuthRequest;
import com.footwork.api.entity.AuthResponse;
import com.footwork.api.entity.UserInfo;
import com.footwork.api.entity.ProfileSetupRequest;
import com.footwork.api.entity.ProfileSetupResponse;
import com.footwork.api.entity.UserProfileResponse;
import com.footwork.api.service.JwtService;
import com.footwork.api.service.TokenRevocationService;
import com.footwork.api.service.UserInfoService;

import lombok.RequiredArgsConstructor;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import org.springframework.web.bind.annotation.*;

import java.util.logging.Logger;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class UserController {

    private static final Logger logger = Logger.getLogger(UserController.class.getName());

    @Autowired
    private UserInfoService service;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private AuthenticationManager authenticationManager;

    @Autowired
    private UserDetailsService userDetailsService;

    @Autowired
    private TokenRevocationService tokenRevocationService;

    @GetMapping("/auth/welcome")
    public String welcome() {
        return "Welcome this endpoint is not secure";
    }

    @PostMapping("/auth/register")
    public String register(@RequestBody UserInfo userInfo) {
        return service.addUser(userInfo);
    }

    @PostMapping("/auth/login")
    public AuthResponse login(@RequestBody AuthRequest authRequest) {
        Authentication authentication = authenticationManager.authenticate(
            new UsernamePasswordAuthenticationToken(authRequest.getEmail(), authRequest.getPassword())
        );
        if (authentication.isAuthenticated()) {
            String accessToken = jwtService.generateToken(authRequest.getEmail());
            String refreshToken = jwtService.generateRefreshToken(authRequest.getEmail());
            return new AuthResponse(accessToken, refreshToken, "Login successful");
        } else {
            throw new UsernameNotFoundException("Invalid user request!");
        }
    }

    @PostMapping("/auth/refresh")
    public AuthResponse refresh(@RequestHeader("Authorization") String refreshToken) {
        logger.info("=== REFRESH ENDPOINT CALLED ===");
        logger.info("Refresh endpoint called with token: " + (refreshToken != null ? refreshToken.substring(0, Math.min(20, refreshToken.length())) + "..." : "null"));
        
        if (refreshToken != null && refreshToken.startsWith("Bearer ")) {
            String token = refreshToken.substring(7);
            
            // Check if token is revoked
            boolean isRevoked = tokenRevocationService.isTokenRevoked(token);
            logger.info("Token revoked check: " + isRevoked);
            
            if (isRevoked) {
                logger.warning("Token has been revoked!");
                throw new UsernameNotFoundException("Token has been revoked!");
            }
            
            String email = jwtService.extractUsername(token);
            logger.info("Extracted email: " + email);
            
            if (email != null) {
                UserDetails userDetails = userDetailsService.loadUserByUsername(email);
                if (jwtService.validateToken(token, userDetails)) {
                    // Revoke the old refresh token
                    tokenRevocationService.revokeToken(token);
                    logger.info("Old refresh token revoked");
                    
                    // Generate new tokens
                    String newAccessToken = jwtService.generateToken(email);
                    String newRefreshToken = jwtService.generateRefreshToken(email);
                    logger.info("Token refresh successful for: " + email);
                    return new AuthResponse(newAccessToken, newRefreshToken, "Token refreshed successfully");
                }
            }
        }
        throw new UsernameNotFoundException("Invalid refresh token!");
    }

    @PostMapping("/auth/logout")
    public ResponseEntity<String> logout(@RequestHeader("Authorization") String refreshToken) {
        logger.info("Logout endpoint called with token: " + (refreshToken != null ? refreshToken.substring(0, Math.min(20, refreshToken.length())) + "..." : "null"));
        
        if (refreshToken != null && refreshToken.startsWith("Bearer ")) {
            String token = refreshToken.substring(7);
            
            // Revoke the refresh token
            tokenRevocationService.revokeToken(token);
            logger.info("Token revoked successfully");
            
            return ResponseEntity.ok("Logout successful");
        }
        return ResponseEntity.badRequest().body("Invalid refresh token");
    }

    @PostMapping("/auth/debug/check-token")
    public ResponseEntity<String> checkTokenStatus(@RequestHeader("Authorization") String token) {
        if (token != null && token.startsWith("Bearer ")) {
            String actualToken = token.substring(7);
            boolean isRevoked = tokenRevocationService.isTokenRevoked(actualToken);
            return ResponseEntity.ok("Token revoked: " + isRevoked);
        }
        return ResponseEntity.badRequest().body("Invalid token");
    }
    
    @PostMapping("/user/profile")
    public ResponseEntity<ProfileSetupResponse> setupProfile(
            @RequestHeader("Authorization") String token,
            @RequestBody ProfileSetupRequest request) {
        try {
            if (token != null && token.startsWith("Bearer ")) {
                String actualToken = token.substring(7);
                String email = jwtService.extractUsername(actualToken);
                
                if (email != null) {
                    UserDetails userDetails = userDetailsService.loadUserByUsername(email);
                    if (jwtService.validateToken(actualToken, userDetails)) {
                        UserInfo updatedUser = service.setupProfile(email, request);
                        UserProfileResponse safeUser = service.toUserProfileResponse(updatedUser);
                        return ResponseEntity.ok(new ProfileSetupResponse(
                            "Profile setup completed successfully", 
                            true, 
                            safeUser
                        ));
                    }
                }
            }
            return ResponseEntity.badRequest().body(new ProfileSetupResponse(
                "Invalid or expired token", 
                false
            ));
        } catch (Exception e) {
            logger.warning("Profile setup error: " + e.getMessage());
            return ResponseEntity.badRequest().body(new ProfileSetupResponse(
                "Profile setup failed: " + e.getMessage(), 
                false
            ));
        }
    }
    
    @GetMapping("/user/me")
    public ResponseEntity<UserProfileResponse> getProfile(@RequestHeader("Authorization") String token) {
        try {
            if (token != null && token.startsWith("Bearer ")) {
                String actualToken = token.substring(7);
                String email = jwtService.extractUsername(actualToken);
                
                if (email != null) {
                    UserDetails userDetails = userDetailsService.loadUserByUsername(email);
                    if (jwtService.validateToken(actualToken, userDetails)) {
                        UserInfo user = service.getUserByEmail(email);
                        UserProfileResponse safeUser = service.toUserProfileResponse(user);
                        return ResponseEntity.ok(safeUser);
                    }
                }
            }
            return ResponseEntity.badRequest().build();
        } catch (Exception e) {
            logger.warning("Get profile error: " + e.getMessage());
            return ResponseEntity.badRequest().build();
        }
    }
}