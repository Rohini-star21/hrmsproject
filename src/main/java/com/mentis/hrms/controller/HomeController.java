package com.mentis.hrms.controller;

import com.mentis.hrms.model.Job;
import com.mentis.hrms.model.JobApplication;
import com.mentis.hrms.service.JobService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.ArrayList;

@Controller
public class HomeController {

    private static final Logger logger = LoggerFactory.getLogger(HomeController.class);

    @Autowired
    private JobService jobService;

    @GetMapping("/")
    public String showRoot() {
        return "redirect:/home";
    }

    @GetMapping("/home")
    public String showHome(Model model) {
        try {
            List<Job> jobs = jobService.getActiveJobs();
            model.addAttribute("jobs", jobs);
            model.addAttribute("jobCount", jobs != null ? jobs.size() : 0);
            logger.info("Loaded {} active jobs for home page", jobs != null ? jobs.size() : 0);
            return "home";
        } catch (Exception e) {
            logger.error("Error loading home page: {}", e.getMessage(), e);
            model.addAttribute("error", "Failed to load job listings");
            return "home";
        }
    }

    @GetMapping("/job-description")
    public String showJobDescription(@RequestParam("id") Long jobId, Model model) {
        try {
            logger.info("Loading job description for job ID: {}", jobId);
            Job job = jobService.getJobById(jobId);
            if (job != null) {
                // Ensure lists are initialized
                if (job.getRequirementList() == null) {
                    job.setRequirementList(new ArrayList<>());
                }
                if (job.getResponsibilities() == null) {
                    job.setResponsibilities(new ArrayList<>());
                }

                model.addAttribute("job", job);
                model.addAttribute("application", new JobApplication()); // Add empty application object

                logger.info("Successfully loaded job: {}", job.getTitle());
                return "jobdescription";
            } else {
                logger.warn("Job not found with ID: {}", jobId);
                model.addAttribute("error", "Job not found");
                return "redirect:/?error=Job not found";
            }
        } catch (Exception e) {
            logger.error("Error loading job description for ID {}: {}", jobId, e.getMessage(), e);
            model.addAttribute("error", "Failed to load job description");
            return "redirect:/?error=Failed to load job description";
        }
    }
}