// File: src/main/java/com/mentis/hrms/dto/HiredCandidateViewModel.java
package com.mentis.hrms.dto;

import java.time.LocalDateTime;

public class HiredCandidateViewModel {
    private Long id;
    private String employeeId;
    private String firstName;
    private String lastName;
    private String email;
    private String phone;
    private String source; // "APPLICATION" or "EMPLOYEE"
    private String position;
    private String department;
    private LocalDateTime date;
    private String status;
    private Long applicationId;
    private String employeeIdStr;
    private String workLocation;
    private String employmentType;

    // Constructors
    public HiredCandidateViewModel() {}

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getEmployeeId() { return employeeId; }
    public void setEmployeeId(String employeeId) { this.employeeId = employeeId; }

    public String getFirstName() { return firstName; }
    public void setFirstName(String firstName) { this.firstName = firstName; }

    public String getLastName() { return lastName; }
    public void setLastName(String lastName) { this.lastName = lastName; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }

    public String getSource() { return source; }
    public void setSource(String source) { this.source = source; }

    public String getPosition() { return position; }
    public void setPosition(String position) { this.position = position; }

    public String getDepartment() { return department; }
    public void setDepartment(String department) { this.department = department; }

    public LocalDateTime getDate() { return date; }
    public void setDate(LocalDateTime date) { this.date = date; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public Long getApplicationId() { return applicationId; }
    public void setApplicationId(Long applicationId) { this.applicationId = applicationId; }

    public String getEmployeeIdStr() { return employeeIdStr; }
    public void setEmployeeIdStr(String employeeIdStr) { this.employeeIdStr = employeeIdStr; }

    public String getWorkLocation() { return workLocation; }
    public void setWorkLocation(String workLocation) { this.workLocation = workLocation; }

    public String getEmploymentType() { return employmentType; }
    public void setEmploymentType(String employmentType) { this.employmentType = employmentType; }

    // Helper methods
    public String getFullName() {
        return firstName + " " + lastName;
    }

    public boolean isFromApplication() {
        return "APPLICATION".equals(source);
    }

    public boolean isFromEmployee() {
        return "EMPLOYEE".equals(source);
    }
}