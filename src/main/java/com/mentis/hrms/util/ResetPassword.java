package com.mentis.hrms.util;

public class ResetPassword {
    public static void main(String[] args) {
        // Create PasswordUtil instance
        PasswordUtil passwordUtil = new PasswordUtil();

        // Choose a simple password
        String newPassword = "admin123";

        // Generate the hash
        String generatedHash = passwordUtil.encodePassword(newPassword);

        // Print the results
        System.out.println("==========================================");
        System.out.println("PASSWORD RESET UTILITY");
        System.out.println("==========================================");
        System.out.println("Employee ID: SA001");
        System.out.println("New Password: " + newPassword);
        System.out.println("\nGenerated Hash:");
        System.out.println(generatedHash);
        System.out.println("\nCopy the SQL command below:");
        System.out.println("==========================================");
        System.out.println("\nRUN THIS IN MYSQL:");
        System.out.println("------------------------------------------");
        System.out.println("UPDATE employee SET password = '" + generatedHash + "' WHERE employee_id = 'SA001';");
        System.out.println("------------------------------------------");

        // Verify the hash works
        boolean verify = passwordUtil.matchesPassword(newPassword, generatedHash);
        System.out.println("\nVerification (should be true): " + verify);
        System.out.println("==========================================");
    }
}