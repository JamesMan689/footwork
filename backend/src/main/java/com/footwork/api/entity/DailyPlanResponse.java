package com.footwork.api.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDate;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class DailyPlanResponse {
    private Long planId;
    private Integer userId;
    private LocalDate planDate;
    private String userLevel;
    private String position;
    private Integer sessionDuration;
    private boolean completed;
    private List<PlanDrillResponse> warmUpDrills;
    private List<PlanDrillResponse> coreDrills;
    private List<PlanDrillResponse> fitnessDrills;
    private List<PlanDrillResponse> cooldownDrills;
} 