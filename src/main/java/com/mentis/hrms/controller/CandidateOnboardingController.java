package com.mentis.hrms.controller;

import com.mentis.hrms.model.Employee;
import com.mentis.hrms.model.OnboardingDocument;
import com.mentis.hrms.model.Notification; // ADD THIS IMPORT
import com.mentis.hrms.service.DocumentService;
import com.mentis.hrms.service.EmployeeService;
import com.mentis.hrms.service.ProfilePictureService;
import com.mentis.hrms.util.PasswordUtil;
import jakarta.servlet.http.HttpSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.*;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import com.mentis.hrms.service.NotificationService;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;
import com.mentis.hrms.model.enums.UserRole;
import com.mentis.hrms.service.EmployeeService;

import com.mentis.hrms.model.Attendance;
import com.mentis.hrms.model.Employee;
import com.mentis.hrms.service.AttendanceService;  // ADD THIS IMPORT
import com.mentis.hrms.service.EmployeeService;

import java.time.LocalDateTime;
import java.util.Optional;
@Controller
@RequestMapping("/candidate")


public class CandidateOnboardingController {
    @Autowired
    private AttendanceService attendanceService;


    // ADD THIS BACK - You need a GET handler for the login page
    // In CandidateOnboardingController - KEEP ONLY THIS:
    @GetMapping("/login")
    public String showLoginPage(@RequestParam(value = "error", required = false) String error,
                                @RequestParam(value = "logout", required = false) String logout,
                                Model model) {
        if (error != null) {
            model.addAttribute("error", "Invalid Employee ID or password");
        }
        if (logout != null) {
            model.addAttribute("success", "You have been logged out successfully");
        }
        return "candidate/login-single";
    }

    private static final Logger logger = LoggerFactory.getLogger(CandidateOnboardingController.class);

    @Autowired private EmployeeService employeeService;
    @Autowired private DocumentService documentService;
    @Autowired private ProfilePictureService profilePictureService;
    @Autowired private PasswordUtil passwordUtil;
    @Autowired
    private NotificationService notificationService;
    @Value("${app.upload.base-path:C:/hrms/uploads}")
    private String basePath;

    /*  ==================== 1. LOGIN PAGE ====================  */


    /*  ==================== 2. LOGIN PROCESS ====================  */
    /*  ==================== 2. LOGIN PROCESS (REWRITTEN) ====================  */
//    @PostMapping("/login")
//    public String processLogin(
//            @RequestParam String employeeId,
//            @RequestParam(required = false) String password,
//            jakarta.servlet.http.HttpServletRequest request,
//            RedirectAttributes ra) {
//
//        logger.info("=== LOGIN ATTEMPT: {} ===", employeeId);
//
//        try {
//            // 1. Fetch Employee
//            Optional<Employee> employeeOpt = employeeService.getEmployeeByEmployeeId(employeeId);
//
//            if (employeeOpt.isEmpty()) {
//                ra.addFlashAttribute("error", "No account found with this Employee ID");
//                return "redirect:/candidate/login";
//            }
//
//            Employee employee = employeeOpt.get();
//
//            // 2. Check Account Status
//            if (!employee.isActive() && employee.getPassword() != null) {
//                ra.addFlashAttribute("error", "Your account is deactivated. Please contact HR.");
//                return "redirect:/candidate/login";
//            }
//
//            // 3. Check for First Time Login (No password set yet)
//            if (employee.getPassword() == null || employee.getPassword().isEmpty()) {
//                logger.info("First time login detected for: {}", employeeId);
//                // We use a clean session for password creation
//                jakarta.servlet.http.HttpSession tempSession = request.getSession(true);
//                tempSession.setAttribute("tempEmployeeId", employee.getEmployeeId());
//                return "redirect:/candidate/create-password";
//            }
//
//            // 4. Validate Password for Returning Users
//            if (password == null || password.trim().isEmpty()) {
//                ra.addFlashAttribute("error", "Please enter your password");
//                return "redirect:/candidate/login";
//            }
//
//            if (!passwordUtil.matchesPassword(password, employee.getPassword())) {
//                ra.addFlashAttribute("error", "Invalid Employee ID or password");
//                return "redirect:/candidate/login";
//            }
//
//            // 5. SESSION SECURITY: Invalidate old session and create a fresh one
//            // This prevents "Overlap" where one user's tab affects another's
//            jakarta.servlet.http.HttpSession existingSession = request.getSession(false);
//            if (existingSession != null) {
//                existingSession.invalidate();
//            }
//
//            jakarta.servlet.http.HttpSession newSession = request.getSession(true);
//
//            // 6. Set Standardized Session Attributes (Used by RoleInterceptor)
//            newSession.setAttribute("userId", employee.getEmployeeId());
//            newSession.setAttribute("userRole", employee.getRole().name()); // e.g., "HR", "EMPLOYEE"
//            newSession.setAttribute("userName", employee.getFirstName() + " " + employee.getLastName());
//
//            // Internal attributes for specific page logic
//            newSession.setAttribute("candidateEmployeeId", employee.getEmployeeId());
//            newSession.setMaxInactiveInterval(1800); // 30 minutes
//
//            logger.info("✅ Login Successful. User: {}, Role: {}", employeeId, employee.getRole());
//
//
//            /* ==================== ATTENDANCE AUTO CHECK-IN ==================== */
//            logger.info(">>> CHECK-IN START (OnboardingController): Employee {} with role {}", employeeId, employee.getRole().name());
//
//            try {
//                String roleName = employee.getRole().name();
//                String cleanRole = roleName.replace("ROLE_", "").toUpperCase();
//                boolean isEligible = "EMPLOYEE".equals(cleanRole) || "HR".equals(cleanRole);
//
//                if (isEligible) {
//                    Attendance attendance = attendanceService.checkIn(employeeId);
//                    newSession.setAttribute("todayAttendance", attendance);
//                    newSession.setAttribute("attendanceStatus", attendance.getStatus());
//
//                    if (attendance.getCheckInTime() != null) {
//                        long checkInMillis = attendance.getCheckInTime()
//                                .atZone(java.time.ZoneId.systemDefault())
//                                .toInstant()
//                                .toEpochMilli();
//                        newSession.setAttribute("checkInTimeMillis", checkInMillis);
//                    }
//                    logger.info(">>> CHECK-IN SUCCESS (OnboardingController): {}", attendance.getCheckInTime());
//                }
//            } catch (Exception e) {
//                logger.error(">>> CHECK-IN FAILED (OnboardingController): {}", e.getMessage());
//            }
//            /* ==================== END ATTENDANCE BLOCK ==================== */
//
//
//            // 7. Explicit Role-Based Redirection
//            // This ensures the user lands on the correct dashboard immediately
//            switch (employee.getRole()) {
//                case SUPER_ADMIN:
//                    return "redirect:/dashboard/admin";
//
//                case HR:
//                    return "redirect:/dashboard/hr";
//
//                case EMPLOYEE:
//                    return "redirect:/candidate/dashboard/" + employee.getEmployeeId();
//
//                default:
//                    logger.warn("Unknown role for user {}, defaulting to Employee dashboard", employeeId);
//                    return "redirect:/candidate/dashboard/" + employee.getEmployeeId();
//            }
//
//        } catch (Exception e) {
//            logger.error("❌ Login System Error: {}", e.getMessage(), e);
//            ra.addFlashAttribute("error", "An internal error occurred. Please try again.");
//            return "redirect:/candidate/login";
//        }
//    }



    /*  ==================== 3. CREATE PASSWORD PAGE ====================  */
    @GetMapping("/create-password")
    public String showCreatePasswordPage(HttpSession session, Model model) {
        String employeeId = (String) session.getAttribute("tempEmployeeId");
        if (employeeId == null) return "redirect:/candidate/login?error=Please+start+from+login+page";

        Optional<Employee> employeeOpt = employeeService.getEmployeeByEmployeeId(employeeId);
        if (employeeOpt.isEmpty()) {
            session.removeAttribute("tempEmployeeId");
            return "redirect:/candidate/login?error=Employee+not+found";
        }
        Employee employee = employeeOpt.get();

        if (employee.getPassword() != null && !employee.getPassword().isEmpty()) {
            session.removeAttribute("tempEmployeeId");
            return "redirect:/candidate/login?info=Password+already+created.+Please+login";
        }

        model.addAttribute("employeeId", employeeId);
        model.addAttribute("employeeName", employee.getFirstName() + " " + employee.getLastName());
        return "candidate/create-password";
    }

    /*  ==================== 4. PROCESS PASSWORD CREATION ====================  */
    @PostMapping("/create-password")
    public String createPassword(@RequestParam String password,
                                 @RequestParam String confirmPassword,
                                 HttpSession session,
                                 RedirectAttributes ra) {

        String employeeId = (String) session.getAttribute("tempEmployeeId");
        if (employeeId == null) return "redirect:/candidate/login?error=Session+expired";

        if (!password.equals(confirmPassword)) {
            ra.addFlashAttribute("error", "Passwords do not match");
            return "redirect:/candidate/create-password";
        }
        if (password.length() < 8) {
            ra.addFlashAttribute("error", "Password must be at least 8 characters");
            return "redirect:/candidate/create-password";
        }

        Optional<Employee> employeeOpt = employeeService.getEmployeeByEmployeeId(employeeId);
        if (employeeOpt.isEmpty()) {
            session.removeAttribute("tempEmployeeId");
            return "redirect:/candidate/login?error=Employee+not+found";
        }
        Employee employee = employeeOpt.get();

        if (employee.getPassword() != null && !employee.getPassword().isEmpty()) {
            session.removeAttribute("tempEmployeeId");
            ra.addFlashAttribute("error", "Password already exists. Please use login page.");
            return "redirect:/candidate/login";
        }

        employee.setPassword(passwordUtil.encodePassword(password));
        employee.setCredentialsCreated(true);
        employee.setActive(true);
        employee.setOnboardingStatus("NOT_STARTED");
        employee.setResetToken(null);
        employee.setTokenExpiry(null);

        /* ==========  MODIFIED: REAL-TIME PRESENCE - OFFLINE ON PASSWORD CREATION  ========== */
        employee.setPresenceStatus("OFFLINE");
        employee.setLastPresenceUpdate(LocalDateTime.now());

        employeeService.saveEmployee(employee);

        session.setAttribute("candidateEmployeeId", employeeId);
        session.setAttribute("candidateName", employee.getFirstName() + " " + employee.getLastName());
        session.setAttribute("candidateEmail", employee.getEmail());
        session.setAttribute("loginTime", LocalDateTime.now());
        session.setMaxInactiveInterval(30 * 60);

        session.removeAttribute("tempEmployeeId");

        logger.info("Password created & employee OFFLINE for: {}", employeeId);
        return "redirect:/candidate/dashboard/" + employeeId;
    }

    /*  ==================== 5. SHOW DASHBOARD ====================  */
    @GetMapping("/dashboard/{employeeId}")
    public String showDashboard(@PathVariable String employeeId,
                                HttpSession session,
                                Model model) {

        // CHANGE FROM: candidateEmployeeId
        // TO: userId (to match what CandidateAuthController sets)
        String sessionEmployeeId = (String) session.getAttribute("userId");

        if (sessionEmployeeId == null || !sessionEmployeeId.equals(employeeId)) {
            session.invalidate();
            return "redirect:/candidate/login?error=Please+login+first";
        }

        // Also verify role
        String userRole = (String) session.getAttribute("userRole");
        if (!"EMPLOYEE".equals(userRole) && !"HR".equals(userRole)) {
            return "redirect:/candidate/login?error=Unauthorized";
        }

        Optional<Employee> employeeOpt = employeeService.getEmployeeByEmployeeId(employeeId);
        if (employeeOpt.isEmpty()) {
            session.invalidate();
            return "redirect:/candidate/login?error=Employee+not+found";
        }
        Employee employee = employeeOpt.get();

        if (!employee.isActive()) {
            session.invalidate();
            return "redirect:/candidate/login?error=Account+deactivated.+Contact+HR";
        }

        List<OnboardingDocument> documents = documentService.getDocumentsByEmployee(employee);
        long uploadedCount = documents.stream()
                .filter(doc -> doc.getFilePath() != null && !doc.getFilePath().trim().isEmpty())
                .count();

        model.addAttribute("employee", employee);
        model.addAttribute("documents", documents);
        model.addAttribute("uploadedCount", uploadedCount);
        model.addAttribute("progress", calculateProgress(employee));
        model.addAttribute("timestamp", System.currentTimeMillis());
        // Add user info for header
        model.addAttribute("userRole", session.getAttribute("userRole"));
        model.addAttribute("userName", session.getAttribute("userName"));

        // FIXED: Use getUnreadCount for backward compatibility
        model.addAttribute("unreadCount", notificationService.getUnreadCount(employeeId, "EMPLOYEE"));
        return "candidate/dashboard";
    }

    /*  ==================== 6. DOCUMENT UPLOAD ====================  */
    @GetMapping("/onboarding/{employeeId}/upload/{documentType}")
    public String showUploadForm(@PathVariable String employeeId,
                                 @PathVariable String documentType,
                                 HttpSession session,
                                 Model model) {

        String sessionEmployeeId = (String) session.getAttribute("candidateEmployeeId");
        if (sessionEmployeeId == null || !sessionEmployeeId.equals(employeeId)) {
            return "redirect:/candidate/login?error=Please+login+first";
        }

        Optional<Employee> employeeOpt = employeeService.getEmployeeByEmployeeId(employeeId);
        if (employeeOpt.isEmpty()) return "redirect:/candidate/login?error=Employee+not+found";

        model.addAttribute("employee", employeeOpt.get());
        model.addAttribute("documentType", documentType);
        model.addAttribute("documentName", getDocumentDisplayName(documentType));

        return "candidate/upload-document";
    }

    @PostMapping("/onboarding/{employeeId}/upload/{documentType}")
    public String uploadDocument(@PathVariable String employeeId,
                                 @PathVariable String documentType,
                                 @RequestParam("documentFile") MultipartFile file,
                                 @RequestParam(required = false) String notes,
                                 HttpSession session,
                                 RedirectAttributes ra) {

        String sessionEmployeeId = (String) session.getAttribute("candidateEmployeeId");
        if (sessionEmployeeId == null || !sessionEmployeeId.equals(employeeId)) {
            return "redirect:/candidate/login?error=Please+login+first";
        }

        try {
            documentService.uploadDocument(employeeId, documentType, file, notes);

            // Check if all documents are now uploaded
            Optional<Employee> employeeOpt = employeeService.getEmployeeByEmployeeId(employeeId);
            if (employeeOpt.isPresent()) {
                checkAndNotifyAllDocumentsUploaded(employeeOpt.get());
            }

            ra.addFlashAttribute("success", "✅ Document uploaded successfully! HR will verify it.");
            return "redirect:/candidate/dashboard/" + employeeId + "?activeTab=profile&activeSubTab=documents";
        } catch (Exception e) {
            logger.error("Upload error: {}", e.getMessage());
            ra.addFlashAttribute("error", "Upload failed: " + e.getMessage());
            return "redirect:/candidate/onboarding/" + employeeId + "/upload/" + documentType;
        }
    }

    /*  ==================== 7. PROFILE PICTURE ====================  */
    @PostMapping("/profile-picture/upload/{employeeId}")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> uploadProfilePicture(
            @PathVariable String employeeId,
            @RequestParam("profilePicture") MultipartFile file,
            HttpSession session) {

        Map<String, Object> response = new HashMap<>();
        String sessionEmployeeId = (String) session.getAttribute("candidateEmployeeId");
        if (sessionEmployeeId == null || !sessionEmployeeId.equals(employeeId)) {
            response.put("success", false);
            response.put("error", "Session expired. Please login again.");
            return ResponseEntity.status(401).body(response);
        }

        try {
            String profilePicturePath = profilePictureService.uploadProfilePicture(employeeId, file);
            Optional<Employee> employeeOpt = employeeService.getEmployeeByEmployeeId(employeeId);

            response.put("success", true);
            response.put("message", "Profile picture uploaded successfully!");
            response.put("profilePicturePath", profilePicturePath);
            response.put("hasProfilePicture", true);
            response.put("initials", employeeOpt.map(Employee::getInitials).orElse("U"));

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            logger.error("Profile picture upload failed: {}", e.getMessage());
            response.put("success", false);
            response.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    @PostMapping("/profile-picture/delete/{employeeId}")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> deleteProfilePicture(
            @PathVariable String employeeId,
            HttpSession session) {

        Map<String, Object> response = new HashMap<>();
        String sessionEmployeeId = (String) session.getAttribute("candidateEmployeeId");
        if (sessionEmployeeId == null || !sessionEmployeeId.equals(employeeId)) {
            response.put("success", false);
            response.put("error", "Session expired. Please login again.");
            return ResponseEntity.status(401).body(response);
        }

        try {
            boolean deleted = profilePictureService.deleteProfilePicture(employeeId);
            Optional<Employee> employeeOpt = employeeService.getEmployeeByEmployeeId(employeeId);

            response.put("success", true);
            response.put("message", "Profile picture deleted successfully!");
            response.put("deleted", deleted);
            response.put("hasProfilePicture", false);
            response.put("initials", employeeOpt.map(Employee::getInitials).orElse("U"));

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            logger.error("Profile picture delete failed: {}", e.getMessage());
            response.put("success", false);
            response.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    /*  ==================== 8. DOWNLOAD DOCUMENT ====================  */
    @GetMapping("/download/{documentId}")
    @ResponseBody
    public ResponseEntity<byte[]> downloadDocument(@PathVariable Long documentId, HttpSession session) {
        try {
            String sessionEmployeeId = (String) session.getAttribute("candidateEmployeeId");
            if (sessionEmployeeId == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(null);

            OnboardingDocument doc = documentService.getDocumentById(documentId);
            if (doc == null || !doc.getEmployee().getEmployeeId().equals(sessionEmployeeId)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(null);
            }

            Path file = Paths.get(basePath).resolve(doc.getFilePath()).normalize();
            if (!Files.exists(file)) return ResponseEntity.notFound().build();

            byte[] bytes = Files.readAllBytes(file);
            String fileName = file.getFileName().toString();
            String contentType = Files.probeContentType(file);
            if (contentType == null) contentType = "application/octet-stream";

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.parseMediaType(contentType));
            headers.setContentDisposition(ContentDisposition.attachment().filename(fileName).build());
            headers.setContentLength(bytes.length);

            return new ResponseEntity<>(bytes, headers, HttpStatus.OK);

        } catch (Exception e) {
            logger.error("Download error doc {}: {}", documentId, e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
        }
    }

    /*  ==================== 9. LOGOUT ====================  */
    @GetMapping("/logout")
    public String logout(HttpSession session, RedirectAttributes ra) {
        String employeeId = (String) session.getAttribute("candidateEmployeeId");
        if (employeeId != null) {
            logger.info("Candidate logging out: {}", employeeId);
            Optional<Employee> empOpt = employeeService.getEmployeeByEmployeeId(employeeId);
            empOpt.ifPresent(emp -> {
                emp.setPresenceStatus("OFFLINE");
                emp.setLastPresenceUpdate(LocalDateTime.now());
                employeeService.updateEmployee(emp);
            });
        }
        session.invalidate();
        ra.addFlashAttribute("success", "You have been logged out successfully.");
        return "redirect:/candidate/login"; // REMOVE query param, let FlashAttribute handle it
    }

    /*  ==================== 10. HELPER METHODS ====================  */
    private Map<String, Object> calculateProgress(Employee employee) {
        List<OnboardingDocument> docs = documentService.getDocumentsByEmployee(employee);
        int total = docs.size();
        int submitted = (int) docs.stream()
                .filter(d -> d.getFilePath() != null && !d.getFilePath().trim().isEmpty())
                .count();
        int verified = (int) docs.stream()
                .filter(d -> "VERIFIED".equals(d.getStatus()))
                .count();

        Map<String, Object> progress = new HashMap<>();
        progress.put("totalDocuments", total);
        progress.put("submittedDocuments", submitted);
        progress.put("verifiedDocuments", verified);
        progress.put("submissionProgress", total > 0 ? (submitted * 100) / total : 0);
        progress.put("verificationProgress", total > 0 ? (verified * 100) / total : 0);
        progress.put("pendingDocuments", total - submitted);
        progress.put("pendingVerification", submitted - verified);
        return progress;
    }

    /* ==================== 11. NEW: Check and send permanent notification when all docs uploaded ==================== */
    private void checkAndNotifyAllDocumentsUploaded(Employee employee) {
        try {
            List<OnboardingDocument> documents = documentService.getDocumentsByEmployee(employee);
            long uploadedCount = documents.stream()
                    .filter(doc -> doc.getFilePath() != null && !doc.getFilePath().trim().isEmpty())
                    .count();

            // If all documents are uploaded
            if (uploadedCount == documents.size() && documents.size() > 0) {
                // Check if permanent notification already exists
                List<Notification> existingNotifications = notificationService.getPersistentNotifications(
                        employee.getEmployeeId(), "EMPLOYEE");

                boolean alreadyNotified = existingNotifications.stream()
                        .anyMatch(n -> "ALL_DOCUMENTS_UPLOADED".equals(n.getType()));

                if (!alreadyNotified) {
                    // Send permanent notification
                    notificationService.notifyAllDocumentsUploaded(employee);
                }
            }
        } catch (Exception e) {
            logger.error("Error checking all documents uploaded: {}", e.getMessage());
        }
    }

    // ADD this method at the end of CandidateOnboardingController class
    @GetMapping("/dashboard/{employeeId}/documents-fragment")
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
                boolean canUpload = doc.getStatus().equals("PENDING") || doc.getStatus().equals("REJECTED");
                boolean hasFile = doc.getFilePath() != null;

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

                html.append("</td><td>");

                // HR Validation Message
                if (doc.getVerificationNotes() != null && !doc.getVerificationNotes().isEmpty()) {
                    String cleanedNotes = doc.getVerificationNotes().replace("candidate", "employee");
                    html.append("<div class='validation-message'>").append(cleanedNotes).append("</div>");
                } else {
                    html.append("<div style='color: var(--dark-text-secondary); font-style: italic;'><i class='fas fa-minus'></i></div>");
                }

                html.append("</td><td><div class='action-buttons'>");

                // Upload button (if allowed)
                if (canUpload) {
                    html.append("<a href='/candidate/onboarding/")
                            .append(employeeId).append("/upload/").append(doc.getDocumentType())
                            .append("' class='btn-icon btn-icon-upload' title='Upload'><i class='fas fa-upload'></i></a>");
                }

                // Download button (if file exists)
                if (hasFile) {
                    html.append("<a href='/candidate/download/").append(doc.getId())
                            .append("' class='btn-icon btn-icon-download' download title='Download'><i class='fas fa-download'></i></a>");
                }

                // View button (if file exists)
                if (hasFile) {
                    html.append("<button class='btn-icon btn-icon-view' onclick='viewDocument(\"")
                            .append(doc.getId()).append("\", \"").append(doc.getDocumentType()).append("\")' title='View'><i class='fas fa-eye'></i></button>");
                }

                html.append("</div></td></tr>");
            }

            return html.toString();
        } catch (Exception e) {
            logger.error("Error generating documents fragment", e);
            return "<tr><td colspan='6' style='text-align: center; padding: 20px; color: #ef4444;'><i class='fas fa-exclamation-triangle'></i> Error loading documents</td></tr>";
        }
    }

    private String getDocumentDisplayName(String documentType) {
        Map<String, String> names = Map.of(
                "RESUME", "Resume/CV",
                "PAN_CARD", "PAN Card",
                "AADHAAR_CARD", "Aadhaar Card",
                "PASSPORT", "Passport",
                "OFFER_LETTER", "Offer Letter",
                "EXPERIENCE_LETTERS", "Experience Letters",
                "EDUCATIONAL_CERTIFICATES", "Educational Certificates"
        );
        return names.getOrDefault(documentType, documentType.replace("_", " "));
    }
}