package com.aeromatx.back.service;

import com.aeromatx.back.dto.user.EditProfileRequest;
import com.aeromatx.back.dto.user.UserDto;
import com.aeromatx.back.entity.ERole;
import com.aeromatx.back.entity.User;
import com.aeromatx.back.repository.UserRepository;
import com.aeromatx.back.util.PhoneUtil;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class UserService {

    private final UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Transactional(readOnly = true)
    public List<UserDto> getAllUsers() {
        return userRepository.findAll().stream()
                .map(UserDto::fromEntity)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public Optional<UserDto> getUserById(Long id) {
        return userRepository.findById(id)
                .map(UserDto::fromEntity);
    }

    @Transactional(readOnly = true)
    public List<UserDto> findUsersByRole(String roleName) {
        String effectiveRoleName = roleName.toUpperCase();
        if (!effectiveRoleName.startsWith("ROLE_")) {
            effectiveRoleName = "ROLE_" + effectiveRoleName;
        }

        ERole enumRole;
        try {
            enumRole = ERole.valueOf(effectiveRoleName);
        } catch (IllegalArgumentException e) {
            System.err.println("Invalid role name requested: " + roleName + ". Error: " + e.getMessage());
            return Collections.emptyList();
        }

        List<User> users = userRepository.findByRoleName(enumRole);
        return users.stream()
                .map(UserDto::fromEntity)
                .collect(Collectors.toList());
    }

    @Transactional
    public Optional<UserDto> updateUser(Long id, UserDto userDto) {
        return userRepository.findById(id).map(existingUser -> {
            if (userDto.getUsername() != null && !userDto.getUsername().isBlank()) {
                existingUser.setUsername(userDto.getUsername());
            }
            if (userDto.getEmail() != null && !userDto.getEmail().isBlank()) {
                existingUser.setEmail(userDto.getEmail());
            }
            if (userDto.getCompany() != null) {
                existingUser.setCompany(userDto.getCompany());
            }

            User updatedUser = userRepository.save(existingUser);
            return UserDto.fromEntity(updatedUser);
        });
    }

    @Transactional
    public boolean deleteUser(Long id) {
        if (!userRepository.existsById(id)) {
            return false;
        }

        try {
            userRepository.deleteById(id);
            return true;
        } catch (DataIntegrityViolationException e) {
            throw new RuntimeException("Cannot delete user as they have associated records", e);
        }
    }

    public ResponseEntity<?> changePassword(Long userId, String currentPassword, String newPassword) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (!passwordEncoder.matches(currentPassword, user.getPassword())) {
            return ResponseEntity.badRequest().body("Current password is incorrect");
        }

        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);
        return ResponseEntity.ok("Password updated successfully");
    }

    public void updateUserProfile(Long userId, EditProfileRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (user.getEmail() == null || !user.getEmail().equals(request.getEmail())) {
            user.setEmail(request.getEmail());
            user.setEmailVerified(false);
        }

        if (user.getMobile() == null || !user.getMobile().equals(request.getMobileNumber())) {
            user.setMobile(request.getMobileNumber());
            user.setMobileVerified(false);
        }

        user.setUsername(request.getUsername());
        user.setPermanentAddress(request.getPermanentAddress());
        user.setTemporaryAddress(request.getTemporaryAddress());

        userRepository.save(user);
    }

    public void setEmailVerified(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));
        user.setEmailVerified(true);
        userRepository.save(user);
    }

    // public void setMobileVerified(String mobile) {
    //     String dbMobile = PhoneUtil.stripCountryCode(mobile);
    //     User user = userRepository.findByMobile(dbMobile)
    //             .orElseThrow(() -> new RuntimeException("User not found"));
    //     user.setMobileVerified(true);
    //     userRepository.save(user);
    // }

public void setMobileVerified(String mobile) {
    String dbMobile = PhoneUtil.stripCountryCode(mobile);
    System.out.println("Normalized mobile for verification: " + dbMobile);

    User user = userRepository.findByMobile(dbMobile)
            .orElseThrow(() -> new RuntimeException("User not found"));
    user.setMobileVerified(true);
    userRepository.save(user);
}

public List<UserDto> getAllAdmins() {
    return findUsersByRole("ADMIN");
}




}
