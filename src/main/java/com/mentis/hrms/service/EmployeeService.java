package com.mentis.hrms.service;

import com.mentis.hrms.model.Employee;
import com.mentis.hrms.repository.EmployeeRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Random;

@Service
@Transactional
public class EmployeeService {

    private static final Logger logger = LoggerFactory.getLogger(EmployeeService.class);

    @Autowired
    private EmployeeRepository employeeRepository;

    /**
     * Save employee with all details
     */
    @Transactional
    public Employee saveEmployee(Employee employee) {
        try {
            // Generate employee ID if not set
            if (employee.getEmployeeId() == null || employee.getEmployeeId().isEmpty()) {
                employee.setEmployeeId(generateEmployeeId());
            }

            // Set audit fields
            if (employee.getCreatedDate() == null) {
                employee.setCreatedDate(LocalDateTime.now());
            }
            employee.setUpdatedDate(LocalDateTime.now());

            // Set default status if not set
            if (employee.getStatus() == null || employee.getStatus().isEmpty()) {
                employee.setStatus("Active");
            }

            // Set default onboarding status
            if (employee.getOnboardingStatus() == null || employee.getOnboardingStatus().isEmpty()) {
                employee.setOnboardingStatus("NOT_STARTED");
            }

            // Ensure email fields are consistent
            if (employee.getEmail() == null && employee.getPersonalEmail() != null) {
                employee.setEmail(employee.getPersonalEmail());
            }

            logger.info("Saving employee: {} {} (ID: {})",
                    employee.getFirstName(), employee.getLastName(), employee.getEmployeeId());

            Employee savedEmployee = employeeRepository.save(employee);
            employeeRepository.flush(); // Force immediate save

            logger.info("Employee saved successfully: {}", savedEmployee.getEmployeeId());
            return savedEmployee;

        } catch (Exception e) {
            logger.error("Error saving employee: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to save employee: " + e.getMessage(), e);
        }
    }

    /**
     * Update existing employee
     */
    @Transactional
    public Employee updateEmployee(Employee employee) {
        try {
            if (employee.getId() == null) {
                throw new IllegalArgumentException("Cannot update employee without ID");
            }

            // Set update timestamp
            employee.setUpdatedDate(LocalDateTime.now());

            // Ensure email consistency
            if (employee.getEmail() == null && employee.getPersonalEmail() != null) {
                employee.setEmail(employee.getPersonalEmail());
            }

            logger.info("Updating employee: {} (ID: {})",
                    employee.getEmployeeName(), employee.getEmployeeId());

            Employee updatedEmployee = employeeRepository.save(employee);
            employeeRepository.flush();

            logger.info("Employee updated successfully: {}", updatedEmployee.getEmployeeId());
            return updatedEmployee;

        } catch (Exception e) {
            logger.error("Error updating employee: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to update employee: " + e.getMessage(), e);
        }
    }

    @Transactional
    public Employee updateEmployeeColor(String employeeId, String color) {
        Optional<Employee> employeeOpt = getEmployeeByEmployeeId(employeeId);
        if (employeeOpt.isPresent()) {
            Employee employee = employeeOpt.get();
            employee.setProfileColor(color);
            return updateEmployee(employee);
        }
        throw new RuntimeException("Employee not found: " + employeeId);
    }

    @Transactional(readOnly = true)
    public List<Employee> getAllEmployeesSortedByNewest() {
        return employeeRepository.findAllByOrderByCreatedDateDesc();
    }

    /**
     * Get employee by employee ID with all details
     */
    @Transactional(readOnly = true)
    public Optional<Employee> getEmployeeByEmployeeId(String employeeId) {
        try {
            return employeeRepository.findByEmployeeId(employeeId);
        } catch (Exception e) {
            logger.error("Error fetching employee {}: {}", employeeId, e.getMessage());
            throw new RuntimeException("Failed to fetch employee: " + e.getMessage(), e);
        }
    }

    /**
     * Get all employees
     */
    @Transactional(readOnly = true)
    public List<Employee> getAllEmployees() {
        return employeeRepository.findAll();
    }

    // Add these methods to EmployeeService.java
    public String getStatusColor(String presenceStatus) {
        if ("ACTIVE".equals(presenceStatus)) return "#10b981";
        if ("BREAK".equals(presenceStatus)) return "#f59e0b";
        if ("CHECKED_OUT".equals(presenceStatus)) return "#ef4444";
        return "#ef4444"; // Default for OFFLINE
    }

    public String getStatusDisplay(String presenceStatus) {
        if ("ACTIVE".equals(presenceStatus)) return "Active";
        if ("BREAK".equals(presenceStatus)) return "On Break";
        if ("CHECKED_OUT".equals(presenceStatus)) return "Checked Out";
        return "Offline";
    }

    /**
     * Delete employee by ID
     */
    @Transactional
    public void deleteEmployee(Long id) {
        try {
            if (!employeeRepository.existsById(id)) {
                throw new IllegalArgumentException("Employee not found with ID: " + id);
            }
            employeeRepository.deleteById(id);
            logger.info("Employee deleted: {}", id);
        } catch (Exception e) {
            logger.error("Error deleting employee {}: {}", id, e.getMessage(), e);
            throw new RuntimeException("Failed to delete employee: " + e.getMessage(), e);
        }
    }

    /**
     * Check if employee exists
     */
    public boolean employeeExists(String employeeId) {
        return employeeRepository.existsByEmployeeId(employeeId);
    }

    /**
     * Generate sequential employee ID (EMP001, EMP002, etc.)
     */
    private String generateEmployeeId() {
        try {
            // Get all employees and find the highest number
            List<Employee> allEmployees = getAllEmployees();
            int maxNum = 0;

            for (Employee emp : allEmployees) {
                String empId = emp.getEmployeeId();
                if (empId != null && empId.startsWith("EMP")) {
                    try {
                        // Extract the number part after "EMP"
                        String numPart = empId.substring(3);
                        int num = Integer.parseInt(numPart);
                        if (num > maxNum) {
                            maxNum = num;
                        }
                    } catch (NumberFormatException e) {
                        // Skip non-numeric suffixes
                    }
                }
            }

            // Generate next number
            int nextNum = maxNum + 1;
            String employeeId = String.format("EMP%03d", nextNum);

            logger.info("Generated sequential Employee ID: {}", employeeId);
            return employeeId;

        } catch (Exception e) {
            logger.error("Error generating employee ID, using fallback: {}", e.getMessage());
            // Fallback to timestamp-based if something goes wrong
            return "EMP" + System.currentTimeMillis();
        }
    }
}