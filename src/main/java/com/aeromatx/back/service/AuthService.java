package com.aeromatx.back.service;

import com.aeromatx.back.dto.auth.JwtResponse;
import com.aeromatx.back.dto.auth.LoginRequest;
import com.aeromatx.back.dto.auth.RegisterRequest;
import com.aeromatx.back.entity.ERole;
import com.aeromatx.back.entity.Role;
import com.aeromatx.back.entity.User;
import com.aeromatx.back.entity.RegistrationOtp;
import com.aeromatx.back.exception.EmailAlreadyInUseException;
import com.aeromatx.back.exception.RoleNotFoundException;
import com.aeromatx.back.exception.UsernameAlreadyExistsException;
import com.aeromatx.back.exception.OtpMismatchException;
import com.aeromatx.back.exception.OtpExpiredException;
import com.aeromatx.back.repository.RoleRepository;
import com.aeromatx.back.repository.UserRepository;
import com.aeromatx.back.repository.RegistrationOtpRepository;
import com.aeromatx.back.security.UserDetailsImpl;
import com.aeromatx.back.util.JwtUtil;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

@Service
@Transactional
public class AuthService {

    private final AuthenticationManager authenticationManager;
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder encoder;
    private final JwtUtil jwtUtil;
    private final EmailService emailService;
    private final RegistrationOtpRepository registrationOtpRepository;

    private final long TOKEN_VALIDITY_MINUTES = 15;
    private final long OTP_VALIDITY_MINUTES = 5;

    public AuthService(AuthenticationManager authenticationManager,
                       UserRepository userRepository,
                       RoleRepository roleRepository,
                       PasswordEncoder encoder,
                       JwtUtil jwtUtil,
                       EmailService emailService,
                       RegistrationOtpRepository registrationOtpRepository) {
        this.authenticationManager = authenticationManager;
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.encoder = encoder;
        this.jwtUtil = jwtUtil;
        this.emailService = emailService;
        this.registrationOtpRepository = registrationOtpRepository;
    }

    public void initiateRegistrationWithOtp(RegisterRequest registerRequest) {
        if (userRepository.existsByUsername(registerRequest.getUsername())) {
            throw new UsernameAlreadyExistsException("Username is already taken!");
        }
        if (userRepository.existsByEmail(registerRequest.getEmail())) {
            throw new EmailAlreadyInUseException("Email is already in use by an existing account!");
        }

        String otp = String.format("%06d", ThreadLocalRandom.current().nextInt(1000000));
        LocalDateTime expiryTime = LocalDateTime.now().plusMinutes(OTP_VALIDITY_MINUTES);

        Optional<RegistrationOtp> existingOtp = registrationOtpRepository.findByEmail(registerRequest.getEmail());
        RegistrationOtp registrationOtp = existingOtp.orElseGet(RegistrationOtp::new);
        registrationOtp.setEmail(registerRequest.getEmail());
        registrationOtp.setOtp(otp);
        registrationOtp.setExpiryDate(expiryTime);
        registrationOtp.setUsername(registerRequest.getUsername());
        registrationOtp.setPassword(encoder.encode(registerRequest.getPassword()));
        registrationOtp.setCompany(registerRequest.getCompany());
        registrationOtp.setRole(registerRequest.getRole());

        registrationOtpRepository.save(registrationOtp);
        emailService.sendOtpEmail(registerRequest.getEmail(), otp);
    }

    public String completeRegistration(String email, String otp) {
        RegistrationOtp storedOtpData = registrationOtpRepository.findByEmail(email)
                .orElseThrow(() -> new OtpMismatchException("Invalid or no pending registration for this email."));

        if (storedOtpData.getExpiryDate().isBefore(LocalDateTime.now())) {
            registrationOtpRepository.delete(storedOtpData);
            throw new OtpExpiredException("OTP has expired. Please request a new one.");
        }

        if (!storedOtpData.getOtp().equals(otp)) {
            throw new OtpMismatchException("Invalid OTP. Please try again.");
        }

        User user = new User(
                storedOtpData.getUsername(),
                storedOtpData.getEmail(),
                storedOtpData.getPassword(),
                storedOtpData.getCompany()
        );

        String strRole = storedOtpData.getRole();
        Set<Role> roles = new HashSet<>();

        if (strRole == null || strRole.isEmpty()) {
            Role customerRole = roleRepository.findByName(ERole.ROLE_CUSTOMER)
                    .orElseThrow(() -> new RoleNotFoundException("Error: Default role not found."));
            roles.add(customerRole);
        } else {
            switch (strRole.toLowerCase()) {
                case "customer":
                    roles.add(roleRepository.findByName(ERole.ROLE_CUSTOMER)
                            .orElseThrow(() -> new RoleNotFoundException("Role CUSTOMER not found")));
                    break;
                case "oem":
                    roles.add(roleRepository.findByName(ERole.ROLE_OEM)
                            .orElseThrow(() -> new RoleNotFoundException("Role OEM not found")));
                    break;
                case "admin":
                    roles.add(roleRepository.findByName(ERole.ROLE_ADMIN)
                            .orElseThrow(() -> new RoleNotFoundException("Role ADMIN not found")));
                    break;
                default:
                    throw new IllegalArgumentException("Invalid role specified: " + strRole);
            }
        }

        // Enforce only one main admin
        if (strRole.equalsIgnoreCase("admin")) {
            boolean anyMainAdminExists = userRepository.existsByIsMainAdminTrue();
            if (!anyMainAdminExists) {
                user.setMainAdmin(true);
            }
        }

        user.setRoles(roles);
        userRepository.save(user);
        registrationOtpRepository.delete(storedOtpData);
        return user.getUsername();
    }

    public JwtResponse authenticateUser(LoginRequest loginRequest) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(loginRequest.getEmail(), loginRequest.getPassword()));

        SecurityContextHolder.getContext().setAuthentication(authentication);
        String jwt = jwtUtil.generateJwtToken(authentication);

        UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();
        List<String> roles = userDetails.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.toList());

        return new JwtResponse(jwt,
                userDetails.getId(),
                userDetails.getUsername(),
                userDetails.getEmail(),
                userDetails.getMobile(),
                userDetails.getPermanentAddress(),
                roles,
                userDetails.isMainAdmin()
        );
    }

    public boolean initiatePasswordReset(String email) {
        return userRepository.findByEmail(email).map(user -> {
            String resetToken = UUID.randomUUID().toString();
            LocalDateTime expiryDate = LocalDateTime.now().plusMinutes(TOKEN_VALIDITY_MINUTES);

            user.setResetPasswordToken(resetToken);
            user.setResetPasswordExpiryDate(expiryDate);
            userRepository.save(user);
            emailService.sendPasswordResetEmail(email, resetToken);
            return true;
        }).orElseGet(() -> true);
    }

    public boolean resetPassword(String token, String newPassword) {
        return userRepository.findByResetPasswordToken(token).map(user -> {
            if (user.getResetPasswordExpiryDate() == null || user.getResetPasswordExpiryDate().isBefore(LocalDateTime.now())) {
                user.setResetPasswordToken(null);
                user.setResetPasswordExpiryDate(null);
                userRepository.save(user);
                return false;
            }
            user.setPassword(encoder.encode(newPassword));
            user.setResetPasswordToken(null);
            user.setResetPasswordExpiryDate(null);
            userRepository.save(user);
            return true;
        }).orElseGet(() -> false);
    }
}
