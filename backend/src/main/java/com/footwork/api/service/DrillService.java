package com.footwork.api.service;

import java.util.List;
import java.util.Optional;
import java.util.ArrayList;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.footwork.api.entity.Drill;
import com.footwork.api.entity.DrillFilterRequest;
import com.footwork.api.repository.DrillRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class DrillService {

    @Autowired
    private DrillRepository drillRepository;

    /**
     * Get all drills
     */
    public List<Drill> getAllDrills() {
        List<Drill> drills = drillRepository.findAll();
        System.out.println("Found " + drills.size() + " drills in database");
        if (!drills.isEmpty()) {
            System.out.println("First drill: " + drills.get(0).getName());
        }
        return drills;
    }

    /**
     * Get drill by ID
     */
    public Optional<Drill> getDrillById(Long id) {
        return drillRepository.findById(id);
    }

    /**
     * Get drills with filters
     */
    public List<Drill> getDrillsWithFilters(DrillFilterRequest filterRequest) {
        // If no filters are provided, return all drills
        if (filterRequest == null || 
            (filterRequest.getDifficulty() == null && 
             filterRequest.getPosition() == null && 
             filterRequest.getCategory() == null && 
             filterRequest.getMinDuration() == null && 
             filterRequest.getMaxDuration() == null &&
             filterRequest.getType() == null)) {
            return getAllDrills();
        }

        // Get all drills and filter in service layer for now
        List<Drill> allDrills = getAllDrills();
        List<Drill> filteredDrills = new ArrayList<>();
        
        for (Drill drill : allDrills) {
            boolean matches = true;
            
            // Filter by difficulty
            if (filterRequest.getDifficulty() != null && !filterRequest.getDifficulty().isEmpty()) {
                if (!drill.getDifficulty().equalsIgnoreCase(filterRequest.getDifficulty())) {
                    matches = false;
                }
            }
            
            // Filter by position
            if (filterRequest.getPosition() != null && !filterRequest.getPosition().isEmpty()) {
                if (!drill.getPosition().toLowerCase().contains(filterRequest.getPosition().toLowerCase())) {
                    matches = false;
                }
            }
            
            // Filter by category
            if (filterRequest.getCategory() != null && !filterRequest.getCategory().isEmpty()) {
                if (!drill.getCategory().toLowerCase().contains(filterRequest.getCategory().toLowerCase())) {
                    matches = false;
                }
            }
            
            // Filter by duration range
            if (filterRequest.getMinDuration() != null && drill.getDuration() < filterRequest.getMinDuration()) {
                matches = false;
            }
            if (filterRequest.getMaxDuration() != null && drill.getDuration() > filterRequest.getMaxDuration()) {
                matches = false;
            }
            
            // Filter by type
            if (filterRequest.getType() != null && !filterRequest.getType().isEmpty()) {
                if (drill.getType() == null || !drill.getType().equalsIgnoreCase(filterRequest.getType())) {
                    matches = false;
                }
            }
            
            if (matches) {
                filteredDrills.add(drill);
            }
        }
        
        return filteredDrills;
    }

    /**
     * Get drills by difficulty
     */
    public List<Drill> getDrillsByDifficulty(String difficulty) {
        List<Drill> allDrills = getAllDrills();
        List<Drill> filteredDrills = new ArrayList<>();
        
        for (Drill drill : allDrills) {
            if (drill.getDifficulty().equalsIgnoreCase(difficulty)) {
                filteredDrills.add(drill);
            }
        }
        
        return filteredDrills;
    }

    /**
     * Get drills by position (supports comma-separated positions)
     */
    public List<Drill> getDrillsByPosition(String position) {
        List<Drill> allDrills = getAllDrills();
        List<Drill> filteredDrills = new ArrayList<>();
        
        for (Drill drill : allDrills) {
            if (drill.getPosition().toLowerCase().contains(position.toLowerCase())) {
                filteredDrills.add(drill);
            }
        }
        
        return filteredDrills;
    }

    /**
     * Get drills by category (supports comma-separated categories)
     */
    public List<Drill> getDrillsByCategory(String category) {
        List<Drill> allDrills = getAllDrills();
        List<Drill> filteredDrills = new ArrayList<>();
        
        for (Drill drill : allDrills) {
            if (drill.getCategory().toLowerCase().contains(category.toLowerCase())) {
                filteredDrills.add(drill);
            }
        }
        
        return filteredDrills;
    }

    /**
     * Get drills by difficulty and position (supports comma-separated positions)
     */
    public List<Drill> getDrillsByDifficultyAndPosition(String difficulty, String position) {
        List<Drill> allDrills = getAllDrills();
        List<Drill> filteredDrills = new ArrayList<>();
        
        for (Drill drill : allDrills) {
            if (drill.getDifficulty().equalsIgnoreCase(difficulty) && 
                drill.getPosition().toLowerCase().contains(position.toLowerCase())) {
                filteredDrills.add(drill);
            }
        }
        
        return filteredDrills;
    }



    /**
     * Get drills by duration range
     */
    public List<Drill> getDrillsByDurationRange(Integer minDuration, Integer maxDuration) {
        List<Drill> allDrills = getAllDrills();
        List<Drill> filteredDrills = new ArrayList<>();
        
        for (Drill drill : allDrills) {
            if (drill.getDuration() >= minDuration && drill.getDuration() <= maxDuration) {
                filteredDrills.add(drill);
            }
        }
        
        return filteredDrills;
    }

    /**
     * Get drills with duration less than or equal to specified minutes
     */
    public List<Drill> getDrillsByMaxDuration(Integer maxDuration) {
        List<Drill> allDrills = getAllDrills();
        List<Drill> filteredDrills = new ArrayList<>();
        
        for (Drill drill : allDrills) {
            if (drill.getDuration() <= maxDuration) {
                filteredDrills.add(drill);
            }
        }
        
        return filteredDrills;
    }

    /**
     * Get drills with duration greater than or equal to specified minutes
     */
    public List<Drill> getDrillsByMinDuration(Integer minDuration) {
        List<Drill> allDrills = getAllDrills();
        List<Drill> filteredDrills = new ArrayList<>();
        
        for (Drill drill : allDrills) {
            if (drill.getDuration() >= minDuration) {
                filteredDrills.add(drill);
            }
        }
        
        return filteredDrills;
    }

    /**
     * Get drills by type
     */
    public List<Drill> getDrillsByType(String type) {
        List<Drill> allDrills = getAllDrills();
        List<Drill> filteredDrills = new ArrayList<>();
        
        for (Drill drill : allDrills) {
            if (drill.getType() != null && drill.getType().equalsIgnoreCase(type)) {
                filteredDrills.add(drill);
            }
        }
        
        return filteredDrills;
    }
} 