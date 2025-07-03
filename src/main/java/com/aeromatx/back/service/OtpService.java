package com.aeromatx.back.service;

import com.twilio.rest.verify.v2.service.Verification;
import com.twilio.rest.verify.v2.service.VerificationCheck;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class OtpService {

    // ------------ Email OTP (In-Memory) ------------

    private static class OtpEntry {
        String otp;
        long timestamp;

        public OtpEntry(String otp) {
            this.otp = otp;
            this.timestamp = System.currentTimeMillis();
        }
    }

    private final Map<String, OtpEntry> emailOtpMap = new ConcurrentHashMap<>();
    private static final long OTP_EXPIRY_MILLIS = 5 * 60 * 1000; // 5 minutes

    public void generateOtpForEmail(String email) {
        if (email == null || email.isBlank()) return;

        email = email.trim().toLowerCase();
        String otp = String.valueOf(new Random().nextInt(900000) + 100000);
        emailOtpMap.put(email, new OtpEntry(otp));

        System.out.println("OTP for email " + email + " is: " + otp);
        printAllOtps();
    }

    public boolean verifyEmailOtp(String email, String otp) {
        if (email == null || otp == null || email.isBlank() || otp.isBlank()) return false;

        email = email.trim().toLowerCase();
        OtpEntry entry = emailOtpMap.get(email);
        if (entry == null || isExpired(entry.timestamp)) {
            emailOtpMap.remove(email);
            System.out.println("OTP expired or not found for email: " + email);
            return false;
        }

        boolean match = otp.equals(entry.otp);
        if (match) emailOtpMap.remove(email); // one-time use
        return match;
    }

    private boolean isExpired(long timestamp) {
        return System.currentTimeMillis() - timestamp > OTP_EXPIRY_MILLIS;
    }

    public String getOtpForEmail(String email) {
        if (email == null) return null;
        OtpEntry entry = emailOtpMap.get(email.trim().toLowerCase());
        return (entry != null) ? entry.otp : null;
    }

    // ------------ Mobile OTP via Twilio Verify (No 'From' number needed) ------------

    @Value("${twilio.verify.sid}")
    private String verifySid;

    public void generateOtpForMobile(String mobile) {
        if (mobile == null || mobile.isBlank()) return;

        mobile = formatMobile(mobile);
        try {
            Verification verification = Verification.creator(
                    verifySid,
                    mobile,
                    "sms"
            ).create();

            System.out.println("OTP sent to: " + mobile + ", Status: " + verification.getStatus());
        } catch (Exception e) {
            System.err.println("Twilio send OTP failed: " + e.getMessage());
        }

        printAllOtps();
    }

    public boolean verifyMobileOtp(String mobile, String otp) {
        if (mobile == null || otp == null || mobile.isBlank() || otp.isBlank()) return false;

        mobile = formatMobile(mobile);
        try {
            VerificationCheck check = VerificationCheck.creator(
                    verifySid, otp
            ).setTo(mobile).create();

            System.out.println("Verifying OTP for: " + mobile + ", Status: " + check.getStatus());
            return "approved".equalsIgnoreCase(check.getStatus());
        } catch (Exception e) {
            System.err.println("Twilio OTP verification failed: " + e.getMessage());
            return false;
        }
    }

    private String formatMobile(String mobile) {
        mobile = mobile.trim().replaceAll("\\s+", "");
        if (!mobile.startsWith("+")) {
            mobile = "+91" + mobile;
        }
        return mobile;
    }

    // ------------ Custom OTP (Used for Email Change, In-Memory) ------------

    @Autowired
    private SmsService smsService;

    private final Map<String, String> simpleOtpStore = new ConcurrentHashMap<>();

    public String generateSimpleOtp(String mobile) {
        String otp = String.valueOf(new Random().nextInt(900000) + 100000);
        simpleOtpStore.put(mobile, otp);

        try {
            smsService.sendSms(mobile, "Your OTP is: " + otp); // Simulated or real send
        } catch (Exception e) {
            System.err.println("Failed to send SMS: " + e.getMessage());
        }

        System.out.println("Generated simple OTP for mobile: " + mobile + " -> OTP: " + otp);
        printAllOtps();
        return otp;
    }

    public boolean verifySimpleOtp(String mobile, String inputOtp) {
        return simpleOtpStore.containsKey(mobile) && simpleOtpStore.get(mobile).equals(inputOtp);
    }

    // ------------ DEBUG METHOD to print all OTPs ------------
    public void printAllOtps() {
        System.out.println("\n================ CURRENT OTP STATE ================");
        System.out.println(">>> Email OTPs:");
        emailOtpMap.forEach((email, entry) ->
                System.out.println("Email: " + email + " -> OTP: " + entry.otp + " (Created: " + new Date(entry.timestamp) + ")"));

        System.out.println(">>> Simple Mobile OTPs (for email change):");
        simpleOtpStore.forEach((mobile, otp) ->
                System.out.println("Mobile: " + mobile + " -> OTP: " + otp));
        System.out.println("====================================================\n");
    }
}
