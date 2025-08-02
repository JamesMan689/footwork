package com.footwork.api.repository;

import com.footwork.api.entity.PlanDrill;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface PlanDrillRepository extends JpaRepository<PlanDrill, Long> {
    List<PlanDrill> findByDailyPlanOrderByOrderIndex(com.footwork.api.entity.DailyPlan dailyPlan);
    List<PlanDrill> findByDailyPlanAndSectionOrderByOrderIndex(com.footwork.api.entity.DailyPlan dailyPlan, String section);
    void deleteByDailyPlan(com.footwork.api.entity.DailyPlan dailyPlan);
} 