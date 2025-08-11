package com.footwork.api.repository;


import org.springframework.data.jpa.repository.JpaRepository;

import com.footwork.api.entity.Drill;

public interface DrillRepository extends JpaRepository<Drill, Long> {
    
    // Find all drills (inherited from JpaRepository)
    
    // Note: findAll() is inherited from JpaRepository
} 