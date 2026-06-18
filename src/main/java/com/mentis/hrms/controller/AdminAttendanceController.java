package com.mentis.hrms.controller;

import com.mentis.hrms.model.Attendance;
import com.mentis.hrms.model.Employee;
import com.mentis.hrms.repository.AttendanceRepository;
import com.mentis.hrms.service.AttendanceService;
import com.mentis.hrms.service.EmployeeService;
import jakarta.servlet.http.HttpSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/dashboard/admin/attendance")
public class AdminAttendanceController {

    private static final Logger logger = LoggerFactory.getLogger(AdminAttendanceController.class);

    @Autowired
    private AttendanceService attendanceService;

    @Autowired
    private EmployeeService employeeService;

    @Autowired
    private AttendanceRepository attendanceRepository;

    /**
     * API endpoint for admin to get today's live attendance
     * FIXED: Shows ONLY real data, no fake defaults
     */
    @GetMapping("/api/today")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> getTodayAttendance(HttpSession session) {
        Map<String, Object> response = new HashMap<>();

        String role = (String) session.getAttribute("userRole");
        if (!"SUPER_ADMIN".equals(role)) {
            response.put("success", false);
            response.put("error", "Unauthorized");
            return ResponseEntity.status(403).body(response);
        }

        try {
            LocalDate today = LocalDate.now();

            // Get ALL attendance records for today using your service
            // This returns List<Map<String, Object>> with all the processing already done
            List<Map<String, Object>> attendances = attendanceService.getTodayAllAttendances();

            // Get all active employees for reference
            List<Employee> allEmployees = employeeService.getAllEmployees();
            Set<String> employeesWithAttendance = attendances.stream()
                    .map(a -> (String) a.get("employeeId"))
                    .collect(Collectors.toSet());

            // Create a list to hold ALL employees (with or without attendance)
            List<Map<String, Object>> completeList = new ArrayList<>();

            // First add all employees with REAL attendance data
            for (Map<String, Object> att : attendances) {
                // Ensure checkout flag is set correctly
                boolean isCheckedOut = att.get("checkOutTime") != null;
                att.put("isCheckedOut", isCheckedOut);
                att.put("hasCheckedOut", isCheckedOut);

                // Set display status
                if (isCheckedOut) {
                    att.put("displayStatus", "Checked Out");
                } else if ("ACTIVE".equals(att.get("status"))) {
                    att.put("displayStatus", "Present");
                } else if ("BREAK".equals(att.get("status"))) {
                    att.put("displayStatus", "On Break");
                } else {
                    att.put("displayStatus", "Unknown");
                }

                completeList.add(att);
            }

            // Then add employees with NO attendance - show as ABSENT with no times
            for (Employee emp : allEmployees) {
                if (!emp.isActive()) continue; // Skip inactive employees

                if (!employeesWithAttendance.contains(emp.getEmployeeId())) {
                    Map<String, Object> absentMap = new HashMap<>();
                    absentMap.put("employeeId", emp.getEmployeeId());
                    absentMap.put("employeeName", emp.getFirstName() + " " + emp.getLastName());
                    absentMap.put("department", emp.getDepartment() != null ? emp.getDepartment() : "N/A");
                    absentMap.put("checkInTime", "--:--");
                    absentMap.put("checkOutTime", "--:--");
                    absentMap.put("workingHours", "--:--");
                    absentMap.put("displayStatus", "Absent");
                    absentMap.put("status", "ABSENT");
                    absentMap.put("isCheckedOut", false);
                    absentMap.put("hasCheckedOut", false);
                    absentMap.put("checkInFormatted", "--:--");
                    absentMap.put("checkOutFormatted", "--:--");

                    completeList.add(absentMap);
                }
            }

            // Sort by employee ID for consistency
            completeList.sort((a, b) -> {
                String idA = (String) a.get("employeeId");
                String idB = (String) b.get("employeeId");
                return idA.compareTo(idB);
            });

            response.put("success", true);
            response.put("attendances", completeList);
            response.put("count", completeList.size());
            response.put("date", today.toString());
            response.put("message", "Real attendance data only - no fake times");

        } catch (Exception e) {
            logger.error("Error fetching today's attendance for admin", e);
            response.put("success", false);
            response.put("error", e.getMessage());
        }

        return ResponseEntity.ok(response);
    }

    /**
     * Helper method to calculate working hours
     */
    private String calculateWorkingHours(Attendance attendance) {
        if (attendance.getCheckInTime() == null) {
            return "--:--";
        }

        LocalDateTime endTime = attendance.getCheckOutTime() != null ?
                attendance.getCheckOutTime() : LocalDateTime.now();

        long totalMinutes = java.time.Duration.between(attendance.getCheckInTime(), endTime).toMinutes();
        totalMinutes -= attendance.getTotalBreakMinutes() != null ? attendance.getTotalBreakMinutes() : 0;

        if (totalMinutes < 0) totalMinutes = 0;

        long hours = totalMinutes / 60;
        long minutes = totalMinutes % 60;

        return String.format("%dh %02dm", hours, minutes);
    }

    /**
     * API endpoint for admin to get daily attendance by date
     * FIXED: Shows real data only, no fake defaults
     */
    @GetMapping("/api/daily")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> getDailyAttendance(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam(required = false) String department,
            @RequestParam(required = false) String status,
            HttpSession session) {

        Map<String, Object> response = new HashMap<>();

        String role = (String) session.getAttribute("userRole");
        if (!"SUPER_ADMIN".equals(role)) {
            response.put("success", false);
            response.put("error", "Unauthorized");
            return ResponseEntity.status(403).body(response);
        }

        try {
            // Get REAL attendance data from service
            List<Map<String, Object>> attendances = attendanceService.getAttendanceByDate(
                    date,
                    department != null ? department : "",
                    status != null ? status : ""
            );

            // Get all active employees
            List<Employee> allEmployees = employeeService.getAllEmployees();
            Set<String> employeesWithAttendance = attendances.stream()
                    .map(a -> (String) a.get("employeeId"))
                    .collect(Collectors.toSet());

            List<Map<String, Object>> completeList = new ArrayList<>();

            // Add REAL attendance records
            for (Map<String, Object> att : attendances) {
                boolean isCheckedOut = att.get("checkOutTime") != null;
                att.put("isCheckedOut", isCheckedOut);
                att.put("hasCheckedOut", isCheckedOut);

                // Set display status
                if (isCheckedOut) {
                    att.put("displayStatus", "Checked Out");
                } else if ("ACTIVE".equals(att.get("status"))) {
                    att.put("displayStatus", "Present");
                } else if ("BREAK".equals(att.get("status"))) {
                    att.put("displayStatus", "On Break");
                } else {
                    att.put("displayStatus", "Present"); // Default for checked-in but not active?
                }

                completeList.add(att);
            }

            // Add employees with NO attendance - show as ABSENT
            for (Employee emp : allEmployees) {
                if (!emp.isActive()) continue;

                if (!employeesWithAttendance.contains(emp.getEmployeeId())) {
                    // Check if we should apply department filter
                    if (department != null && !department.isEmpty() &&
                            !department.equalsIgnoreCase(emp.getDepartment())) {
                        continue;
                    }

                    Map<String, Object> absentMap = new HashMap<>();
                    absentMap.put("employeeId", emp.getEmployeeId());
                    absentMap.put("employeeName", emp.getFirstName() + " " + emp.getLastName());
                    absentMap.put("department", emp.getDepartment() != null ? emp.getDepartment() : "N/A");
                    absentMap.put("checkInFormatted", "--:--");
                    absentMap.put("checkOutFormatted", "--:--");
                    absentMap.put("workingHours", "--:--");
                    absentMap.put("displayStatus", "Absent");
                    absentMap.put("status", "ABSENT");
                    absentMap.put("isCheckedOut", false);
                    absentMap.put("hasCheckedOut", false);

                    completeList.add(absentMap);
                }
            }

            // Apply status filter if needed
            if (status != null && !status.isEmpty()) {
                completeList = completeList.stream()
                        .filter(a -> status.equalsIgnoreCase((String) a.get("displayStatus")))
                        .collect(Collectors.toList());
            }

            // Sort by employee ID
            completeList.sort((a, b) -> {
                String idA = (String) a.get("employeeId");
                String idB = (String) b.get("employeeId");
                return idA.compareTo(idB);
            });

            response.put("success", true);
            response.put("attendances", completeList);
            response.put("count", completeList.size());
            response.put("date", date.toString());

        } catch (Exception e) {
            logger.error("Error fetching daily attendance for admin", e);
            response.put("success", false);
            response.put("error", e.getMessage());
        }

        return ResponseEntity.ok(response);
    }

    /**
     * API endpoint for admin to get attendance for a specific employee
     */
    @GetMapping("/api/employee/{employeeId}")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> getEmployeeAttendance(
            @PathVariable String employeeId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            HttpSession session) {

        Map<String, Object> response = new HashMap<>();

        String role = (String) session.getAttribute("userRole");
        if (!"SUPER_ADMIN".equals(role)) {
            response.put("success", false);
            response.put("error", "Unauthorized");
            return ResponseEntity.status(403).body(response);
        }

        try {
            // Use the existing service method to get attendance for this employee on this date
            Attendance attendance = attendanceService.getTodayAttendance(employeeId);

            // Check if the attendance is for the requested date
            if (attendance != null && attendance.getAttendanceDate().equals(date)) {
                Map<String, Object> attMap = new HashMap<>();
                attMap.put("id", attendance.getId());
                attMap.put("employeeId", attendance.getEmployeeId());
                attMap.put("date", attendance.getAttendanceDate().toString());
                attMap.put("checkInTime", attendance.getCheckInTime());
                attMap.put("checkOutTime", attendance.getCheckOutTime());
                attMap.put("status", attendance.getStatus());
                attMap.put("isCheckedOut", attendance.getCheckOutTime() != null);

                // Format times for display
                DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("hh:mm a");
                if (attendance.getCheckInTime() != null) {
                    attMap.put("checkInFormatted", attendance.getCheckInTime().format(timeFormatter));
                } else {
                    attMap.put("checkInFormatted", "--:--");
                }

                if (attendance.getCheckOutTime() != null) {
                    attMap.put("checkOutFormatted", attendance.getCheckOutTime().format(timeFormatter));
                } else {
                    attMap.put("checkOutFormatted", "--:--");
                }

                // Calculate working hours
                if (attendance.getCheckInTime() != null) {
                    if (attendance.getCheckOutTime() != null) {
                        long minutes = java.time.Duration.between(attendance.getCheckInTime(), attendance.getCheckOutTime()).toMinutes();
                        minutes -= attendance.getTotalBreakMinutes() != null ? attendance.getTotalBreakMinutes() : 0;
                        long hours = minutes / 60;
                        long mins = minutes % 60;
                        attMap.put("workingHours", String.format("%dh %02dm", hours, mins));
                    } else {
                        // Currently working
                        long minutes = java.time.Duration.between(attendance.getCheckInTime(), LocalDateTime.now()).toMinutes();
                        minutes -= attendance.getTotalBreakMinutes() != null ? attendance.getTotalBreakMinutes() : 0;
                        long hours = minutes / 60;
                        long mins = minutes % 60;
                        attMap.put("workingHours", String.format("%dh %02dm (ongoing)", hours, mins));
                    }
                } else {
                    attMap.put("workingHours", "--:--");
                }

                response.put("success", true);
                response.put("attendance", attMap);
            } else {
                response.put("success", true);
                response.put("attendance", null);
                response.put("message", "No attendance found for this employee on " + date);
            }

        } catch (Exception e) {
            logger.error("Error fetching employee attendance", e);
            response.put("success", false);
            response.put("error", e.getMessage());
        }

        return ResponseEntity.ok(response);
    }

    /**
     * API endpoint for admin to update attendance (check-in/check-out times)
     */
    @PostMapping("/api/update")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> updateAttendance(
            @RequestBody Map<String, Object> payload,
            HttpSession session) {

        Map<String, Object> response = new HashMap<>();

        // Verify super admin
        String role = (String) session.getAttribute("userRole");
        if (!"SUPER_ADMIN".equals(role)) {
            response.put("success", false);
            response.put("error", "Unauthorized");
            return ResponseEntity.status(403).body(response);
        }

        try {
            Long attendanceId = Long.valueOf(payload.get("attendanceId").toString());
            String checkInTimeStr = (String) payload.get("checkInTime");
            String checkOutTimeStr = (String) payload.get("checkOutTime");
            String status = (String) payload.get("status");
            String remarks = (String) payload.get("remarks");

            // Find the attendance record
            Optional<Attendance> attendanceOpt = attendanceRepository.findById(attendanceId);

            if (attendanceOpt.isEmpty()) {
                response.put("success", false);
                response.put("error", "Attendance record not found");
                return ResponseEntity.badRequest().body(response);
            }

            Attendance attendance = attendanceOpt.get();

            // Parse and update check-in time if provided
            if (checkInTimeStr != null && !checkInTimeStr.isEmpty()) {
                try {
                    // Handle different time formats
                    LocalDateTime checkInTime = parseDateTime(attendance.getAttendanceDate(), checkInTimeStr);
                    attendance.setCheckInTime(checkInTime);
                } catch (Exception e) {
                    logger.error("Error parsing check-in time: {}", e.getMessage());
                }
            }

            // Parse and update check-out time if provided
            if (checkOutTimeStr != null && !checkOutTimeStr.isEmpty() && !checkOutTimeStr.equals("--:--")) {
                try {
                    LocalDateTime checkOutTime = parseDateTime(attendance.getAttendanceDate(), checkOutTimeStr);
                    attendance.setCheckOutTime(checkOutTime);

                    // Recalculate working minutes
                    if (attendance.getCheckInTime() != null) {
                        long totalMinutes = java.time.Duration.between(attendance.getCheckInTime(), checkOutTime).toMinutes();
                        int workingMinutes = (int) (totalMinutes - (attendance.getTotalBreakMinutes() != null ? attendance.getTotalBreakMinutes() : 0));
                        attendance.setTotalWorkingMinutes(Math.max(0, workingMinutes));

                        long hours = workingMinutes / 60;
                        long mins = workingMinutes % 60;
                        attendance.setTotalWorkingHours(String.format("%dh %02dm", hours, mins));
                    }

                    attendance.setIsActiveSession(false);
                } catch (Exception e) {
                    logger.error("Error parsing check-out time: {}", e.getMessage());
                }
            }

            // Update status if provided
            if (status != null && !status.isEmpty()) {
                attendance.setStatus(status);
            }

            // Save the updated attendance
            attendance = attendanceRepository.save(attendance);

            // Update employee presence if needed
            if (attendance.getCheckOutTime() != null) {
                attendanceService.updateEmployeePresence(attendance.getEmployeeId(), "OFFLINE");
            } else if ("ACTIVE".equals(attendance.getStatus())) {
                attendanceService.updateEmployeePresence(attendance.getEmployeeId(), "ACTIVE");
            } else if ("BREAK".equals(attendance.getStatus())) {
                attendanceService.updateEmployeePresence(attendance.getEmployeeId(), "BREAK");
            }

            response.put("success", true);
            response.put("message", "Attendance updated successfully");
            response.put("attendanceId", attendance.getId());

        } catch (Exception e) {
            logger.error("Error updating attendance", e);
            response.put("success", false);
            response.put("error", e.getMessage());
        }

        return ResponseEntity.ok(response);
    }

    /**
     * Helper method to parse time string into LocalDateTime for a given date
     */
    private LocalDateTime parseDateTime(LocalDate date, String timeStr) {
        try {
            // Try parsing as "hh:mm a" format (e.g., "02:30 PM")
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("hh:mm a");
            java.time.LocalTime time = java.time.LocalTime.parse(timeStr.trim().toUpperCase(), formatter);
            return LocalDateTime.of(date, time);
        } catch (Exception e) {
            try {
                // Try parsing as "HH:mm" format (e.g., "14:30")
                DateTimeFormatter formatter = DateTimeFormatter.ofPattern("HH:mm");
                java.time.LocalTime time = java.time.LocalTime.parse(timeStr.trim(), formatter);
                return LocalDateTime.of(date, time);
            } catch (Exception ex) {
                // Try parsing as ISO format
                return LocalDateTime.parse(timeStr);
            }
        }
    }

    /**
     * API endpoint for admin to get attendance statistics
     */
    @GetMapping("/api/stats")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> getAttendanceStats(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            HttpSession session) {

        Map<String, Object> response = new HashMap<>();

        String role = (String) session.getAttribute("userRole");
        if (!"SUPER_ADMIN".equals(role)) {
            response.put("success", false);
            response.put("error", "Unauthorized");
            return ResponseEntity.status(403).body(response);
        }

        try {
            LocalDate targetDate = date != null ? date : LocalDate.now();

            // Get all attendances for the date
            List<Map<String, Object>> attendances = attendanceService.getAttendanceByDate(targetDate, "", "");

            int present = 0;
            int checkedOut = 0;
            int onBreak = 0;
            int absent = 0;

            for (Map<String, Object> att : attendances) {
                boolean isCheckedOut = (boolean) att.getOrDefault("isCheckedOut", false);
                String status = (String) att.get("status");

                if (isCheckedOut) {
                    checkedOut++;
                } else if ("ACTIVE".equals(status)) {
                    present++;
                } else if ("BREAK".equals(status)) {
                    onBreak++;
                } else {
                    absent++;
                }
            }

            // Get total employees
            List<Employee> allEmployees = employeeService.getAllEmployees();
            int totalEmployees = (int) allEmployees.stream().filter(Employee::isActive).count();

            Map<String, Object> stats = new HashMap<>();
            stats.put("totalEmployees", totalEmployees);
            stats.put("present", present);
            stats.put("checkedOut", checkedOut);
            stats.put("onBreak", onBreak);
            stats.put("absent", absent);
            stats.put("attendancePercentage", totalEmployees > 0 ?
                    ((present + onBreak) * 100 / totalEmployees) : 0);
            stats.put("date", targetDate.toString());

            response.put("success", true);
            response.put("stats", stats);

        } catch (Exception e) {
            logger.error("Error fetching attendance stats", e);
            response.put("success", false);
            response.put("error", e.getMessage());
        }

        return ResponseEntity.ok(response);
    }

    /**
     * API endpoint for admin to manually check-in an employee
     */
    @PostMapping("/api/checkin")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> manualCheckIn(
            @RequestBody Map<String, String> payload,
            HttpSession session) {

        Map<String, Object> response = new HashMap<>();

        String role = (String) session.getAttribute("userRole");
        if (!"SUPER_ADMIN".equals(role)) {
            response.put("success", false);
            response.put("error", "Unauthorized");
            return ResponseEntity.status(403).body(response);
        }

        try {
            String employeeId = payload.get("employeeId");

            // Check if employee exists
            Optional<Employee> empOpt = employeeService.getEmployeeByEmployeeId(employeeId);
            if (empOpt.isEmpty()) {
                response.put("success", false);
                response.put("error", "Employee not found");
                return ResponseEntity.badRequest().body(response);
            }

            // Perform check-in using existing service
            Attendance attendance = attendanceService.checkIn(employeeId);

            response.put("success", true);
            response.put("message", "Employee checked in successfully");
            response.put("attendanceId", attendance.getId());
            response.put("checkInTime", attendance.getCheckInTime());

        } catch (Exception e) {
            logger.error("Error during manual check-in", e);
            response.put("success", false);
            response.put("error", e.getMessage());
        }

        return ResponseEntity.ok(response);
    }

    /**
     * API endpoint for admin to manually check-out an employee
     */
    @PostMapping("/api/checkout")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> manualCheckOut(
            @RequestBody Map<String, String> payload,
            HttpSession session) {

        Map<String, Object> response = new HashMap<>();

        String role = (String) session.getAttribute("userRole");
        if (!"SUPER_ADMIN".equals(role)) {
            response.put("success", false);
            response.put("error", "Unauthorized");
            return ResponseEntity.status(403).body(response);
        }

        try {
            String employeeId = payload.get("employeeId");

            // Check if employee exists
            Optional<Employee> empOpt = employeeService.getEmployeeByEmployeeId(employeeId);
            if (empOpt.isEmpty()) {
                response.put("success", false);
                response.put("error", "Employee not found");
                return ResponseEntity.badRequest().body(response);
            }

            // Perform check-out using existing service
            Attendance attendance = attendanceService.checkOut(employeeId);

            response.put("success", true);
            response.put("message", "Employee checked out successfully");
            response.put("attendanceId", attendance.getId());
            response.put("checkOutTime", attendance.getCheckOutTime());
            response.put("workingHours", attendance.getTotalWorkingHours());

        } catch (Exception e) {
            logger.error("Error during manual check-out", e);
            response.put("success", false);
            response.put("error", e.getMessage());
        }

        return ResponseEntity.ok(response);
    }
}