package com.mentis.hrms.service;

import com.mentis.hrms.model.JobApplication;
import com.mentis.hrms.repository.JobApplicationRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.Map;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

@Service
@Transactional
public class JobApplicationService {

    private static final Logger logger = LoggerFactory.getLogger(JobApplicationService.class);
    private static final String UPLOAD_DIR = "uploads";

    @Autowired
    private JobApplicationRepository repo;

    @Autowired
    private EmailService emailService;

    @PersistenceContext
    private EntityManager entityManager;

    /* =========================================================
       FIXED: getApplicationById method
    ========================================================= */
 /* =========================================================
   FIXED: getApplicationById method - WITH CACHE REFRESH
======================================================== */
    @Transactional(readOnly = true)
    public JobApplication getApplicationById(Long id) {
        try {
            logger.info("=== ATTEMPTING TO LOAD APPLICATION WITH ID: {} ===", id);

            // Clear any cached data in the persistence context
            entityManager.clear();

            // Fetch the application from repository
            Optional<JobApplication> optionalApp = repo.findById(id);

            if (optionalApp.isPresent()) {
                JobApplication app = optionalApp.get();

                // Force refresh from database to ensure we have the latest data
                entityManager.refresh(app);

                // Manually initialize the job to avoid LazyInitializationException
                if (app.getJob() != null) {
                    app.getJob().getTitle();
                    app.getJob().getDepartment();
                    app.getJob().getJobType();
                    app.getJob().getLocation();
                }

                logger.info("=== SUCCESSFULLY LOADED APPLICATION ===");
                logger.info("Application ID: {}", app.getId());
                logger.info("Status: {}", app.getStatus());  // Log the status to verify fresh data
                logger.info("Candidate: {} {}", app.getFirstName(), app.getLastName());
                logger.info("Email: {}", app.getEmail());
                logger.info("Phone: {}", app.getPhone());
                logger.info("Has Job: {}", app.getJob() != null);

                if (app.getJob() != null) {
                    logger.info("Job Title: {}", app.getJob().getTitle());
                    logger.info("Job Department: {}", app.getJob().getDepartment());
                    logger.info("Job Type: {}", app.getJob().getJobType());
                    logger.info("Job Location: {}", app.getJob().getLocation());
                }

                return app;
            } else {
                logger.warn("No application found with ID: {}", id);
                return null;
            }

        } catch (Exception e) {
            logger.error("Error loading application with ID {}: {}", id, e.getMessage(), e);
            return null;
        }
    }

    /* ----------------------------------------------------------
     Save with file upload - ENSURE CONSISTENT PATH
    ---------------------------------------------------------- */
    public JobApplication saveApplication(JobApplication app, MultipartFile file) throws IOException {
        try {
            logger.info("=== SAVING APPLICATION ===");
            logger.info("Candidate: {}", app.getFullName());
            logger.info("Email: {}", app.getEmail());
            logger.info("Job: {}", app.getJob() != null ? app.getJob().getTitle() : "No job");

            // Validate application
            if (app.getFirstName() == null || app.getFirstName().trim().isEmpty() ||
                    app.getLastName() == null || app.getLastName().trim().isEmpty() ||
                    app.getEmail() == null || app.getEmail().trim().isEmpty() ||
                    app.getPhone() == null || app.getPhone().trim().isEmpty() ||
                    app.getJob() == null || app.getJob().getId() == null) {
                throw new IllegalArgumentException("Application data is incomplete");
            }

            // Handle file upload
            if (file != null && !file.isEmpty()) {
                String originalFileName = file.getOriginalFilename();
                String safeFileName = originalFileName.replaceAll("[^a-zA-Z0-9.-]", "_");
                String fileName = System.currentTimeMillis() + "_" + safeFileName;

                // Use path from application.properties or default
                String uploadDirPath = "C:/hrms/uploads";
                Path uploadDir = Paths.get(uploadDirPath);

                // Create directory if it doesn't exist
                if (!Files.exists(uploadDir)) {
                    Files.createDirectories(uploadDir);
                    logger.info("Created upload directory: {}", uploadDir.toAbsolutePath());
                }

                // Save file
                Path filePath = uploadDir.resolve(fileName);
                Files.write(filePath, file.getBytes());

                // Store relative path or just filename
                app.setResumePath(fileName);
                logger.info("File saved successfully: {}", filePath.toAbsolutePath());
                logger.info("File size: {} bytes", file.getSize());
            } else {
                logger.warn("No resume file provided for application");
                // You might want to throw an exception here or handle as needed
            }

            // Set default values
            if (app.getStatus() == null || app.getStatus().trim().isEmpty()) {
                app.setStatus("Applied");
            }
            if (app.getApplicationDate() == null) {
                app.setApplicationDate(java.time.LocalDateTime.now());
            }

            // Save to database
            JobApplication saved = repo.save(app);
            logger.info("Application saved to database with ID: {}", saved.getId());

            // Send confirmation email (if email service is configured)
            try {
                if (emailService != null) {
                    emailService.sendApplicationConfirmation(
                            app.getEmail(),
                            app.getFullName(),
                            app.getJobTitle()
                    );
                    logger.info("Confirmation email sent to: {}", app.getEmail());
                }
            } catch (Exception emailEx) {
                logger.warn("Failed to send confirmation email: {}", emailEx.getMessage());
                // Don't throw exception for email failure
            }

            return saved;

        } catch (Exception e) {
            logger.error("Error saving application: {}", e.getMessage(), e);
            throw e;
        }
    }
    public List<JobApplication> getRecentApplications(int limit) {
        try {
            logger.info("Getting {} most recent applications", limit);

            // If limit <= 5, use the optimized query
            if (limit <= 5) {
                return repo.findTop5ByOrderByApplicationDateDesc()
                        .stream().limit(limit).collect(Collectors.toList());
            }

            // Otherwise get all ordered
            List<JobApplication> allApps = repo.findAllByOrderByApplicationDateDesc();
            return allApps.stream().limit(limit).collect(Collectors.toList());

        } catch (Exception e) {
            logger.error("Error getting recent applications: {}", e.getMessage(), e);
            return new ArrayList<>();
        }
    }
    /* ----------------------------------------------------------
       Save without file (status updates, interview scheduling)
    ---------------------------------------------------------- */
    public JobApplication saveApplicationWithoutFile(JobApplication app) {
        try {
            if (app.getStatus() == null) {
                app.setStatus("Applied");
            }
            if (app.getApplicationDate() == null) {
                app.setApplicationDate(java.time.LocalDateTime.now());
            }
            JobApplication saved = repo.save(app);
            logger.info("Application saved without file – ID: {}, Candidate: {}", saved.getId(), app.getFullName());
            return saved;
        } catch (Exception e) {
            logger.error("Error saving application without file: {}", e.getMessage(), e);
            throw e;
        }
    }

    /* ----------------------------------------------------------
       Duplicate-check
    ---------------------------------------------------------- */
    public boolean hasAlreadyApplied(String email, Long jobId) {
        return repo.existsByEmailIgnoreCaseAndJobId(email.toLowerCase(), jobId);
    }

    /* ----------------------------------------------------------
       Lists
    ---------------------------------------------------------- */
    public List<JobApplication> getAllApplications() {
        try {
            List<JobApplication> list = repo.findAll();
            logger.info("Retrieved {} applications", list.size());
            return list;
        } catch (Exception e) {
            logger.error("Error retrieving applications: {}", e.getMessage(), e);
            return new ArrayList<>();
        }
    }

    public List<JobApplication> getRecentApplications() {
        try {
            List<JobApplication> list = repo.findTop5ByOrderByApplicationDateDesc();
            logger.info("Retrieved {} recent applications", list.size());
            return list;
        } catch (Exception e) {
            logger.error("Error retrieving recent applications: {}", e.getMessage(), e);
            return new ArrayList<>();
        }
    }

    /* =========================================================
       NEW: Get recent applications filtered by status
    ========================================================= */
    public List<JobApplication> getRecentApplicationsByStatus(String status) {
        try {
            // Use the new repository method for better performance
            List<JobApplication> filteredApplications = repo.findTop5ByStatusOrderByApplicationDateDesc(status);

            logger.info("Retrieved {} recent applications with status: {}", filteredApplications.size(), status);
            return filteredApplications;
        } catch (Exception e) {
            logger.error("Error retrieving recent applications by status {}: {}", status, e.getMessage(), e);
            return new ArrayList<>();
        }
    }

    /* =========================================================
       NEW: Get applications filtered by specific status
    ========================================================= */
    public List<JobApplication> getApplicationsByStatus(String status) {
        try {
            // Use the new repository method with case-insensitive search
            List<JobApplication> filteredApplications = repo.findByStatusIgnoreCase(status);

            logger.info("Retrieved {} applications with status: {}", filteredApplications.size(), status);
            return filteredApplications;
        } catch (Exception e) {
            logger.error("Error retrieving applications by status {}: {}", status, e.getMessage(), e);
            return new ArrayList<>();
        }
    }

    public List<JobApplication> getApplicationsWithJobs() {
        try {
            // Use the new repository method that fetches jobs
            List<JobApplication> list = repo.findAllWithJob();
            logger.info("Retrieved {} applications with jobs", list.size());
            return list;
        } catch (Exception e) {
            logger.error("Error retrieving applications with jobs: {}", e.getMessage(), e);
            return new ArrayList<>();
        }
    }

    /* =========================================================
       FIXED: Get all hired candidates
    ========================================================= */
    @Transactional(readOnly = true)
    public List<JobApplication> getHiredCandidates() {
        try {
            logger.info("=== GETTING HIRED CANDIDATES ===");

            // Use the new repository query method
            List<JobApplication> hiredApplications = repo.findHiredCandidates();

            logger.info("Found {} hired candidates via repository query", hiredApplications.size());

            // Debug: Also check with case-insensitive manual filtering
            List<JobApplication> allApplications = repo.findAll();
            List<JobApplication> manualFiltered = allApplications.stream()
                    .filter(app -> app.getStatus() != null && app.getStatus().equalsIgnoreCase("Hired"))
                    .sorted((a1, a2) -> a2.getApplicationDate().compareTo(a1.getApplicationDate()))
                    .collect(Collectors.toList());

            logger.info("Manual filtering found {} hired candidates", manualFiltered.size());

            // If repository query returned empty but manual filter found some,
            // update the database to have consistent status values
            if (hiredApplications.isEmpty() && !manualFiltered.isEmpty()) {
                logger.warn("Repository query returned empty but manual filter found candidates. Status values may be inconsistent.");

                // Log all statuses found for debugging
                Map<String, Long> statusCounts = allApplications.stream()
                        .collect(Collectors.groupingBy(
                                app -> app.getStatus() != null ? app.getStatus() : "NULL",
                                Collectors.counting()
                        ));
                logger.info("All application status counts: {}", statusCounts);

                // Update the database to use "Hired" (with capital H)
                for (JobApplication app : manualFiltered) {
                    if (!"Hired".equals(app.getStatus())) {
                        app.setStatus("Hired");
                        repo.save(app);
                        logger.info("Updated status to 'Hired' for: {} {}",
                                app.getFirstName(), app.getLastName());
                    }
                }

                // Now query again
                hiredApplications = repo.findHiredCandidates();
                logger.info("After normalization, found {} hired candidates", hiredApplications.size());
            }

            // Trigger lazy load for jobs to avoid LazyInitializationException
            hiredApplications.forEach(app -> {
                if (app.getJob() != null) {
                    app.getJob().getTitle();
                    app.getJob().getDepartment();
                }

                // Log each hired candidate for debugging
                logger.info("Hired Candidate: ID={}, Name={} {}, Email={}, Status={}, Job={}",
                        app.getId(),
                        app.getFirstName(),
                        app.getLastName(),
                        app.getEmail(),
                        app.getStatus(),
                        app.getJob() != null ? app.getJob().getTitle() : "No Job");
            });

            logger.info("Successfully retrieved {} hired candidates", hiredApplications.size());
            return hiredApplications;

        } catch (Exception e) {
            logger.error("Error retrieving hired candidates: {}", e.getMessage(), e);
            logger.error("Stack trace:", e);
            return new ArrayList<>();
        }
    }

    /* =========================================================
       FIXED: Get count of hired candidates
    ========================================================= */
    public long getHiredCandidatesCount() {
        try {
            long count = repo.countByStatus("Hired");

            // If count is 0, try case-insensitive count
            if (count == 0) {
                List<JobApplication> allApplications = repo.findAll();
                count = allApplications.stream()
                        .filter(app -> app.getStatus() != null && app.getStatus().equalsIgnoreCase("Hired"))
                        .count();
            }

            logger.info("Hired candidates count: {}", count);
            return count;
        } catch (Exception e) {
            logger.error("Error counting hired candidates: {}", e.getMessage(), e);
            return 0;
        }
    }

    /* =========================================================
       NEW: Get count of hired candidates this month
    ========================================================= */
    public long getHiredThisMonthCount() {
        try {
            List<JobApplication> allApplications = repo.findAll();
            LocalDate firstDayOfMonth = LocalDate.now().withDayOfMonth(1);

            long count = allApplications.stream()
                    .filter(app -> app.getStatus() != null && app.getStatus().equalsIgnoreCase("Hired"))
                    .filter(app -> app.getApplicationDate() != null &&
                            app.getApplicationDate().toLocalDate().isAfter(firstDayOfMonth.minusDays(1)))
                    .count();

            logger.info("Hired this month count: {}", count);
            return count;
        } catch (Exception e) {
            logger.error("Error counting hired this month: {}", e.getMessage(), e);
            return 0;
        }
    }

    /* ----------------------------------------------------------
       Counters
    ---------------------------------------------------------- */
    public long getTotalApplicationsCount() {
        try {
            return repo.count();
        } catch (Exception e) {
            logger.error("Error counting applications: {}", e.getMessage(), e);
            return 0;
        }
    }

    /* =========================================================
       FIXED: Get applications count by status
    ========================================================= */
    public long getApplicationsCountByStatus(String status) {
        try {
            long count = repo.countByStatus(status);

            // If count is 0, try case-insensitive count
            if (count == 0 && status != null) {
                List<JobApplication> allApplications = repo.findAll();
                count = allApplications.stream()
                        .filter(app -> app.getStatus() != null && app.getStatus().equalsIgnoreCase(status))
                        .count();
            }

            logger.info("Applications count for status '{}': {}", status, count);
            return count;
        } catch (Exception e) {
            logger.error("Error counting applications by status: {}", e.getMessage(), e);
            return 0;
        }
    }

    /* =========================================================
       NEW: Create test hired candidates for development
    ========================================================= */
    @Transactional
    public void createTestHiredCandidates() {
        try {
            logger.info("Creating test hired candidates...");

            // Get some existing applications
            List<JobApplication> applications = repo.findAll();

            int hiredCount = 0;
            for (JobApplication app : applications) {
                if (hiredCount >= 3) break; // Create only 3 for testing

                // Only update if not already hired
                if (!"Hired".equalsIgnoreCase(app.getStatus())) {
                    // Update status to Hired
                    app.setStatus("Hired");
                    repo.save(app);
                    hiredCount++;

                    logger.info("Marked as hired: {} {} (ID: {})",
                            app.getFirstName(), app.getLastName(), app.getId());
                }
            }

            logger.info("Created {} test hired candidates", hiredCount);

        } catch (Exception e) {
            logger.error("Error creating test hired candidates: {}", e.getMessage(), e);
        }
    }

    /* =========================================================
       NEW: Normalize all status values to standard format
    ========================================================= */
    @Transactional
    public void normalizeStatusValues() {
        try {
            logger.info("Normalizing application status values...");

            List<JobApplication> allApplications = repo.findAll();
            int updatedCount = 0;

            for (JobApplication app : allApplications) {
                String originalStatus = app.getStatus();
                String normalizedStatus = null;

                if (originalStatus != null) {
                    switch (originalStatus.toLowerCase()) {
                        case "applied":
                        case "application":
                            normalizedStatus = "Applied";
                            break;
                        case "in review":
                        case "review":
                        case "under review":
                            normalizedStatus = "In Review";
                            break;
                        case "interview":
                        case "interview scheduled":
                        case "scheduled":
                            normalizedStatus = "Interview";
                            break;
                        case "interviewed":
                            normalizedStatus = "Interviewed";
                            break;
                        case "hired":
                        case "hired!":
                        case "hired.":
                            normalizedStatus = "Hired";
                            break;
                        case "on hold":
                        case "hold":
                            normalizedStatus = "On Hold";
                            break;
                        case "rejected":
                        case "reject":
                            normalizedStatus = "Rejected";
                            break;
                        default:
                            normalizedStatus = originalStatus;
                    }

                    if (!originalStatus.equals(normalizedStatus)) {
                        app.setStatus(normalizedStatus);
                        repo.save(app);
                        updatedCount++;
                        logger.info("Normalized status for {} {}: {} -> {}",
                                app.getFirstName(), app.getLastName(), originalStatus, normalizedStatus);
                    }
                }
            }

            logger.info("Normalized {} application status values", updatedCount);

        } catch (Exception e) {
            logger.error("Error normalizing status values: {}", e.getMessage(), e);
        }
    }
}