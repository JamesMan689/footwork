package com.footwork.api.repository;

import com.footwork.api.entity.DailyPlan;
import com.footwork.api.entity.UserInfo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface DailyPlanRepository extends JpaRepository<DailyPlan, Long> {
    List<DailyPlan> findByUserOrderByPlanDateDesc(UserInfo user);
    Optional<DailyPlan> findByUserAndPlanDate(UserInfo user, LocalDate planDate);
    List<DailyPlan> findByUserAndPlanDateBetween(UserInfo user, LocalDate startDate, LocalDate endDate);
} 