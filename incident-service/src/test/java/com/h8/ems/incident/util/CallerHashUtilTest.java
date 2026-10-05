package com.h8.ems.incident.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class CallerHashUtilTest {

    @Test
    void hashesPhoneNumberConsistently() {
        String phone = "+442079460000";
        String hash1 = CallerHashUtil.hashPhoneNumber(phone);
        String hash2 = CallerHashUtil.hashPhoneNumber(phone);

        assertNotNull(hash1);
        assertEquals(64, hash1.length(), "SHA-256 hash must be 64 hex characters");
        assertEquals(hash1, hash2, "Same phone must produce identical hash");
    }

    @Test
    void differentPhonesProduceDifferentHashes() {
        String hash1 = CallerHashUtil.hashPhoneNumber("+442079460001");
        String hash2 = CallerHashUtil.hashPhoneNumber("+442079460002");

        assertNotEquals(hash1, hash2);
    }

    @Test
    void handlesNullAndBlank() {
        assertNull(CallerHashUtil.hashPhoneNumber(null));
        assertNull(CallerHashUtil.hashPhoneNumber(""));
        assertNull(CallerHashUtil.hashPhoneNumber("   "));
    }

    @Test
    void customSaltAltersHash() {
        String phone = "+442079460000";
        String hashDefault = CallerHashUtil.hashPhoneNumber(phone);
        String hashCustom = CallerHashUtil.hashPhoneNumber(phone, "CUSTOM_SALT_XYZ");

        assertNotEquals(hashDefault, hashCustom, "Custom salt must alter hash output");
    }
}
