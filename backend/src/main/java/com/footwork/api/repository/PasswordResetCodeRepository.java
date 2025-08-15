package com.footwork.api.repository;

import com.footwork.api.entity.PasswordResetCode;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface PasswordResetCodeRepository extends JpaRepository<PasswordResetCode, Long> {
    
    @Query("SELECT prc FROM PasswordResetCode prc WHERE prc.email = :email AND prc.consumed = false AND prc.expiresAt > :now ORDER BY prc.createdAt DESC")
    List<PasswordResetCode> findActiveCodesByEmail(@Param("email") String email, @Param("now") LocalDateTime now);
    
    @Query("SELECT COUNT(prc) FROM PasswordResetCode prc WHERE prc.email = :email AND prc.createdAt > :since")
    long countRecentCodesByEmail(@Param("email") String email, @Param("since") LocalDateTime since);
    
    @Query("SELECT COUNT(prc) FROM PasswordResetCode prc WHERE prc.ipAddress = :ipAddress AND prc.createdAt > :since")
    long countRecentCodesByIpAddress(@Param("ipAddress") String ipAddress, @Param("since") LocalDateTime since);
    
    @Modifying
    @Query("DELETE FROM PasswordResetCode prc WHERE prc.expiresAt < :now")
    void deleteExpiredCodes(@Param("now") LocalDateTime now);
    
    @Modifying
    @Query("UPDATE PasswordResetCode prc SET prc.consumed = true WHERE prc.id = :id")
    void markAsConsumed(@Param("id") Long id);
    
    @Modifying
    @Query("UPDATE PasswordResetCode prc SET prc.attempts = prc.attempts + 1 WHERE prc.id = :id")
    void incrementAttempts(@Param("id") Long id);
    
    @Modifying
    @Query("DELETE FROM PasswordResetCode prc WHERE prc.email = :email")
    void deleteAllByEmail(@Param("email") String email);
    
    @Modifying
    @Query("DELETE FROM PasswordResetCode prc WHERE prc.createdAt < :cutoffTime")
    void deleteOldCodes(@Param("cutoffTime") LocalDateTime cutoffTime);
}
