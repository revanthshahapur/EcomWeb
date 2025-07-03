package com.aeromatx.back.dto.user;

import com.aeromatx.back.entity.User;

public class UserDto {
    private Long id;
    private String username;
    private String email;
    private String mobile;
    private String permanentAddress;
    private String temporaryAddress;
    private String company;
    private boolean emailVerified;
    private boolean mobileVerified;

    public UserDto() {}

    public UserDto(Long id, String username, String email, String company,
                   String mobile, String permanentAddress, String temporaryAddress,
                   boolean emailVerified, boolean mobileVerified) {
        this.id = id;
        this.username = username;
        this.email = email;
        this.company = company;
        this.mobile = mobile;
        this.permanentAddress = permanentAddress;
        this.temporaryAddress = temporaryAddress;
        this.emailVerified = emailVerified;
        this.mobileVerified = mobileVerified;
    }

    public static UserDto fromEntity(User user) {
        if (user == null) {
            return null;
        }
        UserDto dto = new UserDto();
        dto.setId(user.getId());
        dto.setUsername(user.getUsername());
        dto.setEmail(user.getEmail());
        dto.setCompany(user.getCompany());
        dto.setMobile(user.getMobile());
        dto.setPermanentAddress(user.getPermanentAddress());
        dto.setTemporaryAddress(user.getTemporaryAddress());
        dto.setEmailVerified(user.getEmailVerified());
        dto.setMobileVerified(user.getMobileVerified());
        return dto;
    }

    // Getters
    public Long getId() { return id; }
    public String getUsername() { return username; }
    public String getEmail() { return email; }
    public String getCompany() { return company; }
    public String getMobile() { return mobile; }
    public String getPermanentAddress() { return permanentAddress; }
    public String getTemporaryAddress() { return temporaryAddress; }
    public boolean isEmailVerified() { return emailVerified; }
    public boolean isMobileVerified() { return mobileVerified; }

    // Setters
    public void setId(Long id) { this.id = id; }
    public void setUsername(String username) { this.username = username; }
    public void setEmail(String email) { this.email = email; }
    public void setCompany(String company) { this.company = company; }
    public void setMobile(String mobile) { this.mobile = mobile; }
    public void setPermanentAddress(String permanentAddress) { this.permanentAddress = permanentAddress; }
    public void setTemporaryAddress(String temporaryAddress) { this.temporaryAddress = temporaryAddress; }
    public void setEmailVerified(boolean emailVerified) { this.emailVerified = emailVerified; }
    public void setMobileVerified(boolean mobileVerified) { this.mobileVerified = mobileVerified; }
}
