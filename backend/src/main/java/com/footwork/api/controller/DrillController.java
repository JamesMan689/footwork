package com.footwork.api.controller;

import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.footwork.api.entity.Drill;
import com.footwork.api.entity.DrillFilterRequest;
import com.footwork.api.service.DrillService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class DrillController {

    @Autowired
    private DrillService drillService;

    /**
     * GET /api/drills - Return all drills with optional filters
     * 
     * Query parameters:
     * - category: Filter by category (CONTROL, FITNESS, PASSING, SHOOTING, DEFENDING)
     * - position: Filter by position (ALL, DEFENDER, MIDFIELDER, FORWARD)
     * - difficulty: Filter by difficulty (EASY, MEDIUM, HARD)
     * - minDuration: Minimum duration in minutes
     * - maxDuration: Maximum duration in minutes
     * - type: Filter by drill type
     */
    @GetMapping("/drills")
    public ResponseEntity<List<Drill>> getDrills(
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String position,
            @RequestParam(required = false) String difficulty,
            @RequestParam(required = false) Integer minDuration,
            @RequestParam(required = false) Integer maxDuration,
            @RequestParam(required = false) String type) {
        
        try {
            System.out.println("DrillController: Received request for drills");
            
            // Create filter request object
            DrillFilterRequest filterRequest = new DrillFilterRequest(
                null, category, position, difficulty, minDuration, maxDuration, type
            );
            
            // Get drills with filters
            List<Drill> drills = drillService.getDrillsWithFilters(filterRequest);
            
            System.out.println("DrillController: Returning " + drills.size() + " drills");
            
            return ResponseEntity.ok(drills);
            
        } catch (Exception e) {
            System.err.println("DrillController: Error occurred: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * GET /api/drills/{id} - Get a specific drill by ID
     */
    @GetMapping("/drills/{id}")
    public ResponseEntity<Drill> getDrillById(@PathVariable Long id) {
        try {
            Optional<Drill> drill = drillService.getDrillById(id);
            
            if (drill.isPresent()) {
                return ResponseEntity.ok(drill.get());
            } else {
                return ResponseEntity.notFound().build();
            }
            
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * GET /api/drills/category/{category} - Get drills by category
     */
    @GetMapping("/drills/category/{category}")
    public ResponseEntity<List<Drill>> getDrillsByCategory(@PathVariable String category) {
        try {
            List<Drill> drills = drillService.getDrillsByCategory(category);
            return ResponseEntity.ok(drills);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * GET /api/drills/position/{position} - Get drills by position
     */
    @GetMapping("/drills/position/{position}")
    public ResponseEntity<List<Drill>> getDrillsByPosition(@PathVariable String position) {
        try {
            List<Drill> drills = drillService.getDrillsByPosition(position);
            return ResponseEntity.ok(drills);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * GET /api/drills/difficulty/{difficulty} - Get drills by difficulty
     */
    @GetMapping("/drills/difficulty/{difficulty}")
    public ResponseEntity<List<Drill>> getDrillsByDifficulty(@PathVariable String difficulty) {
        try {
            List<Drill> drills = drillService.getDrillsByDifficulty(difficulty);
            return ResponseEntity.ok(drills);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }


} 