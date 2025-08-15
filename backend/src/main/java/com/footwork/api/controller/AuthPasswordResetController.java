package com.footwork.api.controller;

import com.footwork.api.entity.ForgotPasswordRequest;
import com.footwork.api.entity.ResetPasswordRequest;
import com.footwork.api.service.PasswordResetService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.util.logging.Logger;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Slf4j
public class AuthPasswordResetController {

    private static final Logger logger = Logger.getLogger(AuthPasswordResetController.class.getName());
    
    private final PasswordResetService passwordResetService;

    /**
     * Request a password reset code
     * POST /api/auth/forgot-password
     * Always returns 200 to prevent email enumeration
     */
    @PostMapping("/forgot-password")
    public ResponseEntity<String> forgotPassword(
            @Valid @RequestBody ForgotPasswordRequest request,
            HttpServletRequest httpRequest) {
        
        try {
            String ipAddress = getClientIpAddress(httpRequest);
            logger.info("Password reset requested for email: " + request.getEmail() + " from IP: " + ipAddress);
            
            passwordResetService.requestPasswordReset(request.getEmail(), ipAddress);
            
            // Always return success to prevent email enumeration
            return ResponseEntity.ok("If an account with that email exists, a password reset code has been sent.");
            
        } catch (RuntimeException e) {
            if (e.getMessage().contains("Too many password reset requests")) {
                logger.warning("Rate limit exceeded for password reset: " + request.getEmail());
                return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                    .body("Too many password reset requests. Please try again later.");
            }
            
            logger.severe("Error processing password reset request for " + request.getEmail() + ": " + e.getMessage());
            // Still return 200 to prevent email enumeration
            return ResponseEntity.ok("If an account with that email exists, a password reset code has been sent.");
        } catch (Exception e) {
            logger.severe("Unexpected error processing password reset request for " + request.getEmail() + ": " + e.getMessage());
            // Still return 200 to prevent email enumeration
            return ResponseEntity.ok("If an account with that email exists, a password reset code has been sent.");
        }
    }

    /**
     * Reset password using the provided code
     * POST /api/auth/reset-password
     */
    @PostMapping("/reset-password")
    public ResponseEntity<String> resetPassword(
            @Valid @RequestBody ResetPasswordRequest request) {
        
        try {
            logger.info("Password reset attempt for email: " + request.getEmail());
            
            passwordResetService.resetPassword(request.getEmail(), request.getCode(), request.getNewPassword());
            
            return ResponseEntity.ok("Password reset successful. You can now log in with your new password.");
            
        } catch (RuntimeException e) {
            String errorMessage = e.getMessage();
            
            if (errorMessage.contains("Invalid") || errorMessage.contains("expired") || 
                errorMessage.contains("already been used") || errorMessage.contains("Too many failed attempts")) {
                logger.warning("Password reset failed for " + request.getEmail() + ": " + errorMessage);
                return ResponseEntity.badRequest().body(errorMessage);
            }
            
            logger.severe("Error processing password reset for " + request.getEmail() + ": " + errorMessage);
            return ResponseEntity.badRequest().body("An error occurred while resetting your password. Please try again.");
            
        } catch (Exception e) {
            logger.severe("Unexpected error processing password reset for " + request.getEmail() + ": " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body("An unexpected error occurred. Please try again later.");
        }
    }

    /**
     * Get client IP address from request
     * Handles various proxy scenarios
     */
    private String getClientIpAddress(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty() && !"unknown".equalsIgnoreCase(xForwardedFor)) {
            return xForwardedFor.split(",")[0].trim();
        }
        
        String xRealIp = request.getHeader("X-Real-IP");
        if (xRealIp != null && !xRealIp.isEmpty() && !"unknown".equalsIgnoreCase(xRealIp)) {
            return xRealIp;
        }
        
        String xClientIp = request.getHeader("X-Client-IP");
        if (xClientIp != null && !xClientIp.isEmpty() && !"unknown".equalsIgnoreCase(xClientIp)) {
            return xClientIp;
        }
        
        return request.getRemoteAddr();
    }
}
