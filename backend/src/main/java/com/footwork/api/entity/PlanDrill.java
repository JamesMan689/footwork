package com.footwork.api.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "plan_drills")
@Data
@AllArgsConstructor
@NoArgsConstructor
public class PlanDrill {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "daily_plan_id")
    private DailyPlan dailyPlan;

    @ManyToOne
    @JoinColumn(name = "drill_id")
    private Drill drill;

    private String drillType; // CONTROL, PASSING, DEFENDING, SHOOTING, FITNESS
    private Integer orderIndex; // Order in the plan
    private Integer duration; // Duration for this drill in minutes
    private String section; // WARM_UP, CORE, COOLDOWN, FITNESS
} 