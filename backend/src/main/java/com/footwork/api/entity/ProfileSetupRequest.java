package com.footwork.api.entity;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ProfileSetupRequest {
    
    @NotNull(message = "Age is required")
    private Integer age;
    
    @NotBlank(message = "Experience level is required")
    @Pattern(regexp = "^(BEGINNER|INTERMEDIATE|ADVANCED)$", 
             message = "Experience level must be BEGINNER, INTERMEDIATE, ADVANCED")
    private String experienceLevel;
    
    @NotBlank(message = "Primary position is required")
    @Pattern(regexp = "^(DEFENDER|MIDFIELDER|FORWARD)$", 
             message = "Primary position must be DEFENDER, MIDFIELDER, or FORWARD")
    private String primaryPosition;
} 