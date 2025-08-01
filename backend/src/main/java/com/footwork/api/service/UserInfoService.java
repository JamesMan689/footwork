package com.footwork.api.service;

import java.util.List;
import java.util.Optional;

import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.footwork.api.entity.UserInfo;
import com.footwork.api.entity.ProfileSetupRequest;
import com.footwork.api.repository.UserInfoRepository;
import com.footwork.api.entity.UserProfileResponse;

@Service
public class UserInfoService implements UserDetailsService {

  private final UserInfoRepository repository;
  private final PasswordEncoder passwordEncoder;

  public UserInfoService(UserInfoRepository repository, PasswordEncoder passwordEncoder) {
    this.repository = repository;
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
  
  public UserInfo getUserByEmail(String email) {
    Optional<UserInfo> userOptional = repository.findByEmail(email);
    if (userOptional.isEmpty()) {
      throw new UsernameNotFoundException("User not found with email: " + email);
    }
    return userOptional.get();
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
      user.isProfileCompleted()
    );
  }
}
