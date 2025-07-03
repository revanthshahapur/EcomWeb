package com.aeromatx.back.dto.auth;

public class ChangePasswordRequest {
    private String currentPassword;
    private String newPassword;
    //private String otp;

    // Getters and Setters
    public String getCurrentPassword() {
        return currentPassword;
    }

    public void setCurrentPassword(String currentPassword) {
        this.currentPassword = currentPassword;
    }

    public String getNewPassword() {
        return newPassword;
    }

    public void setNewPassword(String newPassword) {
        this.newPassword = newPassword;
    }

    // public String getOtp() {
    //     return otp;
    // }

    // public void setOtp(String otp) {
    //     this.otp = otp;
    // }
}