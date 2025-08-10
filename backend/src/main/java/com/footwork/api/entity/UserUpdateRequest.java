package com.footwork.api.entity;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class UserUpdateRequest {
    
    @Size(min = 2, max = 50, message = "Name must be between 2 and 50 characters")
    private String name;
    
    @Email(message = "Invalid email format")
    private String email;
    
    @Pattern(regexp = "ROLE_(USER|ADMIN)", message = "Invalid role format")
    private String roles;
    
    private Integer age;
    
    @Pattern(regexp = "^(BEGINNER|INTERMEDIATE|ADVANCED)$", 
             message = "Experience level must be BEGINNER, INTERMEDIATE, ADVANCED")
    private String experienceLevel;
    
    @Pattern(regexp = "^(DEFENDER|MIDFIELDER|FORWARD)$", 
             message = "Primary position must be DEFENDER, MIDFIELDER, or FORWARD")
    private String primaryPosition;
} 