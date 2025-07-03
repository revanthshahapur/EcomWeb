package com.aeromatx.back.controller;

import com.aeromatx.back.dto.user.EditProfileRequest;
import com.aeromatx.back.dto.user.EmailChangeRequest;
import com.aeromatx.back.dto.user.UserDto;
import com.aeromatx.back.security.UserDetailsImpl;
import com.aeromatx.back.service.OtpService;
import com.aeromatx.back.service.UserService;
import com.aeromatx.back.util.PhoneUtil;
import com.aeromatx.back.entity.User;
import com.aeromatx.back.repository.UserRepository;

import java.util.Map;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/users")
@CrossOrigin(origins = "http://localhost:8080")
public class UserController {

    private final UserService userService;

    @Autowired
    private OtpService otpService;

    @Autowired
    private UserRepository userRepository;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<UserDto>> getAllUsers() {
        List<UserDto> users = userService.getAllUsers();
        return ResponseEntity.ok(users);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN') or #id == authentication.principal.id")
    public ResponseEntity<UserDto> getUserById(@PathVariable Long id) {
        return userService.getUserById(id)
                .map(ResponseEntity::ok)
                .orElse(new ResponseEntity<>(HttpStatus.NOT_FOUND));
    }

    @GetMapping("/by-role")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<UserDto>> getUsersByRole(@RequestParam String role) {
        List<UserDto> users = userService.findUsersByRole(role);
        if (users.isEmpty()) {
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.ok(users);
    }

    @GetMapping("/customers")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<UserDto>> getCustomers() {
        return ResponseEntity.ok(userService.findUsersByRole("CUSTOMER"));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteUser(@PathVariable Long id) {
        boolean deleted = userService.deleteUser(id);
        return deleted ? ResponseEntity.ok().build() : ResponseEntity.notFound().build();
    }

    @PutMapping("/profile")
    @PreAuthorize("hasAnyRole('CUSTOMER', 'OEM', 'ADMIN')")
    public ResponseEntity<?> updateUserProfile(
            @AuthenticationPrincipal UserDetailsImpl userDetails,
            @RequestBody EditProfileRequest request
    ) {
        try {
            userService.updateUserProfile(userDetails.getId(), request);
            return ResponseEntity.ok("Profile updated successfully. If you changed email or mobile, please verify via OTP.");
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Error updating profile: " + e.getMessage());
        }
    }

    @GetMapping("/profile")
    @PreAuthorize("hasAnyRole('CUSTOMER', 'OEM', 'ADMIN')")
    public ResponseEntity<UserDto> getCurrentUserProfile() {
        Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();

        if (principal instanceof UserDetailsImpl userDetails) {
            return userService.getUserById(userDetails.getId())
                    .map(ResponseEntity::ok)
                    .orElse(ResponseEntity.status(HttpStatus.NOT_FOUND).build());
        } else {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
    }

    @PostMapping("/send-otp/mobile")
    @PreAuthorize("permitAll()")
    public ResponseEntity<String> sendMobileOtp(@RequestParam String mobile) {
        otpService.generateOtpForMobile(mobile);
        return ResponseEntity.ok("OTP sent to: " + mobile);
    }

    @PostMapping("/send-otp/email")
    @PreAuthorize("permitAll()")
    public ResponseEntity<String> sendEmailOtp(@RequestParam String email) {
        otpService.generateOtpForEmail(email);
        return ResponseEntity.ok("OTP sent to: " + email);
    }

    @PutMapping("/verify/email")
    public ResponseEntity<String> verifyEmailOtp(@RequestParam String email, @RequestParam String otp) {
        boolean isVerified = otpService.verifyEmailOtp(email, otp);
        if (isVerified) {
            userService.setEmailVerified(email);
            return ResponseEntity.ok("Email verified successfully.");
        }
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Invalid or expired OTP.");
    }

    @PutMapping("/verify/mobile")
    public ResponseEntity<String> verifyMobileOtp(@RequestParam String mobile, @RequestParam String otp) {
        String cleanedMobile = PhoneUtil.stripCountryCode(mobile);

        boolean isVerified = otpService.verifyMobileOtp(cleanedMobile, otp);
        if (isVerified) {
            userService.setMobileVerified(cleanedMobile);
            return ResponseEntity.ok("Mobile verified successfully.");
        }
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Invalid or expired OTP.");
    }

    @GetMapping("/test/send-otp")
    public ResponseEntity<String> testSendOtp(@RequestParam String email, @RequestParam String mobile) {
        otpService.generateOtpForEmail(email);
        otpService.generateOtpForMobile(mobile);
        return ResponseEntity.ok("Test OTPs generated.");
    }

    @GetMapping("/resend-otp")
    public ResponseEntity<String> resendOtp(@RequestParam String type, @RequestParam String value) {
        if (type.equals("email")) {
            otpService.generateOtpForEmail(value);
        } else if (type.equals("mobile")) {
            otpService.generateOtpForMobile(value);
        } else {
            return ResponseEntity.badRequest().body("Invalid type");
        }
        return ResponseEntity.ok("OTP resent");
    }

    @GetMapping("/check-mobile-verification")
    public ResponseEntity<?> checkMobileVerification(Authentication authentication) {
        UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();
        User user = userRepository.findById(userDetails.getId())
                .orElseThrow(() -> new RuntimeException("User not found"));

        boolean isMobileVerified = Boolean.TRUE.equals(user.getMobileVerified());
        return ResponseEntity.ok(Map.of("mobileVerified", isMobileVerified));
    }

    @PostMapping("/email-change/send-otp")
    public ResponseEntity<?> sendOtpForEmailChange(Authentication authentication) {
        UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();
        User user = userRepository.findById(userDetails.getId())
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (!Boolean.TRUE.equals(user.getMobileVerified())) {
            return ResponseEntity.badRequest().body("Mobile number not verified");
        }

        String otp = otpService.generateSimpleOtp(user.getMobile());
        System.out.println("OTP for email change to mobile " + user.getMobile() + ": " + otp);

        return ResponseEntity.ok(Map.of("message", "OTP sent to registered mobile"));
    }

    @PostMapping("/email-change/verify-otp")
    public ResponseEntity<?> verifyOtpAndChangeEmail(@RequestBody EmailChangeRequest request, Authentication authentication) {
        UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();
        User user = userRepository.findById(userDetails.getId())
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (!otpService.verifySimpleOtp(user.getMobile(), request.getOtp())) {
            return ResponseEntity.badRequest().body("Invalid or expired OTP");
        }

        user.setEmail(request.getNewEmail());
        user.setEmailVerified(false);
        userRepository.save(user);

        return ResponseEntity.ok(Map.of("message", "Profile updated successfully."));
    }
}
