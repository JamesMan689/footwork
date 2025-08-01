package com.footwork.api.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class DrillFilterRequest {
    private String name;
    private String category; // CONTROL, FITNESS, PASSING, SHOOTING, DEFENDING
    private String position; // ALL, DEFENDER, MIDFIELDER, FORWARD (will match any position in comma-separated list)
    private String difficulty; // EASY, MEDIUM, HARD
    private Integer minDuration;
    private Integer maxDuration;
    private String type;
} 