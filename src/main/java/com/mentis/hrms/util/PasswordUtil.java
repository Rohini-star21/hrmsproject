package com.mentis.hrms.util;

import org.springframework.stereotype.Component;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Base64;

@Component
public class PasswordUtil {

    // Simple SHA-256 password hashing
    public String encodePassword(String rawPassword) {
        if (rawPassword == null) return null;

        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(rawPassword.getBytes(StandardCharsets.UTF_8));
            String encoded = Base64.getEncoder().encodeToString(hash);

            System.out.println("🔐 [PasswordUtil] encodePassword called");
            System.out.println("   Input: '" + rawPassword + "'");
            System.out.println("   Output (full): '" + encoded + "'");
            System.out.println("   Output length: " + encoded.length());

            return encoded;
        } catch (Exception e) {
            // Fallback to simple Base64 encoding
            System.err.println("[ERROR] SHA-256 failed: " + e.getMessage());
            String fallback = Base64.getEncoder().encodeToString(
                    rawPassword.getBytes(StandardCharsets.UTF_8));
            System.out.println("[DEBUG] Using Base64 fallback: " + fallback);
            return fallback;
        }
    }

    public boolean matchesPassword(String rawPassword, String storedPassword) {
        if (rawPassword == null || storedPassword == null) {
            System.out.println("🔐 [PasswordUtil] Null password comparison");
            return false;
        }

        System.out.println("\n🔐 [PasswordUtil] matchesPassword called");
        System.out.println("   Raw input: '" + rawPassword + "'");
        System.out.println("   Stored: '" + storedPassword + "'");
        System.out.println("   Stored length: " + storedPassword.length());

        String encodedInput = encodePassword(rawPassword);
        boolean matches = encodedInput.equals(storedPassword);

        System.out.println("   Encoded input: '" + encodedInput + "'");
        System.out.println("   Encoded input length: " + encodedInput.length());
        System.out.println("   Match result: " + matches);

        if (!matches && storedPassword.length() < 60) {
            // Try with empty string (for first login)
            String emptyEncoded = encodePassword("");
            boolean emptyMatches = emptyEncoded.equals(storedPassword);
            System.out.println("   Empty password matches? " + emptyMatches);
        }

        return matches;
    }
}