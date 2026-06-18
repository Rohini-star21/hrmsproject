package com.mentis.hrms.util;

public class StringCleaner {

    public static String cleanName(String name) {
        if (name == null || name.trim().isEmpty()) {
            return "Candidate";
        }

        String trimmed = name.trim();

        // Remove everything after first comma
        if (trimmed.contains(",")) {
            return trimmed.substring(0, trimmed.indexOf(",")).trim();
        }

        return trimmed;
    }

    public static String cleanEmail(String email) {
        if (email == null || email.trim().isEmpty()) {
            return "candidate@example.com";
        }

        String trimmed = email.trim();

        // Remove everything after first comma or space
        if (trimmed.contains(",")) {
            trimmed = trimmed.substring(0, trimmed.indexOf(",")).trim();
        }

        // Remove all whitespace
        return trimmed.replaceAll("\\s+", "");
    }

    public static String forceCleanName(String name) {
        return cleanName(name);
    }
}