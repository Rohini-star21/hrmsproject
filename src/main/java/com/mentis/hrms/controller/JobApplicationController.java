package com.mentis.hrms.controller;

import com.mentis.hrms.model.Job;
import com.mentis.hrms.model.JobApplication;
import com.mentis.hrms.service.JobApplicationService;
import com.mentis.hrms.service.JobService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.HashMap;

@Controller
@RequestMapping("/apply")
public class JobApplicationController {

    private static final Logger logger = LoggerFactory.getLogger(JobApplicationController.class);

    @Autowired
    private JobApplicationService applicationService;

    @Autowired
    private JobService jobService;

    @PostMapping("/submit")
    public String submitApplication(
            @ModelAttribute JobApplication application,
            @RequestParam("jobId") Long jobId,
            @RequestParam("resumeFile") MultipartFile resumeFile,
            RedirectAttributes ra) {

        try {
            logger.info("=== SUBMITTING APPLICATION FOR JOB ID: {} ===", jobId);
            logger.info("Candidate: {} {}", application.getFirstName(), application.getLastName());
            logger.info("Email: {}", application.getEmail());
            logger.info("Resume File: {} ({} bytes)",
                    resumeFile.getOriginalFilename(),
                    resumeFile.getSize());

            // 1. Get Job
            Job job = jobService.getJobById(jobId);
            if (job == null) {
                logger.error("Job not found with ID: {}", jobId);
                ra.addFlashAttribute("error", "Job not found");
                return "redirect:/job-description?id=" + jobId;
            }
            logger.info("Found job: {}", job.getTitle());

            // 2. Check for duplicate application
            if (applicationService.hasAlreadyApplied(application.getEmail(), jobId)) {
                logger.warn("Duplicate application detected for email: {} and job: {}",
                        application.getEmail(), jobId);
                ra.addFlashAttribute("error", "You have already applied for this position with this email address.");
                return "redirect:/job-description?id=" + jobId + "&duplicate=true";
            }

            // 3. Validate required fields
            if (application.getFirstName() == null || application.getFirstName().trim().isEmpty() ||
                    application.getLastName() == null || application.getLastName().trim().isEmpty() ||
                    application.getEmail() == null || application.getEmail().trim().isEmpty() ||
                    application.getPhone() == null || application.getPhone().trim().isEmpty()) {
                logger.error("Missing required fields");
                ra.addFlashAttribute("error", "Please fill all required fields");
                return "redirect:/job-description?id=" + jobId + "&error=missing_fields";
            }

            // 4. Validate resume file
            if (resumeFile == null || resumeFile.isEmpty()) {
                logger.error("No resume file uploaded");
                ra.addFlashAttribute("error", "Please upload your resume");
                return "redirect:/job-description?id=" + jobId + "&error=no_resume";
            }

            // 5. Validate file type
            String fileName = resumeFile.getOriginalFilename();
            if (fileName == null || !fileName.matches(".*\\.(pdf|doc|docx|jpg|jpeg|png)$")) {
                logger.error("Invalid file type: {}", fileName);
                ra.addFlashAttribute("error", "Please upload PDF, DOC, DOCX, JPG, JPEG, or PNG files only");
                return "redirect:/job-description?id=" + jobId + "&error=invalid_file";
            }

            // 6. Set job and save application
            application.setJob(job);
            application.setStatus("Applied");

            try {
                JobApplication savedApplication = applicationService.saveApplication(application, resumeFile);
                logger.info("Application saved successfully! ID: {}", savedApplication.getId());

                // Success - redirect with success message
                ra.addFlashAttribute("success",
                        "Application submitted successfully! We will review your application and get back to you soon.");
                return "redirect:/job-description?id=" + jobId + "&success=true";

            } catch (IOException e) {
                logger.error("File upload error: {}", e.getMessage(), e);
                ra.addFlashAttribute("error", "Failed to upload resume: " + e.getMessage());
                return "redirect:/job-description?id=" + jobId + "&error=upload_failed";
            }

        } catch (Exception e) {
            logger.error("Error submitting application: {}", e.getMessage(), e);
            ra.addFlashAttribute("error", "Failed to submit application: " +
                    (e.getMessage() != null ? e.getMessage() : "Unknown error"));
            return "redirect:/job-description?id=" + jobId + "&error=submission_failed";
        }
    }

    @GetMapping("/test-upload")
    @ResponseBody
    public String testUpload() {
        String uploadDir = "C:/hrms/uploads";
        Path path = Paths.get(uploadDir);

        try {
            if (!Files.exists(path)) {
                Files.createDirectories(path);
                return "Created upload directory: " + path.toAbsolutePath();
            }
            return "Upload directory exists and is writable: " + path.toAbsolutePath();
        } catch (IOException e) {
            return "Error creating directory: " + e.getMessage();
        }
    }

    @PostMapping("/debug-submit")
    @ResponseBody
    public Map<String, Object> debugSubmit(
            @RequestParam Map<String, String> allParams,
            @RequestParam("resumeFile") MultipartFile resumeFile) {

        Map<String, Object> response = new HashMap<>();
        response.put("timestamp", LocalDateTime.now());
        response.put("parameters", allParams);
        response.put("file_name", resumeFile.getOriginalFilename());
        response.put("file_size", resumeFile.getSize());
        response.put("file_content_type", resumeFile.getContentType());

        return response;
    }
}