package com.aeromatx.back.dto.user;

import lombok.Data;

@Data

public class EditProfileRequest {
    private String username;
private String email; // New email (needs OTP)
private String mobileNumber; // Needs OTP
private String permanentAddress;
private String temporaryAddress;

}
