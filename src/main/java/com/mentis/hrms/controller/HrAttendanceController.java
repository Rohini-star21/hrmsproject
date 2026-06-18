package com.mentis.hrms.controller;

import com.mentis.hrms.service.AttendanceService;
import com.mentis.hrms.service.EmployeeService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.security.Principal; // ADDED
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;



/**
 * Show HR attendance management page
 */

@Controller
@RequestMapping("/dashboard/hr/attendance")
public class HrAttendanceController {

    private static final Logger logger = LoggerFactory.getLogger(HrAttendanceController.class);

    @Autowired
    private AttendanceService attendanceService;

    @Autowired
    private EmployeeService employeeService;

    /**
     * Show HR attendance management page
     */
    @GetMapping
    public String showHrAttendance(Model model, Principal principal) {
        // Simple permission check - log who's accessing
        if (principal != null) {
            logger.info("Loading HR attendance management page for user: {}", principal.getName());
        } else {
            logger.info("Loading HR attendance management page (no authenticated user)");
        }

        // Get today's date
        LocalDate today = LocalDate.now();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("EEEE, MMMM d, yyyy");
        String todayFormatted = today.format(formatter);

        // Get all attendance for today
        List<Map<String, Object>> todayAttendances = attendanceService.getTodayAllAttendances();

        // Calculate statistics
        long presentCount = todayAttendances.stream()
                .filter(a -> "ACTIVE".equals(a.get("status")) || "BREAK".equals(a.get("status")))
                .count();
        long absentCount = todayAttendances.stream()
                .filter(a -> "OFFLINE".equals(a.get("status")) || "ABSENT".equals(a.get("status")))
                .count();
        long lateCount = todayAttendances.stream()
                .filter(a -> {
                    LocalDateTime checkIn = (LocalDateTime) a.get("checkInTime");
                    return checkIn != null && checkIn.getHour() >= 9 && checkIn.getMinute() > 15;
                })
                .count();

        model.addAttribute("todayDate", todayFormatted);
        model.addAttribute("attendances", todayAttendances);
        model.addAttribute("presentCount", presentCount);
        model.addAttribute("absentCount", absentCount);
        model.addAttribute("lateCount", lateCount);
        model.addAttribute("totalEmployees", employeeService.getAllEmployees().size());

        return "dashboard"; // This will render the HR dashboard with attendance
    }

    /**
     * API: Get today's attendance for HR dashboard (recent attendance section)
     */
    // In HrAttendanceController.java - Update the getTodayAttendanceApi() method

    @GetMapping("/api/today")
    @ResponseBody
    public ResponseEntity<?> getTodayAttendanceApi() {
        try {
            List<Map<String, Object>> attendances = attendanceService.getTodayAllAttendances();

            // Format for display - ensure all fields are properly set
            attendances.forEach(att -> {
                // Check if checked out
                boolean isCheckedOut = att.get("checkOutTime") != null;
                att.put("isCheckedOut", isCheckedOut);

                String status = (String) att.get("status");
                String statusColor = "green";
                String statusText = "Present";

                // CRITICAL: Check for checkout first
                if (isCheckedOut) {
                    statusColor = "red";
                    statusText = "Checked Out";
                } else if ("OFFLINE".equals(status) || "ABSENT".equals(status)) {
                    statusColor = "red";
                    statusText = "Absent";
                } else if ("BREAK".equals(status)) {
                    statusColor = "orange";
                    statusText = "On Break";
                } else if ("ACTIVE".equals(status)) {
                    statusColor = "green";
                    statusText = "Present";
                }

                att.put("statusColor", statusColor);
                att.put("statusText", statusText);

                // Ensure formatted times exist
                if (!att.containsKey("checkInFormatted")) {
                    att.put("checkInFormatted", att.get("checkInTime") != null ?
                            ((LocalDateTime)att.get("checkInTime")).format(DateTimeFormatter.ofPattern("hh:mm a")) : "--:--");
                }
                if (!att.containsKey("checkOutFormatted")) {
                    att.put("checkOutFormatted", att.get("checkOutTime") != null ?
                            ((LocalDateTime)att.get("checkOutTime")).format(DateTimeFormatter.ofPattern("hh:mm a")) : "--:--");
                }
            });

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "attendances", attendances,
                    "total", attendances.size(),
                    "timestamp", LocalDateTime.now()
            ));

        } catch (Exception e) {
            logger.error("Error fetching attendance for HR: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "error", e.getMessage()
            ));
        }
    }



    /**
     * API: Update attendance status (manual override by HR)
     */
    @PostMapping("/api/update-status/{employeeId}")
    @ResponseBody
    public ResponseEntity<?> updateAttendanceStatus(
            @PathVariable String employeeId,
            @RequestParam String status,
            @RequestParam(required = false) String remarks) {

        try {
            // Get today's attendance
            LocalDate today = LocalDate.now();

            // FIXED: Use service method instead of direct repository access
            // You need to add this method to your AttendanceService
            var attendanceOpt = attendanceService.findAttendanceByEmployeeAndDate(employeeId, today);

            if (attendanceOpt.isPresent()) {
                var attendance = attendanceOpt.get();
                attendance.setStatus(status);

                // Update employee presence
                attendanceService.updateEmployeePresence(employeeId, status);

                // Save changes - use service method
                attendanceService.saveAttendance(attendance);

                return ResponseEntity.ok(Map.of(
                        "success", true,
                        "message", "Attendance status updated successfully",
                        "employeeId", employeeId,
                        "newStatus", status
                ));
            } else {
                return ResponseEntity.badRequest().body(Map.of(
                        "success", false,
                        "error", "No attendance record found for today"
                ));
            }

        } catch (Exception e) {
            logger.error("Error updating attendance status: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "error", e.getMessage()
            ));
        }
    }
}