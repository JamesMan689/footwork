package com.footwork.api.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDate;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class DailyTipResponse {
    private Long id;
    private String tip;
    private String author;
    private String category;
    private LocalDate tipDate;
} 