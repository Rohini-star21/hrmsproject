package com.mentis.hrms.controller;

import com.mentis.hrms.model.Attendance;
import com.mentis.hrms.model.Employee;
import com.mentis.hrms.service.AttendanceService;  // ADD THIS IMPORT
import com.mentis.hrms.service.EmployeeService;
import com.mentis.hrms.util.PasswordUtil;
import jakarta.servlet.http.HttpSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

@Controller
@RequestMapping("/candidate/auth")
public class CandidateAuthController {

    private static final Logger logger = LoggerFactory.getLogger(CandidateAuthController.class);

    @Autowired
    private EmployeeService employeeService;

    @Autowired
    private PasswordUtil passwordUtil;

    // ADD THIS FIELD
    @Autowired
    private AttendanceService attendanceService;

    /* ==================== LOGIN METHOD UPDATED ==================== */
    @PostMapping("/login")
    public String processLogin(
            @RequestParam String employeeId,
            @RequestParam(required = false) String password,
            jakarta.servlet.http.HttpServletRequest request,
            RedirectAttributes ra) {

        // ========== STEP 1: INITIAL DEBUG ==========
        System.out.println("\n🔴🔴🔴🔴🔴🔴🔴🔴🔴🔴🔴🔴🔴🔴🔴🔴🔴🔴🔴🔴🔴🔴🔴🔴");
        System.out.println("🔴 LOGIN ATTEMPT - " + new java.util.Date());
        System.out.println("🔴🔴🔴🔴🔴🔴🔴🔴🔴🔴🔴🔴🔴🔴🔴🔴🔴🔴🔴🔴🔴🔴🔴🔴");
        System.out.println("Employee ID entered: '" + employeeId + "'");
        System.out.println("Password entered: '" + (password != null ? password : "NULL") + "'");
        System.out.println("Password length: " + (password != null ? password.length() : 0));
        System.out.println("Request URL: " + request.getRequestURL());
        System.out.println("Request Method: " + request.getMethod());

        logger.info("=== LOGIN ATTEMPT: {} ===", employeeId);

        try {
            // ========== STEP 2: FETCH EMPLOYEE ==========
            System.out.println("\n📋 STEP 2: Fetching employee from database...");
            Optional<Employee> employeeOpt = employeeService.getEmployeeByEmployeeId(employeeId);

            if (employeeOpt.isEmpty()) {
                System.out.println("❌ EMPLOYEE NOT FOUND in database!");

                // Debug: List all employees to see what's available
                System.out.println("\n📋 DEBUG: All employees in database:");
                var allEmployees = employeeService.getAllEmployees();
                for (Employee emp : allEmployees) {
                    System.out.println("   - ID: '" + emp.getEmployeeId() + "', Role: " + emp.getRole() +
                            ", Active: " + emp.isActive() +
                            ", Has Password: " + (emp.getPassword() != null));
                }

                ra.addFlashAttribute("error", "No account found with this Employee ID");
                return "redirect:/candidate/login";
            }

            Employee employee = employeeOpt.get();

            // ========== STEP 3: EMPLOYEE FOUND - PRINT DETAILS ==========
            System.out.println("\n✅ EMPLOYEE FOUND IN DATABASE:");
            System.out.println("   - Employee ID from DB: '" + employee.getEmployeeId() + "'");
            System.out.println("   - First Name: " + employee.getFirstName());
            System.out.println("   - Last Name: " + employee.getLastName());
            System.out.println("   - Role: " + employee.getRole());
            System.out.println("   - Role name(): " + employee.getRole().name());
            System.out.println("   - Is Active: " + employee.isActive());
            System.out.println("   - Password from DB: '" + employee.getPassword() + "'");
            System.out.println("   - Password length: " + (employee.getPassword() != null ? employee.getPassword().length() : 0));
            System.out.println("   - Password is null? " + (employee.getPassword() == null));
            System.out.println("   - Password is empty? " + (employee.getPassword() != null && employee.getPassword().isEmpty()));

            // ========== STEP 4: CHECK ACCOUNT STATUS ==========
            System.out.println("\n📋 STEP 4: Checking account status...");
            if (!employee.isActive() && employee.getPassword() != null) {
                System.out.println("❌ Account is DEACTIVATED!");
                ra.addFlashAttribute("error", "Your account is deactivated. Please contact HR.");
                return "redirect:/candidate/login";
            }
            System.out.println("✅ Account is active");

            // ========== STEP 5: CHECK FOR FIRST TIME LOGIN ==========
            System.out.println("\n📋 STEP 5: Checking if first time login...");
            if (employee.getPassword() == null || employee.getPassword().isEmpty()) {
                System.out.println("✅ First time login detected (no password set)");
                jakarta.servlet.http.HttpSession tempSession = request.getSession(true);
                tempSession.setAttribute("tempEmployeeId", employee.getEmployeeId());
                return "redirect:/candidate/create-password";
            }
            System.out.println("✅ Password exists in database");

            // ========== STEP 6: VALIDATE PASSWORD INPUT ==========
            System.out.println("\n📋 STEP 6: Validating password input...");
            if (password == null || password.trim().isEmpty()) {
                System.out.println("❌ Password is empty or null");
                ra.addFlashAttribute("error", "Please enter your password");
                return "redirect:/candidate/login";
            }
            System.out.println("✅ Password input is valid: '" + password + "'");

            // ========== STEP 7: CHECK PASSWORD MATCH ==========
            System.out.println("\n📋 STEP 7: Checking password match...");
            System.out.println("   - Calling passwordUtil.matchesPassword()");
            System.out.println("   - Raw password: '" + password + "'");
            System.out.println("   - Stored hash: '" + employee.getPassword() + "'");

            // Debug: Show what hash WOULD be generated for this password
            String generatedHashForInput = passwordUtil.encodePassword(password);
            System.out.println("   - Hash that WOULD be generated for '" + password + "': '" + generatedHashForInput + "'");
            System.out.println("   - Does generated hash match stored? " + generatedHashForInput.equals(employee.getPassword()));

            boolean passwordMatches = passwordUtil.matchesPassword(password, employee.getPassword());
            System.out.println("   - passwordUtil.matchesPassword() returned: " + passwordMatches);

            if (!passwordMatches) {
                System.out.println("❌ PASSWORD MISMATCH!");

                // Extra debug: Try with empty password (sometimes first login uses empty)
                boolean emptyMatches = passwordUtil.matchesPassword("", employee.getPassword());
                System.out.println("   - Empty password matches? " + emptyMatches);

                ra.addFlashAttribute("error", "Invalid Employee ID or password");
                return "redirect:/candidate/login";
            }
            System.out.println("✅ PASSWORD MATCHES!");

            // ========== STEP 8: SESSION HANDLING ==========
            System.out.println("\n📋 STEP 8: Handling session...");
            jakarta.servlet.http.HttpSession existingSession = request.getSession(false);
            if (existingSession != null) {
                System.out.println("   - Invalidating existing session: " + existingSession.getId());
                existingSession.invalidate();
            }

            jakarta.servlet.http.HttpSession newSession = request.getSession(true);
            System.out.println("   - Created new session with ID: " + newSession.getId());

            // ========== STEP 9: SET SESSION ATTRIBUTES ==========
            System.out.println("\n📋 STEP 9: Setting session attributes...");
            newSession.setAttribute("userId", employee.getEmployeeId());
            newSession.setAttribute("userRole", employee.getRole().name());
            newSession.setAttribute("userName", employee.getFirstName() + " " + employee.getLastName());
            newSession.setAttribute("candidateEmployeeId", employee.getEmployeeId());
            newSession.setMaxInactiveInterval(1800); // 30 minutes

            System.out.println("   - userId set to: " + newSession.getAttribute("userId"));
            System.out.println("   - userRole set to: " + newSession.getAttribute("userRole"));
            System.out.println("   - userName set to: " + newSession.getAttribute("userName"));

            logger.info("✅ Login Successful. User: {}, Role: {}", employeeId, employee.getRole());

            // ========== STEP 10: ATTENDANCE AUTO CHECK-IN ==========
            System.out.println("\n📋 STEP 10: Attendance auto check-in...");
            try {
                String roleName = employee.getRole().name();
                logger.info(">>> STEP 3: Checking if role '{}' is eligible", roleName);

                // ✅ FIXED: Include SUPER_ADMIN in auto check-in
                boolean isEmployee = "EMPLOYEE".equals(roleName);
                boolean isHR = "HR".equals(roleName);
                boolean isSuperAdmin = "SUPER_ADMIN".equals(roleName);

                logger.info(">>> STEP 4: isEmployee={}, isHR={}, isSuperAdmin={}",
                        isEmployee, isHR, isSuperAdmin);

                // ✅ ALLOW SUPER_ADMIN to check in too
                if (isEmployee || isHR || isSuperAdmin) {
                    logger.info(">>> STEP 5: Role is ELIGIBLE, calling checkIn()");

                    // Check if already checked in today
                    Attendance todayAttendance = attendanceService.getTodayAttendance(employeeId);

                    if (todayAttendance == null) {
                        // No attendance record - create new check-in
                        logger.info(">>> No attendance found, creating new check-in for {}", employeeId);
                        Attendance attendance = attendanceService.checkIn(employeeId);

                        logger.info(">>> STEP 6: checkIn() returned successfully");
                        logger.info(">>> STEP 7: Attendance ID: {}", attendance.getId());
                        logger.info(">>> STEP 8: Check-in time: {}", attendance.getCheckInTime());
                        logger.info(">>> STEP 9: Status: {}", attendance.getStatus());

                        newSession.setAttribute("todayAttendance", attendance);
                        newSession.setAttribute("attendanceStatus", attendance.getStatus());

                        logger.info(">>> STEP 10: Session attributes set successfully");
                    } else if (todayAttendance.getCheckOutTime() != null) {
                        // Already checked out - create new session
                        logger.info(">>> Found checked out record, creating new check-in");
                        Attendance attendance = attendanceService.checkIn(employeeId);
                        newSession.setAttribute("todayAttendance", attendance);
                        newSession.setAttribute("attendanceStatus", attendance.getStatus());
                    } else {
                        // Already checked in and active
                        logger.info(">>> Already checked in today at {}", todayAttendance.getCheckInTime());
                        newSession.setAttribute("todayAttendance", todayAttendance);
                        newSession.setAttribute("attendanceStatus", todayAttendance.getStatus());
                    }
                } else {
                    logger.warn(">>> STEP 5: Role '{}' NOT eligible for auto check-in", roleName);
                }
            } catch (Exception e) {
                logger.error(">>> ERROR in auto check-in: {}", e.getMessage());
                logger.error(">>> EXCEPTION: ", e);
            }

            // ========== STEP 11: REDIRECT BASED ON ROLE ==========
            System.out.println("\n📋 STEP 11: Redirecting based on role...");
            System.out.println("   - Employee role: " + employee.getRole());

            switch (employee.getRole()) {
                case SUPER_ADMIN:
                    System.out.println("   → Redirecting to SUPER_ADMIN dashboard: /dashboard/admin?showAttendance=true");
                    return "redirect:/dashboard/admin?showAttendance=true";
                case HR:
                    System.out.println("   → Redirecting to HR dashboard: /dashboard/hr?showAttendance=true");
                    return "redirect:/dashboard/hr?showAttendance=true";
                case EMPLOYEE:
                    System.out.println("   → Redirecting to EMPLOYEE dashboard: /candidate/dashboard/" + employee.getEmployeeId() + "?showAttendance=true");
                    return "redirect:/candidate/dashboard/" + employee.getEmployeeId() + "?showAttendance=true";
                default:
                    System.out.println("   → Unknown role, defaulting to employee dashboard");
                    return "redirect:/candidate/dashboard/" + employee.getEmployeeId() + "?showAttendance=true";
            }

        } catch (Exception e) {
            System.out.println("\n❌ EXCEPTION in login process:");
            e.printStackTrace();
            logger.error("❌ Login System Error: {}", e.getMessage(), e);
            ra.addFlashAttribute("error", "An internal error occurred. Please try again.");
            return "redirect:/candidate/login";
        }
    }

    /* ==================== HELPER METHOD (ADD THIS) ==================== */
    /**
     * Handles post-login attendance check-in (kept for manual use if needed)
     */
    private void handlePostLoginAttendance(String employeeId, HttpSession session) {
        try {
            logger.info("Manual check-in triggered for: {}", employeeId);
            Attendance attendance = attendanceService.checkIn(employeeId);

            session.setAttribute("todayAttendance", attendance);
            session.setAttribute("attendanceStatus", attendance.getStatus());

            if (attendance.getCheckInTime() != null) {
                long checkInMillis = attendance.getCheckInTime()
                        .atZone(java.time.ZoneId.systemDefault())
                        .toInstant()
                        .toEpochMilli();
                session.setAttribute("checkInTimeMillis", checkInMillis);
            }
        } catch (Exception e) {
            logger.error("Manual check-in failed for {}: {}", employeeId, e.getMessage());
        }
    }

    /* ==================== OTHER METHODS (keep existing) ==================== */
    @GetMapping("/forgot-password")
    public String showForgotPasswordPage() {
        return "candidate/forgot-password";
    }

    @PostMapping("/forgot-password")
    public String sendResetLink(@RequestParam String email,
                                @RequestParam(required = false) String employeeId,
                                RedirectAttributes ra) {

        logger.info("Forgot password request for email: {}, employeeId: {}", email, employeeId);

        try {
            Optional<Employee> employeeOpt;

            if (employeeId != null && !employeeId.trim().isEmpty()) {
                employeeOpt = employeeService.getEmployeeByEmployeeId(employeeId.trim());
            } else {
                employeeOpt = employeeService.getAllEmployees().stream()
                        .filter(e -> email.equalsIgnoreCase(e.getEmail()) ||
                                email.equalsIgnoreCase(e.getPersonalEmail()))
                        .findFirst();
            }

            if (employeeOpt.isEmpty()) {
                ra.addFlashAttribute("error", "No account found with provided email/employee ID.");
                return "redirect:/candidate/auth/forgot-password";
            }

            Employee employee = employeeOpt.get();

            if (!employee.isActive()) {
                ra.addFlashAttribute("error", "Account is deactivated. Please contact HR.");
                return "redirect:/candidate/auth/forgot-password";
            }

            if (employee.getPassword() == null || employee.getPassword().isEmpty()) {
                ra.addFlashAttribute("error", "You haven't created your credentials yet. Please use the login page first.");
                return "redirect:/candidate/auth/forgot-password";
            }

            String resetToken = UUID.randomUUID().toString();
            employee.setResetToken(resetToken);
            employee.setTokenExpiry(LocalDateTime.now().plusHours(24));

            employeeService.saveEmployee(employee);

            String resetLink = "http://localhost:8080/candidate/auth/reset-password?token=" + resetToken;

            ra.addFlashAttribute("resetLink", resetLink);
            ra.addFlashAttribute("successMessage", "Password reset link generated successfully!");

            logger.info("Reset token generated for {}: {}", employee.getEmployeeId(), resetToken);

            return "redirect:/candidate/auth/forgot-password";

        } catch (Exception e) {
            logger.error("Error in forgot password: {}", e.getMessage(), e);
            ra.addFlashAttribute("error", "An error occurred. Please try again.");
            return "redirect:/candidate/auth/forgot-password";
        }
    }

    @GetMapping("/change-password")
    public String showChangePasswordPage(HttpSession session, Model model) {
        String employeeId = (String) session.getAttribute("userId");
        if (employeeId == null) {
            return "redirect:/candidate/auth/login?error=Please+login+first";
        }

        Optional<Employee> employeeOpt = employeeService.getEmployeeByEmployeeId(employeeId);
        if (employeeOpt.isEmpty()) {
            return "redirect:/candidate/auth/login?error=Employee+not+found";
        }

        model.addAttribute("employee", employeeOpt.get());
        return "candidate/change-password";
    }

    @PostMapping("/change-password")
    public String changePassword(
            @RequestParam String currentPassword,
            @RequestParam String newPassword,
            @RequestParam String confirmPassword,
            HttpSession session,
            RedirectAttributes ra) {

        String employeeId = (String) session.getAttribute("userId");
        if (employeeId == null) {
            return "redirect:/candidate/auth/login?error=Session+expired";
        }

        Optional<Employee> employeeOpt = employeeService.getEmployeeByEmployeeId(employeeId);
        if (employeeOpt.isEmpty()) {
            return "redirect:/candidate/auth/login?error=Employee+not+found";
        }

        Employee employee = employeeOpt.get();

        if (!passwordUtil.matchesPassword(currentPassword, employee.getPassword())) {
            ra.addFlashAttribute("error", "Current password is incorrect");
            return "redirect:/candidate/auth/change-password";
        }

        if (!newPassword.equals(confirmPassword)) {
            ra.addFlashAttribute("error", "New passwords do not match");
            return "redirect:/candidate/auth/change-password";
        }

        if (newPassword.length() < 8) {
            ra.addFlashAttribute("error", "New password must be at least 8 characters");
            return "redirect:/candidate/auth/change-password";
        }

        employee.setPassword(passwordUtil.encodePassword(newPassword));
        employeeService.saveEmployee(employee);

        ra.addFlashAttribute("success", "Password changed successfully!");
        return "redirect:/candidate/dashboard/" + employee.getEmployeeId();
    }

    @GetMapping("/reset-password")
    public String showResetPasswordPage(@RequestParam String token, Model model) {
        try {
            logger.info("Processing reset password request with token: {}", token);

            Optional<Employee> employeeOpt = employeeService.getAllEmployees().stream()
                    .filter(e -> token.equals(e.getResetToken()) &&
                            e.getTokenExpiry() != null &&
                            e.getTokenExpiry().isAfter(LocalDateTime.now()))
                    .findFirst();

            if (employeeOpt.isEmpty()) {
                logger.warn("Invalid or expired reset token: {}", token);
                model.addAttribute("error", "Invalid or expired reset token.");
                return "candidate/reset-password-error";
            }

            Employee employee = employeeOpt.get();
            model.addAttribute("token", token);
            model.addAttribute("employeeId", employee.getEmployeeId());
            model.addAttribute("employeeName", employee.getFirstName() + " " + employee.getLastName());

            logger.info("Reset password page loaded for employee: {}", employee.getEmployeeId());
            return "candidate/reset-password";

        } catch (Exception e) {
            logger.error("Error showing reset password page: {}", e.getMessage(), e);
            model.addAttribute("error", "Invalid reset token.");
            return "candidate/reset-password-error";
        }
    }

    @PostMapping("/reset-password")
    public String resetPassword(@RequestParam String token,
                                @RequestParam String password,
                                @RequestParam String confirmPassword,
                                RedirectAttributes ra) {

        try {
            Optional<Employee> employeeOpt = employeeService.getAllEmployees().stream()
                    .filter(e -> token.equals(e.getResetToken()) &&
                            e.getTokenExpiry() != null &&
                            e.getTokenExpiry().isAfter(LocalDateTime.now()))
                    .findFirst();

            if (employeeOpt.isEmpty()) {
                ra.addFlashAttribute("error", "Invalid or expired reset token.");
                return "redirect:/candidate/login";
            }

            if (!password.equals(confirmPassword)) {
                ra.addFlashAttribute("error", "Passwords do not match.");
                return "redirect:/candidate/auth/reset-password?token=" + token;
            }

            if (password.length() < 8) {
                ra.addFlashAttribute("error", "Password must be at least 8 characters long.");
                return "redirect:/candidate/auth/reset-password?token=" + token;
            }

            Employee employee = employeeOpt.get();

            employee.setPassword(passwordUtil.encodePassword(password));
            employee.setResetToken(null);
            employee.setTokenExpiry(null);

            employeeService.saveEmployee(employee);

            ra.addFlashAttribute("success", "Password reset successfully! You can now login with your new password.");
            return "redirect:/candidate/login";

        } catch (Exception e) {
            logger.error("Error resetting password: {}", e.getMessage(), e);
            ra.addFlashAttribute("error", "An error occurred. Please try again.");
            return "redirect:/candidate/auth/reset-password?token=" + token;
        }
    }

    @GetMapping("/logout")
    public String logout(HttpSession session) {
        // Optional: Check-out on logout (uncomment if you want auto checkout on logout)

        String employeeId = (String) session.getAttribute("userId");
        if (employeeId != null) {
            try {
                attendanceService.checkOut(employeeId);
                logger.info("Auto check-out on logout for: {}", employeeId);
            } catch (Exception e) {
                logger.error("Auto check-out failed: {}", e.getMessage());
            }
        }


        if (session != null) {
            session.invalidate();
        }
        return "redirect:/candidate/login?logout=success";
    }
}