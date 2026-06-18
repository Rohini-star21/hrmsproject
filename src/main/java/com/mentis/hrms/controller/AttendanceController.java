package com.mentis.hrms.controller;

import com.mentis.hrms.service.AttendanceService;
import com.mentis.hrms.service.EmployeeService;
import com.mentis.hrms.model.Attendance; // Add this import
import com.mentis.hrms.model.Employee;
import jakarta.servlet.http.HttpSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.time.Duration; // Add this import
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle; // Add this import
import java.util.Locale; // Add this import
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/attendance")
public class AttendanceController {

    private static final Logger logger = LoggerFactory.getLogger(AttendanceController.class);

    @Autowired
    private AttendanceService attendanceService;

    @Autowired
    private EmployeeService employeeService;

    // ========== EXISTING API METHODS ==========

    @GetMapping("/today/{employeeId}")
    public ResponseEntity<?> getTodayAttendance(@PathVariable String employeeId, HttpSession session) {
        try {
            Attendance attendance = attendanceService.getTodayAttendance(employeeId);

            Map<String, Object> response = new HashMap<>();

            if (attendance != null) {
                response.put("success", true);
                response.put("status", attendance.getStatus());

                // CRITICAL FIX: Return ISO format timestamp, not formatted string!
                // JavaScript needs: "2026-02-21T08:33:00" not "08:33 am"
                if (attendance.getCheckInTime() != null) {
                    response.put("checkInTime", attendance.getCheckInTime().toString()); // ISO format: 2026-02-21T08:33:00
                    response.put("checkIn", attendance.getCheckInTime().toString()); // Alternative field name

                    // Also keep formatted version for display purposes
                    DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("hh:mm a");
                    response.put("checkInTimeFormatted", attendance.getCheckInTime().format(timeFormatter));
                } else {
                    response.put("checkInTime", null);
                    response.put("checkIn", null);
                    response.put("checkInTimeFormatted", "--:--");
                }

                // Same for checkout
                if (attendance.getCheckOutTime() != null) {
                    response.put("checkOutTime", attendance.getCheckOutTime().toString()); // ISO format
                    response.put("checkOut", attendance.getCheckOutTime().toString());

                    DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("hh:mm a");
                    response.put("checkOutTimeFormatted", attendance.getCheckOutTime().format(timeFormatter));
                } else {
                    response.put("checkOutTime", null);
                    response.put("checkOut", null);
                    response.put("checkOutTimeFormatted", "--:--");
                }

                response.put("breakMinutes", attendance.getTotalBreakMinutes());
                response.put("workingTime", attendance.getCurrentSessionTime());

                // Keep milliseconds for precise calculations
                if (attendance.getCheckInTime() != null) {
                    long checkInMillis = attendance.getCheckInTime().atZone(
                            java.time.ZoneId.systemDefault()).toInstant().toEpochMilli();
                    response.put("checkInTimeMillis", checkInMillis);
                }

                session.setAttribute("attendanceStatus", attendance.getStatus());
            } else {
                response.put("success", true);
                response.put("status", "OFFLINE");
                response.put("workingTime", "0h 00m");
            }

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            logger.error("Error fetching attendance: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of("success", false, "error", e.getMessage()));
        }
    }

    @PostMapping("/checkin/{employeeId}")
    public ResponseEntity<?> checkIn(@PathVariable String employeeId) {
        try {
            Attendance attendance = attendanceService.checkIn(employeeId);
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Checked in successfully");

            // CRITICAL FIX: Return ISO format timestamp
            if (attendance.getCheckInTime() != null) {
                response.put("checkInTime", attendance.getCheckInTime().toString()); // ISO: 2026-02-21T08:33:00
                response.put("checkIn", attendance.getCheckInTime().toString());

                // Also provide formatted version for display
                DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("hh:mm a");
                response.put("checkInTimeFormatted", attendance.getCheckInTime().format(timeFormatter));

                long checkInMillis = attendance.getCheckInTime().atZone(
                        java.time.ZoneId.systemDefault()).toInstant().toEpochMilli();
                response.put("checkInTimeMillis", checkInMillis);
            }

            response.put("status", attendance.getStatus());

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "error", e.getMessage()));
        }
    }

    @PostMapping("/checkout/{employeeId}")
    public ResponseEntity<?> checkOut(@PathVariable String employeeId) {
        try {
            Attendance attendance = attendanceService.checkOut(employeeId);

            // Format the response properly
            DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("hh:mm a");

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Checked out successfully");
            response.put("totalWorkingMinutes", attendance.getTotalWorkingMinutes());
            response.put("checkOutTime", attendance.getCheckOutTime());
            response.put("checkOutTimeFormatted", attendance.getCheckOutTime() != null ?
                    attendance.getCheckOutTime().format(timeFormatter) : null);
            response.put("status", "OFFLINE");
            response.put("workingTime", attendance.getCurrentSessionTime());

            // CRITICAL: Add these flags for frontend
            response.put("isCheckedOut", true);
            response.put("displayStatus", "Checked Out");

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            logger.error("Checkout error: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "error", e.getMessage()
            ));
        }
    }

    @PostMapping("/break/{employeeId}")
    public ResponseEntity<?> toggleBreak(@PathVariable String employeeId, @RequestParam String action) {
        try {
            Attendance attendance; // Fixed: Using imported class
            String message;

            if ("start".equalsIgnoreCase(action)) {
                attendance = attendanceService.startBreak(employeeId);
                message = "Break started";
            } else {
                attendance = attendanceService.endBreak(employeeId);
                message = "Break ended";
            }

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", message,
                    "status", attendance.getStatus(),
                    "totalBreakMinutes", attendance.getTotalBreakMinutes()
            ));

        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "error", e.getMessage()));
        }
    }

    @GetMapping("/today-all")
    public ResponseEntity<?> getTodayAllAttendances() {
        try {
            List<Map<String, Object>> attendances = attendanceService.getTodayAllAttendances();
            logger.info("Fetched {} attendance records for today", attendances.size());
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "attendances", attendances,
                    "timestamp", LocalDateTime.now()
            ));

        } catch (Exception e) {
            logger.error("Error fetching all attendances: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of("success", false, "error", e.getMessage()));
        }
    }

    @GetMapping("/history/{employeeId}")
    public ResponseEntity<?> getHistory(@PathVariable String employeeId) {
        try {
            List<Attendance> history = attendanceService.getAttendanceHistory(employeeId); // Fixed: Using imported class
            return ResponseEntity.ok(Map.of("success", true, "history", history));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "error", e.getMessage()));
        }
    }

    @GetMapping("/daily")
    public ResponseEntity<?> getDailyAttendance(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam(required = false) String department,
            @RequestParam(required = false) String status) {

        try {
            LocalDate targetDate = date != null ? date : LocalDate.now();

            List<Map<String, Object>> attendances = attendanceService.getAttendanceByDate(
                    targetDate, department, status);

            attendances.forEach(this::formatAttendanceForDisplay);

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "attendances", attendances,
                    "date", targetDate,
                    "timestamp", LocalDateTime.now()
            ));

        } catch (Exception e) {
            logger.error("Error fetching daily attendance: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "error", e.getMessage()
            ));
        }
    }

    private void formatAttendanceForDisplay(Map<String, Object> att) {
        LocalDateTime checkIn = (LocalDateTime) att.get("checkInTime");
        LocalDateTime checkOut = (LocalDateTime) att.get("checkOutTime");

        DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("hh:mm a");

        if (checkIn != null) {
            att.put("checkInFormatted", checkIn.format(timeFormatter));
        } else {
            att.put("checkInFormatted", "--:--");
        }

        if (checkOut != null) {
            att.put("checkOutFormatted", checkOut.format(timeFormatter));
        } else {
            att.put("checkOutFormatted", "--:--");
        }

        String status = (String) att.get("status");
        String statusColor = "green";
        String statusText = "Present";

        // FIXED: Proper status mapping for checked out employees
        if ("OFFLINE".equals(status)) {
            if (checkOut != null) {
                statusColor = "red";  // Red for checked out
                statusText = "Checked Out";  // Show "Checked Out" not "Absent"
            } else {
                statusColor = "red";
                statusText = "Absent";
            }
        } else if ("ABSENT".equals(status)) {
            statusColor = "red";
            statusText = "Absent";
        } else if ("BREAK".equals(status)) {
            statusColor = "orange";
            statusText = "On Break";
        } else if ("ACTIVE".equals(status)) {
            statusColor = "green";
            statusText = "Present";
        } else if ("LEAVE".equals(status)) {
            statusColor = "purple";
            statusText = "On Leave";
        }

        att.put("statusColor", statusColor);
        att.put("statusText", statusText);

        // CRITICAL FIX: Add explicit checkout status flag for frontend
        att.put("isCheckedOut", checkOut != null);
        att.put("checkOutTimeFormatted", checkOut != null ? checkOut.format(timeFormatter) : null);
    }

    // ========== NEW API METHODS FOR CALENDAR INTEGRATION ==========

    /**
     * Get employee's monthly attendance for calendar
     * URL: /api/attendance/calendar/EMP001/2024/3
     */
    @GetMapping("/calendar/{employeeId}/{year}/{month}")
    public ResponseEntity<?> getEmployeeCalendarAttendance(
            @PathVariable String employeeId,
            @PathVariable String year,
            @PathVariable String month) {

        logger.info("Calendar API called for {}: {}/{}", employeeId, year, month);

        try {
            // Parse and validate year/month
            int yearInt = Integer.parseInt(year);
            int monthInt = Integer.parseInt(month);

            logger.info("Parsed year: {}, month: {}", yearInt, monthInt);

            // Validate employee exists
            Employee employee = attendanceService.findEmployeeById(employeeId);
            if (employee == null) {
                logger.error("Employee not found: {}", employeeId);
                return ResponseEntity.badRequest().body(Map.of(
                        "success", false,
                        "error", "Employee not found: " + employeeId
                ));
            }

            logger.info("Found employee: {}", employee.getEmployeeId());

            // Get attendance data
            Map<String, String> attendanceMap = attendanceService.getEmployeeMonthlyAttendance(employeeId, yearInt, monthInt);

            logger.info("Returning attendance map with {} entries", attendanceMap.size());

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "employeeId", employeeId,
                    "year", yearInt,
                    "month", monthInt,
                    "attendance", attendanceMap
            ));

        } catch (NumberFormatException e) {
            logger.error("Invalid number format: year={}, month={}", year, month, e);
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "error", "Invalid year or month format"
            ));
        } catch (Exception e) {
            logger.error("Error fetching calendar attendance: {}", e.getMessage(), e);
            // IMPORTANT: Return the actual error message
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "error", "Internal error: " + e.getMessage(),
                    "exception", e.getClass().getSimpleName()
            ));
        }
    }

    /**
     * Get all employees monthly attendance for HR calendar
     * URL: /api/attendance/calendar/all/2024/3
     */
    @GetMapping("/calendar/all/{year}/{month}")
    public ResponseEntity<?> getAllEmployeesCalendarAttendance(
            @PathVariable int year,
            @PathVariable int month) {

        logger.info("HR Calendar API called for {}-{}", year, month);

        try {
            // Validate month (1-12)
            if (month < 1 || month > 12) {
                return ResponseEntity.badRequest().body(Map.of(
                        "success", false,
                        "error", "Invalid month. Must be 1-12"
                ));
            }

            Map<String, Map<String, String>> attendanceMap =
                    attendanceService.getAllEmployeesMonthlyAttendance(year, month);

            // Get employee names for display
            Map<String, String> employeeNames = new HashMap<>();
            for (String empId : attendanceMap.keySet()) {
                String name = "Employee " + empId; // Default
                try {
                    // Try to get employee name
                    Employee emp = attendanceService.findEmployeeById(empId);
                    if (emp != null) {
                        name = emp.getFirstName() + " " + emp.getLastName();
                    }
                } catch (Exception e) {
                    logger.warn("Could not get name for employee {}", empId);
                }
                employeeNames.put(empId, name);
            }

            logger.info("Returning attendance for {} employees", attendanceMap.size());

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "year", year,
                    "month", month,
                    "attendance", attendanceMap,
                    "employeeNames", employeeNames,
                    "totalEmployees", attendanceMap.size(),
                    "timestamp", LocalDateTime.now()
            ));
        } catch (Exception e) {
            logger.error("Error fetching all employees calendar attendance: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "error", e.getMessage()
            ));
        }
    }

    // Add these methods to AttendanceController.java

    /**
     * Get employee's recent attendance (for Recent Attendance section)
     * URL: /api/attendance/employee/recent/{employeeId}
     */
    @GetMapping("/employee/recent/{employeeId}")
    public ResponseEntity<?> getEmployeeRecentAttendance(
            @PathVariable String employeeId,
            @RequestParam(defaultValue = "30") int days) {

        try {
            List<Map<String, Object>> recentAttendances =
                    attendanceService.getRecentAttendanceForEmployee(employeeId, days);

            // Format for display
            recentAttendances.forEach(att -> {
                LocalDate date = (LocalDate) att.get("attendanceDate");
                att.put("dateFormatted", date.format(DateTimeFormatter.ofPattern("MMM d, yyyy")));
                att.put("day", date.getDayOfWeek().getDisplayName(TextStyle.FULL, Locale.US));

                // Add status badge
                String status = (String) att.get("status");
                String badgeClass = "present";
                if ("Absent".equalsIgnoreCase(status) || "OFFLINE".equals(status)) {
                    badgeClass = "absent";
                } else if ("LEAVE".equals(status)) {
                    badgeClass = "leave";
                }
                att.put("badgeClass", badgeClass);
            });

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "recentAttendances", recentAttendances,
                    "totalDays", days
            ));

        } catch (Exception e) {
            logger.error("Error fetching recent attendance: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "error", e.getMessage()
            ));
        }
    }

    /**
     * Get employee's monthly summary (for January 2026 Summary section)
     * URL: /api/attendance/employee/summary/{employeeId}/{year}/{month}
     */
    @GetMapping("/employee/summary/{employeeId}/{year}/{month}")
    public ResponseEntity<?> getEmployeeMonthlySummary(
            @PathVariable String employeeId,
            @PathVariable int year,
            @PathVariable int month) {

        try {
            Map<String, Object> summary =
                    attendanceService.getMonthlySummaryForEmployee(employeeId, year, month);

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "summary", summary,
                    "employeeId", employeeId,
                    "year", year,
                    "month", month
            ));

        } catch (Exception e) {
            logger.error("Error fetching monthly summary: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "error", e.getMessage()
            ));
        }
    }

    /**
     * Get employee's timesheet (for Timesheet tab)
     * URL: /api/attendance/employee/timesheet/{employeeId}/{year}/{month}
     */
    @GetMapping("/employee/timesheet/{employeeId}/{year}/{month}")
    public ResponseEntity<?> getEmployeeTimesheet(
            @PathVariable String employeeId,
            @PathVariable int year,
            @PathVariable int month) {

        try {
            List<Map<String, Object>> timesheet =
                    attendanceService.getTimesheetForEmployee(employeeId, year, month);

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "timesheet", timesheet,
                    "employeeId", employeeId,
                    "year", year,
                    "month", month,
                    "totalRecords", timesheet.size()
            ));

        } catch (Exception e) {
            logger.error("Error fetching timesheet: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "error", e.getMessage()
            ));
        }
    }

    /**
     * Get today's attendance for specific employee
     * URL: /api/attendance/employee/today/{employeeId}
     */
    @GetMapping("/employee/today/{employeeId}")
    public ResponseEntity<?> getEmployeeTodayAttendance(@PathVariable String employeeId) {
        try {
            Attendance attendance = attendanceService.getTodayAttendance(employeeId);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);

            if (attendance != null) {
                response.put("hasAttendance", true);
                response.put("status", attendance.getStatus());

                // CRITICAL FIX: Return ISO format, not formatted string!
                if (attendance.getCheckInTime() != null) {
                    response.put("checkInTime", attendance.getCheckInTime().toString()); // ISO format
                    response.put("checkIn", attendance.getCheckInTime().toString());

                    DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("hh:mm a");
                    response.put("checkInTimeFormatted", attendance.getCheckInTime().format(timeFormatter));
                } else {
                    response.put("checkInTime", null);
                    response.put("checkInTimeFormatted", "--:--");
                }

                if (attendance.getCheckOutTime() != null) {
                    response.put("checkOutTime", attendance.getCheckOutTime().toString()); // ISO format
                    response.put("checkOut", attendance.getCheckOutTime().toString());

                    DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("hh:mm a");
                    response.put("checkOutTimeFormatted", attendance.getCheckOutTime().format(timeFormatter));
                } else {
                    response.put("checkOutTime", null);
                    response.put("checkOutTimeFormatted", "--:--");
                }

                response.put("breakMinutes", attendance.getTotalBreakMinutes());
                response.put("workingTime", attendance.getCurrentSessionTime());

                // Calculate working hours
                if (attendance.getCheckInTime() != null) {
                    LocalDateTime checkIn = attendance.getCheckInTime();
                    LocalDateTime now = LocalDateTime.now();
                    long minutes = Duration.between(checkIn, now).toMinutes() - attendance.getTotalBreakMinutes();
                    response.put("totalHours", String.format("%dh %02dm", minutes / 60, minutes % 60));
                }
            } else {
                response.put("hasAttendance", false);
                response.put("status", "Not Checked In");
            }

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            logger.error("Error fetching today's attendance: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "error", e.getMessage()
            ));
        }
    }

    /**
     * Get attendance for a specific date (for testing)
     */
    @GetMapping("/date/{date}")
    public ResponseEntity<?> getAttendanceBySpecificDate(
            @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        try {
            List<Map<String, Object>> attendances = attendanceService.getAttendanceByDate(date, null, null);

            attendances.forEach(this::formatAttendanceForDisplay);

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "date", date,
                    "attendances", attendances,
                    "total", attendances.size()
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "error", e.getMessage()
            ));
        }
    }
}