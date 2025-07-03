package com.aeromatx.back.util;

public class PhoneUtil {

    public static String stripCountryCode(String mobile) {
        if (mobile == null) return null;

        String original = mobile; // for logging
        mobile = mobile.trim().replaceAll("[^0-9]", ""); // remove non-digit chars

        // Remove leading 91 if longer than 10 digits
        if (mobile.length() > 10 && mobile.startsWith("91")) {
            System.out.println("Stripping country code from: " + original + " => " + mobile);
            mobile = mobile.substring(2);
        } else {
            System.out.println("Cleaned mobile: " + original + " => " + mobile);
        }

        return mobile;
    }
}
