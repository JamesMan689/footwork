package com.footwork.api.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import java.util.logging.Logger;

@Service
public class StreakSchedulerService {
    
    private static final Logger logger = Logger.getLogger(StreakSchedulerService.class.getName());
    
    @Autowired
    private UserInfoService userInfoService;
    
    /**
     * Run streak check daily at 12:01 AM to ensure streaks are accurate
     * This will reset any streaks that have been broken due to missed days
     */
    @Scheduled(cron = "0 1 0 * * ?") // Daily at 12:01 AM
    public void dailyStreakCheck() {
        try {
            logger.info("Starting daily streak check...");
            userInfoService.checkAndResetBrokenStreaks();
            logger.info("Daily streak check completed successfully");
        } catch (Exception e) {
            logger.severe("Error during daily streak check: " + e.getMessage());
        }
    }
}
