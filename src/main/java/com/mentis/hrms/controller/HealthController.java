package com.mentis.hrms.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.beans.factory.annotation.Autowired;
import com.mentis.hrms.service.JobService;
import com.mentis.hrms.service.DepartmentService;

@RestController
public class HealthController {

    @Autowired
    private JobService jobService;

    @Autowired
    private DepartmentService departmentService;

    @GetMapping("/health")
    public String healthCheck() {
        return "HRMS Application is running!";
    }

    @GetMapping("/health/db")
    public String databaseHealthCheck() {
        try {
            // Test database connection by trying to retrieve some data
            int jobCount = jobService.getAllJobs().size();
            int deptCount = departmentService.getAllDepartments().size();

            return String.format("Database connection successful! Jobs: %d, Departments: %d", jobCount, deptCount);
        } catch (Exception e) {
            return "Database connection failed: " + e.getMessage();
        }
    }

    @GetMapping("/health/services")
    public String servicesHealthCheck() {
        StringBuilder status = new StringBuilder();
        status.append("Services Status:\n");

        // Check JobService
        try {
            jobService.getAllJobs();
            status.append("✓ JobService is working\n");
        } catch (Exception e) {
            status.append("✗ JobService failed: ").append(e.getMessage()).append("\n");
        }

        // Check DepartmentService
        try {
            departmentService.getAllDepartments();
            status.append("✓ DepartmentService is working\n");
        } catch (Exception e) {
            status.append("✗ DepartmentService failed: ").append(e.getMessage()).append("\n");
        }

        return status.toString();
    }
}
