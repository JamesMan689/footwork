package com.footwork.api.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.footwork.api.entity.UserInfo;

public interface UserInfoRepository extends JpaRepository<UserInfo, Integer> {
  Optional<UserInfo> findByEmail(String email);

  boolean existsByEmail(String email);

  List<UserInfo> findByNameContainingIgnoreCase(String query);

  List<UserInfo> findByNameContainingIgnoreCaseOrEmailContainingIgnoreCase(String name, String email);
}
