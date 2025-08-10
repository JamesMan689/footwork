package com.footwork.api.controller;

import com.footwork.api.entity.*;
import com.footwork.api.service.PlanGenerationService;
import com.footwork.api.service.UserInfoService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/plans")
@CrossOrigin(origins = "*")
public class PlanController {

    @Autowired
    private PlanGenerationService planGenerationService;

    @Autowired
    private UserInfoService userInfoService;

    @PostMapping("/generate")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<?> generateDailyPlan(Authentication authentication) {
        try {
            String userEmail = authentication.getName();
            UserInfo user = userInfoService.getUserByEmail(userEmail);
            
            if (user == null) {
                return ResponseEntity.badRequest().body("User not found");
            }

            // Check if user profile is completed
            if (!user.isProfileCompleted()) {
                return ResponseEntity.badRequest().body("User profile must be completed before generating plans. Please complete your profile setup first.");
            }

            // Create empty request since we don't need any parameters
            PlanGenerationRequest request = new PlanGenerationRequest();
            DailyPlanResponse plan = planGenerationService.generateDailyPlan(user, request);
            return ResponseEntity.ok(plan);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error generating plan: " + e.getMessage());
        }
    }

    @GetMapping("/current")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<?> getCurrentPlan(Authentication authentication) {
        try {
            String userEmail = authentication.getName();
            UserInfo user = userInfoService.getUserByEmail(userEmail);
            
            if (user == null) {
                return ResponseEntity.badRequest().body("User not found");
            }

            DailyPlanResponse plan = planGenerationService.getCurrentPlan(user);
            if (plan == null) {
                return ResponseEntity.notFound().build();
            }
            
            return ResponseEntity.ok(plan);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error getting current plan: " + e.getMessage());
        }
    }

    @PostMapping("/{planId}/complete")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<?> markPlanAsCompleted(@PathVariable Long planId, 
                                               Authentication authentication) {
        try {
            String userEmail = authentication.getName();
            UserInfo user = userInfoService.getUserByEmail(userEmail);
            
            if (user == null) {
                return ResponseEntity.badRequest().body("User not found");
            }

            planGenerationService.markPlanAsCompleted(planId);
            return ResponseEntity.ok().body("Plan marked as completed");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error completing plan: " + e.getMessage());
        }
    }


} 