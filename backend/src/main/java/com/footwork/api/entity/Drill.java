package com.footwork.api.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Column;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "drills")
@Data
@AllArgsConstructor
@NoArgsConstructor
public class Drill {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "Drill name is required")
    @Size(min = 2, max = 100, message = "Drill name must be between 2 and 100 characters")
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Size(max = 20, message = "Difficulty must be at most 20 characters")
    private String difficulty; // EASY, MEDIUM, HARD

    @Size(max = 255, message = "Category must be at most 255 characters")
    private String category; // comma-separated: CONTROL, FITNESS, PASSING, SHOOTING, DEFENDING

    @Size(max = 100, message = "Position must be at most 100 characters")
    private String position; // comma-separated: ALL, DEFENDER, MIDFIELDER, FORWARD

    private Integer duration; // in minutes

    @Size(max = 200, message = "Equipment must be at most 200 characters")
    private String equipment; // comma-separated list

    @Column(columnDefinition = "TEXT")
    private String instructions;

    private String thumbnail; // URL or path to thumbnail image

    private String type; // Additional drill type field

    // Note: isActive field removed to match existing database schema
    // If you want to add this column later, run: ALTER TABLE drills ADD COLUMN is_active BOOLEAN DEFAULT TRUE;
} 