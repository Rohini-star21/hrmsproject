package com.mentis.hrms.controller;

import com.mentis.hrms.model.Notification;
import com.mentis.hrms.model.Employee;
import com.mentis.hrms.service.NotificationService;
import com.mentis.hrms.service.EmployeeService;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import java.util.List;

@Controller
@RequestMapping("/notifications")
public class NotificationViewController {

    @Autowired
    private NotificationService notificationService;

    @Autowired
    private EmployeeService employeeService;

    /* ========== EMPLOYEE NOTIFICATIONS PAGE - SHOWS ONLY PERSISTENT NOTIFICATIONS ========== */
    @GetMapping("/employee")
    public String employeeNotifications(HttpSession session, Model model) {
        // Check if employee is logged in
        String employeeId = (String) session.getAttribute("candidateEmployeeId");
        if (employeeId == null) {
            return "redirect:/candidate/login?error=Please+login+first";
        }

        // Get employee details for sidebar
        Employee employee = getEmployeeDetails(employeeId);
        if (employee == null) {
            return "redirect:/candidate/login?error=Employee+not+found";
        }

        // MODIFIED: Get only persistent notifications for this employee
        List<Notification> notifications = notificationService.getPersistentNotifications(employeeId, "EMPLOYEE");
        long unreadCount = notificationService.getUnreadPersistentCount(employeeId, "EMPLOYEE");

        // Add attributes to model
        model.addAttribute("notifications", notifications);
        model.addAttribute("unreadCount", unreadCount);
        model.addAttribute("employee", employee);
        model.addAttribute("totalCount", notifications.size());
        model.addAttribute("isEmployee", true);

        return "notifications/employee-notifications";
    }

    /* ========== HR NOTIFICATIONS PAGE - SHOWS ONLY PERSISTENT NOTIFICATIONS ========== */
    @GetMapping("/hr")
    public String hrNotifications(HttpSession session, Model model) {
        // Check if user is HR or Super Admin
        String userRole = (String) session.getAttribute("userRole");
        if (!"HR".equals(userRole) && !"SUPER_ADMIN".equals(userRole)) {
            return "redirect:/dashboard?error=Access+denied";
        }

        // MODIFIED: Get only persistent HR notifications
        String hrId = "HR_SYSTEM";
        List<Notification> notifications = notificationService.getPersistentNotifications(hrId, "HR");
        long unreadCount = notificationService.getUnreadPersistentCount(hrId, "HR");

        // Get user details for sidebar
        String userName = (String) session.getAttribute("userName");
        String userId = (String) session.getAttribute("userId");

        // Create mock employee for HR user
        Employee hrUser = new Employee();
        hrUser.setEmployeeId(userId != null ? userId : "HR_USER");
        hrUser.setFirstName(userName != null ? userName.split(" ")[0] : "HR");
        hrUser.setLastName(userName != null && userName.contains(" ") ?
                userName.substring(userName.indexOf(" ") + 1) : "User");
        hrUser.setDesignation(userRole.equals("SUPER_ADMIN") ? "Super Admin" : "HR Manager");

        // Add attributes to model
        model.addAttribute("notifications", notifications);
        model.addAttribute("unreadCount", unreadCount);
        model.addAttribute("employee", hrUser);
        model.addAttribute("totalCount", notifications.size());
        model.addAttribute("isHrPage", true);
        model.addAttribute("isEmployee", false);

        return "notifications/hr-notifications";
    }

    /* ========== HELPER METHOD: GET EMPLOYEE DETAILS ========== */
    private Employee getEmployeeDetails(String employeeId) {
        try {
            return employeeService.getEmployeeByEmployeeId(employeeId)
                    .orElseGet(() -> {
                        // Fallback employee if not found
                        Employee emp = new Employee();
                        emp.setEmployeeId(employeeId);
                        emp.setFirstName("Employee");
                        emp.setLastName("User");
                        return emp;
                    });
        } catch (Exception e) {
            // Return minimal employee object on error
            Employee emp = new Employee();
            emp.setEmployeeId(employeeId);
            emp.setFirstName("Employee");
            emp.setLastName("User");
            return emp;
        }
    }

    /* ========== QUICK ACTIONS FOR NOTIFICATIONS ========== */
    @GetMapping("/employee/mark-all-read")
    public String markAllEmployeeNotificationsRead(HttpSession session) {
        String employeeId = (String) session.getAttribute("candidateEmployeeId");
        if (employeeId != null) {
            notificationService.markAllAsRead(employeeId, "EMPLOYEE");
        }
        return "redirect:/notifications/employee";
    }

    @GetMapping("/hr/mark-all-read")
    public String markAllHrNotificationsRead(HttpSession session) {
        String userRole = (String) session.getAttribute("userRole");
        if ("HR".equals(userRole) || "SUPER_ADMIN".equals(userRole)) {
            notificationService.markAllAsRead("HR_SYSTEM", "HR");
        }
        return "redirect:/notifications/hr";
    }
}