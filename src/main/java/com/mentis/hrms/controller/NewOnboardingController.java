package com.mentis.hrms.controller;

import com.mentis.hrms.model.DocumentChecklist;
import com.mentis.hrms.model.Employee;
import com.mentis.hrms.model.OnboardingDocument;
import com.mentis.hrms.service.DocumentService;
import com.mentis.hrms.service.EmployeeService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.web.servlet.support.RequestContextUtils;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.transaction.annotation.Transactional;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import com.mentis.hrms.service.NotificationService;

@Controller
@RequestMapping("/onboarding")
public class NewOnboardingController {
    private static final Logger logger = LoggerFactory.getLogger(NewOnboardingController.class);

    @Autowired
    private DocumentService documentService;

    @Autowired
    private EmployeeService employeeService;

    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    @Autowired
    private NotificationService notificationService;

    @Value("${app.upload.base-path:C:/hrms/uploads}")
    private String basePath;

    // MAIN ONBOARDING DASHBOARD
    @GetMapping
    public String showOnboardingDashboard(Model model) {
        logger.info("=== LOADING ONBOARDING DASHBOARD ===");
        try {
            List<Employee> allEmployees = employeeService.getAllEmployees();

            List<Employee> employeesForOnboarding = allEmployees.stream()
                    .filter(emp -> {
                        String status = emp.getOnboardingStatus();
                        return status != null &&
                                (status.equals("NOT_STARTED") ||
                                        status.equals("DOCUMENTS_PENDING") ||
                                        status.equals("DOCUMENTS_SUBMITTED") ||
                                        status.equals("IN_PROGRESS"));
                    })
                    .collect(Collectors.toList());

            List<Employee> allOnboardingEmployees = allEmployees.stream()
                    .filter(emp -> {
                        String status = emp.getOnboardingStatus();
                        return status != null &&
                                (!status.equals("COMPLETED") &&
                                        !status.equals("CANCELLED"));
                    })
                    .collect(Collectors.toList());

            model.addAttribute("employees", employeesForOnboarding);
            model.addAttribute("allOnboardingEmployees", allOnboardingEmployees);
            model.addAttribute("totalPending", employeesForOnboarding.size());
            model.addAttribute("totalInProgress", allOnboardingEmployees.size());

            logger.info("✅ Dashboard loaded: {} employees for onboarding", employeesForOnboarding.size());
            return "onboarding/dashboard";

        } catch (Exception e) {
            logger.error("Error loading onboarding dashboard: {}", e.getMessage(), e);
            model.addAttribute("error", "Failed to load onboarding dashboard");
            model.addAttribute("employees", new ArrayList<>());
            model.addAttribute("allOnboardingEmployees", new ArrayList<>());
            model.addAttribute("totalPending", 0);
            model.addAttribute("totalInProgress", 0);
            return "onboarding/dashboard";
        }
    }

    // INDIVIDUAL EMPLOYEE ONBOARDING
    @GetMapping("/employee/{employeeId}")
    public String showEmployeeOnboarding(@PathVariable String employeeId,
                                         Model model,
                                         HttpServletRequest request,
                                         RedirectAttributes ra) {
        logger.info("=== LOADING ONBOARDING FOR EMPLOYEE: {} ===", employeeId);

        Optional<Employee> employeeOpt = employeeService.getEmployeeByEmployeeId(employeeId);

        if (employeeOpt.isEmpty()) {
            logger.error("Employee not found: {}", employeeId);
            ra.addFlashAttribute("error", "Employee not found: " + employeeId);
            return "redirect:/onboarding";
        }

        Employee employee = employeeOpt.get();
        List<OnboardingDocument> documents = documentService.getDocumentsByEmployee(employee);

        // Add required attributes for the sidebar
        model.addAttribute("employee", employee);
        model.addAttribute("documents", documents);
        model.addAttribute("progress", calculateOnboardingProgress(employee));

        // Add these attributes that the sidebar expects
        model.addAttribute("totalPending", 0); // You can calculate this if needed
        model.addAttribute("offerCount", 0);   // You can calculate this if needed

        logger.info("✅ Loaded {} documents for employee {}", documents.size(), employeeId);
        return "onboarding/employee-dashboard";
    }

    /* ---------- GET DOCUMENT DETAILS (API ENDPOINT) ---------- */
    @GetMapping("/document/{documentId}")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> getDocumentDetails(@PathVariable Long documentId) {
        Map<String, Object> response = new HashMap<>();

        try {
            Optional<OnboardingDocument> documentOpt = documentService.getDocumentRepository().findById(documentId);

            if (documentOpt.isEmpty()) {
                response.put("success", false);
                response.put("error", "Document not found: " + documentId);
                return ResponseEntity.badRequest().body(response);
            }

            OnboardingDocument document = documentOpt.get();
            Map<String, Object> docData = new HashMap<>();
            docData.put("id", document.getId());
            docData.put("documentName", document.getDocumentName());
            docData.put("documentType", document.getDocumentType());
            docData.put("status", document.getStatus());
            docData.put("description", document.getDescription());
            docData.put("mandatory", document.isMandatory());
            docData.put("submittedDate", document.getSubmittedDate());
            docData.put("verifiedDate", document.getVerifiedDate());
            docData.put("verifiedBy", document.getVerifiedBy());
            docData.put("verificationNotes", document.getVerificationNotes());
            docData.put("filePath", document.getFilePath());

            response.put("success", true);
            response.put("document", docData);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            logger.error("Error getting document details {}: {}", documentId, e.getMessage());
            response.put("success", false);
            response.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    @GetMapping("/employee/{employeeId}/upload/{documentType}")
    public String showUploadForm(@PathVariable String employeeId,
                                 @PathVariable String documentType,
                                 Model model,
                                 HttpServletRequest request) {
        logger.info("=== SHOW UPLOAD FORM FOR EMPLOYEE: {}, DOCUMENT: {} ===", employeeId, documentType);

        Optional<Employee> employeeOpt = employeeService.getEmployeeByEmployeeId(employeeId);
        if (employeeOpt.isPresent()) {
            Employee employee = employeeOpt.get();
            logger.info("Employee found: {} {}", employee.getFirstName(), employee.getLastName());

            String documentName = getDocumentDisplayName(documentType);

            model.addAttribute("employee", employee);
            model.addAttribute("documentType", documentType);
            model.addAttribute("documentName", documentName);

            Map<String, ?> flashMap = RequestContextUtils.getInputFlashMap(request);
            if (flashMap != null) {
                if (flashMap.containsKey("success")) {
                    model.addAttribute("success", flashMap.get("success"));
                }
                if (flashMap.containsKey("error")) {
                    model.addAttribute("error", flashMap.get("error"));
                }
            }

            return "onboarding/upload-document";
        }

        logger.error("Employee not found: {}", employeeId);
        return "redirect:/onboarding?error=Employee+not+found";
    }

    /* ---------- UPLOAD DOCUMENT (Handles both Form and AJAX) ---------- */
    @PostMapping("/employee/{employeeId}/upload/{documentType}")
    public Object uploadDocument(@PathVariable String employeeId,
                                 @PathVariable String documentType,
                                 @RequestParam("documentFile") MultipartFile file,
                                 @RequestParam(value = "notes", required = false) String notes,
                                 HttpSession session,
                                 RedirectAttributes ra) {

        logger.info("=== UPLOAD DOCUMENT REQUEST ===");
        logger.info("Employee ID: {}, Document Type: {}, File Size: {} bytes",
                employeeId, documentType, file.getSize());

        // Check if this is an AJAX request
        String requestedWith = session.getAttribute("requestedWith") != null ?
                (String) session.getAttribute("requestedWith") : "";
        boolean isAjax = "XMLHttpRequest".equals(requestedWith);

        try {
            if (file == null || file.isEmpty()) {
                logger.warn("No file selected for upload");
                if (isAjax) {
                    return ResponseEntity.badRequest().body(Map.of(
                            "success", false,
                            "error", "Please select a file to upload"
                    ));
                }
                ra.addFlashAttribute("error", "Please select a file to upload");
                return "redirect:/onboarding/employee/" + employeeId + "/upload/" + documentType;
            }

            if (file.getSize() > 5 * 1024 * 1024) {
                logger.warn("File size {} exceeds 5MB limit", file.getSize());
                if (isAjax) {
                    return ResponseEntity.badRequest().body(Map.of(
                            "success", false,
                            "error", "File size exceeds 5MB limit"
                    ));
                }
                ra.addFlashAttribute("error", "File size exceeds 5MB limit. Please select a smaller file.");
                return "redirect:/onboarding/employee/" + employeeId + "/upload/" + documentType;
            }

            String fileName = file.getOriginalFilename();
            if (fileName != null) {
                String fileExtension = fileName.substring(fileName.lastIndexOf(".") + 1).toLowerCase();
                List<String> allowedExtensions = Arrays.asList("pdf", "jpg", "jpeg", "png");

                if (!allowedExtensions.contains(fileExtension)) {
                    logger.warn("Invalid file extension: {}", fileExtension);
                    if (isAjax) {
                        return ResponseEntity.badRequest().body(Map.of(
                                "success", false,
                                "error", "Invalid file format. Only PDF, JPG, JPEG, PNG allowed."
                        ));
                    }
                    ra.addFlashAttribute("error", "Invalid file format. Only PDF, JPG, JPEG, PNG files are allowed.");
                    return "redirect:/onboarding/employee/" + employeeId + "/upload/" + documentType;
                }
            }

            Optional<Employee> employeeOpt = employeeService.getEmployeeByEmployeeId(employeeId);
            if (employeeOpt.isEmpty()) {
                logger.error("Employee not found: {}", employeeId);
                if (isAjax) {
                    return ResponseEntity.badRequest().body(Map.of(
                            "success", false,
                            "error", "Employee not found: " + employeeId
                    ));
                }
                ra.addFlashAttribute("error", "Employee not found: " + employeeId);
                return "redirect:/onboarding";
            }

            String hrUserName = (String) session.getAttribute("hrUserName");
            if (hrUserName == null) {
                logger.warn("HR user not found in session, using default");
                hrUserName = "SYSTEM_HR";
            }

            OnboardingDocument document = documentService.hrUploadDocument(
                    employeeId, documentType, file, notes, hrUserName);

            logger.info("✅ Document uploaded and auto-verified: ID={}, Type={}",
                    document.getId(), documentType);

            // Return AJAX response or redirect
            if (isAjax) {
                return ResponseEntity.ok(Map.of(
                        "success", true,
                        "documentId", document.getId(),
                        "documentName", document.getDocumentName(),
                        "status", document.getStatus(),
                        "message", "Document uploaded and verified by " + hrUserName + "!"
                ));
            }

            ra.addFlashAttribute("success",
                    "Document uploaded and verified by " + hrUserName + "!");
            return "redirect:/onboarding/employee/" + employeeId;

        } catch (Exception e) {
            logger.error("Upload failed: {}", e.getMessage(), e);
            if (isAjax) {
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
                        "success", false,
                        "error", e.getMessage()
                ));
            }
            ra.addFlashAttribute("error", "Upload failed: " + e.getMessage());
            return "redirect:/onboarding/employee/" + employeeId + "/upload/" + documentType;
        }
    }

    /* ---------- DEBUG ENDPOINTS ---------- */
    @GetMapping("/debug/employees")
    @ResponseBody
    public List<Map<String, Object>> debugEmployees() {
        List<Employee> allEmployees = employeeService.getAllEmployees();
        return allEmployees.stream().map(emp -> {
            Map<String, Object> data = new HashMap<>();
            data.put("id", emp.getId());
            data.put("employeeId", emp.getEmployeeId());
            data.put("name", emp.getFirstName() + " " + emp.getLastName());
            data.put("onboardingStatus", emp.getOnboardingStatus());
            data.put("totalDocuments", emp.getTotalDocuments());
            data.put("submittedDocuments", emp.getSubmittedDocuments());
            data.put("verifiedDocuments", emp.getVerifiedDocuments());
            return data;
        }).collect(Collectors.toList());
    }

    @PostMapping("/debug/create-test-employee")
    @ResponseBody
    public String createTestEmployee() {
        try {
            Employee testEmployee = new Employee();
            testEmployee.setEmployeeId("TEST" + System.currentTimeMillis());
            testEmployee.setFirstName("Test");
            testEmployee.setLastName("Employee");
            testEmployee.setDepartment("IT");
            testEmployee.setDesignation("Developer");
            testEmployee.setOnboardingStatus("NOT_STARTED");
            testEmployee.setEmail("test@mentis.com");
            testEmployee.setPersonalEmail("test@mentis.com");

            employeeService.saveEmployee(testEmployee);
            return "Test employee created: " + testEmployee.getEmployeeId();
        } catch (Exception e) {
            return "Error: " + e.getMessage();
        }
    }

    @PostMapping("/debug/set-status/{employeeId}/{status}")
    @ResponseBody
    public String setEmployeeStatus(@PathVariable String employeeId, @PathVariable String status) {
        Optional<Employee> employeeOpt = employeeService.getEmployeeByEmployeeId(employeeId);
        if (employeeOpt.isPresent()) {
            Employee employee = employeeOpt.get();
            employee.setOnboardingStatus(status);
            employeeService.saveEmployee(employee);
            return "Status updated to: " + status;
        }
        return "Employee not found";
    }

    /* ---------- INITIALIZE ONBOARDING (Form-based - existing) ---------- */
    @PostMapping("/employee/{employeeId}/initialize")
    public String initializeOnboarding(@PathVariable String employeeId, RedirectAttributes ra) {
        try {
            Optional<Employee> employeeOpt = employeeService.getEmployeeByEmployeeId(employeeId);
            if (employeeOpt.isPresent()) {
                documentService.initializeDocumentChecklist(employeeOpt.get());
                ra.addFlashAttribute("success", "Onboarding initialized successfully!");
            } else {
                ra.addFlashAttribute("error", "Employee not found");
            }
        } catch (Exception e) {
            logger.error("Error initializing onboarding: {}", e.getMessage());
            ra.addFlashAttribute("error", "Failed to initialize onboarding: " + e.getMessage());
        }
        return "redirect:/onboarding/employee/" + employeeId;
    }

    /* ---------- INITIALIZE ONBOARDING (JSON API - different URL) ---------- */
    @PostMapping("/employee/{employeeId}/initialize-api")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> initializeOnboardingApi(
            @PathVariable String employeeId) {

        Map<String, Object> response = new HashMap<>();

        try {
            Optional<Employee> employeeOpt = employeeService.getEmployeeByEmployeeId(employeeId);

            if (employeeOpt.isEmpty()) {
                response.put("success", false);
                response.put("error", "Employee not found");
                return ResponseEntity.badRequest().body(response);
            }

            Employee employee = employeeOpt.get();

            // Initialize document checklist
            documentService.initializeDocumentChecklist(employee);

            // Update employee status
            employee.setOnboardingStatus("DOCUMENTS_PENDING");
            employeeService.saveEmployee(employee);

            response.put("success", true);
            response.put("message", "Onboarding initialized successfully");
            response.put("employeeId", employeeId);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            logger.error("Error initializing onboarding for {}: {}", employeeId, e.getMessage());
            response.put("success", false);
            response.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    /* ---------- HR UPLOAD DOCUMENT (REAL-TIME) ---------- */
    @PostMapping("/hr/upload/{employeeId}")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> hrUploadDocument(
            @PathVariable String employeeId,
            @RequestParam("documentType") String documentType,
            @RequestParam("documentFile") MultipartFile file,
            @RequestParam(value = "notes", required = false) String notes,
            @RequestParam("uploadedBy") String uploadedBy) {

        Map<String, Object> response = new HashMap<>();

        try {
            OnboardingDocument document = documentService.hrUploadDocument(
                    employeeId, documentType, file, notes, uploadedBy);

            response.put("success", true);
            response.put("message", "Document uploaded and verified by HR!");
            response.put("document", document);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            logger.error("HR upload failed: {}", e.getMessage());
            response.put("success", false);
            response.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    /* ---------- GET ONBOARDING EMPLOYEE LIST (WITH REAL-TIME UPDATES) ---------- */
    @GetMapping("/onboarding-list")
    @ResponseBody
    public ResponseEntity<List<Map<String, Object>>> getOnboardingEmployees() {
        List<Map<String, Object>> response = new ArrayList<>();

        try {
            List<Employee> employees = employeeService.getAllEmployees().stream()
                    .filter(emp -> {
                        String status = emp.getOnboardingStatus();
                        return status != null && !"NOT_STARTED".equals(status);
                    })
                    .collect(Collectors.toList());

            for (Employee emp : employees) {
                Map<String, Object> empData = new HashMap<>();
                empData.put("id", emp.getId());
                empData.put("employeeId", emp.getEmployeeId());
                empData.put("name", emp.getFirstName() + " " + emp.getLastName());
                empData.put("department", emp.getDepartment());
                empData.put("designation", emp.getDesignation());
                empData.put("onboardingStatus", emp.getOnboardingStatus());
                empData.put("totalDocuments", emp.getTotalDocuments());
                empData.put("submittedDocuments", emp.getSubmittedDocuments());
                empData.put("verifiedDocuments", emp.getVerifiedDocuments());
                empData.put("progress", calculateOnboardingProgress(emp));
                empData.put("lastUpdated", LocalDateTime.now().toString());

                response.add(empData);
            }

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            logger.error("Error getting onboarding list: {}", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    /* ---------- DEBUG: TEST UPLOAD ENDPOINT ---------- */
    @GetMapping("/debug/test-upload/{employeeId}")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> testUpload(@PathVariable String employeeId) {
        try {
            Optional<Employee> employeeOpt = employeeService.getEmployeeByEmployeeId(employeeId);

            if (employeeOpt.isPresent()) {
                Employee employee = employeeOpt.get();
                List<OnboardingDocument> documents = documentService.getDocumentsByEmployee(employee);

                Map<String, Object> response = new HashMap<>();
                response.put("success", true);
                response.put("employeeId", employeeId);
                response.put("employeeName", employee.getFirstName() + " " + employee.getLastName());
                response.put("onboardingStatus", employee.getOnboardingStatus());
                response.put("totalDocuments", employee.getTotalDocuments());
                response.put("submittedDocuments", employee.getSubmittedDocuments());
                response.put("verifiedDocuments", employee.getVerifiedDocuments());
                response.put("documentsCount", documents.size());
                response.put("message", "Employee is ready for onboarding");

                logger.info("✅ DEBUG: Employee {} is accessible", employeeId);
                return ResponseEntity.ok(response);
            } else {
                Map<String, Object> response = new HashMap<>();
                response.put("success", false);
                response.put("error", "Employee not found");
                return ResponseEntity.badRequest().body(response);
            }

        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("error", e.getMessage());
            return ResponseEntity.status(500).body(response);
        }
    }

    /* ---------- HELPER METHOD FOR DOCUMENT DISPLAY NAMES ---------- */
    private String getDocumentDisplayName(String documentType) {
        Map<String, String> documentNames = new HashMap<>();
        documentNames.put("PAN_CARD", "PAN Card");
        documentNames.put("AADHAAR_CARD", "Aadhaar Card");
        documentNames.put("PASSPORT", "Passport");
        documentNames.put("DRIVING_LICENSE", "Driving License");
        documentNames.put("VOTER_ID", "Voter ID");
        documentNames.put("OFFER_LETTER", "Signed Offer Letter");
        documentNames.put("APPOINTMENT_LETTER", "Appointment Letter");
        documentNames.put("RELIEVING_LETTER", "Relieving Letter");
        documentNames.put("EXPERIENCE_LETTERS", "Experience Letters");
        documentNames.put("TENTH_MARKSHEET", "10th Marksheet");
        documentNames.put("TWELFTH_MARKSHEET", "12th Marksheet");
        documentNames.put("DEGREE_CERTIFICATE", "Degree Certificate");
        documentNames.put("POST_GRADUATION", "Post Graduation");
        documentNames.put("BANK_ACCOUNT", "Bank Account Details");
        documentNames.put("PAN_CARD_COPY", "PAN Card Copy");
        documentNames.put("PASSPORT_PHOTO", "Passport Photo");
        documentNames.put("SIGNATURE", "Digital Signature");
        documentNames.put("MEDICAL_CERTIFICATE", "Medical Certificate");
        documentNames.put("NDA", "Non-Disclosure Agreement");
        documentNames.put("POLICY_ACKNOWLEDGMENT", "Policy Acknowledgment");

        return documentNames.getOrDefault(documentType, documentType.replace("_", " "));
    }

    /* ---------- VERIFY/REJECT DOCUMENT (FLEXIBLE HR AUTH) ---------- */
    @PostMapping("/verify/{documentId}")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> verifyDocumentWithMessage(
            @PathVariable Long documentId,
            @RequestParam String verifiedBy,
            @RequestParam String status,
            @RequestParam(required = false) String remarks,
            HttpSession session) {

        Map<String, Object> response = new HashMap<>();

        // ===== SESSION BACKUP (Prevents HR context loss) =====
        String backupUserId = (String) session.getAttribute("userId");
        String backupUserRole = (String) session.getAttribute("userRole");
        String backupUserName = (String) session.getAttribute("userName");
        logger.debug("Session backed up before verification - Role: {}", backupUserRole);

        try {
            String hrUserName = (String) session.getAttribute("userName");
            String actualVerifier = (hrUserName != null && !hrUserName.isEmpty())
                    ? hrUserName
                    : "HR_ADMIN";

            OnboardingDocument document = documentService.verifyDocumentWithMessage(
                    documentId, actualVerifier, status, remarks);

            Map<String, Object> docMap = new HashMap<>();
            docMap.put("id", document.getId());
            docMap.put("documentName", document.getDocumentName());
            docMap.put("status", document.getStatus());
            docMap.put("verifiedBy", document.getVerifiedBy());
            docMap.put("verifiedDate", document.getVerifiedDate());
            docMap.put("verificationNotes", document.getVerificationNotes());

            if (document.getEmployee() != null) {
                docMap.put("employeeId", document.getEmployee().getEmployeeId());
            }

            response.put("success", true);
            response.put("message", "Document " + status.toLowerCase() + " by " + actualVerifier);
            response.put("document", docMap);

            // ===== SESSION RESTORATION (If corrupted) =====
            if (session.getAttribute("userId") == null && backupUserId != null) {
                logger.warn("⚠️ Session corrupted during verification - RESTORING HR attributes");
                session.setAttribute("userId", backupUserId);
                session.setAttribute("userRole", backupUserRole);
                session.setAttribute("userName", backupUserName);
            }

            logger.info("✅ Document {} verified successfully, HR session preserved", documentId);
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            logger.error("❌ Verification failed: {}", e.getMessage(), e);
            response.put("success", false);
            response.put("error", e.getMessage());

            // Restore session on error too
            if (session.getAttribute("userId") == null && backupUserId != null) {
                session.setAttribute("userId", backupUserId);
                session.setAttribute("userRole", backupUserRole);
                session.setAttribute("userName", backupUserName);            }

            return ResponseEntity.badRequest().body(response);
        }
    }

    /* ---------- GET DOCUMENTS LIST FOR EMPLOYEE (JSON API) ---------- */
    @GetMapping("/employee/{employeeId}/documents-list")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> getDocumentsList(@PathVariable String employeeId) {
        Map<String, Object> response = new HashMap<>();

        try {
            Optional<Employee> employeeOpt = employeeService.getEmployeeByEmployeeId(employeeId);

            if (employeeOpt.isEmpty()) {
                response.put("success", false);
                response.put("error", "Employee not found");
                return ResponseEntity.badRequest().body(response);
            }

            Employee employee = employeeOpt.get();

            // Initialize documents if not already done
            List<OnboardingDocument> documents = documentService.getDocumentsByEmployee(employee);

            if (documents.isEmpty()) {
                // Auto-initialize if empty
                documentService.initializeDocumentChecklist(employee);
                documents = documentService.getDocumentsByEmployee(employee);
            }

            // Convert to simplified format for frontend
            List<Map<String, Object>> docsList = documents.stream().map(doc -> {
                Map<String, Object> docMap = new HashMap<>();
                docMap.put("id", doc.getId());
                docMap.put("documentType", doc.getDocumentType());
                docMap.put("documentName", doc.getDocumentName());
                docMap.put("description", doc.getDescription());
                docMap.put("mandatory", doc.isMandatory());
                docMap.put("status", doc.getStatus());
                docMap.put("submittedDate", doc.getSubmittedDate());
                docMap.put("filePath", doc.getFilePath());
                return docMap;
            }).collect(Collectors.toList());

            response.put("success", true);
            response.put("documents", docsList);
            response.put("employeeId", employeeId);
            response.put("employeeName", employee.getFirstName() + " " + employee.getLastName());
            response.put("totalDocuments", documents.size());
            response.put("submittedDocuments",
                    documents.stream().filter(d -> d.getStatus().equals("SUBMITTED") || d.getStatus().equals("VERIFIED")).count());

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            logger.error("Error getting documents list for {}: {}", employeeId, e.getMessage());
            response.put("success", false);
            response.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    /* ---------- GET DOCUMENTS FRAGMENT (FOR AJAX REFRESH) ---------- */
    @GetMapping("/employee/{employeeId}/documents-fragment")
    @ResponseBody
    public String getDocumentsFragment(@PathVariable String employeeId) {
        try {
            Optional<Employee> employeeOpt = employeeService.getEmployeeByEmployeeId(employeeId);
            if (employeeOpt.isEmpty()) return "";

            Employee employee = employeeOpt.get();
            List<OnboardingDocument> documents = documentService.getDocumentsByEmployee(employee);

            StringBuilder html = new StringBuilder();
            int count = 0;
            for (OnboardingDocument doc : documents) {
                count++;
                html.append("<tr>")
                        .append("<td>").append(count).append("</td>")
                        .append("<td>")
                        .append("<div class='doc-name-wrapper'>")
                        .append("<div class='doc-name'>").append(doc.getDocumentName()).append("</div>")
                        .append("<div><span class='doc-type'>").append(doc.getDocumentType()).append("</span>");

                if (doc.isMandatory()) {
                    html.append("<span class='doc-mandatory'>*</span>");
                }

                html.append("</div></div></td>")
                        .append("<td><span class='status-badge status-").append(doc.getStatus().toLowerCase()).append("'>")
                        .append(doc.getStatus()).append("</span></td>")
                        .append("<td>");

                // Date/time cell
                if (doc.getSubmittedDate() != null) {
                    html.append("<div class='datetime-wrapper'>")
                            .append("<div class='date-row'><i class='fas fa-calendar'></i>")
                            .append("<span>").append(doc.getSubmittedDate().format(java.time.format.DateTimeFormatter.ofPattern("dd MMM yyyy"))).append("</span></div>")
                            .append("<div class='time-row'><i class='fas fa-clock'></i>")
                            .append("<span>").append(doc.getSubmittedDate().format(java.time.format.DateTimeFormatter.ofPattern("HH:mm:ss"))).append("</span></div>")
                            .append("</div>");
                } else {
                    html.append("<i class='fas fa-minus'></i>");
                }

                html.append("</td><td><div class='action-buttons'>");

                // View button (if file exists)
                if (doc.getFilePath() != null) {
                    html.append("<button class='btn-icon btn-icon-view' onclick='viewDocument(\"")
                            .append(doc.getId()).append("\", \"").append(doc.getDocumentType()).append("\")' title='View'>")
                            .append("<i class='fas fa-eye'></i></button>");
                }

                // Details button
                html.append("<button class='btn-icon btn-icon-details' onclick='viewDetails(this)' data-id='")
                        .append(doc.getId()).append("' title='Details'><i class='fas fa-info'></i></button>");

                // Upload button (only for PENDING or REJECTED)
                if ("PENDING".equals(doc.getStatus()) || "REJECTED".equals(doc.getStatus())) {
                    html.append("<a href='/onboarding/employee/").append(employeeId).append("/upload/")
                            .append(doc.getDocumentType()).append("' class='btn-icon btn-icon-upload' title='Upload'>")
                            .append("<i class='fas fa-upload'></i></a>");
                }

                // Download button (if file exists)
                if (doc.getFilePath() != null) {
                    html.append("<a href='/onboarding/employee/").append(employeeId).append("/download/")
                            .append(doc.getId()).append("' class='btn-icon btn-icon-download' download title='Download'>")
                            .append("<i class='fas fa-download'></i></a>");
                }

                // Verify/Reject buttons (only for SUBMITTED)
                if ("SUBMITTED".equals(doc.getStatus())) {
                    html.append("<button class='btn-icon btn-icon-verify' onclick=\"verifyDoc('")
                            .append(doc.getId()).append("', 'VERIFIED')\" title='Verify'><i class='fas fa-check'></i></button>");
                    html.append("<button class='btn-icon btn-icon-reject' onclick=\"verifyDoc('")
                            .append(doc.getId()).append("', 'REJECTED')\" title='Reject'><i class='fas fa-times'></i></button>");
                }

                // Verified icon (if already verified)
                if ("VERIFIED".equals(doc.getStatus())) {
                    html.append("<span class='btn-icon' style='color:#10b981; pointer-events:none;' title='Verified'>")
                            .append("<i class='fas fa-check-double'></i></span>");
                }

                html.append("</div></td></tr>");
            }

            return html.toString();
        } catch (Exception e) {
            logger.error("Error generating documents fragment", e);
            return "<tr><td colspan='5' style='text-align: center; padding: 20px; color: #ef4444;'><i class='fas fa-exclamation-triangle'></i> Error loading documents</td></tr>";
        }
    }

    /* ---------- DOWNLOAD DOCUMENT ---------- */
    @GetMapping("/employee/{employeeId}/download/{documentId}")
    @ResponseBody
    @Transactional(readOnly = true)
    public ResponseEntity<byte[]> downloadEmployeeDocument(
            @PathVariable String employeeId,
            @PathVariable Long documentId) {

        return hrDownloadDocument(documentId);
    }

    @GetMapping("/download/{documentId}")
    @ResponseBody
    @Transactional(readOnly = true)
    public ResponseEntity<byte[]> hrDownloadDocument(@PathVariable Long documentId) {
        try {
            logger.info("HR downloading document: {}", documentId);

            OnboardingDocument document = documentService.getDocumentById(documentId);

            if (document == null) {
                logger.error("Document not found: {}", documentId);
                return ResponseEntity.notFound().build();
            }

            String fullPath = basePath + "/" + document.getFilePath();
            Path filePath = Paths.get(fullPath);

            if (!Files.exists(filePath)) {
                logger.error("File not found: {}", fullPath);
                return ResponseEntity.notFound().build();
            }

            byte[] fileContent = Files.readAllBytes(filePath);

            String fileName = filePath.getFileName().toString();
            String extension = getFileExtension(fileName);
            String downloadName = document.getDocumentType() + "_" +
                    document.getEmployee().getEmployeeId() +
                    (extension.isEmpty() ? "" : "." + extension);

            String contentType = Files.probeContentType(filePath);
            if (contentType == null) {
                contentType = determineContentType(fileName);
            }

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.parseMediaType(contentType));
            headers.setContentDisposition(ContentDisposition.attachment().filename(downloadName).build());
            headers.setContentLength(fileContent.length);
            headers.setCacheControl("no-cache, no-store, must-revalidate");

            logger.info("HR successfully prepared download: {} ({})", downloadName, contentType);
            return new ResponseEntity<>(fileContent, headers, HttpStatus.OK);

        } catch (Exception e) {
            logger.error("Error HR-downloading document {}: {}", documentId, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(("Error downloading file: " + e.getMessage()).getBytes());
        }
    }

    /* ---------- PROGRESS CALCULATION ---------- */
    private Map<String, Object> calculateOnboardingProgress(Employee employee) {
        Map<String, Object> progress = new HashMap<>();

        List<OnboardingDocument> documents = documentService.getDocumentsByEmployee(employee);

        int total = documents.size();
        int submitted = (int) documents.stream()
                .filter(doc -> "SUBMITTED".equals(doc.getStatus()) ||
                        "VERIFIED".equals(doc.getStatus()) ||
                        "REJECTED".equals(doc.getStatus()))
                .count();
        int verified = (int) documents.stream()
                .filter(doc -> "VERIFIED".equals(doc.getStatus()))
                .count();
        int pending = total - submitted;

        progress.put("totalDocuments", total);
        progress.put("submittedDocuments", submitted);
        progress.put("verifiedDocuments", verified);
        progress.put("pendingDocuments", pending);
        progress.put("submissionProgress", total > 0 ? (submitted * 100) / total : 0);
        progress.put("verificationProgress", total > 0 ? (verified * 100) / total : 0);
        progress.put("pendingVerification", submitted - verified);

        logger.debug("Progress for {}: Total={}, Submitted={}, Verified={}, Pending={}",
                employee.getEmployeeId(), total, submitted, verified, pending);

        return progress;
    }

    /* ---------- SET DEADLINE ---------- */
    @PostMapping("/employee/{employeeId}/set-deadline")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> setDeadline(
            @PathVariable String employeeId,
            @RequestParam String deadline) {

        Map<String, Object> response = new HashMap<>();
        try {
            Optional<Employee> empOpt = employeeService.getEmployeeByEmployeeId(employeeId);
            if (empOpt.isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of(
                        "success", false,
                        "error", "Employee not found: " + employeeId
                ));
            }

            Employee employee = empOpt.get();
            LocalDateTime deadlineDateTime = LocalDateTime.parse(deadline);

            employee.setDocumentDeadline(deadlineDateTime);
            employee.setDeadlineWarningSent(false);
            employee.setDeadlineFinalSent(false);
            employeeService.saveEmployee(employee);

            response.put("success", true);
            response.put("message", "Deadline set successfully");
            response.put("deadline", deadlineDateTime.toString());

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            logger.error("Failed to set deadline: {}", e.getMessage(), e);
            response.put("success", false);
            response.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    /* ---------- TEST DEADLINE ---------- */
    @GetMapping("/employee/{employeeId}/set-deadline-test")
    @ResponseBody
    public String setTestDeadline(@PathVariable String employeeId) {
        try {
            Optional<Employee> empOpt = employeeService.getEmployeeByEmployeeId(employeeId);
            if (empOpt.isEmpty()) return "Employee not found";

            Employee employee = empOpt.get();
            employee.setDocumentDeadline(LocalDateTime.now().plusDays(2));
            employee.setDeadlineWarningSent(false);
            employee.setDeadlineFinalSent(false);
            employeeService.saveEmployee(employee);

            return "Deadline set to 2 days from now for " + employeeId;
        } catch (Exception e) {
            return "Error: " + e.getMessage();
        }
    }

    /* ---------- TEST NOTIFICATION ---------- */
    @GetMapping("/debug/send-test-notification/{employeeId}")
    @ResponseBody
    public String sendTestNotification(@PathVariable String employeeId) {
        try {
            Employee employee = employeeService.getEmployeeByEmployeeId(employeeId)
                    .orElseThrow(() -> new RuntimeException("Employee not found"));

            notificationService.notifyDeadlineApproaching(employee, 24);

            return "Test notification sent to " + employeeId;
        } catch (Exception e) {
            return "Error: " + e.getMessage();
        }
    }

    /* ---------- HELPER METHODS FOR DOWNLOAD ---------- */
    private String getFileExtension(String fileName) {
        if (fileName == null || !fileName.contains(".")) return "";
        return fileName.substring(fileName.lastIndexOf(".") + 1);
    }

    private String determineContentType(String fileName) {
        String ext = getFileExtension(fileName).toLowerCase();
        return switch (ext) {
            case "pdf" -> "application/pdf";
            case "jpg", "jpeg" -> "image/jpeg";
            case "png" -> "image/png";
            case "doc" -> "application/msword";
            case "docx" -> "application/vnd.openxmlformats-officedocument.wordprocessingml.document";
            case "txt" -> "text/plain";
            default -> "application/octet-stream";
        };
    }
}