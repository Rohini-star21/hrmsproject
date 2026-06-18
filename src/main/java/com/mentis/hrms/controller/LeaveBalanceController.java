package com.mentis.hrms.controller;

import com.mentis.hrms.service.LeaveBalanceService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.time.LocalDate;
import java.util.Map;

@RestController
@RequestMapping("/api/leave-balance")
public class LeaveBalanceController {

    private static final Logger logger = LoggerFactory.getLogger(LeaveBalanceController.class);

    @Autowired
    private LeaveBalanceService leaveBalanceService;

    /**
     * Get leave balance for current logged-in employee
     */
    @GetMapping("/my-balance")
    public ResponseEntity<?> getMyLeaveBalance(Principal principal) {
        try {
            // Get employee ID from principal (you may need to adjust based on your auth system)
            String employeeId = principal != null ? principal.getName() : null;

            if (employeeId == null || employeeId.isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of(
                        "success", false,
                        "error", "Employee not authenticated"
                ));
            }

            Map<String, Object> result = leaveBalanceService.getEmployeeLeaveBalance(employeeId);
            return ResponseEntity.ok(result);

        } catch (Exception e) {
            logger.error("Error fetching leave balance: {}", e.getMessage());
            return ResponseEntity.status(500).body(Map.of(
                    "success", false,
                    "error", e.getMessage()
            ));
        }
    }

    /**
     * Get leave balance for a specific employee (HR use) - UPDATED
     */
    @GetMapping("/employee/{employeeId}")
    public ResponseEntity<?> getEmployeeLeaveBalance(@PathVariable String employeeId) {
        try {
            Map<String, Object> result = leaveBalanceService.getEmployeeLeaveBalance(employeeId);

            // Log the response for debugging
            logger.info("Leave balance response for {}: {}", employeeId, result);

            return ResponseEntity.ok(result);

        } catch (Exception e) {
            logger.error("Error fetching leave balance for {}: {}", employeeId, e.getMessage());
            return ResponseEntity.status(500).body(Map.of(
                    "success", false,
                    "error", e.getMessage()
            ));
        }
    }

    /**
     * Check if employee has sufficient balance before applying
     */
    @PostMapping("/check-balance")
    public ResponseEntity<?> checkBalance(@RequestBody Map<String, Object> request) {
        try {
            String employeeId = (String) request.get("employeeId");
            String leaveType = (String) request.get("leaveType");
            String startDate = (String) request.get("startDate");
            String endDate = (String) request.get("endDate");
            String leaveDuration = (String) request.get("leaveDuration");
            String halfDayType = (String) request.get("halfDayType");

            if (employeeId == null || leaveType == null || startDate == null || endDate == null) {
                return ResponseEntity.badRequest().body(Map.of(
                        "success", false,
                        "error", "Missing required fields"
                ));
            }

            // Parse dates
            LocalDate start = LocalDate.parse(startDate);
            LocalDate end = LocalDate.parse(endDate);

            boolean hasBalance = leaveBalanceService.hasSufficientBalance(
                    employeeId, leaveType, start, end, leaveDuration, halfDayType);

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "hasSufficientBalance", hasBalance
            ));

        } catch (Exception e) {
            logger.error("Error checking balance: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "error", e.getMessage()
            ));
        }
    }

    /**
     * Recalculate balance for an employee (admin/HR use)
     */
    @PostMapping("/recalculate/{employeeId}")
    public ResponseEntity<?> recalculateBalance(@PathVariable String employeeId,
                                                @RequestParam(required = false) Integer year) {
        try {
            if (year == null) {
                year = LocalDate.now().getYear();
            }

            leaveBalanceService.recalculateAllBalances(employeeId, year);

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Balance recalculated successfully"
            ));

        } catch (Exception e) {
            logger.error("Error recalculating balance: {}", e.getMessage());
            return ResponseEntity.status(500).body(Map.of(
                    "success", false,
                    "error", e.getMessage()
            ));
        }
    }
}