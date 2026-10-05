package com.h8.ems.incident.util;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

/**
 * Utility for hashing caller phone numbers with salt (Rule #6).
 * No raw phone numbers or patient PII are ever persisted.
 */
public final class CallerHashUtil {

    private static final String DEFAULT_SALT = "H8_EMS_PLATFORM_SALT_2026";

    private CallerHashUtil() {}

    public static String hashPhoneNumber(String phone) {
        return hashPhoneNumber(phone, DEFAULT_SALT);
    }

    public static String hashPhoneNumber(String phone, String salt) {
        if (phone == null || phone.isBlank()) {
            return null;
        }
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            md.update(salt.getBytes(StandardCharsets.UTF_8));
            byte[] digest = md.digest(phone.trim().getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 algorithm not available", e);
        }
    }
}
