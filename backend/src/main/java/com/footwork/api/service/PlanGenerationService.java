package com.footwork.api.service;

import com.footwork.api.entity.*;
import com.footwork.api.repository.DailyPlanRepository;
import com.footwork.api.repository.DrillRepository;
import com.footwork.api.repository.PlanDrillRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class PlanGenerationService {

    @Autowired
    private DrillRepository drillRepository;

    @Autowired
    private DailyPlanRepository dailyPlanRepository;

    @Autowired
    private PlanDrillRepository planDrillRepository;

    @Transactional
    public DailyPlanResponse generateDailyPlan(UserInfo user, PlanGenerationRequest request) {
        // Check if user profile is completed
        if (!user.isProfileCompleted()) {
            throw new RuntimeException("User profile must be completed before generating plans");
        }

        // Check if plan already exists for today and delete it
        LocalDate today = LocalDate.now();
        Optional<DailyPlan> existingPlan = dailyPlanRepository.findByUserAndPlanDate(user, today);
        if (existingPlan.isPresent()) {
            // Delete existing plan and its drills
            DailyPlan existingDailyPlan = existingPlan.get();
            planDrillRepository.deleteByDailyPlan(existingDailyPlan);
            dailyPlanRepository.delete(existingDailyPlan);
        }

        // Generate new plan using user's profile data
        DailyPlan dailyPlan = new DailyPlan();
        dailyPlan.setUser(user);
        dailyPlan.setPlanDate(today);
        dailyPlan.setUserLevel(user.getExperienceLevel().toUpperCase());
        dailyPlan.setPosition(user.getPrimaryPosition().toUpperCase());
        dailyPlan.setCompleted(false);

        // Save the plan first
        dailyPlan = dailyPlanRepository.save(dailyPlan);

        // Generate plan drills using user's profile data
        List<PlanDrill> planDrills = generatePlanDrills(dailyPlan, user);
        planDrillRepository.saveAll(planDrills);

        // Calculate total session duration
        int totalDuration = calculateTotalSessionDuration(planDrills);
        dailyPlan.setSessionDuration(totalDuration);
        dailyPlanRepository.save(dailyPlan);

        // Keep only the latest plan for this user (delete older ones)
        // We consider the plan list ordered by most recent date first; keep index 0
        List<DailyPlan> userPlansDesc = dailyPlanRepository.findByUserOrderByPlanDateDesc(user);
        if (userPlansDesc.size() > 1) {
            // Delete drills for older plans first to avoid FK issues
            for (int i = 1; i < userPlansDesc.size(); i++) {
                planDrillRepository.deleteByDailyPlan(userPlansDesc.get(i));
            }
            // Then delete the older plans themselves
            dailyPlanRepository.deleteAll(userPlansDesc.subList(1, userPlansDesc.size()));
        }

        // Reload the plan with drills
        dailyPlan = dailyPlanRepository.findById(dailyPlan.getId()).orElse(dailyPlan);
        return convertToResponse(dailyPlan);
    }

    private List<PlanDrill> generatePlanDrills(DailyPlan dailyPlan, UserInfo user) {
        List<PlanDrill> planDrills = new ArrayList<>();
        int orderIndex = 1;

        // Get all available drills
        List<Drill> allDrills = drillRepository.findAll();
        System.out.println("Total drills in database: " + allDrills.size());

        // Track selected drill IDs to prevent duplicates
        Set<Long> selectedDrillIds = new HashSet<>();

        // Generate warmup drills (10 minutes for all levels)
        List<PlanDrill> warmupDrills = generateWarmupDrills(dailyPlan, allDrills, orderIndex, selectedDrillIds);
        planDrills.addAll(warmupDrills);
        orderIndex += warmupDrills.size();

        // Generate fitness drills (only for advanced level, always 5 minutes)
        if ("ADVANCED".equals(user.getExperienceLevel().toUpperCase())) {
            List<PlanDrill> fitnessDrills = generateFitnessDrills(dailyPlan, allDrills, orderIndex, selectedDrillIds);
            planDrills.addAll(fitnessDrills);
            orderIndex += fitnessDrills.size();
        }

        // Generate core drills based on user's position and level
        List<PlanDrill> coreDrills = generateCoreDrills(dailyPlan, allDrills, user, orderIndex, selectedDrillIds);
        planDrills.addAll(coreDrills);

        // Generate cooldown drills (5 minutes for all levels)
        List<PlanDrill> cooldownDrills = generateCooldownDrills(dailyPlan, allDrills, orderIndex, selectedDrillIds);
        planDrills.addAll(cooldownDrills);

        return planDrills;
    }

    private List<PlanDrill> generateWarmupDrills(DailyPlan dailyPlan, List<Drill> allDrills, int startOrder, Set<Long> selectedDrillIds) {
        List<PlanDrill> warmupDrills = new ArrayList<>();
        
        // Filter warmup drills based on type column, excluding already selected drills
        List<Drill> warmupCandidates = allDrills.stream()
                .filter(drill -> drill.getType() != null && 
                        drill.getType().equalsIgnoreCase("warmup") &&
                        !selectedDrillIds.contains(drill.getId()))
                .collect(Collectors.toList());

        System.out.println("Warmup drills found: " + warmupCandidates.size());

        if (!warmupCandidates.isEmpty()) {
            Drill selectedDrill = getRandomDrill(warmupCandidates);
            PlanDrill planDrill = new PlanDrill();
            planDrill.setDailyPlan(dailyPlan);
            planDrill.setDrill(selectedDrill);
            planDrill.setDrillType("WARMUP");
            planDrill.setOrderIndex(startOrder);
            planDrill.setDuration(10); // Fixed 10 minutes
            planDrill.setSection("WARMUP");
            warmupDrills.add(planDrill);
            selectedDrillIds.add(selectedDrill.getId()); // Track selected drill
        }

        return warmupDrills;
    }

    private List<PlanDrill> generateFitnessDrills(DailyPlan dailyPlan, List<Drill> allDrills, int startOrder, Set<Long> selectedDrillIds) {
        List<PlanDrill> fitnessDrills = new ArrayList<>();
        
        // Filter fitness drills based on type column, excluding already selected drills
        List<Drill> fitnessCandidates = allDrills.stream()
                .filter(drill -> drill.getType() != null && 
                        drill.getType().equalsIgnoreCase("fitness") &&
                        !selectedDrillIds.contains(drill.getId()))
                .collect(Collectors.toList());

        System.out.println("Fitness drills found: " + fitnessCandidates.size());

        if (!fitnessCandidates.isEmpty()) {
            Drill selectedDrill = getRandomDrill(fitnessCandidates);
            PlanDrill planDrill = new PlanDrill();
            planDrill.setDailyPlan(dailyPlan);
            planDrill.setDrill(selectedDrill);
            planDrill.setDrillType("FITNESS");
            planDrill.setOrderIndex(startOrder);
            planDrill.setDuration(5); // Fixed 5 minutes
            planDrill.setSection("FITNESS");
            fitnessDrills.add(planDrill);
            selectedDrillIds.add(selectedDrill.getId()); // Track selected drill
        }

        return fitnessDrills;
    }

    private List<PlanDrill> generateCooldownDrills(DailyPlan dailyPlan, List<Drill> allDrills, int startOrder, Set<Long> selectedDrillIds) {
        List<PlanDrill> cooldownDrills = new ArrayList<>();
        
        // Filter cooldown drills based on type column, excluding already selected drills
        List<Drill> cooldownCandidates = allDrills.stream()
                .filter(drill -> drill.getType() != null && 
                        drill.getType().equalsIgnoreCase("cooldown") &&
                        !selectedDrillIds.contains(drill.getId()))
                .collect(Collectors.toList());

        System.out.println("Cooldown drills found: " + cooldownCandidates.size());

        if (!cooldownCandidates.isEmpty()) {
            Drill selectedDrill = getRandomDrill(cooldownCandidates);
            PlanDrill planDrill = new PlanDrill();
            planDrill.setDailyPlan(dailyPlan);
            planDrill.setDrill(selectedDrill);
            planDrill.setDrillType("COOLDOWN");
            planDrill.setOrderIndex(startOrder);
            planDrill.setDuration(5); // Fixed 5 minutes
            planDrill.setSection("COOLDOWN");
            cooldownDrills.add(planDrill);
            selectedDrillIds.add(selectedDrill.getId()); // Track selected drill
        }

        return cooldownDrills;
    }

    private List<PlanDrill> generateCoreDrills(DailyPlan dailyPlan, List<Drill> allDrills, 
                                              UserInfo user, int startOrder, Set<Long> selectedDrillIds) {
        List<PlanDrill> coreDrills = new ArrayList<>();
        int orderIndex = startOrder;

        // Get drill requirements based on user's position and level
        Map<String, Integer> drillRequirements = getDrillRequirements(user.getPrimaryPosition(), user.getExperienceLevel());
        
        System.out.println("User position: " + user.getPrimaryPosition());
        System.out.println("User level: " + user.getExperienceLevel());
        System.out.println("Drill requirements: " + drillRequirements);
        
        for (Map.Entry<String, Integer> entry : drillRequirements.entrySet()) {
            String drillType = entry.getKey();
            int count = entry.getValue();
            
            System.out.println("Looking for " + count + " drills of type: " + drillType);
            
            for (int i = 0; i < count; i++) {
                List<Drill> candidates = filterDrillsByType(allDrills, drillType, user.getPrimaryPosition());
                
                // Filter out already selected drills
                candidates = candidates.stream()
                        .filter(drill -> !selectedDrillIds.contains(drill.getId()))
                        .collect(Collectors.toList());
                
                System.out.println("Found " + candidates.size() + " candidates for " + drillType + " (after filtering duplicates)");
                
                if (!candidates.isEmpty()) {
                    Drill selectedDrill = getRandomDrill(candidates);
                    PlanDrill planDrill = new PlanDrill();
                    planDrill.setDailyPlan(dailyPlan);
                    planDrill.setDrill(selectedDrill);
                    planDrill.setDrillType(drillType);
                    planDrill.setOrderIndex(orderIndex++);
                    planDrill.setDuration(selectedDrill.getDuration()); // Use actual drill duration
                    planDrill.setSection("CORE");
                    coreDrills.add(planDrill);
                    selectedDrillIds.add(selectedDrill.getId()); // Track selected drill
                    System.out.println("Added drill: " + selectedDrill.getName() + " (type: " + drillType + ")");
                } else {
                    System.out.println("No drills found for type: " + drillType + " and position: " + user.getPrimaryPosition());
                }
            }
        }

        System.out.println("Total core drills generated: " + coreDrills.size());
        return coreDrills;
    }

    private Map<String, Integer> getDrillRequirements(String position, String level) {
        Map<String, Integer> requirements = new HashMap<>();
        
        switch (position.toUpperCase()) {
            case "DEFENDER":
                switch (level.toUpperCase()) {
                    case "BEGINNER":
                        requirements.put("CONTROL", 1);
                        requirements.put("DEFENDING", 1);
                        break;
                    case "INTERMEDIATE":
                        requirements.put("CONTROL", 1);
                        requirements.put("PASSING", 1);
                        requirements.put("DEFENDING", 1);
                        break;
                    case "ADVANCED":
                        requirements.put("CONTROL", 1);
                        requirements.put("PASSING", 1);
                        requirements.put("DEFENDING", 2);
                        requirements.put("SHOOTING", 1);
                        break;
                }
                break;
            case "MIDFIELDER":
                switch (level.toUpperCase()) {
                    case "BEGINNER":
                        requirements.put("CONTROL", 1);
                        requirements.put("PASSING", 1);
                        break;
                    case "INTERMEDIATE":
                        requirements.put("CONTROL", 1);
                        requirements.put("PASSING", 1);
                        requirements.put("SHOOTING", 1);
                        break;
                    case "ADVANCED":
                        requirements.put("CONTROL", 2);
                        requirements.put("PASSING", 2);
                        requirements.put("SHOOTING", 1);
                        break;
                }
                break;
            case "FORWARD":
                switch (level.toUpperCase()) {
                    case "BEGINNER":
                        requirements.put("CONTROL", 1);
                        requirements.put("SHOOTING", 1);
                        break;
                    case "INTERMEDIATE":
                        requirements.put("CONTROL", 1);
                        requirements.put("SHOOTING", 2);
                        break;
                    case "ADVANCED":
                        requirements.put("CONTROL", 2);
                        requirements.put("PASSING", 1);
                        requirements.put("SHOOTING", 2);
                        break;
                }
                break;
        }
        
        return requirements;
    }

    private List<Drill> filterDrillsByType(List<Drill> allDrills, String drillType, String position) {
        // More flexible filtering - check both type and category fields
        return allDrills.stream()
                .filter(drill -> {
                    boolean typeMatches = false;
                    boolean positionMatches = false;
                    
                    // Check type field
                    if (drill.getType() != null && drill.getType().equalsIgnoreCase(drillType)) {
                        typeMatches = true;
                    }
                    
                    // Also check category field as fallback
                    if (drill.getCategory() != null && drill.getCategory().toLowerCase().contains(drillType.toLowerCase())) {
                        typeMatches = true;
                    }
                    
                    // Check position - be more flexible
                    if (drill.getPosition() != null) {
                        String drillPosition = drill.getPosition().toLowerCase();
                        String userPosition = position.toLowerCase();
                        
                        // Check if position contains the user's position or is "ALL"
                        if (drillPosition.contains(userPosition) || drillPosition.contains("all")) {
                            positionMatches = true;
                        }
                    }
                    
                    return typeMatches && positionMatches;
                })
                .collect(Collectors.toList());
    }

    private Drill getRandomDrill(List<Drill> drills) {
        if (drills.isEmpty()) {
            return null;
        }
        Random random = new Random();
        return drills.get(random.nextInt(drills.size()));
    }

    private int calculateTotalSessionDuration(List<PlanDrill> planDrills) {
        // Calculate total duration from all drills 
        return planDrills.stream()
                .mapToInt(PlanDrill::getDuration)
                .sum();
    }

    private DailyPlanResponse convertToResponse(DailyPlan dailyPlan) {
        DailyPlanResponse response = new DailyPlanResponse();
        response.setPlanId(dailyPlan.getId());
        response.setUserId(dailyPlan.getUser().getId());
        response.setPlanDate(dailyPlan.getPlanDate());
        response.setUserLevel(dailyPlan.getUserLevel());
        response.setPosition(dailyPlan.getPosition());
        
        // Group drills by section
        List<PlanDrill> allPlanDrills = planDrillRepository.findByDailyPlanOrderByOrderIndex(dailyPlan);
        
        response.setSessionDuration(dailyPlan.getSessionDuration());
        
        response.setCompleted(dailyPlan.isCompleted());
        
        response.setWarmUpDrills(convertToDrillResponses(
            allPlanDrills.stream().filter(d -> "WARMUP".equals(d.getSection())).collect(Collectors.toList())));
        response.setCoreDrills(convertToDrillResponses(
            allPlanDrills.stream().filter(d -> "CORE".equals(d.getSection())).collect(Collectors.toList())));
        response.setFitnessDrills(convertToDrillResponses(
            allPlanDrills.stream().filter(d -> "FITNESS".equals(d.getSection())).collect(Collectors.toList())));
        response.setCooldownDrills(convertToDrillResponses(
            allPlanDrills.stream().filter(d -> "COOLDOWN".equals(d.getSection())).collect(Collectors.toList())));

        return response;
    }

    private List<PlanDrillResponse> convertToDrillResponses(List<PlanDrill> planDrills) {
        return planDrills.stream()
                .map(this::convertToDrillResponse)
                .collect(Collectors.toList());
    }

    private PlanDrillResponse convertToDrillResponse(PlanDrill planDrill) {
        PlanDrillResponse response = new PlanDrillResponse();
        Drill drill = planDrill.getDrill();
        response.setDrillId(drill.getId());
        response.setDrillName(drill.getName());
        response.setDrillDescription(drill.getDescription());
        response.setDrillType(planDrill.getDrillType());
        response.setDuration(planDrill.getDuration());
        response.setSection(planDrill.getSection());
        response.setOrderIndex(planDrill.getOrderIndex());
        response.setInstructions(drill.getInstructions());
        response.setEquipment(drill.getEquipment());
        return response;
    }

    public DailyPlanResponse getCurrentPlan(UserInfo user) {
        LocalDate today = LocalDate.now();
        Optional<DailyPlan> plan = dailyPlanRepository.findByUserAndPlanDate(user, today);
        return plan.map(this::convertToResponse).orElse(null);
    }

    public void markPlanAsCompleted(Long planId) {
        Optional<DailyPlan> plan = dailyPlanRepository.findById(planId);
        if (plan.isPresent()) {
            DailyPlan dailyPlan = plan.get();
            dailyPlan.setCompleted(true);
            dailyPlanRepository.save(dailyPlan);
        }
    }


} 