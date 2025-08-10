package com.footwork.api.controller;

import com.footwork.api.entity.DailyQuoteResponse;
import com.footwork.api.service.DailyQuoteService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/dashboard")
@CrossOrigin(origins = "*")
public class DashboardController {

    @Autowired
    private DailyQuoteService dailyQuoteService;

    @GetMapping("/quote")
    public ResponseEntity<DailyQuoteResponse> getDailyQuote() {
        try {
            DailyQuoteResponse quote = dailyQuoteService.getDailyQuote();
            return ResponseEntity.ok(quote);
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }
} 