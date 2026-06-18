package com.mentis.hrms.controller;

import com.mentis.hrms.model.Attendance;
import com.mentis.hrms.model.Employee;
import com.mentis.hrms.service.AttendanceService;
import com.mentis.hrms.service.EmployeeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/presence")
public class PresenceController {

    @Autowired
    private EmployeeService employeeService;

    @Autowired
    private AttendanceService attendanceService;

    /* Employee sets own presence (ACTIVE / BREAK / OFFLINE) */
    @PostMapping("/set/{employeeId}")
    public ResponseEntity<Map<String,Object>> setPresence(
            @PathVariable String employeeId,
            @RequestParam String status){

        Map<String,Object> resp=new HashMap<>();
        try{
            Employee emp=employeeService.getEmployeeByEmployeeId(employeeId).orElse(null);
            if(emp==null) {
                resp.put("success", false);
                resp.put("error", "Employee not found");
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(resp);
            }

            emp.setPresenceStatus(status);
            emp.setLastPresenceUpdate(LocalDateTime.now());
            employeeService.updateEmployee(emp);

            resp.put("success",true);
            resp.put("status",status);
            resp.put("employeeId",employeeId);
            return ResponseEntity.ok(resp);
        }catch(Exception e){
            resp.put("success",false);
            resp.put("error",e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(resp);
        }
    }

    /* HR / others fetch full presence list with attendance sync */
    @GetMapping("/list")
    public ResponseEntity<Map<String,Object>> getAllPresence(){
        Map<String,Object> response = new HashMap<>();
        List<Map<String,Object>> list = new ArrayList<>();

        try {
            List<Employee> employees = employeeService.getAllEmployees();
            LocalDate today = LocalDate.now();

            if (employees == null || employees.isEmpty()) {
                response.put("success", true);
                response.put("presence", list);
                response.put("total", 0);
                return ResponseEntity.ok(response);
            }

            for (Employee e : employees) {
                try {
                    if (e == null || e.getEmployeeId() == null) continue;

                    Map<String,Object> m = new HashMap<>();
                    m.put("employeeId", e.getEmployeeId());

                    // Get today's attendance to determine actual status
                    Attendance todayAttendance = attendanceService.getTodayAttendance(e.getEmployeeId());

                    String presenceStatus;
                    String displayStatus;
                    String statusColor;

                    if (todayAttendance != null) {
                        // Sync with attendance system
                        if (todayAttendance.getCheckOutTime() != null) {
                            presenceStatus = "CHECKED_OUT";
                            displayStatus = "Checked Out";
                            statusColor = "#ef4444";
                        } else if ("BREAK".equals(todayAttendance.getStatus())) {
                            presenceStatus = "BREAK";
                            displayStatus = "On Break";
                            statusColor = "#f59e0b";
                        } else if ("ACTIVE".equals(todayAttendance.getStatus())) {
                            presenceStatus = "ACTIVE";
                            displayStatus = "Active";
                            statusColor = "#10b981";
                        } else {
                            presenceStatus = e.getPresenceStatus() != null ? e.getPresenceStatus() : "OFFLINE";
                            displayStatus = "Offline";
                            statusColor = "#ef4444";
                        }
                    } else {
                        // No attendance today - use employee presence status
                        presenceStatus = e.getPresenceStatus() != null ? e.getPresenceStatus() : "OFFLINE";
                        if ("ACTIVE".equals(presenceStatus)) {
                            displayStatus = "Active";
                            statusColor = "#10b981";
                        } else if ("BREAK".equals(presenceStatus)) {
                            displayStatus = "On Break";
                            statusColor = "#f59e0b";
                        } else {
                            displayStatus = "Offline";
                            statusColor = "#ef4444";
                        }
                    }

                    m.put("presenceStatus", presenceStatus);
                    m.put("displayStatus", displayStatus);
                    m.put("statusColor", statusColor);
                    m.put("lastUpdate", e.getLastPresenceUpdate() != null ? e.getLastPresenceUpdate().toString() : null);

                    list.add(m);
                } catch (Exception ex) {
                    System.err.println("Error processing employee in presence list: " + ex.getMessage());
                    // Continue with next employee
                }
            }

            response.put("success", true);
            response.put("presence", list);
            response.put("total", list.size());
            response.put("timestamp", LocalDateTime.now().toString());
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            e.printStackTrace();
            response.put("success", false);
            response.put("error", "Failed to fetch presence list: " + e.getMessage());
            response.put("presence", list);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    /* Get single employee presence */
    @GetMapping("/{employeeId}")
    public ResponseEntity<Map<String,Object>> getEmployeePresence(@PathVariable String employeeId) {
        Map<String,Object> response = new HashMap<>();

        try {
            Employee emp = employeeService.getEmployeeByEmployeeId(employeeId).orElse(null);
            if (emp == null) {
                response.put("success", false);
                response.put("error", "Employee not found");
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
            }

            Attendance todayAttendance = attendanceService.getTodayAttendance(employeeId);

            String presenceStatus;
            String displayStatus;
            String statusColor;

            if (todayAttendance != null) {
                if (todayAttendance.getCheckOutTime() != null) {
                    presenceStatus = "CHECKED_OUT";
                    displayStatus = "Checked Out";
                    statusColor = "#ef4444";
                } else if ("BREAK".equals(todayAttendance.getStatus())) {
                    presenceStatus = "BREAK";
                    displayStatus = "On Break";
                    statusColor = "#f59e0b";
                } else if ("ACTIVE".equals(todayAttendance.getStatus())) {
                    presenceStatus = "ACTIVE";
                    displayStatus = "Active";
                    statusColor = "#10b981";
                } else {
                    presenceStatus = emp.getPresenceStatus() != null ? emp.getPresenceStatus() : "OFFLINE";
                    displayStatus = "Offline";
                    statusColor = "#ef4444";
                }
            } else {
                presenceStatus = emp.getPresenceStatus() != null ? emp.getPresenceStatus() : "OFFLINE";
                if ("ACTIVE".equals(presenceStatus)) {
                    displayStatus = "Active";
                    statusColor = "#10b981";
                } else if ("BREAK".equals(presenceStatus)) {
                    displayStatus = "On Break";
                    statusColor = "#f59e0b";
                } else {
                    displayStatus = "Offline";
                    statusColor = "#ef4444";
                }
            }

            response.put("success", true);
            response.put("employeeId", employeeId);
            response.put("presenceStatus", presenceStatus);
            response.put("displayStatus", displayStatus);
            response.put("statusColor", statusColor);
            response.put("lastUpdate", emp.getLastPresenceUpdate());

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            response.put("success", false);
            response.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }
}