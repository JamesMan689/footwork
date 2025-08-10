package com.footwork.api.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class DailyQuoteResponse {
    private Long id;
    private String quote;
    private String author;
    private String category;
} 