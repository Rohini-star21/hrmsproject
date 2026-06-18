package com.mentis.hrms.service;

import com.mentis.hrms.model.Job;
import com.mentis.hrms.model.Department;
import com.mentis.hrms.model.JobApplication;
import com.mentis.hrms.service.JobService;
import com.mentis.hrms.service.DepartmentService;
import com.mentis.hrms.service.JobApplicationService;
import com.mentis.hrms.service.OfferService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.ui.Model;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.List;
// ADD THESE MISSING IMPORTS
import java.util.ArrayList;
import java.util.List;
@Service
public class DashboardServiceImpl implements DashboardService {
    private static final Logger logger = LoggerFactory.getLogger(DashboardServiceImpl.class);

    @Autowired
    private JobService jobService;

    @Autowired
    private DepartmentService departmentService;

    @Autowired
    private JobApplicationService applicationService;

    @Autowired
    private OfferService offerService;

    @Override
    public void loadDashboardData(Model model) {
        try {
            List<Job> jobs = jobService.getAllJobs();
            List<Department> departments = departmentService.getAllDepartments();

            // FIX: Get recent applications here
            List<JobApplication> recentApplications = applicationService.getRecentApplicationsByStatus("Applied");

            model.addAttribute("jobs", jobs);
            model.addAttribute("departments", departments);
            model.addAttribute("recentApplications", recentApplications);  // Add this line
            model.addAttribute("totalCandidates", calculateTotalCandidatesSafely());
            model.addAttribute("interviewsScheduled", calculateInterviewsScheduledSafely());
            model.addAttribute("hiredThisMonth", calculateHiredThisMonthSafely());
            model.addAttribute("inOnboarding", calculateInOnboardingSafely());
            model.addAttribute("offerCount", offerService.getOfferCount());

            logger.info("Dashboard data loaded successfully with {} recent applications",
                    recentApplications.size());
        } catch (Exception e) {
            logger.error("Failed to load dashboard data: {}", e.getMessage(), e);
            model.addAttribute("error", "Failed to load dashboard data.");
            model.addAttribute("offerCount", 0);
            model.addAttribute("recentApplications", new ArrayList<>());  // Add empty list on error
        }
    }

    @Override
    public List<Job> getAllJobs() {
        try {
            return jobService.getAllJobs();
        } catch (Exception e) {
            logger.error("Error getting jobs: {}", e.getMessage());
            return List.of();
        }
    }

    @Override
    public List<Department> getAllDepartments() {
        try {
            return departmentService.getAllDepartments();
        } catch (Exception e) {
            logger.error("Error getting departments: {}", e.getMessage());
            return List.of();
        }
    }

    @Override
    public List<JobApplication> getRecentApplicationsByStatus(String status) {
        try {
            return applicationService.getRecentApplicationsByStatus(status);
        } catch (Exception e) {
            logger.error("Error getting recent applications: {}", e.getMessage());
            return List.of();
        }
    }

    @Override
    public int calculateTotalCandidatesSafely() {
        try {
            return applicationService.getAllApplications().size();
        } catch (Exception e) {
            logger.warn("Error calculating total candidates: {}", e.getMessage());
            return 0;
        }
    }

    @Override
    public int calculateInterviewsScheduledSafely() {
        try {
            return (int) applicationService.getAllApplications().stream()
                    .filter(app -> "Interview".equals(app.getStatus())).count();
        } catch (Exception e) {
            logger.warn("Error calculating interviews: {}", e.getMessage());
            return 0;
        }
    }

    @Override
    public int calculateHiredThisMonthSafely() {
        try {
            return (int) applicationService.getAllApplications().stream()
                    .filter(app -> "Hired".equals(app.getStatus())).count();
        } catch (Exception e) {
            logger.warn("Error calculating hired this month: {}", e.getMessage());
            return 0;
        }
    }

    @Override
    public int calculateInOnboardingSafely() {
        try {
            return (int) applicationService.getAllApplications().stream()
                    .filter(app -> "Onboarding".equals(app.getStatus())).count();
        } catch (Exception e) {
            logger.warn("Error calculating in onboarding: {}", e.getMessage());
            return 0;
        }
    }
}