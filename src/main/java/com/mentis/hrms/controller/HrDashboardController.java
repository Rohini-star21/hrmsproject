
        package com.mentis.hrms.controller;

import com.mentis.hrms.dto.OfferRequest;
import com.mentis.hrms.model.Employee;
import com.mentis.hrms.dto.EmployeeFormDTO;
import com.mentis.hrms.interceptor.RoleInterceptor;
import com.mentis.hrms.model.Job;
import com.mentis.hrms.model.JobApplication;
import com.mentis.hrms.model.OfferLetter;
import com.mentis.hrms.model.enums.UserRole;
import com.mentis.hrms.service.DashboardService;
import com.mentis.hrms.service.DepartmentService;
import com.mentis.hrms.service.EmployeeService;
import com.mentis.hrms.service.JobApplicationService;
import com.mentis.hrms.service.JobService;
import com.mentis.hrms.service.OfferService;
import com.mentis.hrms.repository.OfferLetterRepository;
import com.mentis.hrms.util.StringCleaner;
import jakarta.servlet.http.HttpSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.*;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RequestBody;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.time.LocalDateTime;
import org.springframework.web.bind.annotation.DeleteMapping;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.Map;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RequestBody;
import java.util.Map;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Optional;
import com.mentis.hrms.model.OnboardingDocument;
import com.mentis.hrms.service.DocumentService;
import org.springframework.beans.factory.annotation.Value;
@Controller
@RequestMapping("/dashboard/hr")
public class HrDashboardController {
    private static final Logger logger = LoggerFactory.getLogger(HrDashboardController.class);
    // ADD THIS FIELD
    @Value("${app.upload.base-path:C:/hrms/uploads}")
    private String basePath;
    @Autowired
    private DashboardService dashboardService;

    @Autowired
    private OfferService offerService;

    @Autowired
    private EmployeeService employeeService;

    @Autowired
    private JobService jobService;

    @Autowired
    private JobApplicationService applicationService;

    @Autowired
    private DepartmentService departmentService;
    @Autowired
    private DocumentService documentService;
    /* ========== HR DASHBOARD HOME ========== */
    @GetMapping
    public String hrDashboard(Model model, HttpSession session, RedirectAttributes ra) {
        if (!isAuthorized(session, ra)) {
            return "redirect:/candidate/login";
        }
        dashboardService.loadDashboardData(model);  // This now includes recentApplications
        addHRAttributes(model, session);

        return "dashboard";
    }




    /* ========== ROLES & PERMISSIONS ========== */
    @GetMapping("/roles-access")
    public String rolesAccessPage(Model model, HttpSession session, RedirectAttributes ra) {
        logger.info("=== ROLES & PERMISSIONS PAGE REQUESTED ===");

        if (!isAuthorized(session, ra)) {
            return "redirect:/candidate/login";
        }

        String role = (String) session.getAttribute("userRole");
        if (!"SUPER_ADMIN".equals(role)) {
            logger.warn("❌ Access denied to Roles & Permissions");
            ra.addFlashAttribute("error", "Access denied: Super Admin only");
            return "redirect:/dashboard/hr";
        }

        addHRAttributes(model, session);
        model.addAttribute("isSuperAdmin", true);
        return "rolesaccess";
    }


    /* ========== APPLICATIONS ========== */
    /* ========== APPLICATIONS ========== */
    @GetMapping("/applications")
    public String hrApplications(@RequestParam(value = "status", required = false) String status,
                                 Model model, HttpSession session, RedirectAttributes ra) {
        logger.info("=== HR APPLICATIONS PAGE REQUESTED ===");
        logger.info("Filter status: {}", status);

        if (!isAuthorized(session, ra)) {
            return "redirect:/candidate/login";
        }

        try {
            List<JobApplication> applications;

            // Get applications based on status filter
            if (status != null && !status.isEmpty() && !status.equals("all")) {
                applications = applicationService.getApplicationsByStatus(status);
                logger.info("Filtered by status '{}': {} applications", status, applications.size());
            } else {
                applications = applicationService.getApplicationsWithJobs();
                logger.info("All applications: {}", applications.size());
            }

            // Get counts for all status types
            long totalApplications = applicationService.getTotalApplicationsCount();
            long appliedCount = applicationService.getApplicationsCountByStatus("Applied");
            long inReviewCount = applicationService.getApplicationsCountByStatus("In Review");
            long interviewCount = applicationService.getApplicationsCountByStatus("Interview");
            long interviewedCount = applicationService.getApplicationsCountByStatus("Interviewed");
            long onHoldCount = applicationService.getApplicationsCountByStatus("On Hold");
            long selectedCount = applicationService.getApplicationsCountByStatus("Selected");
            long hiredCount = applicationService.getApplicationsCountByStatus("Hired");
            long rejectedCount = applicationService.getApplicationsCountByStatus("Rejected");

            // Add to model
            model.addAttribute("applications", applications);
            model.addAttribute("selectedStatus", status != null ? status : "all");

            // Counts for stat cards
            model.addAttribute("totalApplications", totalApplications);
            model.addAttribute("newApplications", appliedCount); // For backward compatibility
            model.addAttribute("appliedCount", appliedCount);
            model.addAttribute("inReviewCount", inReviewCount);
            model.addAttribute("interviewCount", interviewCount);
            model.addAttribute("interviewScheduled", interviewCount); // For backward compatibility
            model.addAttribute("interviewedCount", interviewedCount);
            model.addAttribute("onHoldCount", onHoldCount);
            model.addAttribute("selectedCount", selectedCount);  // NEW - for Selected stat card
            model.addAttribute("hiredCount", hiredCount);
            model.addAttribute("rejectedCount", rejectedCount);
            model.addAttribute("offerCount", offerService.getOfferCount());

            logger.info("Counts - Total: {}, Applied: {}, In Review: {}, Interview: {}, Interviewed: {}, On Hold: {}, Selected: {}, Hired: {}, Rejected: {}",
                    totalApplications, appliedCount, inReviewCount, interviewCount, interviewedCount, onHoldCount, selectedCount, hiredCount, rejectedCount);

            addHRAttributes(model, session);
            return "applications";

        } catch (Exception e) {
            logger.error("Error loading applications: {}", e.getMessage(), e);
            model.addAttribute("error", "Failed to load applications: " + e.getMessage());
            model.addAttribute("applications", new ArrayList<JobApplication>());
            model.addAttribute("offerCount", 0);
            model.addAttribute("selectedCount", 0);  // Add default value
            addHRAttributes(model, session);
            return "applications";
        }
    }


    @GetMapping("/viewApplication/{id}")
    public String hrViewApplication(@PathVariable("id") Long applicationId,
                                    Model model, HttpSession session, RedirectAttributes ra) {
        logger.info("=== HR VIEW APPLICATION {} REQUESTED ===", applicationId);

        if (!isAuthorized(session, ra)) {
            return "redirect:/candidate/login";
        }

        try {
            // Service now handles cache clearing - just fetch the application
            JobApplication app = applicationService.getApplicationById(applicationId);

            if (app == null) {
                ra.addFlashAttribute("error", "Application not found");
                return "redirect:/dashboard/hr/applications";
            }

            // Log the fetched status for debugging
            logger.info("Fetched application ID: {}, Status: {}", applicationId, app.getStatus());

            // ============ ADD ALL REQUIRED MODEL ATTRIBUTES ============

            // Basic application info - THIS IS CRITICAL
            model.addAttribute("application", app);  // <-- ENSURE THIS LINE EXISTS
            model.addAttribute("applicationId", applicationId); // Backup for forms

            // Job information
            model.addAttribute("jobTitle", app.getJobTitle() != null ? app.getJobTitle() : "Position Not Specified");
            model.addAttribute("jobDepartment", app.getJobDepartment() != null ? app.getJobDepartment() : "Not specified");
            model.addAttribute("jobType", app.getJobType() != null ? app.getJobType() : "Not specified");
            model.addAttribute("jobLocation", app.getJobLocation() != null ? app.getJobLocation() : "Not specified");

            // Candidate information
            model.addAttribute("email", app.getEmail() != null ? app.getEmail() : "");
            model.addAttribute("phone", app.getPhone() != null ? app.getPhone() : "");
            model.addAttribute("linkedinProfile", app.getLinkedinProfile());
            model.addAttribute("experience", app.getExperience() != null ? app.getExperience() : "Not specified");
            model.addAttribute("coverLetter", app.getCoverLetter());

            // Date formatting
            model.addAttribute("formattedApplicationDate", app.getFormattedApplicationDate());

            // ============ RESUME ATTRIBUTES ============

            // Check if resume path exists
            boolean hasResume = app.getResumePath() != null && !app.getResumePath().trim().isEmpty();
            boolean resumeExists = false;
            String resumeFileName = null;
            String resumeFileSize = null;
            String resumeFileType = null;

            if (hasResume) {
                try {
                    Path filePath = Paths.get("C:/hrms/uploads").resolve(app.getResumePath()).normalize();
                    resumeExists = Files.exists(filePath);

                    if (resumeExists) {
                        // Extract filename
                        Path fileNamePath = Paths.get(app.getResumePath());
                        resumeFileName = fileNamePath.getFileName().toString();

                        // Get file size
                        File file = filePath.toFile();
                        long size = file.length();
                        resumeFileSize = formatFileSize(size);

                        // Determine file type for template
                        String ext = resumeFileName.toLowerCase();
                        if (ext.endsWith(".pdf")) {
                            resumeFileType = "PDF";
                        } else if (ext.endsWith(".jpg") || ext.endsWith(".jpeg") || ext.endsWith(".png")) {
                            resumeFileType = "IMAGE";
                        } else if (ext.endsWith(".doc") || ext.endsWith(".docx")) {
                            resumeFileType = "DOCUMENT";
                        } else {
                            resumeFileType = "FILE";
                        }
                    }
                } catch (Exception e) {
                    logger.warn("Could not check resume file existence: {}", e.getMessage());
                    resumeExists = false;
                }
            }

            // Add ALL resume attributes to model - NEVER null, always boolean
            model.addAttribute("hasResume", hasResume);
            model.addAttribute("resumeExists", resumeExists);
            model.addAttribute("resumeFileName", resumeFileName != null ? resumeFileName : "No resume");
            model.addAttribute("resumeFileSize", resumeFileSize != null ? resumeFileSize : "0 B");
            model.addAttribute("resumeFileType", resumeFileType != null ? resumeFileType : "UNKNOWN");

            // Other dashboard attributes
            model.addAttribute("offerCount", offerService.getOfferCount());

            // HR attributes (sidebar, user info)
            addHRAttributes(model, session);

            logger.info("✅ Application details loaded for ID: {} - Status: {}, Has Resume: {}, Resume Exists: {}",
                    applicationId, app.getStatus(), hasResume, resumeExists);

            return "application-details";

        } catch (Exception e) {
            logger.error("Error loading application {}: {}", applicationId, e.getMessage(), e);
            ra.addFlashAttribute("error", "Failed to load application details: " + e.getMessage());
            return "redirect:/dashboard/hr/applications";
        }
    }

    /**
     * API endpoint to get the next available employee ID
     */
    @GetMapping("/api/next-employee-id")
    @ResponseBody
    public Map<String, Object> getNextEmployeeId() {
        logger.info("=== GET NEXT EMPLOYEE ID ===");

        Map<String, Object> response = new HashMap<>();

        try {
            // Get all employees and find the highest EMP number
            List<Employee> allEmployees = employeeService.getAllEmployees();
            int maxNum = 0;

            for (Employee emp : allEmployees) {
                String empId = emp.getEmployeeId();
                if (empId != null && empId.startsWith("EMP")) {
                    try {
                        String numPart = empId.substring(3);
                        int num = Integer.parseInt(numPart);
                        if (num > maxNum) {
                            maxNum = num;
                        }
                    } catch (NumberFormatException e) {
                        // Skip non-numeric suffixes
                    }
                }
            }

            // Generate next ID
            int nextNum = maxNum + 1;
            String nextEmployeeId = String.format("EMP%03d", nextNum);

            response.put("success", true);
            response.put("employeeId", nextEmployeeId);
            response.put("currentMax", maxNum);

            logger.info("Next employee ID: {} (current max: {})", nextEmployeeId, maxNum);

        } catch (Exception e) {
            logger.error("Error generating next employee ID: {}", e.getMessage());
            response.put("success", false);
            response.put("error", e.getMessage());
            response.put("employeeId", "EMP001"); // Fallback
        }

        return response;
    }


    /* ========== UPDATE APPLICATION STATUS ========== */
    /* ========== UPDATE APPLICATION STATUS ========== */
    /* ========== UPDATE APPLICATION STATUS ========== */
    @PostMapping(value = {"/update-status", "/update-status/"})
    public String updateApplicationStatus(
            @RequestParam(value = "applicationId", required = false) String applicationIdStr,
            @RequestParam("newStatus") String newStatus,
            @RequestParam(value = "statusNotes", required = false) String statusNotes,
            RedirectAttributes ra,
            HttpSession session) {

        logger.info("=== UPDATING APPLICATION STATUS ===");
        logger.info("Received applicationIdStr: '{}', newStatus: '{}'", applicationIdStr, newStatus);

        if (!isAuthorized(session, ra)) {
            return "redirect:/candidate/login";
        }

        // Validate applicationId
        if (applicationIdStr == null || applicationIdStr.trim().isEmpty() || "null".equals(applicationIdStr)) {
            logger.error("Application ID is missing or empty");
            ra.addFlashAttribute("error", "Application ID is required. Please try again.");
            return "redirect:/dashboard/hr/applications";
        }

        Long applicationId;
        try {
            applicationId = Long.parseLong(applicationIdStr.trim());
        } catch (NumberFormatException e) {
            logger.error("Invalid Application ID format: {}", applicationIdStr);
            ra.addFlashAttribute("error", "Invalid Application ID format");
            return "redirect:/dashboard/hr/applications";
        }

        try {
            JobApplication application = applicationService.getApplicationById(applicationId);
            if (application == null) {
                logger.error("Application not found with ID: {}", applicationId);
                ra.addFlashAttribute("error", "Application not found");
                return "redirect:/dashboard/hr/applications";
            }

            // Update status
            String oldStatus = application.getStatus();
            application.setStatus(newStatus);
            applicationService.saveApplicationWithoutFile(application);

            logger.info("Status updated for Application {}: {} -> {}", applicationId, oldStatus, newStatus);

            // Add success message
            ra.addFlashAttribute("success", "Status updated successfully from '" + oldStatus + "' to '" + newStatus + "'");
            ra.addFlashAttribute("showStatusUpdateSuccess", true);

            // If status is Selected, add special message
            if ("Selected".equalsIgnoreCase(newStatus)) {
                ra.addFlashAttribute("selectedMessage", "Candidate has been marked as Selected. You can now generate an offer letter.");
            }

            // Redirect back to application details
            return "redirect:/dashboard/hr/viewApplication/" + applicationId;

        } catch (Exception e) {
            logger.error("Error updating status: {}", e.getMessage(), e);
            ra.addFlashAttribute("error", "Failed to update status: " + e.getMessage());
            return "redirect:/dashboard/hr/viewApplication/" + applicationId;
        }
    }
    @GetMapping("/interview-schedule/{id}")
    public String hrInterviewSchedule(@PathVariable("id") Long applicationId,
                                      Model model, HttpSession session, RedirectAttributes ra) {
        logger.info("=== HR INTERVIEW SCHEDULE PAGE FOR APPLICATION {} ===", applicationId);

        if (!isAuthorized(session, ra)) {
            return "redirect:/candidate/login";
        }

        try {
            JobApplication app = applicationService.getApplicationById(applicationId);
            if (app == null) {
                return "redirect:/dashboard/hr/applications?error=Application+not+found";
            }
            model.addAttribute("application", app);  // <-- This is the key!
            model.addAttribute("selectedApplication", app);  // <-- Add this line!
            model.addAttribute("offerCount", offerService.getOfferCount());
            addHRAttributes(model, session);
            return "interview-schedule";
        } catch (Exception e) {
            logger.error("Error loading interview schedule: {}", e.getMessage(), e);
            return "redirect:/dashboard/hr/applications?error=Failed+to+load+interview+schedule";
        }
    }

    /* ========== DELETE EMPLOYEE ========== */
    @DeleteMapping("/delete-employee/{employeeId}")
    @ResponseBody
    public ResponseEntity<?> deleteEmployee(@PathVariable String employeeId, HttpSession session) {
        logger.info("=== DELETE EMPLOYEE REQUESTED: {} ===", employeeId);

        // Check authorization
        String role = (String) session.getAttribute("userRole");
        if (!"HR".equals(role) && !"SUPER_ADMIN".equals(role)) {
            logger.warn("Unauthorized delete attempt by role: {}", role);
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("success", false, "message", "Unauthorized access"));
        }

        try {
            // Find employee by employeeId
            Optional<Employee> employeeOpt = employeeService.getEmployeeByEmployeeId(employeeId);

            if (employeeOpt.isEmpty()) {
                logger.warn("Employee not found with ID: {}", employeeId);
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Map.of("success", false, "message", "Employee not found with ID: " + employeeId));
            }

            Employee employee = employeeOpt.get();

            // Delete using the employee's database ID (Long)
            employeeService.deleteEmployee(employee.getId());

            logger.info("✅ Employee deleted successfully: {} ({})", employee.getEmployeeName(), employeeId);

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Employee deleted successfully"
            ));

        } catch (Exception e) {
            logger.error("Error deleting employee {}: {}", employeeId, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("success", false, "message", "Failed to delete employee: " + e.getMessage()));
        }
    }

    /* ========== JOB MANAGEMENT ========== */
    @GetMapping("/job-form")
    public String hrJobForm(Model model, HttpSession session, RedirectAttributes ra,
                            @RequestParam(value = "id", required = false) Long jobId) {

        // Security check
        if (!isAuthorized(session, ra)) {
            return "redirect:/candidate/login";
        }

        try {
            // CRITICAL: Always pass departments and HR attributes so the page doesn't crash
            model.addAttribute("departments", departmentService.getAllDepartments());
            addHRAttributes(model, session);

            // If editing, load the existing job
            if (jobId != null) {
                Job job = jobService.getJobById(jobId);
                if (job != null) {
                    model.addAttribute("job", job);
                }
            }
            return "job-form";

        } catch (Exception e) {
            logger.error("Error loading job form: {}", e.getMessage());
            ra.addFlashAttribute("error", "Failed to load job form.");
            return "redirect:/dashboard/hr";
        }
    }
    @GetMapping("/viewJob/{id}")
    public String hrViewJob(@PathVariable("id") Long jobId, Model model,
                            HttpSession session, RedirectAttributes ra) {
        logger.info("=== HR VIEW JOB {} REQUESTED ===", jobId);

        if (!isAuthorized(session, ra)) {
            return "redirect:/candidate/login";
        }

        try {
            Job job = jobService.getJobById(jobId);
            if (job == null) {
                ra.addFlashAttribute("error", "Job not found");
                return "redirect:/dashboard/hr";
            }

            model.addAttribute("job", job);
            model.addAttribute("offerCount", offerService.getOfferCount());
            addHRAttributes(model, session);

            return "job-details"; // Make sure this template exists
        } catch (Exception e) {
            logger.error("Error viewing job: {}", e.getMessage(), e);
            ra.addFlashAttribute("error", "Failed to load job details");
            return "redirect:/dashboard/hr";
        }
    }
// Add this method to HrDashboardController.java

    /**
     * DELETE JOB - POST Method (Matches form submission)
     */
    @PostMapping("/deleteJob/{id}")
    public String deleteJob(@PathVariable Long id, RedirectAttributes ra, HttpSession session) {
        logger.info("=== DELETE JOB REQUESTED: {} ===", id);

        // Check authorization
        if (!isAuthorized(session, ra)) {
            return "redirect:/candidate/login";
        }

        try {
            // Check if job exists
            Job job = jobService.getJobById(id);
            if (job == null) {
                logger.warn("Job not found with ID: {}", id);
                ra.addFlashAttribute("error", "Job not found with ID: " + id);
                return "redirect:/dashboard/hr";
            }

            // Delete the job
            jobService.deleteJob(id);

            // Add success message
            ra.addFlashAttribute("success", "Job '" + job.getTitle() + "' deleted successfully!");
            logger.info("✅ Job deleted successfully: {} (ID: {})", job.getTitle(), id);

        } catch (Exception e) {
            logger.error("Error deleting job {}: {}", id, e.getMessage(), e);
            ra.addFlashAttribute("error", "Failed to delete job: " + e.getMessage());
        }

        return "redirect:/dashboard/hr";
    }
    /* ========== OFFER MANAGEMENT ========== */
    @GetMapping("/offer-candidates")
    public String hrOfferCandidates(Model model, HttpSession session, RedirectAttributes ra) {
        logger.info("=== HR OFFER CANDIDATES PAGE REQUESTED ===");

        if (!isAuthorized(session, ra)) {
            return "redirect:/candidate/login";
        }

        try {
            List<OfferLetter> offerCandidates = offerService.getAllOfferLetters();
            model.addAttribute("offerCandidates", offerCandidates);
            model.addAttribute("totalOffers", offerService.getOfferCount());
            model.addAttribute("sentOffers", offerService.getSentOfferCount());
            model.addAttribute("pendingOffers", offerService.getOfferCount() - offerService.getSentOfferCount());
            model.addAttribute("offerCount", offerService.getOfferCount());

            // FIX: Add isEmployee attribute (default false for offer candidates list)
            model.addAttribute("isEmployee", false);

            addHRAttributes(model, session);
            return "offer/offer-candidates";
        } catch (Exception e) {
            logger.error("Error loading offer candidates: {}", e.getMessage(), e);
            model.addAttribute("error", "Failed to load offer candidates");
            model.addAttribute("offerCount", 0);
            model.addAttribute("isEmployee", false); // Add here too
            addHRAttributes(model, session);
            return "offer/offer-candidates";
        }
    }



    @GetMapping("/generate-offer/{id}")
    public String hrGenerateOffer(@PathVariable("id") String id,
                                  @RequestParam(value = "type", defaultValue = "APPLICATION") String type,
                                  Model model, HttpSession session, RedirectAttributes ra) {
        logger.info("=== HR GENERATE OFFER PAGE REQUESTED ===");

        if (!isAuthorized(session, ra)) {
            return "redirect:/candidate/login";
        }

        try {
            OfferRequest offerRequest = new OfferRequest();
            offerRequest.setType(type);

            // CRITICAL FIX: Fetch and populate employee data for EMPLOYEE type
            if ("EMPLOYEE".equalsIgnoreCase(type)) {
                Optional<Employee> employeeOpt = employeeService.getEmployeeByEmployeeId(id);

                if (employeeOpt.isPresent()) {
                    Employee employee = employeeOpt.get();

                    // FIX: Clean the name - ensure no duplicates
                    String fullName = employee.getFirstName() + " " + employee.getLastName();
                    // Remove any potential duplicates (like "Vamsi V,Vamsi V")
                    if (fullName.contains(",")) {
                        fullName = fullName.split(",")[0].trim();
                    }

                    // FIX: Clean the email - ensure no duplicates
                    String email = employee.getEmail() != null ? employee.getEmail() : employee.getPersonalEmail();
                    if (email != null && email.contains(",")) {
                        email = email.split(",")[0].trim();
                    }

                    // Populate OfferRequest with cleaned employee data
                    offerRequest.setEmployeeId(id);
                    offerRequest.setCandidateName(fullName);
                    offerRequest.setCandidateEmail(email);
                    offerRequest.setDesignation(employee.getDesignation() != null ? employee.getDesignation() : "Not Specified");
                    offerRequest.setDepartment(employee.getDepartment() != null ? employee.getDepartment() : "Not Specified");

                    logger.info("✅ Employee data loaded for offer generation - Name: {}, Email: {}",
                            fullName, email);
                } else {
                    logger.warn("⚠️ Employee not found with ID: {}", id);
                    ra.addFlashAttribute("error", "Employee not found with ID: " + id);
                    return "redirect:/dashboard/hr/employees";
                }
            }
            // Handle APPLICATION type
            else {
                try {
                    Long applicationId = Long.parseLong(id);
                    JobApplication app = applicationService.getApplicationById(applicationId);

                    if (app != null) {
                        offerRequest.setApplicationId(applicationId);

                        // FIX: Clean the name
                        String fullName = app.getFirstName() + " " + app.getLastName();
                        if (fullName.contains(",")) {
                            fullName = fullName.split(",")[0].trim();
                        }

                        // FIX: Clean the email
                        String email = app.getEmail();
                        if (email != null && email.contains(",")) {
                            email = email.split(",")[0].trim();
                        }

                        offerRequest.setCandidateName(fullName);
                        offerRequest.setCandidateEmail(email);
                        offerRequest.setDesignation(app.getJobTitle() != null ? app.getJobTitle() : "Software Engineer");
                        offerRequest.setDepartment(app.getJobDepartment() != null ? app.getJobDepartment() : "IT");
                    }
                } catch (NumberFormatException e) {
                    logger.error("Invalid application ID format: {}", id);
                }
            }

            model.addAttribute("isEmployee", "EMPLOYEE".equalsIgnoreCase(type));
            model.addAttribute("offerRequest", offerRequest);
            model.addAttribute("offerCount", offerService.getOfferCount());
            addHRAttributes(model, session);

            return "offer/create-offer";

        } catch (Exception e) {
            logger.error("Error loading offer form: {}", e.getMessage(), e);
            ra.addFlashAttribute("error", "Failed to load offer form: " + e.getMessage());
            return "redirect:/dashboard/hr/offer-candidates";
        }
    }



    /* ========== SAVE OFFER LETTER ========== */
    @PostMapping("/save-offer")
    public String saveOffer(@ModelAttribute OfferRequest offerRequest,
                            @RequestParam(value = "signatureFile", required = false) MultipartFile signatureFile,
                            RedirectAttributes ra) {
        logger.info("=== SAVING OFFER LETTER ===");

        try {
            // Set signature file if provided
            if (signatureFile != null && !signatureFile.isEmpty()) {
                offerRequest.setSignatureFile(signatureFile);
            }

            // Create the offer using the service
            OfferLetter offerLetter = offerService.createOffer(offerRequest);

            logger.info("✅ Offer letter created successfully with ID: {}", offerLetter.getId());

            // Set all flash attributes needed by the popup
            ra.addFlashAttribute("showSuccessPopup", true);
            ra.addFlashAttribute("offerId", offerLetter.getId());

            // IMPORTANT: Also pass employeeId if available
            if (offerLetter.getEmployeeId() != null) {
                ra.addFlashAttribute("employeeId", offerLetter.getEmployeeId());
            }

            // Clean the name before sending to popup
            String displayName = offerLetter.getCandidateName();
            if (displayName != null && displayName.contains(",")) {
                displayName = displayName.substring(0, displayName.indexOf(",")).trim();
            }
            ra.addFlashAttribute("candidateName", displayName);
            ra.addFlashAttribute("success", offerLetter.getOfferType() + " letter generated successfully!");

            // Redirect to offer candidates page
            return "redirect:/dashboard/hr/offer-candidates";

        } catch (Exception e) {
            logger.error("❌ Error saving offer: {}", e.getMessage(), e);
            ra.addFlashAttribute("error", "Failed to generate offer: " + e.getMessage());
            return "redirect:/dashboard/hr/generate-offer/" +
                    (offerRequest.getEmployeeId() != null ? offerRequest.getEmployeeId() : offerRequest.getApplicationId()) +
                    "?type=" + offerRequest.getType();
        }
    }
    /**
     * EDIT EMPLOYEE - Load employee data for editing
     */
    @GetMapping("/edit-employee/{employeeId}")
    public String hrEditEmployee(@PathVariable String employeeId,
                                 Model model,
                                 HttpSession session,
                                 RedirectAttributes ra) {
        logger.info("=== HR EDIT EMPLOYEE PAGE REQUESTED: {} ===", employeeId);

        // Check authorization
        if (!isAuthorized(session, ra)) {
            return "redirect:/candidate/login";
        }

        try {
            // Fetch the employee to edit
            Optional<Employee> employeeOpt = employeeService.getEmployeeByEmployeeId(employeeId);

            if (employeeOpt.isEmpty()) {
                logger.error("Employee not found with ID: {}", employeeId);
                ra.addFlashAttribute("error", "Employee not found with ID: " + employeeId);
                return "redirect:/dashboard/hr/employees";
            }

            Employee employee = employeeOpt.get();

            // Add employee data to model for pre-filling the form
            model.addAttribute("employee", employee);
            model.addAttribute("isEditMode", true);

            // Prefill data for the form
            model.addAttribute("prefillFirstName", employee.getFirstName());
            model.addAttribute("prefillLastName", employee.getLastName());
            model.addAttribute("prefillDepartment", employee.getDepartment());
            model.addAttribute("prefillDesignation", employee.getDesignation());
            model.addAttribute("prefillEmploymentType", employee.getEmploymentType());
            model.addAttribute("prefillWorkLocation", employee.getWorkLocation());
            model.addAttribute("prefillWorkType", employee.getWorkType());
            model.addAttribute("prefillManager", employee.getManager());

            // Add other needed attributes
            model.addAttribute("isSuperAdmin", "SUPER_ADMIN".equals(session.getAttribute("userRole")));
            model.addAttribute("offerCount", offerService.getOfferCount());
            addHRAttributes(model, session);

            return "addemployee"; // Reuse the same template

        } catch (Exception e) {
            logger.error("Error loading employee for edit: {}", e.getMessage(), e);
            ra.addFlashAttribute("error", "Failed to load employee data: " + e.getMessage());
            return "redirect:/dashboard/hr/employees";
        }
    }

    /**
     * UPDATE EMPLOYEE - Save edited employee data
     */
    @PostMapping("/update-employee")
    public String updateEmployee(@ModelAttribute EmployeeFormDTO employeeDTO,
                                 @RequestParam(value = "profilePicture", required = false) MultipartFile profilePicture,
                                 @RequestParam(value = "benefits", required = false) List<String> benefits,
                                 @RequestParam("employeeId") String originalEmployeeId,
                                 HttpSession session,
                                 RedirectAttributes ra) {

        logger.info("=== UPDATING EMPLOYEE: {} {} (ID: {}) ===",
                employeeDTO.getFirstName(), employeeDTO.getLastName(), originalEmployeeId);

        try {
            String role = (String) session.getAttribute("userRole");
            if (!"HR".equals(role) && !"SUPER_ADMIN".equals(role)) {
                ra.addFlashAttribute("error", "Unauthorized access");
                return "redirect:/candidate/login";
            }

            // Find existing employee
            Optional<Employee> existingEmployeeOpt = employeeService.getEmployeeByEmployeeId(originalEmployeeId);
            if (existingEmployeeOpt.isEmpty()) {
                ra.addFlashAttribute("error", "Employee not found: " + originalEmployeeId);
                return "redirect:/dashboard/hr/employees";
            }

            Employee employee = existingEmployeeOpt.get();

            // Update basic fields from DTO
            employee.setFirstName(employeeDTO.getFirstName());
            employee.setLastName(employeeDTO.getLastName());
            employee.setPersonalEmail(employeeDTO.getPersonalEmail());
            employee.setPhone(employeeDTO.getPhone());
            employee.setDateOfBirth(employeeDTO.getDateOfBirth());
            employee.setGender(employeeDTO.getGender());

            // Update job info (preserve original employeeId if needed)
            if (employeeDTO.getEmployeeId() != null && !employeeDTO.getEmployeeId().equals(originalEmployeeId)) {
                // If employee ID changed, check if new ID is available
                if (!employeeService.employeeExists(employeeDTO.getEmployeeId())) {
                    employee.setEmployeeId(employeeDTO.getEmployeeId());
                } else {
                    logger.warn("New employee ID {} already exists, keeping original", employeeDTO.getEmployeeId());
                }
            }

            employee.setDepartment(employeeDTO.getDepartment());
            employee.setDesignation(employeeDTO.getDesignation());
            employee.setWorkLocation(employeeDTO.getWorkLocation());
            employee.setEmploymentType(employeeDTO.getEmploymentType());
            employee.setWorkType(employeeDTO.getWorkType());
            employee.setStartDate(employeeDTO.getStartDate());
            employee.setManager(employeeDTO.getManager());

            // Update compensation
            employee.setSalary(employeeDTO.getSalary());
            employee.setPayFrequency(employeeDTO.getPayFrequency());
            employee.setCurrency(employeeDTO.getCurrency());

            // Update address info
            employee.setPermanentAddress(employeeDTO.getPermanentAddress());
            employee.setPermanentCity(employeeDTO.getPermanentCity());
            employee.setPermanentState(employeeDTO.getPermanentState());
            employee.setPermanentZipCode(employeeDTO.getPermanentZipCode());
            employee.setPermanentCountry(employeeDTO.getPermanentCountry());
            employee.setAddressType(employeeDTO.getAddressType());

            // Update residential address if different
            if ("different".equals(employeeDTO.getAddressType())) {
                employee.setResidentialAddress(employeeDTO.getResidentialAddress());
                employee.setResidentialCity(employeeDTO.getResidentialCity());
                employee.setResidentialState(employeeDTO.getResidentialState());
                employee.setResidentialZipCode(employeeDTO.getResidentialZipCode());
                employee.setResidentialCountry(employeeDTO.getResidentialCountry());
            }

            // Update contact info
            employee.setEmergencyContact(employeeDTO.getEmergencyContact());
            employee.setLinkedin(employeeDTO.getLinkedin());

            // Handle profile picture if new one uploaded
            if (profilePicture != null && !profilePicture.isEmpty()) {
                String fileName = System.currentTimeMillis() + "_" + profilePicture.getOriginalFilename();
                String uploadDir = "C:/hrms/uploads/employee-profiles/";

                File dir = new File(uploadDir);
                if (!dir.exists()) {
                    dir.mkdirs();
                }

                File file = new File(dir.getAbsolutePath() + File.separator + fileName);
                profilePicture.transferTo(file);
                employee.setProfilePicture("employee-profiles/" + fileName);
            }

            // Update benefits
            if (benefits != null && !benefits.isEmpty()) {
                employee.setBenefits(benefits);
            }

            // Update computed fields
            employee.setEmployeeName(employeeDTO.getFirstName() + " " + employeeDTO.getLastName());
            employee.setUpdatedDate(LocalDateTime.now());

            // Save updated employee
            Employee savedEmployee = employeeService.updateEmployee(employee);

            // Success message
            ra.addFlashAttribute("showSuccessPopup", true);
            ra.addFlashAttribute("employeeId", savedEmployee.getEmployeeId());
            ra.addFlashAttribute("employeeName", savedEmployee.getEmployeeName());
            ra.addFlashAttribute("successMessage", "Employee updated successfully!");
            ra.addFlashAttribute("isUpdateMode", true);

            logger.info("✅ Employee updated successfully: {} (ID: {})",
                    savedEmployee.getEmployeeName(), savedEmployee.getEmployeeId());

            return "redirect:/dashboard/hr/employees";

        } catch (Exception e) {
            logger.error("❌ Error updating employee: {}", e.getMessage(), e);
            ra.addFlashAttribute("error", "Failed to update employee: " + e.getMessage());
            return "redirect:/dashboard/hr/edit-employee/" + originalEmployeeId;
        }
    }

    @GetMapping("/start-onboarding/{offerId}")
    public String startOnboardingFromOffer(@PathVariable Long offerId,
                                           RedirectAttributes ra,
                                           HttpSession session) {
        logger.info("=== START ONBOARDING FROM OFFER ID: {} ===", offerId);

        if (!isAuthorized(session, ra)) {
            return "redirect:/candidate/login";
        }

        try {
            Optional<OfferLetter> offerOpt = offerService.getOfferById(offerId);
            if (offerOpt.isEmpty()) {
                ra.addFlashAttribute("error", "Offer not found");
                return "redirect:/dashboard/hr/offer-candidates";
            }

            OfferLetter offer = offerOpt.get();
            String employeeId = offer.getEmployeeId();

            // If no employeeId in offer, try to find by email or create
            if (employeeId == null || employeeId.trim().isEmpty()) {
                logger.info("No employeeId in offer, attempting to find or create employee...");

                // Try to find existing employee by email
                List<Employee> employees = employeeService.getAllEmployees();
                for (Employee emp : employees) {
                    String empEmail = emp.getEmail() != null ? emp.getEmail() : emp.getPersonalEmail();
                    if (empEmail != null && offer.getCandidateEmail() != null &&
                            empEmail.equalsIgnoreCase(offer.getCandidateEmail())) {
                        employeeId = emp.getEmployeeId();
                        logger.info("Found existing employee by email: {}", employeeId);
                        break;
                    }
                }

                // If still not found, create new employee
                if (employeeId == null) {
                    employeeId = createEmployeeFromOffer(offer);
                    logger.info("Created new employee: {}", employeeId);
                }

                // Update offer with employeeId - SAVE IT
                offer.setEmployeeId(employeeId);
                offerService.saveOfferLetter(offer); // UNCOMMENTED - Make sure this method exists
            }

            // Get the employee
            Optional<Employee> employeeOpt = employeeService.getEmployeeByEmployeeId(employeeId);
            if (employeeOpt.isEmpty()) {
                ra.addFlashAttribute("error", "Employee not found for this offer");
                return "redirect:/dashboard/hr/offer-candidates";
            }

            Employee employee = employeeOpt.get();

            // Update onboarding status
            if ("NOT_STARTED".equals(employee.getOnboardingStatus()) || employee.getOnboardingStatus() == null) {
                employee.setOnboardingStatus("DOCUMENTS_PENDING");
                employeeService.saveEmployee(employee);
            }

            // Redirect to the employee onboarding dashboard
            return "redirect:/dashboard/hr/onboarding/employee/" + employeeId;

        } catch (Exception e) {
            logger.error("Error starting onboarding for offer {}: {}", offerId, e.getMessage(), e);
            ra.addFlashAttribute("error", "Failed to start onboarding: " + e.getMessage());
            return "redirect:/dashboard/hr/offer-candidates";
        }
    }

    // Helper method to create employee from offer
    // In HrDashboardController.java, modify createEmployeeFromOffer method:

    private String createEmployeeFromOffer(OfferLetter offer) {
        // Use the service's method to generate consistent ID
        Employee employee = new Employee();

        // Let the service generate the ID when saving
        // Don't set employeeId here - it will be generated by service

        // Split name into first and last
        String fullName = offer.getCandidateName();
        if (fullName != null && fullName.contains(" ")) {
            employee.setFirstName(fullName.substring(0, fullName.lastIndexOf(" ")).trim());
            employee.setLastName(fullName.substring(fullName.lastIndexOf(" ") + 1).trim());
        } else {
            employee.setFirstName(fullName);
            employee.setLastName("");
        }

        employee.setEmail(offer.getCandidateEmail());
        employee.setPersonalEmail(offer.getCandidateEmail());
        employee.setDepartment(offer.getDepartment());
        employee.setDesignation(offer.getDesignation());

        // Parse joining date
        try {
            if (offer.getJoiningDate() != null && !offer.getJoiningDate().isEmpty()) {
                employee.setStartDate(LocalDate.parse(offer.getJoiningDate()));
            } else {
                employee.setStartDate(LocalDate.now().plusDays(14));
            }
        } catch (Exception e) {
            employee.setStartDate(LocalDate.now().plusDays(14));
        }

        employee.setStatus("Active");
        employee.setOnboardingStatus("NOT_STARTED");
        employee.setRole(UserRole.EMPLOYEE);
        employee.setEmployeeName(offer.getCandidateName());

        // Save employee - ID will be generated by service
        Employee savedEmployee = employeeService.saveEmployee(employee);
        return savedEmployee.getEmployeeId();
    }

    private String generateEmployeeId() {
        long count = employeeService.getAllEmployees().size() + 1;
        return String.format("EMP%03d", count);
    }
    /* ========== DOWNLOAD OFFER PDF ========== */
    @GetMapping("/download-offer/{offerId}")
    public ResponseEntity<Resource> downloadOfferPdf(@PathVariable Long offerId,
                                                     @RequestParam(value = "download", required = false, defaultValue = "false") boolean download) {
        logger.info("=== {} OFFER PDF FOR ID: {} ===", download ? "DOWNLOADING" : "VIEWING", offerId);

        try {
            Optional<OfferLetter> offerOpt = offerService.getOfferById(offerId);
            if (offerOpt.isEmpty()) {
                return ResponseEntity.notFound().build();
            }

            OfferLetter offer = offerOpt.get();
            if (offer.getOfferFilePath() == null || offer.getOfferFilePath().isEmpty()) {
                return ResponseEntity.badRequest().build();
            }

            Path pdfPath = Paths.get(basePath).resolve(offer.getOfferFilePath()).normalize();
            Resource resource = new UrlResource(pdfPath.toUri());

            if (!resource.exists()) {
                return ResponseEntity.notFound().build();
            }

            String fileName = String.format("Offer_%s_%s.pdf",
                    offer.getCandidateName().replaceAll("[^a-zA-Z0-9.-]", "_"),
                    offer.getId());

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_PDF);

            // Set disposition based on download parameter
            if (download) {
                headers.setContentDisposition(ContentDisposition.attachment()
                        .filename(fileName, StandardCharsets.UTF_8)
                        .build());
            } else {
                headers.setContentDisposition(ContentDisposition.inline()
                        .filename(fileName, StandardCharsets.UTF_8)
                        .build());
            }

            return ResponseEntity.ok()
                    .headers(headers)
                    .contentLength(resource.contentLength())
                    .body(resource);

        } catch (Exception e) {
            logger.error("Error downloading offer PDF: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /* ========== EMPLOYEE MANAGEMENT ========== */
    @GetMapping("/add-employee")
    public String hrAddEmployee(
            @RequestParam(value = "firstName", required = false) String firstName,
            @RequestParam(value = "lastName", required = false) String lastName,
            @RequestParam(value = "department", required = false) String department,
            @RequestParam(value = "designation", required = false) String designation,
            @RequestParam(value = "employmentType", required = false) String employmentType,
            Model model,
            HttpSession session,
            RedirectAttributes ra) {

        logger.info("=== HR ADD EMPLOYEE PAGE REQUESTED ===");
        logger.info("Prefill data - FirstName: {}, LastName: {}, Dept: {}, Desig: {}, Type: {}",
                firstName, lastName, department, designation, employmentType);

        if (!isAuthorized(session, ra)) {
            return "redirect:/candidate/login";
        }

        // Add prefill data to model (will be null if not provided)
        model.addAttribute("prefillFirstName", firstName);
        model.addAttribute("prefillLastName", lastName);
        model.addAttribute("prefillDepartment", department);
        model.addAttribute("prefillDesignation", designation);
        model.addAttribute("prefillEmploymentType", employmentType);

        model.addAttribute("isSuperAdmin", "SUPER_ADMIN".equals(session.getAttribute("userRole")));
        model.addAttribute("offerCount", offerService.getOfferCount());
        addHRAttributes(model, session);
        return "addemployee";
    }

    @GetMapping("/employees")
    public String hrEmployees(Model model, HttpSession session, RedirectAttributes ra) {
        logger.info("=== HR EMPLOYEES LIST PAGE REQUESTED ===");

        if (!isAuthorized(session, ra)) {
            return "redirect:/candidate/login";
        }

        try {
            // Use the sorted method instead
            List<Employee> employees = employeeService.getAllEmployeesSortedByNewest();

            logger.info("Total employees fetched: {}", employees != null ? employees.size() : "null");

            model.addAttribute("employees", employees);
            model.addAttribute("totalEmployees", employees != null ? employees.size() : 0);
            model.addAttribute("activeEmployees", employees != null ?
                    employees.stream().filter(e -> "Active".equals(e.getStatus())).count() : 0);
            model.addAttribute("offerCount", offerService.getOfferCount());
            addHRAttributes(model, session);
            return "employees";
        } catch (Exception e) {
            logger.error("Error loading employees: {}", e.getMessage(), e);
            model.addAttribute("error", "Failed to load employees: " + e.getMessage());
            addHRAttributes(model, session);
            return "employees";
        }
    }
    /* ========== POST JOB WITH SUCCESS POPUP ========== */
    @PostMapping("/post-job")
    public String postJobWithPopup(@RequestParam("title") String title,
                                   @RequestParam("department") String department,
                                   @RequestParam("jobType") String jobType,
                                   @RequestParam("location") String location,
                                   @RequestParam("experienceLevel") String experienceLevel,
                                   @RequestParam(value = "salaryRange", required = false) String salaryRange,
                                   @RequestParam("description") String description,
                                   @RequestParam("requirements") String requirements,
                                   @RequestParam(value = "responsibilities", required = false) String responsibilities,
                                   @RequestParam(value = "applicationInstructions", required = false) String applicationInstructions,
                                   @RequestParam(value = "applicationDeadline", required = false) String applicationDeadlineStr,
                                   @RequestParam(value = "jobId", required = false) Long jobId,
                                   RedirectAttributes ra,
                                   HttpSession session) {
        logger.info("=== POSTING JOB WITH POPUP: {} ===", title);

        // Check authorization
        if (!isAuthorized(session, ra)) {
            return "redirect:/candidate/login";
        }

        try {
            Job job = (jobId != null) ? jobService.getJobById(jobId) : new Job();
            if (job == null) job = new Job();

            job.setTitle(title);
            job.setDepartment(department);
            job.setJobType(jobType);
            job.setLocation(location);
            job.setExperienceLevel(experienceLevel);
            job.setSalaryRange(salaryRange);
            job.setDescription(description);
            job.setRequirements(requirements);
            job.setResponsibilitiesFromString(responsibilities);
            job.setApplicationInstructions(applicationInstructions);
            job.setActive(true);

            if (jobId == null) {
                job.setPostedDate(java.time.LocalDate.now());
            }

            // Parse deadline if provided
            if (applicationDeadlineStr != null && !applicationDeadlineStr.isEmpty()) {
                job.setApplicationDeadline(java.time.LocalDate.parse(applicationDeadlineStr));
            }

            Job savedJob = jobService.saveJob(job);

            // Add flash attributes for success popup
            ra.addFlashAttribute("showSuccessPopup", true);
            ra.addFlashAttribute("jobTitle", savedJob.getTitle());
            ra.addFlashAttribute("jobId", savedJob.getId());
            ra.addFlashAttribute("success", "Job posted successfully!");

            logger.info("✅ Job posted successfully with ID: {}", savedJob.getId());

            return "redirect:/dashboard/hr/job-form";

        } catch (Exception e) {
            logger.error("Error posting job: {}", e.getMessage(), e);
            ra.addFlashAttribute("error", "Failed to post job: " + e.getMessage());
            return "redirect:/dashboard/hr/job-form";
        }
    }
    /* ========== ONBOARDING ========== */
    /* ========== ONBOARDING ========== */
    @GetMapping("/onboarding")
    public String hrOnboarding(Model model, HttpSession session, RedirectAttributes ra) {
        logger.info("=== HR ONBOARDING DASHBOARD REQUESTED ===");

        if (!isAuthorized(session, ra)) {
            return "redirect:/candidate/login";
        }

        try {
            List<Employee> allEmployees = employeeService.getAllEmployees();

            // Filter employees that need onboarding (NOT_STARTED, DOCUMENTS_PENDING, DOCUMENTS_SUBMITTED)
            List<Employee> employeesForOnboarding = allEmployees.stream()
                    .filter(emp -> {
                        String status = emp.getOnboardingStatus();
                        return status != null && (
                                status.equals("NOT_STARTED") ||
                                        status.equals("DOCUMENTS_PENDING") ||
                                        status.equals("DOCUMENTS_SUBMITTED") ||
                                        status.equals("IN_PROGRESS")
                        );
                    })
                    .collect(Collectors.toList());

            int totalPending = employeesForOnboarding.size();
            int totalInProgress = (int) allEmployees.stream()
                    .filter(emp -> {
                        String status = emp.getOnboardingStatus();
                        return status != null && status.equals("IN_PROGRESS");
                    })
                    .count();

            // Add all required model attributes that the template expects
            model.addAttribute("employees", employeesForOnboarding);
            model.addAttribute("allOnboardingEmployees", allEmployees.stream()
                    .filter(emp -> {
                        String status = emp.getOnboardingStatus();
                        return status != null && !status.equals("COMPLETED") && !status.equals("CANCELLED");
                    })
                    .collect(Collectors.toList()));
            model.addAttribute("totalPending", totalPending);
            model.addAttribute("totalInProgress", totalInProgress);

            // Add sidebar attributes
            addHRAttributes(model, session);

            logger.info("✅ Onboarding dashboard loaded: {} pending, {} in progress",
                    totalPending, totalInProgress);

            // Return the correct template path - onboarding/dashboard.html
            return "onboarding/dashboard";

        } catch (Exception e) {
            logger.error("Error loading onboarding data: {}", e.getMessage(), e);
            model.addAttribute("error", "Failed to load onboarding data: " + e.getMessage());
            model.addAttribute("employees", new ArrayList<>());
            model.addAttribute("totalPending", 0);
            model.addAttribute("totalInProgress", 0);
            addHRAttributes(model, session);
            return "onboarding/dashboard";
        }
    }

    /* ========== RESUME PREVIEW AND DOWNLOAD ENDPOINTS ========== */

    @GetMapping("/preview-resume/{applicationId}")
    public ResponseEntity<Resource> hrPreviewResume(@PathVariable("applicationId") Long applicationId) {
        logger.info("=== PREVIEW RESUME FOR APPLICATION {} ===", applicationId);

        try {
            JobApplication application = applicationService.getApplicationById(applicationId);
            if (application == null) {
                return ResponseEntity.notFound().build();
            }

            String resumePath = application.getResumePath();
            if (resumePath == null || resumePath.trim().isEmpty()) {
                return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
            }

            Path filePath = Paths.get("C:/hrms/uploads").resolve(resumePath).normalize();
            Resource resource = new UrlResource(filePath.toUri());

            if (!resource.exists()) {
                return ResponseEntity.notFound().build();
            }

            String contentType = determineContentType(resumePath);
            String fileName = extractResumeFileName(resumePath);

            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType(contentType))
                    .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + fileName + "\"")
                    .body(resource);

        } catch (Exception e) {
            logger.error("Error previewing resume: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

// ============ PUBLIC ACCESS RESUME ENDPOINTS (No Auth Required) ============

    /**
     * Public endpoint for viewing resumes - no authentication required
     * This allows the PDF iframe to load even when session might expire
     */
    @GetMapping("/public/preview-resume/{applicationId}")
    public ResponseEntity<Resource> publicPreviewResume(@PathVariable("applicationId") Long applicationId) {
        logger.info("=== PUBLIC PREVIEW RESUME FOR APPLICATION {} ===", applicationId);

        try {
            JobApplication application = applicationService.getApplicationById(applicationId);
            if (application == null) {
                return ResponseEntity.notFound().build();
            }

            String resumePath = application.getResumePath();
            if (resumePath == null || resumePath.trim().isEmpty()) {
                return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
            }

            Path filePath = Paths.get("C:/hrms/uploads").resolve(resumePath).normalize();
            Resource resource = new UrlResource(filePath.toUri());

            if (!resource.exists()) {
                return ResponseEntity.notFound().build();
            }

            String contentType = determineContentType(resumePath);
            String fileName = extractResumeFileName(resumePath);

            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType(contentType))
                    .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + fileName + "\"")
                    .body(resource);

        } catch (Exception e) {
            logger.error("Error public preview resume: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/preview-image/{applicationId}")
    public ResponseEntity<Resource> hrPreviewImage(@PathVariable("applicationId") Long applicationId) {
        logger.info("=== PREVIEW IMAGE FOR APPLICATION {} ===", applicationId);

        try {
            JobApplication application = applicationService.getApplicationById(applicationId);
            if (application == null) {
                return ResponseEntity.notFound().build();
            }

            String resumePath = application.getResumePath();
            if (resumePath == null || resumePath.trim().isEmpty()) {
                return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
            }

            Path filePath = Paths.get("C:/hrms/uploads").resolve(resumePath).normalize();
            Resource resource = new UrlResource(filePath.toUri());

            if (!resource.exists()) {
                return ResponseEntity.notFound().build();
            }

            String contentType = determineContentType(resumePath);
            String fileName = extractResumeFileName(resumePath);

            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType(contentType))
                    .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + fileName + "\"")
                    .body(resource);

        } catch (Exception e) {
            logger.error("Error previewing image: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/interview-schedule")
    public String hrInterviewSchedulePage(Model model, HttpSession session, RedirectAttributes ra) {
        logger.info("=== HR INTERVIEW SCHEDULE PAGE REQUESTED ===");

        if (!isAuthorized(session, ra)) {
            return "redirect:/candidate/login";
        }

        try {
            // Get ALL applications with "Applied" or "In Review" status for dropdown
            List<JobApplication> availableApplications = new ArrayList<>();

            // Get applications with status "Applied"
            List<JobApplication> appliedApps = applicationService.getApplicationsByStatus("Applied");
            if (appliedApps != null) availableApplications.addAll(appliedApps);

            // Get applications with status "In Review"
            List<JobApplication> reviewApps = applicationService.getApplicationsByStatus("In Review");
            if (reviewApps != null) availableApplications.addAll(reviewApps);

            // If no applications with those status, get all applications
            if (availableApplications.isEmpty()) {
                availableApplications = applicationService.getAllApplications();
            }

            // Sort by application date (newest first)
            availableApplications.sort((a, b) -> {
                if (a.getApplicationDate() == null && b.getApplicationDate() == null) return 0;
                if (a.getApplicationDate() == null) return 1;
                if (b.getApplicationDate() == null) return -1;
                return b.getApplicationDate().compareTo(a.getApplicationDate());
            });

            logger.info("Found {} available applications for interview scheduling", availableApplications.size());

            model.addAttribute("applications", availableApplications);  // <-- List for dropdown
            model.addAttribute("offerCount", offerService.getOfferCount());
            addHRAttributes(model, session);

            // IMPORTANT: Do NOT add "selectedApplication" here - it should be null/absent
            return "interview-schedule";

        } catch (Exception e) {
            logger.error("Error loading interview schedule: {}", e.getMessage(), e);
            ra.addFlashAttribute("error", "Failed to load interview schedule page");
            return "redirect:/dashboard/hr";
        }
    }
    @GetMapping("/api/application/{id}")
    @ResponseBody
    public ResponseEntity<?> getApplicationDetails(@PathVariable("id") Long applicationId) {
        logger.info("=== API: GET APPLICATION DETAILS FOR ID: {} ===", applicationId);

        try {
            JobApplication app = applicationService.getApplicationById(applicationId);
            if (app == null) {
                return ResponseEntity.notFound().build();
            }

            Map<String, Object> appData = new HashMap<>();
            appData.put("id", app.getId());
            appData.put("firstName", app.getFirstName());
            appData.put("lastName", app.getLastName());
            appData.put("email", app.getEmail());
            appData.put("phone", app.getPhone());
            appData.put("status", app.getStatus());
            appData.put("jobTitle", app.getJobTitle() != null ? app.getJobTitle() :
                    (app.getJob() != null ? app.getJob().getTitle() : "N/A"));

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "application", appData
            ));

        } catch (Exception e) {
            logger.error("Error fetching application: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("success", false, "error", e.getMessage()));
        }
    }
    @PostMapping("/save-interview")
    @ResponseBody
    public ResponseEntity<?> saveInterview(@RequestBody Map<String, Object> interviewData, HttpSession session) {
        logger.info("=== SAVING INTERVIEW SCHEDULE ===");
        logger.debug("Received interview data: {}", interviewData);

        try {
            // Check authorization
            if (!isAuthorized(session, null)) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of("success", false, "error", "Unauthorized"));
            }

            // Extract data from request
            Long applicationId = null;
            try {
                String appIdStr = (String) interviewData.get("applicationId");
                if (appIdStr != null && !appIdStr.trim().isEmpty()) {
                    applicationId = Long.parseLong(appIdStr);
                }
            } catch (Exception e) {
                logger.error("Invalid application ID: {}", interviewData.get("applicationId"));
            }

            String interviewType = (String) interviewData.get("interviewType");
            String interviewDate = (String) interviewData.get("interviewDate");
            String interviewTime = (String) interviewData.get("interviewTime");
            String duration = (String) interviewData.get("duration");
            String interviewRound = (String) interviewData.get("interviewRound");
            String priority = (String) interviewData.get("priority");
            String meetingLink = (String) interviewData.get("meetingLink");
            String location = (String) interviewData.get("location");
            String notes = (String) interviewData.get("notes");
            String interviewerEmails = (String) interviewData.get("interviewerEmails");
            String interviewerIds = (String) interviewData.get("interviewerIds");

            logger.info("Processing interview for application ID: {}", applicationId);

            // VALIDATION: Check if we have a valid application ID
            if (applicationId == null) {
                return ResponseEntity.badRequest()
                        .body(Map.of("success", false, "error", "Application ID is required"));
            }

            // Get the application
            JobApplication application = applicationService.getApplicationById(applicationId);
            if (application == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Map.of("success", false, "error", "Application not found with ID: " + applicationId));
            }

            // TODO: Create Interview entity and save to database
            // For now, just update the application status to "Interview"

            application.setStatus("Interview");
            applicationService.saveApplicationWithoutFile(application);

            logger.info("✅ Interview scheduled successfully for application ID: {}", applicationId);
            logger.info("   Type: {}, Date: {}, Time: {}", interviewType, interviewDate, interviewTime);
            logger.info("   Round: {}, Interviewers: {}", interviewRound, interviewerEmails);

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Interview scheduled successfully",
                    "applicationId", applicationId
            ));

        } catch (Exception e) {
            logger.error("Error saving interview: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("success", false, "error", e.getMessage()));
        }
    }

    // ===== ANNOUNCEMENTS - SUPER_ADMIN ONLY =====
    @GetMapping("/announcements")
    public String hrAnnouncementsPage(Model model, HttpSession session, RedirectAttributes ra) {
        logger.info("=== HR ANNOUNCEMENTS PAGE REQUESTED ===");

        if (!isAuthorized(session, ra)) {
            return "redirect:/candidate/login";
        }

        // CHECK: Only SUPER_ADMIN can see this page
        String role = (String) session.getAttribute("userRole");
        if (!"SUPER_ADMIN".equals(role)) {
            logger.warn("❌ Access denied to announcements page for role: {}", role);
            ra.addFlashAttribute("error", "Only Super Admin can create announcements");
            return "redirect:/dashboard/hr";
        }

        try {
            model.addAttribute("isSuperAdmin", true);
            model.addAttribute("offerCount", offerService.getOfferCount());
            addHRAttributes(model, session);

            logger.info("✅ Announcements page loaded for SUPER_ADMIN");
            return "announcements"; // Thymeleaf template

        } catch (Exception e) {
            logger.error("Error loading announcements page: {}", e.getMessage(), e);
            ra.addFlashAttribute("error", "Failed to load announcements page");
            return "redirect:/dashboard/hr";
        }
    }
    /* ========== SAVE EMPLOYEE ========== */
    @PostMapping("/save-employee")
    public String saveEmployee(@ModelAttribute EmployeeFormDTO employeeDTO,
                               @RequestParam(value = "profilePicture", required = false) MultipartFile profilePicture,
                               @RequestParam(value = "benefits", required = false) List<String> benefits,
                               HttpSession session,
                               RedirectAttributes ra) {

        logger.info("=== SAVING EMPLOYEE: {} {} ===", employeeDTO.getFirstName(), employeeDTO.getLastName());

        try {
            String role = (String) session.getAttribute("userRole");
            if (!"HR".equals(role) && !"SUPER_ADMIN".equals(role)) {
                ra.addFlashAttribute("error", "Unauthorized access");
                return "redirect:/candidate/login";
            }

            // Create new Employee entity
            Employee employee = new Employee();

            // Map basic fields from DTO
            employee.setFirstName(employeeDTO.getFirstName());
            employee.setLastName(employeeDTO.getLastName());
            employee.setPersonalEmail(employeeDTO.getPersonalEmail());
            employee.setPhone(employeeDTO.getPhone());
            employee.setDateOfBirth(employeeDTO.getDateOfBirth());
            employee.setGender(employeeDTO.getGender());

            // Map job info
            employee.setEmployeeId(employeeDTO.getEmployeeId());
            employee.setDepartment(employeeDTO.getDepartment());
            employee.setDesignation(employeeDTO.getDesignation());
            employee.setWorkLocation(employeeDTO.getWorkLocation());
            employee.setEmploymentType(employeeDTO.getEmploymentType());
            employee.setWorkType(employeeDTO.getWorkType());
            employee.setStartDate(employeeDTO.getStartDate());
            employee.setManager(employeeDTO.getManager());

            // Map compensation
            employee.setSalary(employeeDTO.getSalary());
            employee.setPayFrequency(employeeDTO.getPayFrequency());
            employee.setCurrency(employeeDTO.getCurrency());

            // Map address info
            employee.setPermanentAddress(employeeDTO.getPermanentAddress());
            employee.setPermanentCity(employeeDTO.getPermanentCity());
            employee.setPermanentState(employeeDTO.getPermanentState());
            employee.setPermanentZipCode(employeeDTO.getPermanentZipCode());
            employee.setPermanentCountry(employeeDTO.getPermanentCountry());

            // Map address type
            employee.setAddressType(employeeDTO.getAddressType());

            // Map residential address if different
            if ("different".equals(employeeDTO.getAddressType())) {
                employee.setResidentialAddress(employeeDTO.getResidentialAddress());
                employee.setResidentialCity(employeeDTO.getResidentialCity());
                employee.setResidentialState(employeeDTO.getResidentialState());
                employee.setResidentialZipCode(employeeDTO.getResidentialZipCode());
                employee.setResidentialCountry(employeeDTO.getResidentialCountry());
            } else {
                // Copy permanent to residential
                employee.setResidentialAddress(employeeDTO.getPermanentAddress());
                employee.setResidentialCity(employeeDTO.getPermanentCity());
                employee.setResidentialState(employeeDTO.getPermanentState());
                employee.setResidentialZipCode(employeeDTO.getPermanentZipCode());
                employee.setResidentialCountry(employeeDTO.getPermanentCountry());
            }

            // Map contact info
            employee.setEmergencyContact(employeeDTO.getEmergencyContact());
            employee.setLinkedin(employeeDTO.getLinkedin());

            // Handle profile picture
            if (profilePicture != null && !profilePicture.isEmpty()) {
                String fileName = System.currentTimeMillis() + "_" + profilePicture.getOriginalFilename();
                String uploadDir = "C:/hrms/uploads/employee-profiles/";

                File dir = new File(uploadDir);
                if (!dir.exists()) {
                    dir.mkdirs();
                }

                File file = new File(dir.getAbsolutePath() + File.separator + fileName);
                profilePicture.transferTo(file);
                employee.setProfilePicture("employee-profiles/" + fileName);
            }

            // Map benefits
            if (benefits != null && !benefits.isEmpty()) {
                employee.setBenefits(benefits);
            }

            // Set computed fields
            employee.setEmployeeName(employeeDTO.getFirstName() + " " + employeeDTO.getLastName());

            // Set email (use personal if work email not provided)
            if (employeeDTO.getPersonalEmail() != null && !employeeDTO.getPersonalEmail().isEmpty()) {
                employee.setEmail(employeeDTO.getPersonalEmail());
            }

            // Set default values
            employee.setOnboardingStatus("NOT_STARTED");
            employee.setCredentialsCreated(false);
            employee.setActive(true);
            employee.setStatus("Active");
            employee.setRole(UserRole.EMPLOYEE);
            employee.setPresenceStatus("OFFLINE");

            // Save employee
            Employee savedEmployee = employeeService.saveEmployee(employee);

            // ✅ NEW: Flash attributes for success popup with navigation options
            ra.addFlashAttribute("showSuccessPopup", true);
            ra.addFlashAttribute("employeeId", savedEmployee.getEmployeeId());
            ra.addFlashAttribute("employeeName", savedEmployee.getEmployeeName());
            ra.addFlashAttribute("successMessage", "Employee added successfully!");
            // Navigation options flags
            ra.addFlashAttribute("showNavigationOptions", true);

            logger.info("✅ Employee saved successfully: {} (ID: {})",
                    savedEmployee.getEmployeeName(), savedEmployee.getEmployeeId());

            return "redirect:/dashboard/hr/add-employee";

        } catch (Exception e) {
            logger.error("❌ Error saving employee: {}", e.getMessage(), e);
            ra.addFlashAttribute("error", "Failed to save employee: " + e.getMessage());
            return "redirect:/dashboard/hr/add-employee";
        }
    }
    /* ---------- ONBOARDING EMPLOYEE DASHBOARD (HR URL) ---------- */
    @GetMapping("/onboarding/employee/{employeeId}")
    public String hrOnboardingEmployee(@PathVariable String employeeId,
                                       Model model,
                                       HttpSession session,
                                       RedirectAttributes ra) {
        logger.info("=== HR ONBOARDING EMPLOYEE PAGE: {} ===", employeeId);

        if (!isAuthorized(session, ra)) {
            return "redirect:/candidate/login";
        }

        try {
            Optional<Employee> employeeOpt = employeeService.getEmployeeByEmployeeId(employeeId);

            if (employeeOpt.isEmpty()) {
                logger.error("Employee not found: {}", employeeId);
                ra.addFlashAttribute("error", "Employee not found: " + employeeId);
                return "redirect:/dashboard/hr/employees";
            }

            Employee employee = employeeOpt.get();

            // Initialize documents if not already done
            List<OnboardingDocument> documents = documentService.getDocumentsByEmployee(employee);
            if (documents.isEmpty()) {
                documentService.initializeDocumentChecklist(employee);
                documents = documentService.getDocumentsByEmployee(employee);
            }

            // Calculate progress
            Map<String, Object> progress = calculateOnboardingProgress(employee);

            model.addAttribute("employee", employee);
            model.addAttribute("documents", documents);
            model.addAttribute("progress", progress);
            model.addAttribute("totalPending", 0);
            model.addAttribute("offerCount", offerService.getOfferCount());

            addHRAttributes(model, session);

            logger.info("✅ Loaded {} documents for employee {}", documents.size(), employeeId);
            return "onboarding/employee-dashboard";

        } catch (Exception e) {
            logger.error("Error loading onboarding for {}: {}", employeeId, e.getMessage(), e);
            ra.addFlashAttribute("error", "Failed to load onboarding: " + e.getMessage());
            return "redirect:/dashboard/hr/employees";
        }
    }

    /* ---------- HELPER METHOD FOR PROGRESS CALCULATION ---------- */
    private Map<String, Object> calculateOnboardingProgress(Employee employee) {
        Map<String, Object> progress = new HashMap<>();
        List<OnboardingDocument> documents = documentService.getDocumentsByEmployee(employee);

        int total = documents.size();
        int submitted = (int) documents.stream()
                .filter(doc -> "SUBMITTED".equals(doc.getStatus()) || "VERIFIED".equals(doc.getStatus()))
                .count();
        int verified = (int) documents.stream()
                .filter(doc -> "VERIFIED".equals(doc.getStatus()))
                .count();

        progress.put("totalDocuments", total);
        progress.put("submittedDocuments", submitted);
        progress.put("verifiedDocuments", verified);
        progress.put("submissionProgress", total > 0 ? (submitted * 100) / total : 0);
        progress.put("verificationProgress", total > 0 ? (verified * 100) / total : 0);

        return progress;
    }
    /* ========== HELPER METHODS ========== */
    private boolean isAuthorized(HttpSession session, RedirectAttributes ra) {
        String userId = (String) session.getAttribute("userId");
        String role = (String) session.getAttribute("userRole");

        if (userId == null) {
            logger.warn("❌ Auth check failed: userId not in session");
            ra.addFlashAttribute("error", "Please login to access HR Dashboard");
            return false;
        }

        if (!"HR".equals(role) && !"SUPER_ADMIN".equals(role)) {
            logger.warn("❌ Auth check failed: User {} has invalid role: {}", userId, role);
            ra.addFlashAttribute("error", "Access denied: HR or Super Admin only");
            return false;
        }

        logger.debug("✅ Auth check passed for user: {} (Role: {})", userId, role);
        return true;
    }

    private void addHRAttributes(Model model, HttpSession session) {
        String role = (String) session.getAttribute("userRole");
        String userName = (String) session.getAttribute("userName");

        model.addAttribute("isHR", true);
        model.addAttribute("isSuperAdmin", "SUPER_ADMIN".equals(role));
        model.addAttribute("userRole", role);
        model.addAttribute("userName", userName);
        model.addAttribute("offerCount", offerService.getOfferCount());

        try {
            List<Employee> allEmployees = employeeService.getAllEmployees();
            int totalPending = allEmployees.stream()
                    .filter(emp -> {
                        String status = emp.getOnboardingStatus();
                        return status != null && (status.equals("NOT_STARTED") ||
                                status.equals("DOCUMENTS_PENDING") ||
                                status.equals("DOCUMENTS_SUBMITTED"));
                    })
                    .collect(Collectors.toList()).size();
            model.addAttribute("totalPending", totalPending);
        } catch (Exception e) {
            model.addAttribute("totalPending", 0);
        }
    }

    @GetMapping("/download-resume/{applicationId}")
    public ResponseEntity<Resource> hrDownloadResume(@PathVariable("applicationId") Long applicationId) {
        logger.info("=== DOWNLOAD RESUME FOR APPLICATION {} ===", applicationId);

        try {
            JobApplication application = applicationService.getApplicationById(applicationId);
            if (application == null) {
                logger.warn("Application not found with ID: {}", applicationId);
                return ResponseEntity.notFound().build();
            }

            String resumePath = application.getResumePath();
            logger.info("Resume path from DB: {}", resumePath);

            if (resumePath == null || resumePath.trim().isEmpty()) {
                logger.warn("No resume path found for application ID: {}", applicationId);
                return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
            }

            Path filePath = Paths.get("C:/hrms/uploads").resolve(resumePath).normalize();
            logger.info("Full file path: {}", filePath.toString());

            Resource resource = new UrlResource(filePath.toUri());
            logger.info("Resource exists: {}", resource.exists());

            if (!resource.exists()) {
                logger.error("File does not exist at path: {}", filePath.toString());
                return ResponseEntity.notFound().build();
            }

            String fileName = extractResumeFileName(resumePath);
            logger.info("Downloading file: {}", fileName);

            return ResponseEntity.ok()
                    .contentType(MediaType.APPLICATION_OCTET_STREAM)
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + fileName + "\"")
                    .body(resource);

        } catch (Exception e) {
            logger.error("Error downloading resume: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /* ========== API FOR LIVE STAT CARDS ========== */
    @GetMapping("/api/employee-stats")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> getLiveEmployeeStats(HttpSession session) {
        logger.info("=== API: FETCHING LIVE EMPLOYEE STATS ===");
        Map<String, Object> response = new HashMap<>();

        try {
            // Check if user is authorized (optional but good practice)
            if (session.getAttribute("userId") == null) {
                response.put("success", false);
                response.put("totalEmployees", 0);
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
            }

            // Get total employees using your existing service
            List<Employee> allEmployees = employeeService.getAllEmployees();
            int totalEmployees = (allEmployees != null) ? allEmployees.size() : 0;

            response.put("success", true);
            response.put("totalEmployees", totalEmployees);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            logger.error("Error fetching live employee stats: {}", e.getMessage());
            response.put("success", false);
            response.put("totalEmployees", 0);
            return ResponseEntity.ok(response); // Return 0 gracefully instead of crashing
        }
    }
    /* ========== API FOR LIVE RECRUITMENT METRICS (CHART & FUNNEL) ========== */
    @GetMapping("/api/recruitment-stats")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> getLiveRecruitmentStats(HttpSession session) {
        logger.info("=== API: FETCHING LIVE RECRUITMENT STATS ===");
        Map<String, Object> response = new HashMap<>();

        try {
            // 1. Fetch all applications
            List<JobApplication> allApps = applicationService.getAllApplications();

            // 2. Calculate Pipeline (Funnel) Stats
            long totalApplications = allApps.size();
            long screenings = allApps.stream().filter(a -> "In Review".equalsIgnoreCase(a.getStatus())).count();
            long interviews = allApps.stream().filter(a -> "Interview".equalsIgnoreCase(a.getStatus()) || "Interviewed".equalsIgnoreCase(a.getStatus())).count();
            long finalRound = allApps.stream().filter(a -> "Selected".equalsIgnoreCase(a.getStatus()) || "On Hold".equalsIgnoreCase(a.getStatus())).count();

            // For Offers, we can use actual generated offers count, or "Hired" status
            long offers = offerService.getOfferCount();

            Map<String, Long> pipeline = new HashMap<>();
            pipeline.put("applications", totalApplications);
            pipeline.put("screenings", screenings);
            pipeline.put("interviews", interviews);
            pipeline.put("finalRound", finalRound);
            pipeline.put("offers", offers);

            // 3. Calculate Monthly Chart Stats (Last 6 Months)
            List<String> labels = new ArrayList<>();
            List<Long> monthlyApplications = new ArrayList<>();
            List<Long> monthlyHired = new ArrayList<>();

            LocalDate now = LocalDate.now();
            java.time.format.DateTimeFormatter monthFormatter = java.time.format.DateTimeFormatter.ofPattern("MMM");

            for (int i = 5; i >= 0; i--) {
                LocalDate targetMonth = now.minusMonths(i);
                labels.add(targetMonth.format(monthFormatter));

                // Count apps for this specific month/year
                long monthApps = allApps.stream()
                        .filter(a -> a.getApplicationDate() != null &&
                                a.getApplicationDate().getMonth() == targetMonth.getMonth() &&
                                a.getApplicationDate().getYear() == targetMonth.getYear())
                        .count();

                // Count hired for this specific month/year
                long monthHired = allApps.stream()
                        .filter(a -> "Hired".equalsIgnoreCase(a.getStatus()) &&
                                a.getApplicationDate() != null &&
                                a.getApplicationDate().getMonth() == targetMonth.getMonth() &&
                                a.getApplicationDate().getYear() == targetMonth.getYear())
                        .count();

                monthlyApplications.add(monthApps);
                monthlyHired.add(monthHired);
            }

            Map<String, Object> chartData = new HashMap<>();
            chartData.put("labels", labels);
            chartData.put("applications", monthlyApplications);
            chartData.put("hired", monthlyHired);

            response.put("success", true);
            response.put("pipeline", pipeline);
            response.put("chart", chartData);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            logger.error("Error fetching live recruitment stats: {}", e.getMessage());
            response.put("success", false);
            return ResponseEntity.ok(response);
        }
    }
    /* ========== FILE HELPER METHODS ========== */
    private String extractResumeFileName(String resumePath) {
        if (resumePath == null || resumePath.trim().isEmpty()) return "document.pdf";
        int lastSlash = Math.max(resumePath.lastIndexOf('/'), resumePath.lastIndexOf('\\'));
        return lastSlash > 0 ? resumePath.substring(lastSlash + 1) : resumePath;
    }

    private String determineContentType(String fileName) {
        if (fileName == null) return "application/octet-stream";
        String lower = fileName.toLowerCase();
        if (lower.endsWith(".pdf")) return "application/pdf";
        if (lower.endsWith(".doc")) return "application/msword";
        if (lower.endsWith(".docx")) return "application/vnd.openxmlformats-officedocument.wordprocessingml.document";
        if (lower.endsWith(".jpg") || lower.endsWith(".jpeg")) return "image/jpeg";
        if (lower.endsWith(".png")) return "image/png";
        if (lower.endsWith(".txt")) return "text/plain";
        return "application/octet-stream";
    }

    private String formatFileSize(long size) {
        if (size < 0) return "0 B";
        if (size < 1024) return size + " B";
        int exp = (int) (Math.log(size) / Math.log(1024));
        String pre = "KMGTPE".charAt(exp-1) + "";
        return String.format("%.1f %sB", size / Math.pow(1024, exp), pre);
    }
}