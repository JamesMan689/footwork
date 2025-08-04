package com.footwork.api.service;

import com.footwork.api.entity.DailyTip;
import com.footwork.api.entity.DailyTipResponse;
import com.footwork.api.repository.DailyTipRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.time.LocalDate;
import java.util.Optional;

@Service
public class DailyTipService {

    @Autowired
    private DailyTipRepository dailyTipRepository;

    public DailyTipResponse getDailyTip() {
        LocalDate today = LocalDate.now();
        
        // Try to get today's tip
        Optional<DailyTip> todayTip = dailyTipRepository.findByTipDateAndActiveTrue(today);
        
        if (todayTip.isPresent()) {
            return convertToResponse(todayTip.get());
        }
        
        // If no tip for today, get the most recent tip
        Optional<DailyTip> latestTip = dailyTipRepository.findFirstByActiveTrueOrderByTipDateDesc();
        
        if (latestTip.isPresent()) {
            return convertToResponse(latestTip.get());
        }
        
        // If no tips exist, return a default motivational tip
        return getDefaultTip();
    }

    private DailyTipResponse convertToResponse(DailyTip dailyTip) {
        return new DailyTipResponse(
            dailyTip.getId(),
            dailyTip.getTip(),
            dailyTip.getAuthor(),
            dailyTip.getCategory(),
            dailyTip.getTipDate()
        );
    }

    private DailyTipResponse getDefaultTip() {
        return new DailyTipResponse(
            0L,
            "Success is not final, failure is not fatal: it is the courage to continue that counts.",
            "Winston Churchill",
            "MOTIVATION",
            LocalDate.now()
        );
    }
} 