package com.mentis.hrms.service;

import com.mentis.hrms.model.Department;
import com.mentis.hrms.model.Designation;
import com.mentis.hrms.repository.DepartmentRepository;
import com.mentis.hrms.repository.DesignationRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
public class DepartmentService {

    private static final Logger logger = LoggerFactory.getLogger(DepartmentService.class);

    @Autowired
    private DepartmentRepository departmentRepository;

    @Autowired
    private DesignationRepository designationRepository;

    /**
     * Get all departments (basic)
     */
    public List<Department> getAllDepartments() {
        try {
            return departmentRepository.findAll();
        } catch (Exception e) {
            logger.error("Error fetching all departments: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to fetch departments", e);
        }
    }

    /**
     * Get all departments with their designations
     */
    public List<Department> getAllDepartmentsWithDesignations() {
        try {
            return departmentRepository.findAllWithDesignations();
        } catch (Exception e) {
            logger.error("Error fetching departments with designations: {}", e.getMessage());
            return departmentRepository.findAll(); // fallback
        }
    }

    /**
     * Get department by ID
     */
    public Department getDepartmentById(Long id) {
        try {
            Optional<Department> dept = departmentRepository.findById(id);
            return dept.orElse(null);
        } catch (Exception e) {
            logger.error("Error fetching department by ID {}: {}", id, e.getMessage(), e);
            throw new RuntimeException("Failed to fetch department", e);
        }
    }

    /**
     * Get department by name
     */
    public Department getDepartmentByName(String name) {
        try {
            Optional<Department> dept = departmentRepository.findByName(name);
            return dept.orElse(null);
        } catch (Exception e) {
            logger.error("Error fetching department by name {}: {}", name, e.getMessage(), e);
            throw new RuntimeException("Failed to fetch department", e);
        }
    }

    /**
     * Get department by name with designations
     */
    public Department getDepartmentByNameWithDesignations(String name) {
        try {
            return departmentRepository.findByNameWithDesignations(name).orElse(null);
        } catch (Exception e) {
            logger.error("Error fetching department {} with designations: {}", name, e.getMessage());
            return getDepartmentByName(name); // fallback
        }
    }

    /**
     * Save department
     */
    @Transactional
    public Department saveDepartment(Department department) {
        try {
            if (department.getName() == null || department.getName().trim().isEmpty()) {
                throw new IllegalArgumentException("Department name is required");
            }

            // Check if department with same name exists (for new departments)
            if (department.getId() == null) {
                Optional<Department> existing = departmentRepository.findByName(department.getName());
                if (existing.isPresent()) {
                    throw new RuntimeException("Department with name '" + department.getName() + "' already exists");
                }
            }

            // Set department for all designations
            if (department.getDesignations() != null) {
                for (Designation designation : department.getDesignations()) {
                    designation.setDepartment(department);
                }
            }

            Department saved = departmentRepository.save(department);
            logger.info("✅ Department saved: {} (ID: {})", saved.getName(), saved.getId());
            return saved;
        } catch (Exception e) {
            logger.error("Error saving department: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to save department: " + e.getMessage(), e);
        }
    }

    /**
     * Delete department
     */
    @Transactional
    public void deleteDepartment(Long id) {
        try {
            if (!departmentRepository.existsById(id)) {
                throw new RuntimeException("Department with ID " + id + " does not exist");
            }
            departmentRepository.deleteById(id);
            logger.info("✅ Department deleted with ID: {}", id);
        } catch (Exception e) {
            logger.error("Error deleting department ID {}: {}", id, e.getMessage(), e);
            throw new RuntimeException("Failed to delete department", e);
        }
    }

    /**
     * Add designation to department
     */
    @Transactional
    public Designation addDesignation(String departmentName, String designationName) {
        try {
            Department department = getDepartmentByName(departmentName);
            if (department == null) {
                throw new RuntimeException("Department not found: " + departmentName);
            }

            // Check if designation already exists
            boolean exists = designationRepository.existsByNameAndDepartmentId(designationName, department.getId());
            if (exists) {
                throw new RuntimeException("Designation '" + designationName + "' already exists in this department");
            }

            Designation designation = new Designation();
            designation.setName(designationName);
            designation.setDepartment(department);

            Designation saved = designationRepository.save(designation);
            logger.info("✅ Designation added: {} to department: {}", designationName, departmentName);

            return saved;
        } catch (Exception e) {
            logger.error("Error adding designation: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to add designation", e);
        }
    }




    /**
     * Get all designations directly for the Roles page
     */
    public List<Designation> getAllDesignations() {
        try {
            return designationRepository.findAll();
        } catch (Exception e) {
            logger.error("Error fetching all designations: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to fetch designations", e);
        }
    }





    /**
     * Get designations by department name
     */
    public List<Designation> getDesignationsByDepartment(String departmentName) {
        try {
            return designationRepository.findByDepartmentName(departmentName);
        } catch (Exception e) {
            logger.error("Error fetching designations for department {}: {}", departmentName, e.getMessage(), e);
            throw new RuntimeException("Failed to fetch designations", e);
        }
    }
}