package com.mentis.hrms.service;

import com.mentis.hrms.model.Job;
import com.mentis.hrms.repository.JobRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.List;
import java.util.Optional;        // ← ADD THIS IMPORT
import java.util.ArrayList;
@Service
public class JobService {

    private static final Logger logger = LoggerFactory.getLogger(JobService.class);
    @Autowired
    private JobRepository jobRepository;


    public List<Job> getAllJobs() {
        try {
            List<Job> jobs = jobRepository.findAll();
            // Initialize lists for each job
            for (Job job : jobs) {
                initializeJobLists(job);
            }
            return jobs;
        } catch (Exception e) {
            throw new RuntimeException("Failed to retrieve jobs: " + e.getMessage(), e);
        }
    }

    public List<Job> getActiveJobs() {
        try {
            List<Job> jobs = jobRepository.findByActiveTrue();
            // Initialize lists for each job
            for (Job job : jobs) {
                initializeJobLists(job);
            }
            return jobs;
        } catch (Exception e) {
            throw new RuntimeException("Failed to retrieve active jobs: " + e.getMessage(), e);
        }
    }

    public Job saveJob(Job job) {
        try {
            // Ensure the job has required fields before saving
            if (job.getTitle() == null || job.getTitle().trim().isEmpty()) {
                throw new IllegalArgumentException("Job title is required");
            }
            if (job.getDepartment() == null || job.getDepartment().trim().isEmpty()) {
                throw new IllegalArgumentException("Department is required");
            }

            // Initialize lists if null
            initializeJobLists(job);

            return jobRepository.save(job);
        } catch (Exception e) {
            throw new RuntimeException("Failed to save job: " + e.getMessage(), e);
        }
    }

    public Job getJobById(Long id) {
        try {
            Optional<Job> job = jobRepository.findById(id);
            if (job.isPresent()) {
                initializeJobLists(job.get());
                return job.get();
            }
            return null;
        } catch (Exception e) {
            throw new RuntimeException("Failed to retrieve job with ID " + id + ": " + e.getMessage(), e);
        }
    }

    public void deleteJob(Long id) {
        logger.info("Deleting job with ID: {}", id);
        try {
            Optional<Job> jobOptional = jobRepository.findById(id);
            if (!jobOptional.isPresent()) {
                logger.warn("Job not found with ID: {}", id);
                throw new RuntimeException("Job not found with ID: " + id);
            }

            // USE SOFT DELETE to prevent database constraint crashes
            Job job = jobOptional.get();
            job.setActive(false);
            jobRepository.save(job);

            logger.info("Job deleted successfully: {}", id);
        } catch (Exception e) {
            logger.error("Error deleting job {}: {}", id, e.getMessage());
            throw e;
        }
    }

    // Additional method to find jobs by department
    public List<Job> getJobsByDepartment(String department) {
        try {
            List<Job> jobs = jobRepository.findByDepartment(department);
            // Initialize lists for each job
            for (Job job : jobs) {
                initializeJobLists(job);
            }
            return jobs;
        } catch (Exception e) {
            throw new RuntimeException("Failed to retrieve jobs for department " + department + ": " + e.getMessage(), e);
        }
    }

    // Helper method to initialize lists
    private void initializeJobLists(Job job) {
        if (job.getRequirementList() == null) {
            job.setRequirementList(new ArrayList<>());
        }
        if (job.getResponsibilities() == null) {
            job.setResponsibilities(new ArrayList<>());
        }
    }
}