package com.mentis.hrms.util;

/**
 * Utility to generate BCrypt password hashes for initial user setup
 * Run this ONCE to get hashes, then paste into SQL
 */
public class PasswordGenerator {

    private static final PasswordUtil passwordUtil = new PasswordUtil();

    public static void main(String[] args) {
        System.out.println("========================================");
        System.out.println("GENERATED PASSWORD HASHES");
        System.out.println("========================================");

        generateAndPrint("Test@123", "HR001 and SA001");

        System.out.println("========================================");
        System.out.println("COPY THE HASH ABOVE AND RUN IN MYSQL");
        System.out.println("========================================");
    }

    private static void generateAndPrint(String password, String user) {
        String hash = passwordUtil.encodePassword(password);
        System.out.println("Password: " + password);
        System.out.println("Hash length: " + hash.length() + " chars");
        System.out.println("Hash: " + hash);
        System.out.println("---");
    }
}