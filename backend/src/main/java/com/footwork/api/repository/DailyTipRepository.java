package com.footwork.api.repository;

import com.footwork.api.entity.DailyTip;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.time.LocalDate;
import java.util.Optional;

@Repository
public interface DailyTipRepository extends JpaRepository<DailyTip, Long> {
    Optional<DailyTip> findByTipDateAndActiveTrue(LocalDate tipDate);
    Optional<DailyTip> findFirstByActiveTrueOrderByTipDateDesc();
} 