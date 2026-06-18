package com.mentis.hrms.controller;

import com.mentis.hrms.model.Employee;
import com.mentis.hrms.model.JobApplication;
import com.mentis.hrms.model.OfferLetter;
import com.mentis.hrms.model.enums.UserRole;
import com.mentis.hrms.service.*;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import java.util.List;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/dashboard/admin")
public class AdminDashboardController {

    @Autowired
    private DashboardController dashboardController;

    @Autowired
    private EmployeeService employeeService;

    @Autowired
    private JobApplicationService applicationService;

    @Autowired
    private OfferService offerService;

    @GetMapping
    public String adminDashboard(Model model, HttpSession session, RedirectAttributes ra) {
        // Verify authentication
        if (session.getAttribute("userId") == null) {
            ra.addFlashAttribute("error", "Session expired. Please login again.");
            return "redirect:/candidate/auth/login";
        }

        // Verify Super Admin role
        String role = (String) session.getAttribute("userRole");
        System.out.println("AdminDashboardController - User role: " + role);

        if (!"SUPER_ADMIN".equals(role)) {
            System.out.println("Unauthorized access attempt to admin dashboard by role: " + role);
            return "redirect:/candidate/login?error=Unauthorized";
        }

        // Load comprehensive system statistics
        loadSuperAdminStats(model);

        // Add user info for header
        model.addAttribute("isSuperAdmin", true);
        model.addAttribute("userRole", role);
        model.addAttribute("userName", session.getAttribute("userName"));

        // Return the dashboard view (not redirect)
        return "dashboard"; // This should be your dashboard.html template
    }

    private void loadSuperAdminStats(Model model) {
        // Fetch all system data
        List<Employee> allUsers = employeeService.getAllEmployees();
        List<JobApplication> allApplications = applicationService.getAllApplications();
        List<OfferLetter> allOffers = offerService.getAllOfferLetters();

        // Calculate role distribution
        long superAdminCount = allUsers.stream()
                .filter(u -> u.getRole() == UserRole.SUPER_ADMIN)
                .count();
        long hrCount = allUsers.stream()
                .filter(u -> u.getRole() == UserRole.HR)
                .count();
        long employeeCount = allUsers.stream()
                .filter(u -> u.getRole() == UserRole.EMPLOYEE)
                .count();

        // Calculate active/inactive
        long activeUsers = allUsers.stream().filter(Employee::isActive).count();
        long inactiveUsers = allUsers.size() - activeUsers;

        // Calculate monthly hires
        java.time.LocalDateTime monthStart = java.time.LocalDateTime.now().withDayOfMonth(1);
        long hiredThisMonth = allApplications.stream()
                .filter(app -> "Hired".equals(app.getStatus()))
                .filter(app -> app.getApplicationDate().isAfter(monthStart))
                .count();

        // Add to model
        model.addAttribute("totalUsers", allUsers.size());
        model.addAttribute("superAdminCount", superAdminCount);
        model.addAttribute("hrCount", hrCount);
        model.addAttribute("employeeCount", employeeCount);
        model.addAttribute("activeUsers", activeUsers);
        model.addAttribute("inactiveUsers", inactiveUsers);
        model.addAttribute("totalCandidates", allApplications.size());
        model.addAttribute("totalOffers", allOffers.size());
        model.addAttribute("hiredThisMonth", hiredThisMonth);
    }
}