package com.footwork.api.controller;

import com.footwork.api.entity.DailyTipResponse;
import com.footwork.api.service.DailyTipService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/dashboard")
@CrossOrigin(origins = "*")
public class DashboardController {

    @Autowired
    private DailyTipService dailyTipService;

    @GetMapping("/tip")
    public ResponseEntity<DailyTipResponse> getDailyTip() {
        try {
            DailyTipResponse tip = dailyTipService.getDailyTip();
            return ResponseEntity.ok(tip);
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }
} 