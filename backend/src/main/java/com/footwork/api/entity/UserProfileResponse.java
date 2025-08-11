package com.footwork.api.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class UserProfileResponse {
    private int id;
    private String name;
    private String email;
    private String roles;
    private Integer age;
    private String experienceLevel;
    private String primaryPosition;
    private boolean profileCompleted;
    private String profileImageUrl;
    private Integer streak;
    private String lastCompletedDate;
} 