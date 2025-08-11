package com.footwork.api.service;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.footwork.api.entity.UserInfo;
import com.footwork.api.entity.ProfileSetupRequest;
import com.footwork.api.entity.UserUpdateRequest;
import com.footwork.api.entity.PasswordUpdateRequest;
import com.footwork.api.entity.DeleteUserRequest;
import com.footwork.api.entity.DailyPlan;
import com.footwork.api.repository.UserInfoRepository;
import com.footwork.api.repository.DailyPlanRepository;
import com.footwork.api.repository.PlanDrillRepository;
import com.footwork.api.entity.UserProfileResponse;

@Service
public class UserInfoService implements UserDetailsService {

  private final UserInfoRepository repository;
  private final DailyPlanRepository dailyPlanRepository;
  private final PlanDrillRepository planDrillRepository;
  private final PasswordEncoder passwordEncoder;

  public UserInfoService(UserInfoRepository repository, DailyPlanRepository dailyPlanRepository, 
                        PlanDrillRepository planDrillRepository, PasswordEncoder passwordEncoder) {
    this.repository = repository;
    this.dailyPlanRepository = dailyPlanRepository;
    this.planDrillRepository = planDrillRepository;
    this.passwordEncoder = passwordEncoder;
  }

  @Override
  public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
    Optional<UserInfo> userDetail = repository.findByEmail(email);
    if (userDetail.isEmpty()) {
      throw new UsernameNotFoundException("User not found with email: " + email);
    }
    return new UserInfoDetails(userDetail.get());
  }

  public String addUser(UserInfo userInfo) {
    if (repository.existsByEmail(userInfo.getEmail())) {
      throw new RuntimeException("Email already exists");
    }
    userInfo.setPassword(passwordEncoder.encode(userInfo.getPassword()));
    
    // Set default role if not provided
    if (userInfo.getRoles() == null || userInfo.getRoles().trim().isEmpty()) {
      userInfo.setRoles("ROLE_USER");
    }
    
    repository.save(userInfo);
    return "user added to system";
  }

  public List<UserInfo> searchUsers(String query) {
    return repository.findByNameContainingIgnoreCaseOrEmailContainingIgnoreCase(query, query);
  }
  
  public UserInfo setupProfile(String email, ProfileSetupRequest request) {
    Optional<UserInfo> userOptional = repository.findByEmail(email);
    if (userOptional.isEmpty()) {
      throw new UsernameNotFoundException("User not found with email: " + email);
    }
    
    UserInfo user = userOptional.get();
    user.setAge(request.getAge());
    user.setExperienceLevel(request.getExperienceLevel());
    user.setPrimaryPosition(request.getPrimaryPosition());
    user.setProfileCompleted(true);
    
    return repository.save(user);
  }
  
  public UserInfo updateUser(String currentEmail, UserUpdateRequest request) {
    Optional<UserInfo> userOptional = repository.findByEmail(currentEmail);
    if (userOptional.isEmpty()) {
      throw new UsernameNotFoundException("User not found with email: " + currentEmail);
    }
    
    UserInfo user = userOptional.get();
    
    // Only update fields that are provided (not null)
    if (request.getName() != null) {
      user.setName(request.getName());
    }
    
    if (request.getEmail() != null) {
      // Check if new email already exists (if email is being changed)
      if (!currentEmail.equals(request.getEmail())) {
        if (repository.existsByEmail(request.getEmail())) {
          throw new RuntimeException("Email already exists: " + request.getEmail());
        }
      }
      user.setEmail(request.getEmail());
    }
    
    if (request.getRoles() != null) {
      user.setRoles(request.getRoles());
    }
    
    if (request.getAge() != null) {
      user.setAge(request.getAge());
    }
    
    if (request.getExperienceLevel() != null) {
      user.setExperienceLevel(request.getExperienceLevel());
    }
    
    if (request.getPrimaryPosition() != null) {
      user.setPrimaryPosition(request.getPrimaryPosition());
    }
    
    user.setProfileCompleted(true);
    
    return repository.save(user);
  }
  
  public void updatePassword(String email, PasswordUpdateRequest request) {
    Optional<UserInfo> userOptional = repository.findByEmail(email);
    if (userOptional.isEmpty()) {
      throw new UsernameNotFoundException("User not found with email: " + email);
    }
    
    UserInfo user = userOptional.get();
    
    // Verify current password
    if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPassword())) {
      throw new RuntimeException("Current password is incorrect");
    }
    
    // Update password
    user.setPassword(passwordEncoder.encode(request.getNewPassword()));
    repository.save(user);
  }
  
  public UserInfo getUserByEmail(String email) {
    Optional<UserInfo> userOptional = repository.findByEmail(email);
    if (userOptional.isEmpty()) {
      throw new UsernameNotFoundException("User not found with email: " + email);
    }
    return userOptional.get();
  }
  
  @Transactional
  public void deleteUser(String email, DeleteUserRequest request) {
    Optional<UserInfo> userOptional = repository.findByEmail(email);
    if (userOptional.isEmpty()) {
      throw new UsernameNotFoundException("User not found with email: " + email);
    }
    
    UserInfo user = userOptional.get();
    
    // Verify password
    if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
      throw new RuntimeException("Password is incorrect");
    }
    
    // Verify confirmation text
    if (!"DELETE MY ACCOUNT".equals(request.getConfirmation())) {
      throw new RuntimeException("Confirmation text must be exactly 'DELETE MY ACCOUNT'");
    }
    
    // Delete all associated data first (cascading deletion)
    
    // 1. Delete all plan drills associated with user's daily plans
    List<DailyPlan> userPlans = dailyPlanRepository.findByUserOrderByPlanDateDesc(user);
    for (DailyPlan plan : userPlans) {
      planDrillRepository.deleteByDailyPlan(plan);
    }
    
    // 2. Delete all daily plans for the user
    dailyPlanRepository.deleteAll(userPlans);
    
    // 3. Finally delete the user
    repository.delete(user);
  }

  public UserProfileResponse toUserProfileResponse(UserInfo user) {
    return new UserProfileResponse(
      user.getId(),
      user.getName(),
      user.getEmail(),
      user.getRoles(),
      user.getAge(),
      user.getExperienceLevel(),
      user.getPrimaryPosition(),
      user.isProfileCompleted(),
      user.getProfileImageUrl(),
      user.getStreak(),
      user.getLastCompletedDate() != null ? user.getLastCompletedDate().toString() : null
    );
  }

  @Transactional
  public UserInfo updateProfileImage(String email, String profileImageUrl) {
    Optional<UserInfo> userOptional = repository.findByEmail(email);
    if (userOptional.isEmpty()) {
      throw new UsernameNotFoundException("User not found with email: " + email);
    }
    UserInfo user = userOptional.get();
    user.setProfileImageUrl(profileImageUrl);
    return repository.save(user);
  }

  @Transactional
  public void updateUserStreak(UserInfo user) {
    LocalDate today = LocalDate.now();
    
    if (user.getLastCompletedDate() == null) {
      // First time completing a plan
      user.setStreak(1);
      user.setLastCompletedDate(today);
    } else if (user.getLastCompletedDate().equals(today.minusDays(1))) {
      // Consecutive day - increment streak
      user.setStreak(user.getStreak() + 1);
      user.setLastCompletedDate(today);
    } else if (user.getLastCompletedDate().equals(today)) {
      // Already completed today - no change
      return;
    } else {
      // Streak broken - reset to 1
      user.setStreak(1);
      user.setLastCompletedDate(today);
    }
    
    repository.save(user);
  }
}
