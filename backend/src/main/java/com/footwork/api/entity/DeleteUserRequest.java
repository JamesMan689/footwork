package com.footwork.api.entity;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class DeleteUserRequest {
    
    @NotBlank(message = "Password confirmation is required")
    private String password;
    
    @NotBlank(message = "Confirmation text is required")
    private String confirmation;
} 