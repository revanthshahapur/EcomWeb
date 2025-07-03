package com.aeromatx.back.dto.auth;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class JwtResponse {
    private String token;
    private String type = "Bearer";
    private Long id;
    private String username;
    private String email;
    private String mobile;             // ✅ New field
    private String permanentAddress;   // ✅ New field
    private List<String> roles;
     private boolean isMainAdmin;       // ✅ Flag for frontend access control


    public JwtResponse(String token, Long id, String username, String email,
                       String mobile, String permanentAddress, List<String> roles, boolean isMainAdmin) {
        this.token = token;
        this.id = id;
        this.username = username;
        this.email = email;
        this.mobile = mobile;
        this.permanentAddress = permanentAddress;
        this.roles = roles;
        this.isMainAdmin = isMainAdmin;
    }
}
