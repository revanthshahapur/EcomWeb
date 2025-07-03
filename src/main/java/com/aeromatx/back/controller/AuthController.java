package com.aeromatx.back.controller;

import com.aeromatx.back.dto.auth.JwtResponse;
import com.aeromatx.back.dto.auth.LoginRequest;
import com.aeromatx.back.dto.auth.RegisterRequest;
import com.aeromatx.back.dto.auth.VerificationRequest;
import com.aeromatx.back.dto.response.MessageResponse;
import com.aeromatx.back.security.UserDetailsImpl;
import com.aeromatx.back.service.AuthService;
import com.aeromatx.back.service.EmailService;
import com.aeromatx.back.util.JwtUtil;

import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;
    private final EmailService emailService;

    @Autowired
    private AuthenticationManager authenticationManager;

    @Autowired
    private JwtUtil jwtUtil;

    public AuthController(AuthService authService, EmailService emailService) {
        this.authService = authService;
        this.emailService = emailService;
    }

    @PostMapping("/register")
    public ResponseEntity<Map<String, String>> initiateRegistration(@Valid @RequestBody RegisterRequest registerRequest) {
        try {
            authService.initiateRegistrationWithOtp(registerRequest);
            Map<String, String> responseBody = new HashMap<>();
            responseBody.put("message", "OTP sent to your email. Please verify to complete registration.");
            return new ResponseEntity<>(responseBody, HttpStatus.OK);
        } catch (RuntimeException e) {
            Map<String, String> errorBody = new HashMap<>();
            errorBody.put("message", e.getMessage());
            return new ResponseEntity<>(errorBody, HttpStatus.BAD_REQUEST);
        }
    }

    @PostMapping("/register/verify-otp")
    public ResponseEntity<Map<String, String>> verifyRegistrationOtp(@Valid @RequestBody VerificationRequest verificationRequest) {
        try {
            String registeredUsername = authService.completeRegistration(verificationRequest.getEmail(), verificationRequest.getOtp());
            emailService.sendRegistrationEmail(verificationRequest.getEmail(), registeredUsername);
            Map<String, String> responseBody = new HashMap<>();
            responseBody.put("message", "User registered successfully!");
            return new ResponseEntity<>(responseBody, HttpStatus.CREATED);
        } catch (RuntimeException e) {
            Map<String, String> errorBody = new HashMap<>();
            errorBody.put("message", e.getMessage());
            return new ResponseEntity<>(errorBody, HttpStatus.BAD_REQUEST);
        }
    }

    @PostMapping("/login")
    public ResponseEntity<?> authenticateUser(@Valid @RequestBody LoginRequest loginRequest) {
        try {
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            loginRequest.getEmail(),
                            loginRequest.getPassword()
                    )
            );

            UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();
            String jwt = jwtUtil.generateJwtToken(authentication);

            List<String> roles = userDetails.getAuthorities().stream()
                    .map(GrantedAuthority::getAuthority)
                    .collect(Collectors.toList());

            return ResponseEntity.ok(new JwtResponse(
                    jwt,
                    userDetails.getId(),
                    userDetails.getUsername(),
                    userDetails.getEmail(),
                    userDetails.getMobile(),            // Added mobile
                    userDetails.getPermanentAddress(),   // Added permanent address
                    roles,
                    userDetails.isMainAdmin()
            ));

        } catch (RuntimeException e) {
            Map<String, String> errorResponse = Map.of(
                    "error", "Authentication Failed",
                    "message", "Invalid email or password."
            );
            return new ResponseEntity<>(errorResponse, HttpStatus.UNAUTHORIZED);
        }
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<Map<String, String>> forgotPassword(@Valid @RequestBody com.aeromatx.back.dto.auth.ForgotPasswordRequest request) {
        authService.initiatePasswordReset(request.getEmail());
        Map<String, String> responseBody = new HashMap<>();
        responseBody.put("message", "If an account with that email exists, a password reset link has been sent to your inbox.");
        return ResponseEntity.ok(responseBody);
    }

    @PostMapping("/reset-password")
    public ResponseEntity<Map<String, String>> resetPassword(@Valid @RequestBody com.aeromatx.back.dto.auth.ResetPasswordRequest request) {
        if (request.getNewPassword() == null || request.getNewPassword().length() < 8) {
            Map<String, String> errorBody = new HashMap<>();
            errorBody.put("message", "New password must be at least 8 characters long.");
            return ResponseEntity.badRequest().body(errorBody);
        }

        boolean resetSuccess = authService.resetPassword(request.getToken(), request.getNewPassword());

        if (resetSuccess) {
            Map<String, String> responseBody = new HashMap<>();
            responseBody.put("message", "Your password has been successfully reset.");
            return ResponseEntity.ok(responseBody);
        } else {
            Map<String, String> errorBody = new HashMap<>();
            errorBody.put("message", "Invalid or expired password reset token. Please try again.");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorBody);
        }
    }

    @PostMapping("/admin-login")
    public ResponseEntity<?> authenticateAdmin(@RequestBody LoginRequest loginRequest) {
        try {
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            loginRequest.getEmail(),
                            loginRequest.getPassword()
                    )
            );

            UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();

            boolean isAdmin = userDetails.getAuthorities().stream()
                    .anyMatch(role -> role.getAuthority().equals("ROLE_ADMIN"));

            if (!isAdmin) {
                return ResponseEntity
                        .status(HttpStatus.FORBIDDEN)
                        .body(new MessageResponse("Error: Admin access only"));
            }

            String jwt = jwtUtil.generateJwtToken(authentication);
            List<String> roles = userDetails.getAuthorities().stream()
                    .map(GrantedAuthority::getAuthority)
                    .collect(Collectors.toList());

            return ResponseEntity.ok(new JwtResponse(
                    jwt,
                    userDetails.getId(),
                    userDetails.getUsername(),
                    userDetails.getEmail(),
                    userDetails.getMobile(),            // Admin's mobile
                    userDetails.getPermanentAddress(),   // Admin's address
                    roles,
                    userDetails.isMainAdmin()
            ));

        } catch (AuthenticationException e) {
            return ResponseEntity
                    .status(HttpStatus.UNAUTHORIZED)
                    .body(new MessageResponse("Error: Invalid credentials"));
        }
    }
}
