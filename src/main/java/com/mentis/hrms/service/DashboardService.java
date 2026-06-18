package com.mentis.hrms.service;

import org.springframework.ui.Model;
import java.util.List;
import com.mentis.hrms.model.Job;
import com.mentis.hrms.model.Department;
import com.mentis.hrms.model.JobApplication;

public interface DashboardService {
    void loadDashboardData(Model model);
    List<Job> getAllJobs();
    List<Department> getAllDepartments();
    List<JobApplication> getRecentApplicationsByStatus(String status);
    int calculateTotalCandidatesSafely();
    int calculateInterviewsScheduledSafely();
    int calculateHiredThisMonthSafely();
    int calculateInOnboardingSafely();
}