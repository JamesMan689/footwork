package com.footwork.api.controller;

import com.footwork.api.entity.AuthRequest;
import com.footwork.api.entity.AuthResponse;
import com.footwork.api.entity.UserInfo;
import com.footwork.api.entity.RegistrationRequest;
import com.footwork.api.entity.ProfileSetupRequest;
import com.footwork.api.entity.ProfileSetupResponse;
import com.footwork.api.entity.UserProfileResponse;
import com.footwork.api.entity.UserUpdateRequest;
import com.footwork.api.entity.PasswordUpdateRequest;
import com.footwork.api.entity.DeleteUserRequest;
import com.footwork.api.entity.StreakResponse;
import com.footwork.api.service.JwtService;
import com.footwork.api.service.TokenRevocationService;
import com.footwork.api.service.UserInfoService;
import com.footwork.api.service.S3StorageService;
import com.footwork.api.service.EmailVerificationService;

import lombok.RequiredArgsConstructor;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import jakarta.validation.Valid;

import java.util.logging.Logger;
import java.util.Date;
import java.util.Map;
import jakarta.servlet.http.HttpServletRequest;
import java.util.HashMap;

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

    @Autowired
    private S3StorageService s3StorageService;

    @Autowired
    private EmailVerificationService emailVerificationService;

    @GetMapping("/auth/welcome")
    public String welcome() {
        return "Welcome this endpoint is not secure";
    }

    @PutMapping("/user/profile-picture")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<UserProfileResponse> uploadProfilePicture(@RequestParam("file") MultipartFile file) {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            String email = authentication.getName();

            if (file == null || file.isEmpty()) {
                logger.warning("Profile picture upload: missing file");
                return ResponseEntity.badRequest().build();
            }

            // Enforce basic size/type limits
            long maxBytes = 2L * 1024L * 1024L; // 2 MB
            if (file.getSize() > maxBytes) {
                logger.warning("Profile picture upload: file too large (" + file.getSize() + " bytes)");
                return ResponseEntity.status(413).build();
            }
            String contentType = file.getContentType() != null ? file.getContentType() : "image/jpeg";
            // Accept common variants (some clients send image/jpg)
            boolean allowed = contentType.equalsIgnoreCase("image/jpeg") ||
                              contentType.equalsIgnoreCase("image/jpg") ||
                              contentType.equalsIgnoreCase("image/png") ||
                              contentType.equalsIgnoreCase("image/webp");
            if (!allowed) {
                logger.warning("Profile picture upload: unsupported content type " + contentType);
                return ResponseEntity.badRequest().build();
            }

            // If there is an existing object key, delete it (reupload flow)
            UserInfo existing = service.getUserByEmail(email);
            if (existing.getProfileImageUrl() != null && !existing.getProfileImageUrl().isEmpty()) {
                s3StorageService.deleteObject(existing.getProfileImageUrl());
            }

            String objectKey = s3StorageService.uploadProfileImage(file.getBytes(), contentType, email);

            UserInfo updated = service.updateProfileImage(email, objectKey);
            // Return a short-lived presigned URL for display
            String presigned = s3StorageService.generatePresignedGetUrl(objectKey, java.time.Duration.ofMinutes(15));
            UserProfileResponse resp = service.toUserProfileResponse(updated);
            resp.setProfileImageUrl(presigned);
            return ResponseEntity.ok(resp);
        } catch (Exception e) {
            logger.warning("Profile picture upload failed: " + e.getMessage());
            return ResponseEntity.badRequest().build();
        }
    }

    @PostMapping("/auth/register")
    public ResponseEntity<String> register(@Valid @RequestBody RegistrationRequest request) {
        try {
            // Create UserInfo from the validated request
            UserInfo userInfo = new UserInfo();
            userInfo.setName(request.getName());
            userInfo.setEmail(request.getEmail());
            userInfo.setPassword(request.getPassword());
            
            // Set role with validation - default to ROLE_USER, only allow ROLE_ADMIN if explicitly specified
            String requestedRole = request.getRoles();
            if (requestedRole != null && requestedRole.equals("ROLE_ADMIN")) {
                // Only allow ROLE_ADMIN if explicitly requested (you might want to add admin-only logic here)
                userInfo.setRoles("ROLE_ADMIN");
            } else {
                // Default to ROLE_USER for all registrations
                userInfo.setRoles("ROLE_USER");
            }
            
            // Set default values
            userInfo.setProfileCompleted(false);
            userInfo.setEmailVerified(false);
            userInfo.setStreak(0);
            
            String result = service.addUser(userInfo);
            return ResponseEntity.ok(result);
            
        } catch (Exception e) {
            logger.severe("Registration failed: " + e.getMessage());
            return ResponseEntity.badRequest().body("Registration failed: " + e.getMessage());
        }
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
        
        if (refreshToken == null || !refreshToken.startsWith("Bearer ")) {
            logger.warning("Refresh called without valid Authorization header");
            throw new UsernameNotFoundException("Authorization header required");
        }
        
        String token = refreshToken.substring(7);
        
        try {
            // First check if this is actually a refresh token
            if (!jwtService.isRefreshToken(token)) {
                logger.warning("Refresh attempted with non-refresh token");
                throw new UsernameNotFoundException("Only refresh tokens are accepted for refresh");
            }
            
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
        } catch (UsernameNotFoundException e) {
            throw e; // Re-throw UsernameNotFoundException
        } catch (Exception e) {
            logger.warning("Error during refresh: " + e.getMessage());
            throw new UsernameNotFoundException("Invalid refresh token");
        }
        
        throw new UsernameNotFoundException("Invalid refresh token!");
    }

    @PostMapping("/auth/logout")
    public ResponseEntity<String> logout(@RequestHeader("Authorization") String authHeader) {
        logger.info("Logout endpoint called with token: " + (authHeader != null ? authHeader.substring(0, Math.min(20, authHeader.length())) + "..." : "null"));
        
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            logger.warning("Logout called without valid Authorization header");
            return ResponseEntity.badRequest().body("Authorization header required");
        }
        
        String token = authHeader.substring(7);
        
        try {
            // Extract email from token (works for both access and refresh tokens)
            String email = jwtService.extractUsername(token);
            if (email == null) {
                logger.warning("Invalid token: could not extract email");
                return ResponseEntity.badRequest().body("Invalid token");
            }
            
            // Check if token is expired
            Date expiration = jwtService.extractExpiration(token);
            if (expiration.before(new Date())) {
                logger.warning("Token expired for user: " + email);
                return ResponseEntity.badRequest().body("Token expired");
            }
            
            // Load user details to validate the token
            UserDetails userDetails = userDetailsService.loadUserByUsername(email);
            if (!jwtService.validateToken(token, userDetails)) {
                logger.warning("Invalid token: validation failed for user: " + email);
                return ResponseEntity.badRequest().body("Invalid token");
            }
            
            // Check if token is already revoked
            if (tokenRevocationService.isTokenRevoked(token)) {
                logger.info("Token already revoked for user: " + email);
                return ResponseEntity.ok("Logout successful");
            }
            
            // Revoke ALL tokens for this user to ensure complete logout
            tokenRevocationService.revokeAllUserTokensOnLogout(email);
            logger.info("All tokens revoked successfully for user: " + email);
            
            return ResponseEntity.ok("Logout successful");
            
        } catch (Exception e) {
            logger.warning("Error during logout: " + e.getMessage());
            return ResponseEntity.badRequest().body("Invalid token");
        }
    }

    /**
     * Logout endpoint specifically for access tokens
     * This allows users to logout using their current access token
     */
    @PostMapping("/auth/logout-access")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<String> logoutAccessToken(HttpServletRequest request) {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            String email = authentication.getName();
            
            // Revoke ALL tokens for this user to ensure complete logout
            tokenRevocationService.revokeAllUserTokensOnLogout(email);
            logger.info("All tokens revoked successfully for user: " + email);
            
            return ResponseEntity.ok("Logout successful - Access token revoked");
            
        } catch (Exception e) {
            logger.warning("Error during access token logout: " + e.getMessage());
            return ResponseEntity.badRequest().body("Logout failed: " + e.getMessage());
        }
    }

    /**
     * Force logout for a specific user (admin only)
     * This revokes all active tokens for the specified user
     */
    @PostMapping("/admin/force-logout")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<String> forceLogoutUser(@RequestParam String userEmail) {
        try {
            // Revoke all tokens for the specified user
            tokenRevocationService.revokeAllTokensForUser(userEmail);
            logger.info("Force logout completed for user: " + userEmail);
            
            return ResponseEntity.ok("Force logout successful for user: " + userEmail);
            
        } catch (Exception e) {
            logger.warning("Error during force logout: " + e.getMessage());
            return ResponseEntity.badRequest().body("Force logout failed: " + e.getMessage());
        }
    }

    /**
     * Debug endpoint to show token status for the current user
     * This helps verify that tokens are properly tracked and revoked
     */
    @GetMapping("/user/token-status")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<Map<String, Object>> getTokenStatus() {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            String email = authentication.getName();
            
            Map<String, Object> status = new HashMap<>();
            status.put("userEmail", email);
            status.put("activeTokens", tokenRevocationService.getActiveTokenCountForUser(email));
            status.put("revokedTokens", tokenRevocationService.getRevokedTokenCountForUser(email));
            status.put("totalTrackedTokens", tokenRevocationService.getActiveTokenCountForUser(email) + 
                                      tokenRevocationService.getRevokedTokenCountForUser(email));
            
            return ResponseEntity.ok(status);
            
        } catch (Exception e) {
            logger.warning("Error getting token status: " + e.getMessage());
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * Admin endpoint to manually sync all user streaks
     * This ensures database values match calculated values
     */
    @PostMapping("/admin/sync-streaks")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> syncAllStreaks() {
        try {
            int updatedCount = service.syncAllUserStreaks();
            
            Map<String, Object> result = new HashMap<>();
            result.put("message", "Streak sync completed successfully");
            result.put("usersUpdated", updatedCount);
            
            return ResponseEntity.ok(result);
            
        } catch (Exception e) {
            logger.warning("Error during streak sync: " + e.getMessage());
            
            Map<String, Object> error = new HashMap<>();
            error.put("error", "Streak sync failed: " + e.getMessage());
            
            return ResponseEntity.badRequest().body(error);
        }
    }

    @PostMapping("/user/profile")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<ProfileSetupResponse> setupProfile(@RequestBody ProfileSetupRequest request) {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            String email = authentication.getName();
            
            UserInfo updatedUser = service.setupProfile(email, request);
            UserProfileResponse safeUser = service.toUserProfileResponse(updatedUser);
            return ResponseEntity.ok(new ProfileSetupResponse(
                "Profile setup completed successfully", 
                true, 
                safeUser
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
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<UserProfileResponse> getProfile() {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            String email = authentication.getName();
            
            UserInfo user = service.getUserByEmail(email);
            UserProfileResponse safeUser = service.toUserProfileResponse(user);
            // If a stored object key exists, return a presigned URL for the client
            if (safeUser.getProfileImageUrl() != null && !safeUser.getProfileImageUrl().isEmpty()) {
                String presigned = s3StorageService.generatePresignedGetUrl(safeUser.getProfileImageUrl(), java.time.Duration.ofMinutes(15));
                safeUser.setProfileImageUrl(presigned);
            }
            return ResponseEntity.ok(safeUser);
        } catch (Exception e) {
            logger.warning("Get profile error: " + e.getMessage());
            return ResponseEntity.badRequest().build();
        }
    }
    
    @PutMapping("/user/update")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<?> updateUser(@RequestBody UserUpdateRequest request) {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            String email = authentication.getName();
            
            UserInfo updatedUser = service.updateUser(email, request);
            
            // Check if email was changed
            if (updatedUser.isEmailChanged()) {
                // Return special response indicating email change and need for re-login
                return ResponseEntity.ok(Map.of(
                    "message", "Profile updated successfully. Email changed. Please login again with your new email.",
                    "emailChanged", true,
                    "newEmail", updatedUser.getEmail(),
                    "requiresReLogin", true
                ));
            }
            
            // Normal response for non-email changes
            UserProfileResponse safeUser = service.toUserProfileResponse(updatedUser);
            return ResponseEntity.ok(safeUser);
            
        } catch (Exception e) {
            logger.warning("Update user error: " + e.getMessage());
            return ResponseEntity.badRequest().build();
        }
    }
    
    @PutMapping("/user/password")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<String> updatePassword(@RequestBody PasswordUpdateRequest request) {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            String email = authentication.getName();
            
            service.updatePassword(email, request);
            return ResponseEntity.ok("Password updated successfully");
        } catch (Exception e) {
            logger.warning("Update password error: " + e.getMessage());
            return ResponseEntity.badRequest().body("Password update failed: " + e.getMessage());
        }
    }
    
    @DeleteMapping("/user/delete")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<String> deleteUser(@RequestBody DeleteUserRequest request) {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            String email = authentication.getName();
            
            service.deleteUser(email, request);
            return ResponseEntity.ok("User deleted successfully");
        } catch (Exception e) {
            logger.warning("Delete user error: " + e.getMessage());
            return ResponseEntity.badRequest().body("User deletion failed: " + e.getMessage());
        }
    }

    @GetMapping("/user/streak")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<StreakResponse> getUserStreak() {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            String email = authentication.getName();
            
            UserInfo user = service.getUserByEmail(email);
            if (user == null) {
                return ResponseEntity.badRequest().build();
            }
            
            StreakResponse response = new StreakResponse();
            // Use the new method that properly accounts for missed days
            int currentStreak = service.getCurrentStreak(user);
            response.setStreak(currentStreak);
            response.setMessage("Current streak: " + currentStreak + " days");
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.warning("Get streak error: " + e.getMessage());
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * Manual endpoint to trigger streak check (for testing purposes)
     * This will check and reset broken streaks for all users
     */
    @PostMapping("/admin/check-streaks")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<String> checkStreaks() {
        try {
            service.checkAndResetBrokenStreaks();
            return ResponseEntity.ok("Streak check completed successfully");
        } catch (Exception e) {
            logger.warning("Streak check error: " + e.getMessage());
            return ResponseEntity.badRequest().body("Streak check failed: " + e.getMessage());
        }
    }

    // Email verification endpoints
    @PostMapping("/auth/send-verification")
    public ResponseEntity<String> sendVerificationEmail(@RequestParam String email) {
        try {
            emailVerificationService.sendVerificationEmail(email);
            return ResponseEntity.ok("Verification email sent successfully");
        } catch (Exception e) {
            logger.warning("Send verification email error: " + e.getMessage());
            return ResponseEntity.badRequest().body("Failed to send verification email: " + e.getMessage());
        }
    }

    @PostMapping("/auth/verify-email")
    public ResponseEntity<String> verifyEmail(@RequestParam String code, @RequestParam String email) {
        try {
            boolean verified = emailVerificationService.verifyEmail(code, email);
            if (verified) {
                return ResponseEntity.ok("Email verified successfully");
            } else {
                return ResponseEntity.badRequest().body("Invalid or expired verification code");
            }
        } catch (Exception e) {
            logger.warning("Verify email error: " + e.getMessage());
            return ResponseEntity.badRequest().body("Email verification failed: " + e.getMessage());
        }
    }

    @PostMapping("/auth/resend-verification")
    public ResponseEntity<String> resendVerificationEmail(@RequestParam String email) {
        try {
            emailVerificationService.resendVerificationEmail(email);
            return ResponseEntity.ok("Verification email resent successfully");
        } catch (Exception e) {
            logger.warning("Resend verification email error: " + e.getMessage());
            return ResponseEntity.badRequest().body("Failed to resend verification email: " + e.getMessage());
        }
    }
}