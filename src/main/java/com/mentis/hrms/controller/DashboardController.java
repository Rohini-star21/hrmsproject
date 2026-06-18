package com.mentis.hrms.controller;

import com.mentis.hrms.dto.OfferRequest;
import com.mentis.hrms.model.Employee;
import com.mentis.hrms.model.Job;
import com.mentis.hrms.model.JobApplication;
import com.mentis.hrms.model.OfferLetter;
import com.mentis.hrms.service.*;
import jakarta.servlet.http.HttpSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.*;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.bind.annotation.ResponseBody;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;
import com.mentis.hrms.model.Department;
import com.mentis.hrms.model.Designation;
@Controller
@RequestMapping("/dashboard")
public class DashboardController implements WebMvcConfigurer {
    private static final Logger logger = LoggerFactory.getLogger(DashboardController.class);

    @Autowired
    private DashboardService dashboardService;

    @Autowired
    private JobService jobService;

    @Autowired
    private DepartmentService departmentService;

    @Autowired
    private JobApplicationService applicationService;

    @Autowired
    private GoogleCalendarService googleCalendarService;

    @Autowired
    private OfferService offerService;

    @Autowired
    private DocumentService documentService;

    @Autowired
    private EmployeeService employeeService;

    @Value("${app.upload.base-path:C:/hrms/uploads}")
    private String basePath;

    /* ---------- RESOURCE HANDLERS ---------- */
    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/uploads/**")
                .addResourceLocations("file:///C:/hrms/uploads/")
                .setCachePeriod(3600);
        registry.addResourceHandler("/webjars/**")
                .addResourceLocations("classpath:/META-INF/resources/webjars/");
        registry.addResourceHandler("/static/**")
                .addResourceLocations("classpath:/static/");
        registry.addResourceHandler("/css/**")
                .addResourceLocations("classpath:/static/css/");
        registry.addResourceHandler("/js/**")
                .addResourceLocations("classpath:/static/js/");
        registry.addResourceHandler("/images/**")
                .addResourceLocations("classpath:/static/images/");
    }

    /* ---------- HR ATTRIBUTE PROVIDER (UPDATED FOR SESSION ISOLATION) ---------- */
    @ModelAttribute
    public void addHRAttributes(HttpSession session, Model model) {
        // Check login type first
        String loginType = (String) session.getAttribute("loginType");

        if (!"HR_ADMIN".equals(loginType)) {
            // Not an HR/Admin session, don't add HR attributes
            return;
        }

        String role = (String) session.getAttribute("userRole");
        String userName = (String) session.getAttribute("userName");

        if ("HR".equals(role) || "SUPER_ADMIN".equals(role)) {
            model.addAttribute("isHR", true);
            model.addAttribute("isSuperAdmin", "SUPER_ADMIN".equals(role));
            model.addAttribute("userRole", role);
            model.addAttribute("userName", userName);
            model.addAttribute("offerCount", offerService.getOfferCount());

            // Add onboarding counts for sidebar badge
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
                logger.warn("Could not load onboarding count: {}", e.getMessage());
                model.addAttribute("totalPending", 0);
            }

            logger.debug("✅ HR attributes added to model for user: {} (Role: {})", userName, role);
        }
    }

    /* ---------- MAIN DASHBOARD ---------- */
    @GetMapping
    public String showDashboard(Model model, HttpSession session, RedirectAttributes ra) {
        logger.info("=== LOADING DASHBOARD ===");

        if (session.getAttribute("userId") == null) {
            ra.addFlashAttribute("error", "Session expired. Please login again.");
            return "redirect:/candidate/login";
        }

        String loginType = (String) session.getAttribute("loginType");
        String role = (String) session.getAttribute("userRole");
        String empId = (String) session.getAttribute("userId");

        // Redirect employees to their dashboard
        if (loginType == null || "EMPLOYEE".equals(loginType)) {
            return "redirect:/candidate/dashboard/" + empId;
        }

        // Only HR and Super Admin should reach here
        if (!"HR".equals(role) && !"SUPER_ADMIN".equals(role)) {
            return "redirect:/candidate/dashboard/" + empId;
        }

        // Redirect to HR dashboard
        return "redirect:/dashboard/hr";
    }

    /* ========== APPLICATION MANAGEMENT ========== */
    @GetMapping("/applications")
    public String showApplications(@RequestParam(value = "status", required = false) String status,
                                   Model model, HttpSession session) {
        logger.info("=== LOADING APPLICATIONS PAGE (Status: {}) ===", status);

        String loginType = (String) session.getAttribute("loginType");
        if (!"HR_ADMIN".equals(loginType)) {
            logger.warn("Non-HR/Admin trying to access dashboard: {}", loginType);
            String empId = (String) session.getAttribute("empId");
            if (empId != null) {
                return "redirect:/candidate/dashboard/" + empId;
            }
            return "redirect:/candidate/login?error=Access+denied";
        }

        try {
            List<JobApplication> applications = (status != null && !status.isEmpty() && !status.equals("all"))
                    ? applicationService.getApplicationsByStatus(status)
                    : applicationService.getApplicationsWithJobs();

            model.addAttribute("applications", applications);
            model.addAttribute("selectedStatus", status != null ? status : "all");
            model.addAttribute("totalApplications", applicationService.getTotalApplicationsCount());
            model.addAttribute("interviewScheduled", applicationService.getApplicationsCountByStatus("Interview"));
            model.addAttribute("hiredCount", applicationService.getApplicationsCountByStatus("Hired"));
            model.addAttribute("newApplications", applicationService.getApplicationsCountByStatus("Applied"));
            model.addAttribute("appliedCount", applicationService.getApplicationsCountByStatus("Applied"));
            model.addAttribute("inReviewCount", applicationService.getApplicationsCountByStatus("In Review"));
            model.addAttribute("interviewCount", applicationService.getApplicationsCountByStatus("Interview"));
            model.addAttribute("interviewedCount", applicationService.getApplicationsCountByStatus("Interviewed"));
            model.addAttribute("onHoldCount", applicationService.getApplicationsCountByStatus("On Hold"));
            model.addAttribute("rejectedCount", applicationService.getApplicationsCountByStatus("Rejected"));
            model.addAttribute("offerCount", offerService.getOfferCount());

            logger.info("✅ Applications page loaded successfully for HR user");
            return "applications";
        } catch (Exception e) {
            logger.error("Error loading applications: {}", e.getMessage(), e);
            model.addAttribute("error", "Failed to load applications");
            model.addAttribute("offerCount", 0);
            return "applications";
        }
    }

    @GetMapping("/viewApplication/{id}")
    public String viewApplicationDetails(@PathVariable("id") Long applicationId, Model model, HttpSession session) {
        logger.info("=== VIEW APPLICATION DETAILS FOR ID: {} ===", applicationId);

        String loginType = (String) session.getAttribute("loginType");
        if (!"HR_ADMIN".equals(loginType)) {
            return "redirect:/candidate/login?error=Access+denied";
        }

        try {
            JobApplication app = applicationService.getApplicationById(applicationId);
            if (app == null) {
                return "redirect:/dashboard?error=Application+not+found";
            }

            // ============ ADD ALL REQUIRED MODEL ATTRIBUTES ============
            model.addAttribute("application", app);
            model.addAttribute("applicationId", applicationId);
            model.addAttribute("jobTitle", app.getJobTitle() != null ? app.getJobTitle() : "Position Not Specified");
            model.addAttribute("jobDepartment", app.getJobDepartment() != null ? app.getJobDepartment() : "Not specified");
            model.addAttribute("jobType", app.getJobType() != null ? app.getJobType() : "Not specified");
            model.addAttribute("jobLocation", app.getJobLocation() != null ? app.getJobLocation() : "Not specified");
            model.addAttribute("formattedApplicationDate", app.getFormattedApplicationDate());
            model.addAttribute("email", app.getEmail() != null ? app.getEmail() : "");
            model.addAttribute("phone", app.getPhone() != null ? app.getPhone() : "");
            model.addAttribute("linkedinProfile", app.getLinkedinProfile());
            model.addAttribute("experience", app.getExperience() != null ? app.getExperience() : "Not specified");
            model.addAttribute("coverLetter", app.getCoverLetter());

            // ============ RESUME ATTRIBUTES ============
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
                        Path fileNamePath = Paths.get(app.getResumePath());
                        resumeFileName = fileNamePath.getFileName().toString();
                        File file = filePath.toFile();
                        resumeFileSize = formatFileSize(file.length());

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
                    logger.warn("Could not check resume file: {}", e.getMessage());
                }
            }

            model.addAttribute("hasResume", hasResume);
            model.addAttribute("resumeExists", resumeExists);
            model.addAttribute("resumeFileName", resumeFileName != null ? resumeFileName : "No resume");
            model.addAttribute("resumeFileSize", resumeFileSize != null ? resumeFileSize : "0 B");
            model.addAttribute("resumeFileType", resumeFileType != null ? resumeFileType : "UNKNOWN");

            model.addAttribute("offerCount", offerService.getOfferCount());
            return "application-details";

        } catch (Exception e) {
            logger.error("Error loading application {}: {}", applicationId, e.getMessage(), e);
            return "redirect:/dashboard?error=Failed+to+load+application+details";
        }
    }

    @PostMapping("/update-status")
    public String updateApplicationStatus(@RequestParam("applicationId") Long applicationId,
                                          @RequestParam("newStatus") String newStatus,
                                          @RequestParam(value = "statusNotes", required = false) String statusNotes,
                                          RedirectAttributes ra) {
        logger.info("=== UPDATING STATUS FOR APPLICATION {} TO {} ===", applicationId, newStatus);
        try {
            JobApplication application = applicationService.getApplicationById(applicationId);
            if (application != null) {
                application.setStatus(newStatus);
                applicationService.saveApplicationWithoutFile(application);
                ra.addFlashAttribute("success", "Status updated to: " + newStatus);
            }
            return "redirect:/dashboard/viewApplication/" + applicationId;
        } catch (Exception e) {
            logger.error("Error updating status: {}", e.getMessage(), e);
            ra.addFlashAttribute("error", "Failed to update status");
            return "redirect:/dashboard/viewApplication/" + applicationId;
        }
    }

    /* ========== INTERVIEW SCHEDULING ========== */
    @GetMapping("/interview-schedule/{id}")
    public String showInterviewSchedule(@PathVariable("id") Long applicationId, Model model, HttpSession session) {
        logger.info("=== LOADING INTERVIEW SCHEDULE PAGE FOR APPLICATION {} ===", applicationId);

        String loginType = (String) session.getAttribute("loginType");
        if (!"HR_ADMIN".equals(loginType)) {
            return "redirect:/candidate/login?error=Access+denied";
        }

        try {
            JobApplication app = applicationService.getApplicationById(applicationId);
            if (app == null) {
                return "redirect:/dashboard/applications?error=Application+not+found";
            }
            model.addAttribute("application", app);
            model.addAttribute("offerCount", offerService.getOfferCount());
            return "interview-schedule";
        } catch (Exception e) {
            logger.error("Error loading interview schedule: {}", e.getMessage(), e);
            return "redirect:/dashboard/applications?error=Failed+to+load+interview+schedule";
        }
    }

    @PostMapping("/save-interview")
    public String saveInterviewSchedule(@RequestParam("applicationId") Long applicationId,
                                        @RequestParam("interviewType") String interviewType,
                                        @RequestParam("interviewDate") String interviewDate,
                                        @RequestParam("interviewTime") String interviewTime,
                                        @RequestParam("duration") String duration,
                                        @RequestParam("interviewRound") String interviewRound,
                                        @RequestParam("priority") String priority,
                                        @RequestParam(value = "meetingLink", required = false) String meetingLink,
                                        @RequestParam(value = "location", required = false) String location,
                                        @RequestParam(value = "notes", required = false) String notes,
                                        @RequestParam(value = "interviewers", required = false) List<Long> interviewerIds,
                                        RedirectAttributes ra) {
        logger.info("=== SAVING INTERVIEW SCHEDULE FOR APPLICATION {} ===", applicationId);
        try {
            JobApplication application = applicationService.getApplicationById(applicationId);
            if (application != null) {
                application.setStatus("Interview");
                applicationService.saveApplicationWithoutFile(application);
                ra.addFlashAttribute("success", "Interview scheduled successfully!");
            }
            return "redirect:/dashboard/viewApplication/" + applicationId;
        } catch (Exception e) {
            logger.error("Error scheduling interview: {}", e.getMessage(), e);
            ra.addFlashAttribute("error", "Failed to schedule interview");
            return "redirect:/dashboard/interview-schedule/" + applicationId;
        }
    }

    /* ========== JOB MANAGEMENT ========== */
    @GetMapping("/job-form")
    public String showJobForm(Model model, HttpSession session, @RequestParam(value = "id", required = false) Long jobId) {
        logger.info("=== LOADING JOB FORM ===");

        String loginType = (String) session.getAttribute("loginType");
        if (!"HR_ADMIN".equals(loginType)) {
            logger.warn("🚨 Unauthorized access attempt to job form by login type: {}", loginType);
            return "redirect:/candidate/login?error=Access+denied";
        }

        try {
            model.addAttribute("departments", departmentService.getAllDepartments());
            model.addAttribute("offerCount", offerService.getOfferCount());
            if (jobId != null) {
                Job job = jobService.getJobById(jobId);
                if (job != null) model.addAttribute("job", job);
            }
            logger.info("✅ Job form loaded successfully for HR");
            return "job-form";
        } catch (Exception e) {
            logger.error("Error loading job form: {}", e.getMessage(), e);
            model.addAttribute("error", "Failed to load job form");
            model.addAttribute("offerCount", 0);
            return "job-form";
        }
    }

    @PostMapping("/post-job")
    public String postJob(@RequestParam("title") String title,
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
                          RedirectAttributes ra) {
        logger.info("=== POSTING JOB: {} ===", title);
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

            return "redirect:/dashboard/job-form";
        } catch (Exception e) {
            logger.error("Error posting job: {}", e.getMessage(), e);
            ra.addFlashAttribute("error", "Failed to post job: " + e.getMessage());
            return "redirect:/dashboard/job-form";
        }
    }

    @GetMapping("/delete-job/{id}")
    public String deleteJob(@PathVariable("id") Long jobId, RedirectAttributes ra) {
        logger.info("=== DELETING JOB ID: {} ===", jobId);
        try {
            jobService.deleteJob(jobId);
            ra.addFlashAttribute("success", "Job deleted successfully!");
        } catch (Exception e) {
            logger.error("Error deleting job: {}", e.getMessage(), e);
            ra.addFlashAttribute("error", "Failed to delete job: " + e.getMessage());
        }
        return "redirect:/dashboard";
    }

    @GetMapping("/viewJob/{id}")
    public String viewJob(@PathVariable("id") Long jobId, Model model, HttpSession session) {
        logger.info("=== VIEWING JOB DETAILS ID: {} ===", jobId);

        String loginType = (String) session.getAttribute("loginType");
        if (!"HR_ADMIN".equals(loginType)) {
            return "redirect:/candidate/login?error=Access+denied";
        }

        try {
            Job job = jobService.getJobById(jobId);
            if (job == null) return "redirect:/dashboard?error=Job+not+found";
            model.addAttribute("job", job);
            model.addAttribute("offerCount", offerService.getOfferCount());
            return "job-details";
        } catch (Exception e) {
            logger.error("Error viewing job: {}", e.getMessage(), e);
            return "redirect:/dashboard?error=Failed+to+load+job+details";
        }
    }

    /* ========== OFFER MANAGEMENT ========== */
    @GetMapping("/offer-candidates")
    public String showOfferCandidates(Model model, HttpSession session) {
        logger.info("=== LOADING OFFER CANDIDATES PAGE ===");

        String loginType = (String) session.getAttribute("loginType");
        if (!"HR_ADMIN".equals(loginType)) {
            logger.warn("🚨 Unauthorized access attempt to offer candidates by login type: {}", loginType);
            return "redirect:/candidate/login?error=Access+denied";
        }

        try {
            List<OfferLetter> offerCandidates = offerService.getAllOfferLetters();
            model.addAttribute("offerCandidates", offerCandidates);
            model.addAttribute("totalOffers", offerService.getOfferCount());
            model.addAttribute("sentOffers", offerService.getSentOfferCount());
            model.addAttribute("pendingOffers", offerService.getOfferCount() - offerService.getSentOfferCount());
            model.addAttribute("offerCount", offerService.getOfferCount());
            logger.info("✅ Offer candidates page loaded successfully");
            return "offer/offer-candidates";
        } catch (Exception e) {
            logger.error("Error loading offer candidates: {}", e.getMessage(), e);
            model.addAttribute("error", "Failed to load offer candidates");
            model.addAttribute("offerCount", 0);
            return "offer/offer-candidates";
        }
    }

    @GetMapping("/generate-offer/{id}")
    public String showGenerateOfferForm(@PathVariable("id") String id,
                                        @RequestParam(value = "type", defaultValue = "APPLICATION") String type,
                                        Model model) {
        logger.info("=== LOADING OFFER LETTER FORM FOR {}: {} ===", type, id);
        try {
            OfferRequest offerRequest = new OfferRequest();
            offerRequest.setType(type);

            if ("EMPLOYEE".equalsIgnoreCase(type)) {
                model.addAttribute("isEmployee", true);
            } else {
                model.addAttribute("isEmployee", false);
            }

            model.addAttribute("offerRequest", offerRequest);
            model.addAttribute("offerCount", offerService.getOfferCount());
            return "offer/create-offer";
        } catch (Exception e) {
            logger.error("Error loading offer form: {}", e.getMessage(), e);
            return "redirect:/dashboard/offer-candidates?error=Failed+to+load+form";
        }
    }

    @PostMapping("/save-offer")
    public String saveOffer(@ModelAttribute OfferRequest offerRequest,
                            @RequestParam(value = "signatureFile", required = false) MultipartFile signatureFile,
                            RedirectAttributes ra) {
        logger.info("=== GENERATING OFFER LETTER ===");
        try {
            if (signatureFile != null && !signatureFile.isEmpty()) {
                offerRequest.setSignatureFile(signatureFile);
            }

            OfferLetter offerLetter = offerService.createAndGenerateOffer(offerRequest);
            ra.addFlashAttribute("success", offerLetter.getOfferType() + " letter generated!");
            ra.addFlashAttribute("offerId", offerLetter.getId());
            return "redirect:/dashboard/offer-candidates";
        } catch (Exception e) {
            logger.error("Error generating offer: {}", e.getMessage(), e);
            ra.addFlashAttribute("error", "Failed to generate offer: " + e.getMessage());
            return "redirect:/dashboard/offer-candidates";
        }
    }

    @GetMapping("/download-offer/{offerId}")
    public ResponseEntity<Resource> downloadOfferPdf(@PathVariable Long offerId) {
        logger.info("=== DOWNLOADING OFFER PDF FOR ID: {} ===", offerId);
        try {
            Optional<OfferLetter> offerOpt = offerService.getOfferById(offerId);
            if (offerOpt.isEmpty()) return ResponseEntity.notFound().build();

            OfferLetter offer = offerOpt.get();
            if (offer.getOfferFilePath() == null || offer.getOfferFilePath().isEmpty()) {
                return ResponseEntity.badRequest().build();
            }

            Path pdfPath = Paths.get(basePath).resolve(offer.getOfferFilePath()).normalize();
            Resource resource = new UrlResource(pdfPath.toUri());

            if (!resource.exists()) return ResponseEntity.notFound().build();

            String fileName = String.format("Offer_%s_%s.pdf",
                    offer.getCandidateName().replaceAll("[^a-zA-Z0-9.-]", "_"),
                    offer.getId());

            return ResponseEntity.ok()
                    .contentType(MediaType.APPLICATION_PDF)
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + fileName + "\"")
                    .body(resource);
        } catch (Exception e) {
            logger.error("Error downloading offer PDF: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /* ========== RESUME/DOCUMENT HANDLING ========== */
    @GetMapping("/preview-resume/{applicationId}")
    public ResponseEntity<Resource> previewResume(@PathVariable("applicationId") Long applicationId) {
        try {
            JobApplication application = applicationService.getApplicationById(applicationId);
            if (application == null) return ResponseEntity.notFound().build();

            String resumePath = application.getResumePath();
            if (resumePath == null || resumePath.trim().isEmpty()) {
                return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
            }

            Path filePath = Paths.get("C:/hrms/uploads").resolve(resumePath).normalize();
            Resource resource = new UrlResource(filePath.toUri());

            if (!resource.exists()) return ResponseEntity.notFound().build();

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

    @GetMapping("/download-resume/{applicationId}")
    public ResponseEntity<Resource> downloadResume(@PathVariable("applicationId") Long applicationId) {
        try {
            JobApplication application = applicationService.getApplicationById(applicationId);
            if (application == null) return ResponseEntity.notFound().build();

            String resumePath = application.getResumePath();
            if (resumePath == null || resumePath.trim().isEmpty()) {
                return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
            }

            Path filePath = Paths.get("C:/hrms/uploads").resolve(resumePath).normalize();
            Resource resource = new UrlResource(filePath.toUri());

            if (!resource.exists()) return ResponseEntity.notFound().build();

            String fileName = extractResumeFileName(resumePath);

            return ResponseEntity.ok()
                    .contentType(MediaType.APPLICATION_OCTET_STREAM)
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + fileName + "\"")
                    .body(resource);
        } catch (Exception e) {
            logger.error("Error downloading resume: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    /* ========== DEPARTMENT API ENDPOINTS ========== */

    @GetMapping("/api/departments/all")
    @ResponseBody
    public ResponseEntity<?> getAllDepartmentsApi() {
        try {
            logger.info("Fetching all departments with designations");
            List<Department> departments = departmentService.getAllDepartmentsWithDesignations();

            // Transform to a simpler format for frontend
            List<Map<String, Object>> deptList = departments.stream().map(dept -> {
                Map<String, Object> deptMap = new HashMap<>();
                deptMap.put("id", dept.getId());
                deptMap.put("name", dept.getName());

                // Get designations
                List<Map<String, Object>> desList = dept.getDesignations().stream().map(des -> {
                    Map<String, Object> desMap = new HashMap<>();
                    desMap.put("id", des.getId());
                    desMap.put("name", des.getName());
                    return desMap;
                }).collect(Collectors.toList());

                deptMap.put("designations", desList);
                return deptMap;
            }).collect(Collectors.toList());

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "departments", deptList
            ));
        } catch (Exception e) {
            logger.error("Error fetching departments: {}", e.getMessage(), e);
            return ResponseEntity.ok(Map.of(
                    "success", false,
                    "error", e.getMessage()
            ));
        }
    }

    @PostMapping("/api/departments/create")
    @ResponseBody
    public ResponseEntity<?> createDepartmentApi(@RequestBody Map<String, Object> request) {
        try {
            String departmentName = (String) request.get("departmentName");
            String designationName = (String) request.get("designationName");

            if (departmentName == null || departmentName.trim().isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of(
                        "success", false,
                        "error", "Department name is required"
                ));
            }

            Department existingDept = departmentService.getDepartmentByName(departmentName);
            if (existingDept != null) {
                return ResponseEntity.ok(Map.of(
                        "success", false,
                        "error", "Department already exists"
                ));
            }

            Department department = new Department();
            department.setName(departmentName);

            if (designationName != null && !designationName.trim().isEmpty()) {
                Designation designation = new Designation();
                designation.setName(designationName);
                designation.setDepartment(department);
                department.getDesignations().add(designation);
            }

            Department savedDept = departmentService.saveDepartment(department);

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Department created successfully",
                    "department", savedDept
            ));
        } catch (Exception e) {
            logger.error("Error creating department: {}", e.getMessage(), e);
            return ResponseEntity.ok(Map.of(
                    "success", false,
                    "error", e.getMessage()
            ));
        }
    }
    // ===== EMPLOYEE ANNOUNCEMENTS TAB =====
    @GetMapping("/announcements")
    public String employeeAnnouncementsPage(Model model, HttpSession session, RedirectAttributes ra) {
        logger.info("=== EMPLOYEE ANNOUNCEMENTS PAGE REQUESTED ===");

        if (session.getAttribute("userId") == null) {
            ra.addFlashAttribute("error", "Session expired. Please login again.");
            return "redirect:/candidate/login";
        }

        String loginType = (String) session.getAttribute("loginType");
        String empId = (String) session.getAttribute("userId");

        // Only employees see this
        if (!"EMPLOYEE".equals(loginType)) {
            logger.warn("Non-employee trying to access announcements: {}", loginType);
            return "redirect:/dashboard?error=Access+denied";
        }

        try {
            model.addAttribute("isEmployee", true);
            model.addAttribute("empId", empId);

            logger.info("✅ Announcements page loaded for EMPLOYEE: {}", empId);
            return "candidate/announcements"; // Separate template for employees

        } catch (Exception e) {
            logger.error("Error loading announcements: {}", e.getMessage(), e);
            ra.addFlashAttribute("error", "Failed to load announcements");
            return "redirect:/candidate/dashboard/" + empId;
        }
    }
    @GetMapping("/api/departments/{departmentName}/designations")
    @ResponseBody
    public ResponseEntity<?> getDesignationsByDepartmentApi(@PathVariable String departmentName) {
        try {
            logger.info("Fetching designations for department: {}", departmentName);

            Department department = departmentService.getDepartmentByNameWithDesignations(departmentName);

            if (department == null) {
                return ResponseEntity.ok(Map.of(
                        "success", false,
                        "error", "Department not found: " + departmentName
                ));
            }

            List<Map<String, Object>> designations = department.getDesignations().stream().map(des -> {
                Map<String, Object> desMap = new HashMap<>();
                desMap.put("id", des.getId());
                desMap.put("name", des.getName());
                return desMap;
            }).collect(Collectors.toList());

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "designations", designations
            ));
        } catch (Exception e) {
            logger.error("Error fetching designations: {}", e.getMessage(), e);
            return ResponseEntity.ok(Map.of(
                    "success", false,
                    "error", e.getMessage()
            ));
        }
    }
    /* ========== HELPER METHODS ========== */
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

    @ExceptionHandler(Exception.class)
    public String handleAllExceptions(Exception ex, Model model) {
        logger.error("Global exception handler caught: {}", ex.getMessage(), ex);
        model.addAttribute("error", "An error occurred: " + ex.getMessage());
        return "error";
    }
}