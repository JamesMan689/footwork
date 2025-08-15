package com.footwork.api.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "password_reset_codes")
@Data
@AllArgsConstructor
@NoArgsConstructor
public class PasswordResetCode {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "user_id", nullable = false)
    private Integer userId;
    
    @Column(name = "email", nullable = false)
    private String email;
    
    @Column(name = "code_hash", nullable = false, length = 255)
    private String codeHash;
    
    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;
    
    @Column(name = "attempts", nullable = false)
    private Integer attempts = 0;
    
    @Column(name = "consumed", nullable = false)
    private Boolean consumed = false;
    
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;
    
    @Column(name = "ip_address", length = 45)
    private String ipAddress;
    
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
