package com.footwork.api.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ProfileSetupResponse {
    private String message;
    private boolean success;
    private UserProfileResponse userInfo;
    
    public ProfileSetupResponse(String message, boolean success) {
        this.message = message;
        this.success = success;
    }
} 