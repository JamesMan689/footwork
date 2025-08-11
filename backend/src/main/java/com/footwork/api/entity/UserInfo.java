package com.footwork.api.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Entity
@Data
@AllArgsConstructor
@NoArgsConstructor
public class UserInfo {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private int id;

  @NotBlank(message = "Name is required")
  @Size(min = 2, max = 50, message = "Name must be between 2 and 50 characters")
  private String name;

  @NotBlank(message = "Email is required")
  @Email(message = "Invalid email format")
  private String email;

  @NotBlank(message = "Password is required")
  @Size(min = 8, message = "Password must be at least 8 characters")
  @Pattern(regexp = "^(?=.*[0-9])(?=.*[a-z])(?=.*[A-Z])(?=.*[!@#$%^&*()_+\\-=\\[\\]{};':\"\\\\|,.<>\\/?]).*$", message = "Password must contain at least one digit, one uppercase, one lowercase letter and one special character")
  private String password;

  @NotBlank(message = "Role is required")
  @Pattern(regexp = "ROLE_(USER|ADMIN)", message = "Invalid role format")
  private String roles;
  
  // Profile fields
  private Integer age;
  private String experienceLevel;
  private String primaryPosition;
  private boolean profileCompleted = false;

  // URL to user's profile image
  private String profileImageUrl;
  
  // Streak tracking
  private Integer streak = 0;
  private LocalDate lastCompletedDate;
  
  // Transient field to track email changes during updates
  @jakarta.persistence.Transient
  private boolean emailChanged = false;
}