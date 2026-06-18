package com.mentis.hrms.controller;
// Add this import at the top
import com.mentis.hrms.service.LeaveBalanceService;
import com.mentis.hrms.model.Attendance;
import com.mentis.hrms.model.Employee;
import com.mentis.hrms.model.LeaveRequest;
import com.mentis.hrms.repository.AttendanceRepository;
import com.mentis.hrms.repository.LeaveRequestRepository;
import com.mentis.hrms.service.AttendanceService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/leaves")
public class LeaveManagementController {

    private static final Logger logger = LoggerFactory.getLogger(LeaveManagementController.class);

    @Autowired
    private LeaveRequestRepository leaveRequestRepository;

    @Autowired
    private AttendanceService attendanceService;

    // ========== FIXED: Use String leaveId to match your ID format ==========
    // Add this field
    @Autowired
    private LeaveBalanceService leaveBalanceService;

    @GetMapping("/pending")
    public ResponseEntity<?> getPendingLeaves() {
        List<LeaveRequest> pending = leaveRequestRepository.findByStatus("PENDING");
        return ResponseEntity.ok(Map.of(
                "success", true,
                "leaves", pending,
                "count", pending.size()
        ));
    }
    @PostMapping("/{leaveId}/approve")
    public ResponseEntity<?> approveLeave(@PathVariable String leaveId,
                                          @RequestBody(required = false) Map<String, String> requestBody,
                                          Principal principal) {
        logger.info("Approving leave: {}", leaveId);

        try {
            // Fetch fresh from database
            LeaveRequest leave = leaveRequestRepository.findById(leaveId)
                    .orElseThrow(() -> new RuntimeException("Leave not found: " + leaveId));

            String hrManagerId = (principal != null) ? principal.getName() : "SYSTEM";

            logger.info("Current status before approve: {}", leave.getStatus());

            // ===== CRITICAL: Calculate total days if not set =====
            if (leave.getTotalDays() == null || leave.getTotalDays() == 0) {
                long days = java.time.temporal.ChronoUnit.DAYS.between(leave.getStartDate(), leave.getEndDate()) + 1;
                leave.setTotalDays((double) days);
                leave.setLeaveDuration("full_day");
                logger.info("Calculated total days for leave {}: {}", leaveId, days);
            }
            // ====================================================

            // EXPLICITLY set all fields
            leave.setStatus("APPROVED");
            leave.setApprovedBy(hrManagerId);
            leave.setApprovedDate(LocalDateTime.now());

            // Get remarks from request body if provided
            if (requestBody != null) {
                String remarks = requestBody.get("remarks");
                if (remarks != null && !remarks.trim().isEmpty()) {
                    leave.setApprovalRemarks(remarks);
                }
            }

            // ===== CRITICAL: Deduct from leave balance BEFORE saving =====
            boolean balanceDeducted = leaveBalanceService.deductLeaveBalance(leave);
            if (!balanceDeducted) {
                logger.warn("Failed to deduct leave balance for {} - check if balance exists", leaveId);
                // Still proceed with approval but log warning
            } else {
                logger.info("Successfully deducted leave balance for {}", leaveId);
            }
            // ============================================================

            // Save and flush immediately
            LeaveRequest saved = leaveRequestRepository.saveAndFlush(leave);

            logger.info("Status after save: {}", saved.getStatus());

            // Verify it was saved correctly
            if (!"APPROVED".equals(saved.getStatus())) {
                throw new RuntimeException("Failed to update status to APPROVED");
            }

            // Create attendance records for approved leave
            createAttendanceForApprovedLeave(leave);

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Leave approved successfully",
                    "leaveId", leaveId,
                    "status", saved.getStatus()
            ));

        } catch (Exception e) {
            logger.error("Error approving leave: {}", e.getMessage(), e);
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "error", e.getMessage()
            ));
        }
    }

    // ========== SINGLE getAllLeaves METHOD - handles all cases ==========
    @GetMapping("/all")
    public ResponseEntity<?> getAllLeaves(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String employeeId) {

        try {
            List<LeaveRequest> leaves;
            List<Map<String, Object>> enrichedLeaves = new ArrayList<>();

            if (status != null && !status.isEmpty()) {
                leaves = leaveRequestRepository.findByStatus(status.toUpperCase());
            } else if (employeeId != null && !employeeId.isEmpty()) {
                leaves = leaveRequestRepository.findByEmployeeIdOrderByCreatedAtDesc(employeeId);
            } else {
                // Get ALL leaves for HR dashboard
                leaves = leaveRequestRepository.findAll();
                leaves.sort((a, b) -> {
                    if (a.getCreatedAt() == null || b.getCreatedAt() == null) return 0;
                    return b.getCreatedAt().compareTo(a.getCreatedAt());
                });
            }

            // Enrich leaves with employee details
            for (LeaveRequest leave : leaves) {
                Map<String, Object> enrichedLeave = new HashMap<>();

                // Copy all leave properties
                enrichedLeave.put("id", leave.getId());
                enrichedLeave.put("employeeId", leave.getEmployeeId());
                enrichedLeave.put("startDate", leave.getStartDate());
                enrichedLeave.put("endDate", leave.getEndDate());
                enrichedLeave.put("leaveType", leave.getLeaveType());
                enrichedLeave.put("reason", leave.getReason());
                enrichedLeave.put("status", leave.getStatus());
                enrichedLeave.put("approvedBy", leave.getApprovedBy());
                enrichedLeave.put("approvedDate", leave.getApprovedDate());
                enrichedLeave.put("createdAt", leave.getCreatedAt());
                enrichedLeave.put("rejectionReason", leave.getRejectionReason());
                enrichedLeave.put("approvalRemarks", leave.getApprovalRemarks());

                // Get employee details
                Employee employee = attendanceService.findEmployeeById(leave.getEmployeeId());
                if (employee != null) {
                    enrichedLeave.put("employeeName", employee.getFirstName() + " " + employee.getLastName());
                    enrichedLeave.put("department", employee.getDepartment());
                    enrichedLeave.put("designation", employee.getDesignation());
                } else {
                    enrichedLeave.put("employeeName", "Unknown");
                    enrichedLeave.put("department", "N/A");
                    enrichedLeave.put("designation", "N/A");
                }

                enrichedLeaves.add(enrichedLeave);
            }

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "leaves", enrichedLeaves,
                    "count", enrichedLeaves.size()
            ));

        } catch (Exception e) {
            logger.error("Error fetching leaves: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "error", e.getMessage()
            ));
        }
    }
    @PostMapping("/{leaveId}/reject")
    public ResponseEntity<?> rejectLeave(@PathVariable String leaveId,
                                         @RequestBody(required = false) Map<String, String> requestBody,
                                         Principal principal) {
        logger.info("Rejecting leave: {}", leaveId);

        try {
            // Extract reason from request body
            String reason = "No reason provided";
            if (requestBody != null && requestBody.containsKey("reason")) {
                reason = requestBody.get("reason");
            }

            if (reason == null || reason.trim().isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of(
                        "success", false,
                        "error", "Rejection reason is required"
                ));
            }

            // Fetch leave request from database
            LeaveRequest leave = leaveRequestRepository.findById(leaveId)
                    .orElseThrow(() -> new RuntimeException("Leave not found: " + leaveId));

            String hrManagerId = (principal != null) ? principal.getName() : "SYSTEM";

            logger.info("Current status before reject: {}", leave.getStatus());

            // Update leave request
            leave.setStatus("REJECTED");
            leave.setApprovedBy(hrManagerId);
            leave.setApprovedDate(LocalDateTime.now());
            leave.setRejectionReason(reason); // Save the rejection reason

            // Save changes
            LeaveRequest saved = leaveRequestRepository.saveAndFlush(leave);

            logger.info("Leave {} rejected successfully by {}", leaveId, hrManagerId);

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Leave rejected successfully",
                    "leaveId", leaveId,
                    "status", saved.getStatus(),
                    "rejectedBy", hrManagerId,
                    "rejectionReason", reason
            ));

        } catch (Exception e) {
            logger.error("Error rejecting leave {}: {}", leaveId, e.getMessage(), e);
            return ResponseEntity.status(500).body(Map.of(
                    "success", false,
                    "error", "Failed to reject leave: " + e.getMessage()
            ));
        }
    }

    @GetMapping("/debug/{leaveId}")
    public ResponseEntity<?> debugLeave(@PathVariable String leaveId) {
        try {
            LeaveRequest leave = leaveRequestRepository.findById(leaveId)
                    .orElseThrow(() -> new RuntimeException("Not found"));

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "leaveId", leave.getId(),
                    "status", leave.getStatus(),
                    "statusLength", leave.getStatus() != null ? leave.getStatus().length() : 0,
                    "approvedBy", leave.getApprovedBy(),
                    "approvedDate", leave.getApprovedDate()
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "error", e.getMessage()
            ));
        }
    }

    @PostMapping("/apply")
    public ResponseEntity<?> applyForLeave(@RequestBody LeaveRequest leaveRequest, Principal principal) {
        logger.info("Applying for leave: {}", leaveRequest);

        try {
            // Validate required fields
            if (leaveRequest.getEmployeeId() == null || leaveRequest.getEmployeeId().isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of(
                        "success", false,
                        "error", "Employee ID is required"
                ));
            }

            if (leaveRequest.getStartDate() == null || leaveRequest.getEndDate() == null) {
                return ResponseEntity.badRequest().body(Map.of(
                        "success", false,
                        "error", "Start date and end date are required"
                ));
            }

            // ===== NEW: Check leave balance before applying =====
            boolean hasSufficientBalance = leaveBalanceService.hasSufficientBalance(
                    leaveRequest.getEmployeeId(),
                    leaveRequest.getLeaveType(),
                    leaveRequest.getStartDate(),
                    leaveRequest.getEndDate(),
                    leaveRequest.getLeaveDuration(),
                    leaveRequest.getHalfDayType()
            );

            if (!hasSufficientBalance) {
                return ResponseEntity.badRequest().body(Map.of(
                        "success", false,
                        "error", "Insufficient leave balance"
                ));
            }
            // ====================================================

            // Ensure ID is generated if not provided
            if (leaveRequest.getId() == null || leaveRequest.getId().isEmpty()) {
                leaveRequest.prePersist();
            }

            // Set default status and timestamps
            leaveRequest.setStatus("PENDING");
            leaveRequest.setCreatedAt(LocalDateTime.now());

            // Save to database
            LeaveRequest saved = leaveRequestRepository.save(leaveRequest);

            logger.info("Leave request saved successfully with ID: {}", saved.getId());

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Leave request submitted successfully",
                    "leaveId", saved.getId()
            ));

        } catch (Exception e) {
            logger.error("Error applying for leave: {}", e.getMessage(), e);
            return ResponseEntity.status(500).body(Map.of(
                    "success", false,
                    "error", "Failed to save to database: " + e.getMessage()
            ));
        }
    }


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
                    "hasSufficientBalance", hasBalance,
                    "leaveType", leaveType,
                    "message", leaveType.equalsIgnoreCase("earned") ?
                            "Earned leaves are unlimited" :
                            (hasBalance ? "Sufficient balance" : "Insufficient balance")
            ));

        } catch (Exception e) {
            logger.error("Error checking balance: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "error", e.getMessage()
            ));
        }
    }

    @GetMapping("/check-existing")
    public ResponseEntity<?> checkExistingLeaves(
            @RequestParam String employeeId,
            @RequestParam String date) {

        try {
            LocalDate checkDate = LocalDate.parse(date);

            // Check if employee has approved or pending leave on this date
            List<LeaveRequest> existingLeaves = leaveRequestRepository
                    .findByEmployeeIdOrderByCreatedAtDesc(employeeId)
                    .stream()
                    .filter(leave -> {
                        // Check if date falls within leave range
                        return !checkDate.isBefore(leave.getStartDate()) &&
                                !checkDate.isAfter(leave.getEndDate()) &&
                                (leave.getStatus().equals("PENDING") ||
                                        leave.getStatus().equals("APPROVED"));
                    })
                    .collect(Collectors.toList());

            boolean hasLeave = !existingLeaves.isEmpty();
            String leaveStatus = hasLeave ? existingLeaves.get(0).getStatus() : null;
            String leaveId = hasLeave ? existingLeaves.get(0).getId() : null;

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "hasLeave", hasLeave,
                    "leaveStatus", leaveStatus,
                    "leaveId", leaveId,
                    "date", date
            ));

        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "error", e.getMessage()
            ));
        }
    }
    // ========== NEW METHOD: Create attendance entries for approved leave ==========
    private void createAttendanceForApprovedLeave(LeaveRequest leave) {
        try {
            LocalDate startDate = leave.getStartDate();
            LocalDate endDate = leave.getEndDate();
            String employeeId = leave.getEmployeeId();

            logger.info("Creating attendance records for employee {} from {} to {}",
                    employeeId, startDate, endDate);

            // Iterate through each day of the leave period
            LocalDate currentDate = startDate;
            while (!currentDate.isAfter(endDate)) {
                // Check if attendance record already exists for this date
                attendanceService.createOrUpdateLeaveAttendance(employeeId, currentDate, "LEAVE");
                currentDate = currentDate.plusDays(1);
            }

            logger.info("Successfully created attendance records for leave period");

        } catch (Exception e) {
            logger.error("Error creating attendance for approved leave: {}", e.getMessage(), e);
            // Don't throw - we don't want to rollback the leave approval if attendance creation fails
            // But you might want to handle this differently based on your requirements
        }
    }
}