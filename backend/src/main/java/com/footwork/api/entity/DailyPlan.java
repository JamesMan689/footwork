package com.footwork.api.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDate;
import java.util.List;

@Entity
@Table(name = "daily_plans")
@Data
@AllArgsConstructor
@NoArgsConstructor
public class DailyPlan {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "user_id")
    private UserInfo user;

    private LocalDate planDate;

    private String userLevel; // BEGINNER, INTERMEDIATE, ADVANCED
    private String position; // DEFENDER, MIDFIELDER, FORWARD
    private Integer sessionDuration; // in minutes
    private boolean completed = false;

    @OneToMany(mappedBy = "dailyPlan", cascade = CascadeType.ALL, fetch = FetchType.EAGER)
    private List<PlanDrill> planDrills;
} 