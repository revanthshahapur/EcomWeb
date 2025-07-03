package com.aeromatx.back.controller;


import com.aeromatx.back.entity.ERole;
import com.aeromatx.back.entity.User;
import com.aeromatx.back.repository.UserRepository;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;

import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin")
@PreAuthorize("hasRole('ADMIN')")
public class CustomerController {

    @Autowired
    private UserRepository userRepository;

    @GetMapping("/customers")
    public ResponseEntity<List<User>> getAllCustomers() {
        List<User> customers = userRepository.findByRoleName(ERole.ROLE_CUSTOMER);
        return ResponseEntity.ok(customers);
    }
}
