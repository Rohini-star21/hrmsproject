package com.mentis.hrms.model;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "attendance")
public class Attendance {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "employee_id", nullable = false)
    private String employeeId;

    @Column(name = "attendance_date", nullable = false)
    private LocalDate attendanceDate;

    @Column(name = "check_in_time")
    private LocalDateTime checkInTime;

    @Column(name = "check_out_time")
    private LocalDateTime checkOutTime;

    @Column(name = "break_start_time")
    private LocalDateTime breakStartTime;

    @Column(name = "break_end_time")
    private LocalDateTime breakEndTime;

    @Column(name = "total_break_minutes")
    private Integer totalBreakMinutes = 0;

    @Column(name = "total_working_minutes")
    private Integer totalWorkingMinutes = 0;

    @Column(name = "total_working_hours")
    private String totalWorkingHours;

    @Column(name = "status", length = 20)
    private String status = "OFFLINE";

    @Column(name = "session_number")
    private Integer sessionNumber = 1;

    @Column(name = "is_active_session")
    private Boolean isActiveSession = true;

    @Column(name = "created_at")
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "updated_at")
    private LocalDateTime updatedAt = LocalDateTime.now();

    @Transient
    private String currentSessionTime;

    // Constructors
    public Attendance() {}

    public Attendance(String employeeId, LocalDate attendanceDate) {
        this.employeeId = employeeId;
        this.attendanceDate = attendanceDate;
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getEmployeeId() { return employeeId; }
    public void setEmployeeId(String employeeId) { this.employeeId = employeeId; }

    public LocalDate getAttendanceDate() { return attendanceDate; }
    public void setAttendanceDate(LocalDate attendanceDate) { this.attendanceDate = attendanceDate; }

    public LocalDateTime getCheckInTime() { return checkInTime; }
    public void setCheckInTime(LocalDateTime checkInTime) { this.checkInTime = checkInTime; }

    public LocalDateTime getCheckOutTime() { return checkOutTime; }
    public void setCheckOutTime(LocalDateTime checkOutTime) { this.checkOutTime = checkOutTime; }

    public LocalDateTime getBreakStartTime() { return breakStartTime; }
    public void setBreakStartTime(LocalDateTime breakStartTime) { this.breakStartTime = breakStartTime; }

    public LocalDateTime getBreakEndTime() { return breakEndTime; }
    public void setBreakEndTime(LocalDateTime breakEndTime) { this.breakEndTime = breakEndTime; }

    public Integer getTotalBreakMinutes() { return totalBreakMinutes; }
    public void setTotalBreakMinutes(Integer totalBreakMinutes) { this.totalBreakMinutes = totalBreakMinutes; }

    public Integer getTotalWorkingMinutes() { return totalWorkingMinutes; }
    public void setTotalWorkingMinutes(Integer totalWorkingMinutes) { this.totalWorkingMinutes = totalWorkingMinutes; }

    public String getTotalWorkingHours() { return totalWorkingHours; }
    public void setTotalWorkingHours(String totalWorkingHours) { this.totalWorkingHours = totalWorkingHours; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public Integer getSessionNumber() { return sessionNumber; }
    public void setSessionNumber(Integer sessionNumber) { this.sessionNumber = sessionNumber; }

    public Boolean getIsActiveSession() { return isActiveSession; }
    public void setIsActiveSession(Boolean isActiveSession) { this.isActiveSession = isActiveSession; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }

    public String getCurrentSessionTime() { return currentSessionTime; }
    public void setCurrentSessionTime(String currentSessionTime) { this.currentSessionTime = currentSessionTime; }

    @PreUpdate
    public void preUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    @Transient
    public boolean isCheckedOut() {
        return checkOutTime != null;
    }
}