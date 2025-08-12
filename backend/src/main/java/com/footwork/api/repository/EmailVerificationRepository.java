package com.footwork.api.repository;

import com.footwork.api.entity.EmailVerification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;

@Repository
public interface EmailVerificationRepository extends JpaRepository<EmailVerification, Long> {
    
    Optional<EmailVerification> findByCodeAndEmailAndUsedFalseAndExpiresAtAfter(
        String code, String email, LocalDateTime now);
    
    Optional<EmailVerification> findByEmailAndUsedFalseAndExpiresAtAfter(
        String email, LocalDateTime now);
    
    @Modifying
    @Query("DELETE FROM EmailVerification e WHERE e.expiresAt < :now")
    int deleteExpiredCodes(@Param("now") LocalDateTime now);
    
    @Modifying
    @Query("UPDATE EmailVerification e SET e.used = true WHERE e.email = :email")
    void markAllTokensAsUsedForEmail(@Param("email") String email);
    
    @Modifying
    @Query("DELETE FROM EmailVerification e WHERE e.email = :email")
    void deleteAllByEmail(@Param("email") String email);
}
