package com.footwork.api.repository;

import com.footwork.api.entity.DailyQuote;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface DailyQuoteRepository extends JpaRepository<DailyQuote, Long> {
    Optional<DailyQuote> findFirstByOrderById();
    
    @Query(value = "SELECT * FROM daily_quotes ORDER BY RANDOM() LIMIT 1", nativeQuery = true)
    Optional<DailyQuote> findRandomQuote();
} 