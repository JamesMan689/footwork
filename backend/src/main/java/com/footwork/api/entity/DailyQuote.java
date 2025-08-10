package com.footwork.api.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "daily_quotes")
@Data
@AllArgsConstructor
@NoArgsConstructor
public class DailyQuote {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    private String quote;
    private String author;
    private String category; // e.g., "MOTIVATION", "TECHNIQUE", "MINDSET"
} 