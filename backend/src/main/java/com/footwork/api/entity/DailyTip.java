package com.footwork.api.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDate;

@Entity
@Table(name = "daily_tips")
@Data
@AllArgsConstructor
@NoArgsConstructor
public class DailyTip {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    private String tip;
    private String author;
    private String category; // e.g., "MOTIVATION", "TECHNIQUE", "MINDSET"
    private LocalDate tipDate;
    private boolean active = true;
} 