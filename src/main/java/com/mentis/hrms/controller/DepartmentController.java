package com.mentis.hrms.controller;

import com.mentis.hrms.model.Department;
import com.mentis.hrms.model.Designation;
import com.mentis.hrms.service.DepartmentService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpSession;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Controller
public class DepartmentController {

    private static final Logger logger = LoggerFactory.getLogger(DepartmentController.class);

    @Autowired
    private DepartmentService departmentService;

    // ==================== VIEW METHODS (HTML Pages) ====================

    @GetMapping("/dashboard/hr/departments")
    public String departmentsPage(Model model, HttpSession session) {
        String userRole = (String) session.getAttribute("userRole");
        Boolean isSuperAdmin = (Boolean) session.getAttribute("isSuperAdmin");

        if (!"SUPER_ADMIN".equals(userRole) && (isSuperAdmin == null || !isSuperAdmin)) {
            logger.warn("Unauthorized access attempt to departments page");
            return "redirect:/dashboard/hr";
        }

        model.addAttribute("isSuperAdmin", true);
        model.addAttribute("userName", session.getAttribute("userName"));
        return "departments";
    }

    @GetMapping("/dashboard/hr/roles")
    public String rolesPage(Model model, HttpSession session) {
        String userRole = (String) session.getAttribute("userRole");
        Boolean isSuperAdmin = (Boolean) session.getAttribute("isSuperAdmin");

        if (!"SUPER_ADMIN".equals(userRole) && (isSuperAdmin == null || !isSuperAdmin)) {
            return "redirect:/dashboard/hr";
        }

        model.addAttribute("isSuperAdmin", true);
        model.addAttribute("userName", session.getAttribute("userName"));
        model.addAttribute("designations", departmentService.getAllDesignations());
        return "roles";
    }

    @GetMapping("/dashboard/hr/categories")
    public String categoriesPage(Model model, HttpSession session) {
        String userRole = (String) session.getAttribute("userRole");
        Boolean isSuperAdmin = (Boolean) session.getAttribute("isSuperAdmin");

        if (!"SUPER_ADMIN".equals(userRole) && (isSuperAdmin == null || !isSuperAdmin)) {
            return "redirect:/dashboard/hr";
        }

        model.addAttribute("isSuperAdmin", true);
        model.addAttribute("userName", session.getAttribute("userName"));
        model.addAttribute("departments", departmentService.getAllDepartments());
        return "categories";
    }

    // ==================== API METHODS (JSON/REST) ====================

    @GetMapping("/api/departments/all")
    @ResponseBody
    public ResponseEntity<?> getAllDepartments() {
        try {
            List<Department> departments = departmentService.getAllDepartmentsWithDesignations();
            return ResponseEntity.ok(Map.of("success", true, "departments", departments));
        } catch (Exception e) {
            return ResponseEntity.ok(Map.of("success", false, "error", e.getMessage()));
        }
    }

    @PostMapping("/api/departments/create")
    @ResponseBody
    public ResponseEntity<?> createDepartment(@RequestBody Map<String, Object> request) {
        try {
            String departmentName = (String) request.get("departmentName");
            String designationName = (String) request.get("designationName");
            String description = (String) request.get("description");

            if (departmentName == null || departmentName.trim().isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("success", false, "error", "Department name is required"));
            }

            Department existingDept = departmentService.getDepartmentByName(departmentName);
            if (existingDept != null) {
                return ResponseEntity.ok(Map.of("success", false, "error", "Department already exists"));
            }

            Department department = new Department();
            department.setName(departmentName);

            if (description != null && !description.trim().isEmpty()) {
                department.setDescription(description.trim());
            }

            if (designationName != null && !designationName.trim().isEmpty()) {
                Designation designation = new Designation();
                designation.setName(designationName);
                designation.setDepartment(department);
                department.getDesignations().add(designation);
            }

            Department savedDept = departmentService.saveDepartment(department);
            return ResponseEntity.ok(Map.of("success", true, "message", "Department created successfully", "department", savedDept));
        } catch (Exception e) {
            return ResponseEntity.ok(Map.of("success", false, "error", e.getMessage()));
        }
    }

    // UPDATED: Now uses ID instead of Name
    @GetMapping("/api/departments/{id}/designations")
    @ResponseBody
    public ResponseEntity<?> getDesignationsByDepartment(@PathVariable Long id) {
        try {
            Department department = departmentService.getDepartmentById(id);
            if (department == null) return ResponseEntity.ok(Map.of("success", false, "error", "Department not found"));
            return ResponseEntity.ok(Map.of("success", true, "designations", department.getDesignations()));
        } catch (Exception e) {
            return ResponseEntity.ok(Map.of("success", false, "error", e.getMessage()));
        }
    }

    // UPDATED: Now uses ID instead of Name
    @PostMapping("/api/departments/{id}/add-designation")
    @ResponseBody
    public ResponseEntity<?> addDesignation(@PathVariable Long id, @RequestBody Map<String, String> request) {
        try {
            String designationName = request.get("designationName");
            if (designationName == null || designationName.trim().isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("success", false, "error", "Designation name is required"));
            }

            Department department = departmentService.getDepartmentById(id);
            if (department == null) return ResponseEntity.ok(Map.of("success", false, "error", "Department not found"));

            boolean exists = department.getDesignations().stream().anyMatch(d -> d.getName().equalsIgnoreCase(designationName));
            if (exists) return ResponseEntity.ok(Map.of("success", false, "error", "Designation already exists"));

            Designation designation = new Designation();
            designation.setName(designationName);
            designation.setDepartment(department);
            department.getDesignations().add(designation);
            departmentService.saveDepartment(department);

            return ResponseEntity.ok(Map.of("success", true, "message", "Designation added successfully", "designation", designation));
        } catch (Exception e) {
            return ResponseEntity.ok(Map.of("success", false, "error", e.getMessage()));
        }
    }

    // UPDATED: Now uses ID instead of Name
    @DeleteMapping("/api/departments/{id}/delete")
    @ResponseBody
    public ResponseEntity<?> deleteDepartment(@PathVariable Long id) {
        try {
            departmentService.deleteDepartment(id);
            return ResponseEntity.ok(Map.of("success", true, "message", "Department deleted successfully"));
        } catch (Exception e) {
            return ResponseEntity.ok(Map.of("success", false, "error", e.getMessage()));
        }
    }

    // UPDATED: Now uses Department ID and Designation ID
    @DeleteMapping("/api/departments/{id}/designations/{designationId}/delete")
    @ResponseBody
    public ResponseEntity<?> deleteDesignation(@PathVariable Long id, @PathVariable Long designationId) {
        try {
            Department department = departmentService.getDepartmentById(id);
            if (department == null) return ResponseEntity.ok(Map.of("success", false, "error", "Department not found"));

            Designation toRemove = department.getDesignations().stream()
                    .filter(d -> d.getId().equals(designationId))
                    .findFirst().orElse(null);

            if (toRemove == null) return ResponseEntity.ok(Map.of("success", false, "error", "Designation not found"));

            department.getDesignations().remove(toRemove);
            departmentService.saveDepartment(department);
            return ResponseEntity.ok(Map.of("success", true, "message", "Designation deleted successfully"));
        } catch (Exception e) {
            return ResponseEntity.ok(Map.of("success", false, "error", e.getMessage()));
        }
    }

    // UPDATED: Now uses ID instead of Name
    @PutMapping("/api/departments/{id}/update")
    @ResponseBody
    public ResponseEntity<?> updateDepartment(@PathVariable Long id, @RequestBody Map<String, String> request) {
        try {
            String newName = request.get("name");
            String description = request.get("description");

            Department department = departmentService.getDepartmentById(id);
            if (department == null) return ResponseEntity.ok(Map.of("success", false, "error", "Department not found"));

            if (newName != null && !newName.trim().isEmpty()) {
                department.setName(newName);
            }

            if (description != null) {
                department.setDescription(description.trim());
            }

            departmentService.saveDepartment(department);
            return ResponseEntity.ok(Map.of("success", true, "message", "Department updated successfully", "department", department));
        } catch (Exception e) {
            return ResponseEntity.ok(Map.of("success", false, "error", e.getMessage()));
        }
    }
}