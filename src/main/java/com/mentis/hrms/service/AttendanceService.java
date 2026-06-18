package com.mentis.hrms.service;

import com.mentis.hrms.model.Attendance;
import com.mentis.hrms.model.Employee;
import com.mentis.hrms.model.LeaveRequest;
import com.mentis.hrms.repository.AttendanceRepository;
import com.mentis.hrms.repository.EmployeeRepository;
import com.mentis.hrms.repository.LeaveRequestRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Transactional
public class AttendanceService {

    private static final Logger logger = LoggerFactory.getLogger(AttendanceService.class);

    @Autowired
    private AttendanceRepository attendanceRepository;

    @Autowired
    private EmployeeRepository employeeRepository;

    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    @Autowired
    private LeaveRequestRepository leaveRequestRepository;

    // ========== WEBSOCKET BROADCAST METHODS ==========

    private void broadcastAttendanceUpdate(String employeeId, String status, LocalDateTime checkOutTime) {
        Map<String, Object> update = new HashMap<>();
        update.put("type", "ATTENDANCE_UPDATE");
        update.put("employeeId", employeeId);
        update.put("status", status);
        update.put("checkOutTime", checkOutTime);
        update.put("isCheckedOut", checkOutTime != null);
        update.put("timestamp", LocalDateTime.now());

        messagingTemplate.convertAndSend("/topic/attendance/updates", update);
    }

    private void broadcastDailyAttendanceUpdate(String employeeId, Attendance attendance) {
        Map<String, Object> update = new HashMap<>();
        update.put("type", "DAILY_ATTENDANCE_UPDATE");
        update.put("employeeId", employeeId);
        update.put("status", attendance.getStatus());
        update.put("checkInTime", attendance.getCheckInTime());
        update.put("checkOutTime", attendance.getCheckOutTime());
        update.put("isCheckedOut", attendance.getCheckOutTime() != null);
        update.put("totalBreakMinutes", attendance.getTotalBreakMinutes());
        update.put("currentWorkingTime", attendance.getCurrentSessionTime());
        update.put("timestamp", LocalDateTime.now());

        messagingTemplate.convertAndSend("/topic/attendance/daily", update);
    }

    // ========== FIXED CHECKIN METHOD ==========

    public Attendance checkIn(String employeeId) {
        LocalDate today = LocalDate.now();
        logger.info("====== checkIn() CALLED ======");
        logger.info("Employee: {}, Date: {}", employeeId, today);

        // CRITICAL: Check if there's already an ACTIVE session today
        Optional<Attendance> activeSession = attendanceRepository
                .findActiveAttendanceByEmployeeAndDate(employeeId, today);

        if (activeSession.isPresent()) {
            Attendance attendance = activeSession.get();
            logger.info("Employee {} already has active session at {}",
                    employeeId, attendance.getCheckInTime());
            return attendance;
        }

        // Check if there's a completed session today (checked out)
        List<Attendance> todayAttendances = attendanceRepository
                .findByEmployeeIdAndAttendanceDate(employeeId, today);

        // Get session count for numbering
        long sessionCount = attendanceRepository.countByEmployeeIdAndAttendanceDate(employeeId, today);
        int sessionNumber = (int) sessionCount + 1;

        // Create new attendance record
        Attendance attendance = new Attendance();
        attendance.setEmployeeId(employeeId);
        attendance.setAttendanceDate(today);
        attendance.setCheckInTime(LocalDateTime.now());
        attendance.setStatus("ACTIVE");
        attendance.setTotalBreakMinutes(0);
        attendance.setTotalWorkingMinutes(0);
        attendance.setSessionNumber(sessionNumber);
        attendance.setIsActiveSession(true);

        logger.info("Creating attendance record session {} for {}", sessionNumber, employeeId);

        attendance = attendanceRepository.save(attendance);
        updateEmployeePresence(employeeId, "ACTIVE");

        // Broadcast update
        try {
            broadcastAttendanceUpdate(employeeId, "ACTIVE", null);
            broadcastDailyAttendanceUpdate(employeeId, attendance);
        } catch (Exception e) {
            logger.error("Failed to broadcast checkin: {}", e.getMessage());
        }

        return attendance;
    }

    // ========== FIXED CHECKOUT METHOD ==========

    public Attendance checkOut(String employeeId) {
        LocalDate today = LocalDate.now();
        logger.info("====== checkOut() CALLED for {} ======", employeeId);

        // CRITICAL: Find the active session (not checked out)
        Optional<Attendance> activeAttendance = attendanceRepository
                .findActiveAttendanceByEmployeeAndDate(employeeId, today);

        Attendance attendance;

        if (activeAttendance.isPresent()) {
            // Use the active session
            attendance = activeAttendance.get();
            logger.info("Found active attendance for {} with checkIn at {}",
                    employeeId, attendance.getCheckInTime());
        } else {
            // No active session - check if there's any attendance today
            List<Attendance> todayAttendances = attendanceRepository
                    .findByEmployeeIdAndAttendanceDate(employeeId, today);

            if (!todayAttendances.isEmpty()) {
                // Get the most recent attendance
                attendance = todayAttendances.get(todayAttendances.size() - 1);

                // If already checked out, log and return
                if (attendance.getCheckOutTime() != null) {
                    logger.info("Employee {} already checked out at {}",
                            employeeId, attendance.getCheckOutTime());
                    return attendance;
                }
            } else {
                // No record found - this shouldn't happen normally
                logger.warn("No attendance found for {}, creating checkout record", employeeId);
                attendance = new Attendance();
                attendance.setEmployeeId(employeeId);
                attendance.setAttendanceDate(today);
                attendance.setCheckInTime(LocalDateTime.now().minusHours(8)); // Default 8 hours
                attendance.setTotalBreakMinutes(0);
                attendance.setSessionNumber(1);
                attendance.setIsActiveSession(false);
            }
        }

        // End break if on break
        if ("BREAK".equals(attendance.getStatus()) && attendance.getBreakStartTime() != null) {
            attendance.setBreakEndTime(LocalDateTime.now());
            long breakMinutes = Duration.between(
                    attendance.getBreakStartTime(),
                    attendance.getBreakEndTime()
            ).toMinutes();
            attendance.setTotalBreakMinutes(
                    attendance.getTotalBreakMinutes() + (int) breakMinutes
            );
        }

        // Perform checkout
        LocalDateTime checkOutTime = LocalDateTime.now();
        attendance.setCheckOutTime(checkOutTime);
        attendance.setIsActiveSession(false);

        // Calculate working minutes
        long totalMinutes = Duration.between(
                attendance.getCheckInTime(),
                checkOutTime
        ).toMinutes();

        int workingMinutes = (int) (totalMinutes - attendance.getTotalBreakMinutes());
        attendance.setTotalWorkingMinutes(Math.max(0, workingMinutes));

        // Format working hours
        long hours = workingMinutes / 60;
        long mins = workingMinutes % 60;
        attendance.setTotalWorkingHours(String.format("%dh %02dm", hours, mins));

        attendance.setStatus("OFFLINE");
        attendance.setUpdatedAt(LocalDateTime.now());

        // CRITICAL: Save the updated attendance
        attendance = attendanceRepository.save(attendance);

        // Update employee presence
        updateEmployeePresence(employeeId, "OFFLINE");

        logger.info("Employee {} checked out at {}. Working minutes: {}",
                employeeId, checkOutTime, workingMinutes);

        // Broadcast updates
        try {
            broadcastAttendanceUpdate(employeeId, "OFFLINE", checkOutTime);
            broadcastDailyAttendanceUpdate(employeeId, attendance);
        } catch (Exception e) {
            logger.error("Failed to broadcast checkout update: {}", e.getMessage());
        }

        return attendance;
    }

    // ========== FIXED METHOD TO GET TODAY'S ATTENDANCE (removes duplicates) ==========

    public List<Map<String, Object>> getTodayAllAttendances() {
        LocalDate today = LocalDate.now();

        // Get ALL attendances for today
        List<Attendance> allAttendances = attendanceRepository.findByAttendanceDateOrderByCheckInTimeDesc(today);

        // CRITICAL: Remove duplicates - keep only the latest session per employee
        Map<String, Attendance> latestPerEmployee = new HashMap<>();

        for (Attendance att : allAttendances) {
            String empId = att.getEmployeeId();
            if (!latestPerEmployee.containsKey(empId)) {
                latestPerEmployee.put(empId, att);
            } else {
                Attendance existing = latestPerEmployee.get(empId);
                // If this attendance is more recent, replace
                if (att.getCheckInTime() != null && existing.getCheckInTime() != null &&
                        att.getCheckInTime().isAfter(existing.getCheckInTime())) {
                    latestPerEmployee.put(empId, att);
                }
            }
        }

        DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("hh:mm a");

        return latestPerEmployee.values().stream().map(att -> {
            Map<String, Object> map = new HashMap<>();
            map.put("employeeId", att.getEmployeeId());
            map.put("status", att.getStatus());
            map.put("checkInTime", att.getCheckInTime());
            map.put("checkOutTime", att.getCheckOutTime());

            // CRITICAL: Check for checkout
            boolean isCheckedOut = att.getCheckOutTime() != null;
            map.put("isCheckedOut", isCheckedOut);
            map.put("hasCheckedOut", isCheckedOut);

            // Format times
            if (att.getCheckInTime() != null) {
                map.put("checkInFormatted", att.getCheckInTime().format(timeFormatter));
            } else {
                map.put("checkInFormatted", "--:--");
            }

            if (att.getCheckOutTime() != null) {
                map.put("checkOutFormatted", att.getCheckOutTime().format(timeFormatter));
                map.put("displayStatus", "Checked Out");
                map.put("statusText", "Checked Out");
            } else {
                map.put("checkOutFormatted", "--:--");
                map.put("displayStatus", att.getStatus());
                map.put("statusText", getStatusDisplay(att.getStatus()));
            }

            // Calculate working hours
            calculateCurrentSessionTime(att);
            map.put("currentWorkingTime", att.getCurrentSessionTime());
            map.put("workingHours", att.getTotalWorkingHours() != null ?
                    att.getTotalWorkingHours() : calculateWorkingHoursDisplay(att));

            // Get employee details
            Optional<Employee> empOpt = employeeRepository.findByEmployeeId(att.getEmployeeId());
            if (empOpt.isPresent()) {
                Employee emp = empOpt.get();
                map.put("employeeName", emp.getFirstName() + " " + emp.getLastName());
                map.put("department", emp.getDepartment() != null ? emp.getDepartment() : "N/A");
                map.put("designation", emp.getDesignation() != null ? emp.getDesignation() : "N/A");
            } else {
                map.put("employeeName", "Unknown Employee");
                map.put("department", "N/A");
                map.put("designation", "N/A");
            }

            return map;
        }).collect(Collectors.toList());
    }

    // ========== FIXED METHOD FOR DAILY ATTENDANCE ==========

    // ========== 1. FIX getAttendanceByDate() - Used by HR Daily Attendance ==========

    public List<Map<String, Object>> getAttendanceByDate(LocalDate date, String department, String status) {
        List<Attendance> allAttendances = attendanceRepository.findByAttendanceDate(date);

        // Remove duplicates - keep latest per employee
        Map<String, Attendance> latestPerEmployee = new HashMap<>();
        for (Attendance att : allAttendances) {
            String empId = att.getEmployeeId();
            if (!latestPerEmployee.containsKey(empId)) {
                latestPerEmployee.put(empId, att);
            } else {
                Attendance existing = latestPerEmployee.get(empId);
                if (att.getCheckInTime() != null && existing.getCheckInTime() != null &&
                        att.getCheckInTime().isAfter(existing.getCheckInTime())) {
                    latestPerEmployee.put(empId, att);
                }
            }
        }

        DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("hh:mm a");

        return latestPerEmployee.values().stream()
                .filter(att -> department == null || department.isEmpty() || matchesDepartment(att.getEmployeeId(), department))
                .filter(att -> {
                    if (status == null || status.isEmpty()) return true;
                    String displayStatus = getDisplayStatusForFilter(att);
                    return status.equalsIgnoreCase(displayStatus);
                })
                .map(att -> {
                    Map<String, Object> map = new HashMap<>();
                    map.put("employeeId", att.getEmployeeId());
                    map.put("status", att.getStatus());
                    map.put("checkInTime", att.getCheckInTime());
                    map.put("checkOutTime", att.getCheckOutTime());

                    // CRITICAL: Add checkout flag
                    boolean isCheckedOut = att.getCheckOutTime() != null;
                    map.put("isCheckedOut", isCheckedOut);
                    map.put("hasCheckedOut", isCheckedOut);

                    // Format times
                    if (att.getCheckInTime() != null) {
                        map.put("checkInFormatted", att.getCheckInTime().format(timeFormatter));
                    } else {
                        map.put("checkInFormatted", "--:--");
                    }

                    // CRITICAL: Handle checkout display
                    if (att.getCheckOutTime() != null) {
                        map.put("checkOutFormatted", att.getCheckOutTime().format(timeFormatter));
                        map.put("displayStatus", "Checked Out");
                        map.put("statusForDisplay", "Checked Out");
                        map.put("statusText", "Checked Out");
                        map.put("statusColor", "red");
                    } else {
                        map.put("checkOutFormatted", "--:--");
                        String displayStatus = getStatusDisplay(att.getStatus());
                        map.put("displayStatus", displayStatus);
                        map.put("statusForDisplay", displayStatus);
                        map.put("statusText", displayStatus);
                        map.put("statusColor", getStatusColor(att.getStatus()));
                    }

                    // Calculate working hours
                    calculateCurrentSessionTime(att);
                    map.put("currentWorkingTime", att.getCurrentSessionTime());
                    map.put("workingHours", att.getTotalWorkingHours() != null ?
                            att.getTotalWorkingHours() : calculateWorkingHoursDisplay(att));

                    // Get employee details
                    Optional<Employee> empOpt = employeeRepository.findByEmployeeId(att.getEmployeeId());
                    if (empOpt.isPresent()) {
                        Employee emp = empOpt.get();
                        map.put("employeeName", emp.getFirstName() + " " + emp.getLastName());
                        map.put("department", emp.getDepartment() != null ? emp.getDepartment() : "N/A");
                        map.put("designation", emp.getDesignation() != null ? emp.getDesignation() : "N/A");
                    } else {
                        map.put("employeeName", "Unknown Employee");
                        map.put("department", "N/A");
                        map.put("designation", "N/A");
                    }

                    return map;
                })
                .collect(Collectors.toList());
    }

// ========== 2. FIX getRecentAttendanceForEmployee() - Used by Employee Recent Attendance ==========

    public List<Map<String, Object>> getRecentAttendanceForEmployee(String employeeId, int days) {
        LocalDate endDate = LocalDate.now();
        LocalDate startDate = endDate.minusDays(days);

        DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("MMM d, yyyy");
        DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("hh:mm a");

        List<Attendance> attendances = attendanceRepository
                .findByEmployeeIdAndAttendanceDateBetweenOrderByAttendanceDateDesc(employeeId, startDate, endDate);

        // Remove duplicates per day
        Map<LocalDate, Attendance> latestPerDay = new LinkedHashMap<>();
        for (Attendance att : attendances) {
            if (!latestPerDay.containsKey(att.getAttendanceDate())) {
                latestPerDay.put(att.getAttendanceDate(), att);
            }
        }

        return latestPerDay.values().stream().map(att -> {
            Map<String, Object> map = new HashMap<>();
            map.put("attendanceDate", att.getAttendanceDate());
            map.put("dateFormatted", att.getAttendanceDate().format(dateFormatter));
            map.put("day", att.getAttendanceDate().getDayOfWeek().toString());

            // Format check-in
            if (att.getCheckInTime() != null) {
                map.put("checkInFormatted", att.getCheckInTime().format(timeFormatter));
            } else {
                map.put("checkInFormatted", "--:--");
            }

            // CRITICAL: Handle checkout display
            if (att.getCheckOutTime() != null) {
                map.put("checkOutFormatted", att.getCheckOutTime().format(timeFormatter));
                map.put("status", "CHECKED_OUT");
                map.put("statusText", "Checked Out");
                map.put("badgeClass", "checked-out");
                map.put("displayStatus", "Checked Out");
            } else {
                map.put("checkOutFormatted", "--:--");
                String displayStatus = getStatusDisplay(att.getStatus());
                map.put("status", att.getStatus());
                map.put("statusText", displayStatus);
                map.put("badgeClass", getBadgeClass(att.getStatus()));
                map.put("displayStatus", displayStatus);
            }

            // Calculate working time
            String workingTime;
            if (att.getCheckOutTime() != null) {
                workingTime = att.getTotalWorkingHours() != null ?
                        att.getTotalWorkingHours() : calculateWorkingHoursDisplay(att);
            } else if (att.getCheckInTime() != null) {
                workingTime = calculateWorkingHoursDisplay(att);
            } else {
                workingTime = "0h 00m";
            }
            map.put("currentWorkingTime", workingTime);

            // Add remarks
            map.put("remarks", getRemarks(att));

            return map;
        }).collect(Collectors.toList());
    }

// ========== 3. FIX getTimesheetForEmployee() - Already working but ensure consistency ==========

    public List<Map<String, Object>> getTimesheetForEmployee(String employeeId, int year, int month) {
        LocalDate startDate = LocalDate.of(year, month, 1);
        LocalDate endDate = startDate.withDayOfMonth(startDate.lengthOfMonth());

        List<Attendance> attendances = attendanceRepository
                .findByEmployeeIdAndAttendanceDateBetweenOrderByAttendanceDateDesc(employeeId, startDate, endDate);

        // Remove duplicates per day
        Map<LocalDate, Attendance> latestPerDay = new LinkedHashMap<>();
        for (Attendance att : attendances) {
            if (!latestPerDay.containsKey(att.getAttendanceDate())) {
                latestPerDay.put(att.getAttendanceDate(), att);
            }
        }

        DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("hh:mm a");

        return latestPerDay.values().stream().map(att -> {
            Map<String, Object> map = new HashMap<>();
            map.put("date", att.getAttendanceDate().format(dateFormatter));
            map.put("day", att.getAttendanceDate().getDayOfWeek().toString());

            // Check-in time
            if (att.getCheckInTime() != null) {
                map.put("checkInTime", att.getCheckInTime().format(timeFormatter));
            } else {
                map.put("checkInTime", "--:--");
            }

            // CRITICAL: Check-out time
            if (att.getCheckOutTime() != null) {
                map.put("checkOutTime", att.getCheckOutTime().format(timeFormatter));
                map.put("status", "Checked Out");
            } else {
                map.put("checkOutTime", "--:--");
                map.put("status", getStatusDisplay(att.getStatus()));
            }

            // Calculate hours
            if (att.getCheckInTime() != null && att.getCheckOutTime() != null) {
                long totalMinutes = Duration.between(att.getCheckInTime(), att.getCheckOutTime()).toMinutes();
                int workingMinutes = (int) (totalMinutes - att.getTotalBreakMinutes());
                map.put("totalHours", String.format("%dh %02dm", workingMinutes / 60, workingMinutes % 60));

                int overtimeMinutes = Math.max(0, workingMinutes - (8 * 60));
                map.put("overtime", String.format("%dh %02dm", overtimeMinutes / 60, overtimeMinutes % 60));
            } else {
                map.put("totalHours", "0h 00m");
                map.put("overtime", "0h 00m");
            }

            map.put("remarks", getRemarks(att));

            return map;
        }).collect(Collectors.toList());
    }

// ========== HELPER METHODS ==========

    private String getDisplayStatusForFilter(Attendance att) {
        if (att.getCheckOutTime() != null) {
            return "Checked Out";
        }
        if ("ACTIVE".equals(att.getStatus())) {
            return "Present";
        }
        if ("BREAK".equals(att.getStatus())) {
            return "On Break";
        }
        if ("OFFLINE".equals(att.getStatus())) {
            return "Absent";
        }
        if ("LEAVE".equals(att.getStatus())) {
            return "On Leave";
        }
        return att.getStatus();
    }

    private String getStatusDisplay(String status) {
        if ("ACTIVE".equals(status)) return "Present";
        if ("BREAK".equals(status)) return "On Break";
        if ("OFFLINE".equals(status)) return "Offline";
        if ("ABSENT".equals(status)) return "Absent";
        if ("LEAVE".equals(status)) return "On Leave";
        return status;
    }

    private String getBadgeClass(String status) {
        if ("ACTIVE".equals(status)) return "present";
        if ("BREAK".equals(status)) return "break";
        if ("OFFLINE".equals(status)) return "absent";
        if ("LEAVE".equals(status)) return "leave";
        return "present";
    }

    private String getStatusColor(String status) {
        if ("ACTIVE".equals(status)) return "green";
        if ("BREAK".equals(status)) return "orange";
        if ("OFFLINE".equals(status)) return "red";
        if ("LEAVE".equals(status)) return "purple";
        return "green";
    }

    // ========== HELPER METHODS ==========

    private boolean matchesStatus(String actualStatus, LocalDateTime checkOutTime, String filterStatus) {
        if (filterStatus == null || filterStatus.isEmpty()) return true;

        String displayStatus;
        if (checkOutTime != null) {
            displayStatus = "Checked Out";
        } else if ("ACTIVE".equals(actualStatus)) {
            displayStatus = "Present";
        } else if ("BREAK".equals(actualStatus)) {
            displayStatus = "On Break";
        } else if ("OFFLINE".equals(actualStatus)) {
            displayStatus = "Absent";
        } else if ("LEAVE".equals(actualStatus)) {
            displayStatus = "On Leave";
        } else {
            displayStatus = actualStatus;
        }

        return filterStatus.equalsIgnoreCase(displayStatus);
    }

    public void updateEmployeePresence(String employeeId, String status) {
        Optional<Employee> empOpt = employeeRepository.findByEmployeeId(employeeId);
        if (empOpt.isPresent()) {
            Employee emp = empOpt.get();
            emp.setPresenceStatus(status);
            emp.setLastPresenceUpdate(LocalDateTime.now());
            employeeRepository.save(emp);
            logger.info("Updated presence status for {} to {}", employeeId, status);
        }
    }



    private String calculateWorkingHoursDisplay(Attendance att) {
        if (att.getCheckInTime() == null) {
            return "--:--";
        }

        LocalDateTime endTime = att.getCheckOutTime() != null
                ? att.getCheckOutTime()
                : LocalDateTime.now();

        long totalMinutes = Duration.between(att.getCheckInTime(), endTime).toMinutes();
        totalMinutes -= att.getTotalBreakMinutes() != null ? att.getTotalBreakMinutes() : 0;

        if (totalMinutes < 0) totalMinutes = 0;

        long hours = totalMinutes / 60;
        long minutes = totalMinutes % 60;

        return hours + "h " + String.format("%02d", minutes) + "m";
    }

    private void calculateCurrentSessionTime(Attendance attendance) {
        if (attendance.getCheckInTime() == null) return;

        LocalDateTime now = LocalDateTime.now();
        long totalMinutes;

        if (attendance.getCheckOutTime() != null) {
            totalMinutes = attendance.getTotalWorkingMinutes() != null ?
                    attendance.getTotalWorkingMinutes() : 0;
        } else if ("BREAK".equals(attendance.getStatus()) && attendance.getBreakStartTime() != null) {
            totalMinutes = Duration.between(attendance.getCheckInTime(), attendance.getBreakStartTime()).toMinutes()
                    - (attendance.getTotalBreakMinutes() != null ? attendance.getTotalBreakMinutes() : 0);
        } else {
            totalMinutes = Duration.between(attendance.getCheckInTime(), now).toMinutes()
                    - (attendance.getTotalBreakMinutes() != null ? attendance.getTotalBreakMinutes() : 0);
        }

        long hours = totalMinutes / 60;
        long minutes = totalMinutes % 60;
        attendance.setCurrentSessionTime(String.format("%dh %02dm", hours, minutes));
    }

    private boolean matchesDepartment(String employeeId, String department) {
        if (department == null || department.isEmpty()) return true;
        Optional<Employee> emp = employeeRepository.findByEmployeeId(employeeId);
        return emp.isPresent() && department.equalsIgnoreCase(emp.get().getDepartment());
    }

    // ========== OTHER EXISTING METHODS ==========

    public Attendance getTodayAttendance(String employeeId) {
        LocalDate today = LocalDate.now();
        List<Attendance> attendances = attendanceRepository.findByEmployeeIdAndAttendanceDate(employeeId, today);

        if (!attendances.isEmpty()) {
            // Return the most recent attendance
            return attendances.get(attendances.size() - 1);
        }
        return null;
    }

    public Attendance startBreak(String employeeId) {
        LocalDate today = LocalDate.now();

        Optional<Attendance> activeAttendance = attendanceRepository
                .findActiveAttendanceByEmployeeAndDate(employeeId, today);

        Attendance attendance = activeAttendance
                .orElseThrow(() -> new RuntimeException("Must check in first"));

        if (!"ACTIVE".equals(attendance.getStatus())) {
            throw new RuntimeException("Must be active to take a break");
        }

        attendance.setStatus("BREAK");
        attendance.setBreakStartTime(LocalDateTime.now());
        attendance = attendanceRepository.save(attendance);
        updateEmployeePresence(employeeId, "BREAK");

        try {
            broadcastAttendanceUpdate(employeeId, "BREAK", null);
            broadcastDailyAttendanceUpdate(employeeId, attendance);
        } catch (Exception e) {
            logger.error("Failed to broadcast break start: {}", e.getMessage());
        }

        return attendance;
    }

    public Attendance endBreak(String employeeId) {
        LocalDate today = LocalDate.now();

        Optional<Attendance> activeAttendance = attendanceRepository
                .findActiveAttendanceByEmployeeAndDate(employeeId, today);

        Attendance attendance = activeAttendance
                .orElseThrow(() -> new RuntimeException("No active attendance found"));

        if (!"BREAK".equals(attendance.getStatus())) {
            throw new RuntimeException("Not on break");
        }

        attendance.setBreakEndTime(LocalDateTime.now());
        long breakMinutes = Duration.between(attendance.getBreakStartTime(), attendance.getBreakEndTime()).toMinutes();
        attendance.setTotalBreakMinutes(attendance.getTotalBreakMinutes() + (int) breakMinutes);
        attendance.setStatus("ACTIVE");
        attendance = attendanceRepository.save(attendance);
        updateEmployeePresence(employeeId, "ACTIVE");

        try {
            broadcastAttendanceUpdate(employeeId, "ACTIVE", null);
            broadcastDailyAttendanceUpdate(employeeId, attendance);
        } catch (Exception e) {
            logger.error("Failed to broadcast break end: {}", e.getMessage());
        }

        return attendance;
    }

    public List<Attendance> getAttendanceHistory(String employeeId) {
        return attendanceRepository.findByEmployeeIdOrderByAttendanceDateDesc(employeeId);
    }

    public Employee findEmployeeById(String employeeId) {
        return employeeRepository.findByEmployeeId(employeeId).orElse(null);
    }

    public Map<String, String> getEmployeeMonthlyAttendance(String employeeId, int year, int month) {
        LocalDate startDate = LocalDate.of(year, month, 1);
        LocalDate endDate = startDate.withDayOfMonth(startDate.lengthOfMonth());
        LocalDate today = LocalDate.now();

        List<Attendance> attendances = attendanceRepository.findByEmployeeIdAndAttendanceDateBetween(
                employeeId, startDate, endDate);

        List<LeaveRequest> approvedLeaves = leaveRequestRepository.findApprovedLeavesForEmployeeInRange(
                employeeId, startDate, endDate);

        Map<String, String> attendanceMap = new HashMap<>();

        LocalDate currentDate = startDate;
        while (!currentDate.isAfter(endDate)) {
            String dateStr = currentDate.toString();

            if (hasApprovedLeaveOnDate(currentDate, approvedLeaves)) {
                attendanceMap.put(dateStr, "leave");
            } else if (currentDate.isAfter(today)) {
                attendanceMap.put(dateStr, "");
            } else {
                Attendance att = findAttendanceForDate(attendances, currentDate);
                if (att != null && att.getCheckInTime() != null) {
                    attendanceMap.put(dateStr, "present");
                } else {
                    attendanceMap.put(dateStr, "absent");
                }
            }

            currentDate = currentDate.plusDays(1);
        }

        return attendanceMap;
    }

    public Map<String, Map<String, String>> getAllEmployeesMonthlyAttendance(int year, int month) {
        LocalDate startDate = LocalDate.of(year, month, 1);
        LocalDate endDate = startDate.withDayOfMonth(startDate.lengthOfMonth());

        List<Attendance> allAttendances = attendanceRepository.findByAttendanceDateBetween(startDate, endDate);

        Map<String, Map<String, String>> result = new HashMap<>();

        Map<String, List<Attendance>> groupedByEmployee = allAttendances.stream()
                .collect(Collectors.groupingBy(Attendance::getEmployeeId));

        for (Map.Entry<String, List<Attendance>> entry : groupedByEmployee.entrySet()) {
            String employeeId = entry.getKey();
            Map<String, String> employeeAttendance = new HashMap<>();

            LocalDate currentDate = startDate;
            while (!currentDate.isAfter(endDate)) {
                employeeAttendance.put(currentDate.toString(), "absent");
                currentDate = currentDate.plusDays(1);
            }

            // Keep only latest per day
            Map<LocalDate, Attendance> latestPerDay = new HashMap<>();
            for (Attendance att : entry.getValue()) {
                LocalDate date = att.getAttendanceDate();
                if (!latestPerDay.containsKey(date) ||
                        (att.getCheckInTime() != null && latestPerDay.get(date).getCheckInTime() != null &&
                                att.getCheckInTime().isAfter(latestPerDay.get(date).getCheckInTime()))) {
                    latestPerDay.put(date, att);
                }
            }

            for (Attendance att : latestPerDay.values()) {
                String dateStr = att.getAttendanceDate().toString();
                String status = determineAttendanceStatus(att);
                employeeAttendance.put(dateStr, status);
            }

            result.put(employeeId, employeeAttendance);
        }

        return result;
    }



    public Map<String, Object> getMonthlySummaryForEmployee(String employeeId, int year, int month) {
        LocalDate startDate = LocalDate.of(year, month, 1);
        LocalDate endDate = startDate.withDayOfMonth(startDate.lengthOfMonth());

        List<Attendance> attendances = attendanceRepository
                .findByEmployeeIdAndAttendanceDateBetweenOrderByAttendanceDateDesc(employeeId, startDate, endDate);

        // Remove duplicates per day
        Map<LocalDate, Attendance> latestPerDay = new HashMap<>();
        for (Attendance att : attendances) {
            if (!latestPerDay.containsKey(att.getAttendanceDate())) {
                latestPerDay.put(att.getAttendanceDate(), att);
            }
        }

        int presentDays = 0;
        int absentDays = 0;
        int leaveDays = 0;
        int lateDays = 0;
        int earlyDepartures = 0;
        int totalWorkingMinutes = 0;

        for (Attendance att : latestPerDay.values()) {
            if (att.getCheckInTime() != null) {
                presentDays++;

                if (att.getCheckInTime().getHour() > 9 ||
                        (att.getCheckInTime().getHour() == 9 && att.getCheckInTime().getMinute() > 15)) {
                    lateDays++;
                }

                if (att.getCheckOutTime() != null &&
                        (att.getCheckOutTime().getHour() < 17 ||
                                (att.getCheckOutTime().getHour() == 17 && att.getCheckOutTime().getMinute() < 30))) {
                    earlyDepartures++;
                }

                totalWorkingMinutes += att.getTotalWorkingMinutes() != null ? att.getTotalWorkingMinutes() : 0;
            } else if ("LEAVE".equals(att.getStatus())) {
                leaveDays++;
            }
        }

        absentDays = endDate.getDayOfMonth() - presentDays - leaveDays;
        if (absentDays < 0) absentDays = 0;

        double attendancePercentage = endDate.getDayOfMonth() > 0 ?
                (presentDays * 100.0) / endDate.getDayOfMonth() : 0;

        Map<String, Object> summary = new HashMap<>();
        summary.put("presentDays", presentDays);
        summary.put("absentDays", absentDays);
        summary.put("leaveDays", leaveDays);
        summary.put("lateDays", lateDays);
        summary.put("earlyDepartures", earlyDepartures);
        summary.put("totalWorkingHours", String.format("%dh %02dm",
                totalWorkingMinutes / 60, totalWorkingMinutes % 60));
        summary.put("attendancePercentage", Math.round(attendancePercentage));
        summary.put("totalDays", endDate.getDayOfMonth());

        return summary;
    }



    public Optional<Attendance> findAttendanceByEmployeeAndDate(String employeeId, LocalDate date) {
        List<Attendance> attendances = attendanceRepository.findByEmployeeIdAndAttendanceDate(employeeId, date);
        if (!attendances.isEmpty()) {
            return Optional.of(attendances.get(attendances.size() - 1));
        }
        return Optional.empty();
    }

    public Attendance saveAttendance(Attendance attendance) {
        return attendanceRepository.save(attendance);
    }

    public void createOrUpdateLeaveAttendance(String employeeId, LocalDate date, String status) {
        try {
            List<Attendance> existing = attendanceRepository.findByEmployeeIdAndAttendanceDate(employeeId, date);

            Attendance attendance;
            if (!existing.isEmpty()) {
                attendance = existing.get(0);
            } else {
                attendance = new Attendance();
                attendance.setEmployeeId(employeeId);
                attendance.setAttendanceDate(date);
                attendance.setSessionNumber(1);
            }

            attendance.setStatus(status);
            attendance.setIsActiveSession(false);
            attendance.setUpdatedAt(LocalDateTime.now());

            attendanceRepository.save(attendance);
            logger.info("Created/updated attendance record for {} on {} with status {}",
                    employeeId, date, status);

        } catch (Exception e) {
            logger.error("Error creating attendance for leave: {}", e.getMessage());
            throw new RuntimeException("Failed to create attendance record for leave", e);
        }
    }

    private Attendance findAttendanceForDate(List<Attendance> attendances, LocalDate date) {
        if (attendances == null || date == null) return null;
        for (Attendance att : attendances) {
            if (att != null && att.getAttendanceDate() != null && att.getAttendanceDate().equals(date)) {
                return att;
            }
        }
        return null;
    }

    private boolean hasApprovedLeaveOnDate(LocalDate date, List<LeaveRequest> approvedLeaves) {
        if (approvedLeaves == null || date == null) return false;
        for (LeaveRequest leave : approvedLeaves) {
            if (leave == null || leave.getStartDate() == null || leave.getEndDate() == null) continue;
            if (!date.isBefore(leave.getStartDate()) && !date.isAfter(leave.getEndDate())) {
                return true;
            }
        }
        return false;
    }

    private String determineAttendanceStatus(Attendance att) {
        if (att.getCheckOutTime() != null) return "present";
        if (att.getCheckInTime() != null) {
            if ("ACTIVE".equals(att.getStatus()) || "BREAK".equals(att.getStatus())) {
                return "present";
            } else if ("LEAVE".equals(att.getStatus())) {
                return "leave";
            }
        }
        return "absent";
    }

    private String getRemarks(Attendance att) {
        if (att.getCheckInTime() != null && att.getCheckInTime().getHour() >= 9 &&
                att.getCheckInTime().getMinute() > 15) {
            return "Late Arrival";
        }
        if (att.getCheckOutTime() != null && att.getCheckOutTime().getHour() < 17) {
            return "Early Departure";
        }
        return "Normal Day";
    }

    public List<Attendance> getTodayAttendanceForEmployee(String employeeId) {
        LocalDate today = LocalDate.now();
        return attendanceRepository.findByEmployeeIdAndAttendanceDateOrderByCheckInTimeDesc(employeeId, today);
    }

}