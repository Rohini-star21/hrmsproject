package com.mentis.hrms.service;

import com.mentis.hrms.model.OnboardingDocument;
import com.mentis.hrms.model.Employee;
import com.mentis.hrms.model.Notification;
import com.mentis.hrms.repository.OnboardingDocumentRepository;
import com.mentis.hrms.repository.EmployeeRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Transactional
public class DocumentService {

    @Autowired private OnboardingDocumentRepository documentRepository;
    @Autowired private EmployeeRepository employeeRepository;
    @Autowired private NotificationService notificationService;
    @Autowired private TemporaryNotificationService temporaryNotificationService; // ADDED

    @Value("${app.upload.base-path:C:/hrms/uploads}")
    private String basePath;

    /* ---------------------------------------------------- */
    /* 1.  NEW – used by DashboardController                */
    /* ---------------------------------------------------- */
    public void initializeDocumentChecklist(Employee employee) {
        List<String> mandatory = List.of("PAN_CARD", "AADHAAR_CARD", "OFFER_LETTER");
        List<String> optional  = List.of("RESUME", "PASSPORT", "EXPERIENCE_LETTERS", "EDUCATIONAL_CERTIFICATES");

        for (String type : mandatory) createDocumentIfAbsent(employee, type, true);
        for (String type : optional)  createDocumentIfAbsent(employee, type, false);

        // ✅ IMPORTANT: Update total document count after initialization
        employee.setTotalDocuments(mandatory.size() + optional.size());
        employeeRepository.save(employee);
    }

    private void createDocumentIfAbsent(Employee emp, String type, boolean mandatory) {
        if (documentRepository.findByEmployeeAndDocumentType(emp, type).isEmpty()) {
            OnboardingDocument doc = new OnboardingDocument();
            doc.setEmployee(emp);
            doc.setDocumentType(type);
            doc.setDocumentName(getDisplayName(type));
            doc.setMandatory(mandatory);
            doc.setStatus("PENDING");
            documentRepository.save(doc);
        }
    }

    /* ---------------------------------------------------- */
    /* 2.  EXISTING – fixed upload + toast                 */
    /* ---------------------------------------------------- */
    public OnboardingDocument uploadDocument(String employeeId, String documentType,
                                             MultipartFile file, String notes) throws Exception {
        Employee emp = employeeRepository.findByEmployeeId(employeeId)
                .orElseThrow(() -> new RuntimeException("Employee not found"));

        OnboardingDocument doc = documentRepository
                .findByEmployeeAndDocumentType(emp, documentType)
                .orElseGet(() -> {
                    OnboardingDocument d = new OnboardingDocument();
                    d.setEmployee(emp);
                    d.setDocumentType(documentType);
                    d.setDocumentName(getDisplayName(documentType));
                    d.setMandatory(isMandatory(documentType));
                    d.setStatus("PENDING");
                    return d;
                });

        String savedPath = saveFile(file, employeeId, documentType);
        doc.setFilePath(savedPath);
        doc.setSubmittedDate(LocalDateTime.now());
        doc.setStatus("SUBMITTED");
        OnboardingDocument saved = documentRepository.save(doc);

        // ✅ PERMANENT notification (stored in DB)
        notificationService.notifyDocumentUploaded(saved);

        // ✅ TEMPORARY toast (appears on screen, disappears after few seconds) - ADDED
        temporaryNotificationService.sendDocumentUploadedToast(employeeId, saved.getDocumentName());

        // ✅ NEW: Check if all documents are now uploaded
        checkAllDocumentsUploaded(emp);

        return saved;
    }

    /* ---------------------------------------------------- */
    /* 3.  HR verify – with toast                          */
    /* ---------------------------------------------------- */
    public OnboardingDocument verifyDocumentWithMessage(Long docId, String verifiedBy, String status, String remarks) {
        OnboardingDocument doc = documentRepository.findById(docId)
                .orElseThrow(() -> new RuntimeException("Document not found"));
        doc.setStatus(status);
        doc.setVerificationNotes(remarks);
        doc.setVerifiedBy(verifiedBy);
        doc.setVerifiedDate(LocalDateTime.now());
        OnboardingDocument saved = documentRepository.save(doc);

        // ✅ PERMANENT notification (stored in DB)
        notificationService.notifyDocumentVerified(saved, verifiedBy);

        // ✅ TEMPORARY toast - ADDED
        boolean isVerified = "VERIFIED".equals(status);
        temporaryNotificationService.sendDocumentVerifiedToast(
                doc.getEmployee().getEmployeeId(),
                doc.getDocumentName(),
                isVerified
        );

        return saved;
    }

    /* ---------------------------------------------------- */
    /* 4.  HR Upload Document (Auto-Verify)               */
    /* ---------------------------------------------------- */
    public OnboardingDocument hrUploadDocument(String empId, String docType,
                                               MultipartFile file, String notes,
                                               String hrId) throws Exception {
        // Upload the document first
        OnboardingDocument doc = uploadDocument(empId, docType, file, notes);

        // ✅ AUTO-VERIFY: Immediately set to VERIFIED for HR uploads
        doc.setStatus("VERIFIED");
        doc.setVerifiedBy(hrId);
        doc.setVerifiedDate(LocalDateTime.now());
        doc.setVerificationNotes("Document uploaded and verified by HR");

        OnboardingDocument verifiedDoc = documentRepository.save(doc);

        // Send verification notification (this triggers the GREEN toast)
        notificationService.notifyDocumentVerified(verifiedDoc, hrId);

        // ✅ TEMPORARY toast for HR upload - ADDED
        temporaryNotificationService.sendDocumentVerifiedToast(empId, verifiedDoc.getDocumentName(), true);

        return verifiedDoc;
    }

    /* ---------------------------------------------------- */
    /* 5.  NEW: Check if all documents are uploaded       */
    /* ---------------------------------------------------- */
    private void checkAllDocumentsUploaded(Employee employee) {
        // Refresh employee object to get latest counts
        Employee emp = employeeRepository.findByEmployeeId(employee.getEmployeeId())
                .orElse(null);

        if (emp == null) return;

        // Get all documents for this employee
        List<OnboardingDocument> documents = documentRepository.findByEmployee(emp);

        // Count uploaded documents
        long uploadedCount = documents.stream()
                .filter(doc -> doc.getFilePath() != null && !doc.getFilePath().trim().isEmpty())
                .count();

        // Update employee's submitted documents count
        emp.setSubmittedDocuments((int) uploadedCount);
        employeeRepository.save(emp);

        // Check if all required documents are submitted
        if (uploadedCount == documents.size() && documents.size() > 0) {

            // FIXED: Use getPersistentNotifications instead of getNotifications
            List<Notification> existingNotifications = notificationService.getPersistentNotifications(
                    emp.getEmployeeId(), "EMPLOYEE");

            boolean alreadyNotified = existingNotifications.stream()
                    .anyMatch(n -> "ALL_DOCUMENTS_UPLOADED".equals(n.getType()));

            if (!alreadyNotified) {
                // FIXED: Call the correct method name
                notificationService.notifyAllDocumentsUploaded(emp);

                // ✅ TEMPORARY toast for all documents uploaded - ADDED
                temporaryNotificationService.sendTemporaryNotification(
                        emp.getEmployeeId(),
                        "EMPLOYEE",
                        "ALL_DOCUMENTS_UPLOADED",
                        "🎉 All Documents Uploaded!",
                        "Congratulations! All your documents have been uploaded successfully.",
                        emp.getEmployeeId(),
                        "EMPLOYEE",
                        "HR_SYSTEM"
                );
            }
        }
    }

    /* ---------------------------------------------------- */
    /* 6.  NEW: Helper method to check if employee has all documents uploaded */
    /* ---------------------------------------------------- */
    public boolean hasAllDocumentsUploaded(Employee employee) {
        List<OnboardingDocument> documents = documentRepository.findByEmployee(employee);
        return documents.stream()
                .allMatch(doc -> doc.getFilePath() != null && !doc.getFilePath().trim().isEmpty());
    }

    /* ---------------------------------------------------- */
    /* 7.  NEW: Check and send deadline warnings           */
    /* ---------------------------------------------------- */
    /* 7. NEW: Check and send deadline warnings (TEMPORARY only) */
    public void checkAndSendDeadlineWarnings() {
        List<Employee> employees = employeeRepository.findAll();
        LocalDateTime now = LocalDateTime.now();

        for (Employee emp : employees) {
            // ✅ FIXED: Use correct method name
            if (emp.getDocumentDeadline() != null) {
                LocalDateTime deadline = emp.getDocumentDeadline();
                long hoursLeft = java.time.Duration.between(now, deadline).toHours();

                // Send ONLY temporary toasts for deadlines
                // These will NOT be stored in DB
                if (hoursLeft <= 48 && hoursLeft > 24) {
                    temporaryNotificationService.sendDeadlineWarning(emp.getEmployeeId(), hoursLeft);
                } else if (hoursLeft <= 24 && hoursLeft > 0) {
                    temporaryNotificationService.sendDeadlineWarning(emp.getEmployeeId(), hoursLeft);
                } else if (hoursLeft <= 0 && hoursLeft > -24) {
                    temporaryNotificationService.sendTemporaryNotification(
                            emp.getEmployeeId(),
                            "EMPLOYEE",
                            "DEADLINE_REACHED",
                            "⏰ Deadline Passed!",
                            "Your document upload deadline has passed. Contact HR immediately.",
                            emp.getEmployeeId(),
                            "EMPLOYEE",
                            "HR_SYSTEM"
                    );
                }
            }
        }
    }

    /* ---------------------------------------------------- */
    /* 8.  Utility methods                                */
    /* ---------------------------------------------------- */
    private String saveFile(MultipartFile file, String empId, String docType) throws Exception {
        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null || originalFilename.isEmpty()) {
            throw new Exception("File name is empty");
        }

        String ext = "";
        int lastDotIndex = originalFilename.lastIndexOf('.');
        if (lastDotIndex > 0) {
            ext = originalFilename.substring(lastDotIndex);
        }

        String fileName = docType + "_" + UUID.randomUUID() + ext;
        Path dir = Paths.get(basePath, "documents", empId);
        Files.createDirectories(dir);
        Path target = dir.resolve(fileName);
        Files.copy(file.getInputStream(), target);
        return "documents/" + empId + "/" + fileName;
    }

    private boolean isMandatory(String type) {
        return List.of("PAN_CARD", "AADHAAR_CARD", "OFFER_LETTER").contains(type);
    }

    private String getDisplayName(String type) {
        Map<String, String> displayNames = new HashMap<>();
        displayNames.put("RESUME", "Resume/CV");
        displayNames.put("PAN_CARD", "PAN Card");
        displayNames.put("AADHAAR_CARD", "Aadhaar Card");
        displayNames.put("PASSPORT", "Passport");
        displayNames.put("OFFER_LETTER", "Offer Letter");
        displayNames.put("EXPERIENCE_LETTERS", "Experience Letters");
        displayNames.put("EDUCATIONAL_CERTIFICATES", "Educational Certificates");
        displayNames.put("DRIVING_LICENSE", "Driving License");
        displayNames.put("VOTER_ID", "Voter ID");
        displayNames.put("APPOINTMENT_LETTER", "Appointment Letter");
        displayNames.put("RELIEVING_LETTER", "Relieving Letter");
        displayNames.put("TENTH_MARKSHEET", "10th Marksheet");
        displayNames.put("TWELFTH_MARKSHEET", "12th Marksheet");
        displayNames.put("DEGREE_CERTIFICATE", "Degree Certificate");
        displayNames.put("POST_GRADUATION", "Post Graduation");
        displayNames.put("BANK_ACCOUNT", "Bank Account Details");
        displayNames.put("PAN_CARD_COPY", "PAN Card Copy");
        displayNames.put("PASSPORT_PHOTO", "Passport Photo");
        displayNames.put("SIGNATURE", "Digital Signature");
        displayNames.put("MEDICAL_CERTIFICATE", "Medical Certificate");
        displayNames.put("NDA", "Non-Disclosure Agreement");
        displayNames.put("POLICY_ACKNOWLEDGMENT", "Policy Acknowledgment");

        return displayNames.getOrDefault(type, type.replace('_', ' '));
    }

    /* ---------------------------------------------------- */
    /* 9.  Repository wrappers                            */
    /* ---------------------------------------------------- */
    public List<OnboardingDocument> getDocumentsByEmployee(Employee emp) {
        return documentRepository.findByEmployee(emp);
    }

    public OnboardingDocument getDocumentById(Long id) {
        return documentRepository.findById(id).orElse(null);
    }

    public OnboardingDocumentRepository getDocumentRepository() {
        return documentRepository;
    }

    /* ---------------------------------------------------- */
    /* 10. NEW: Methods to update employee document counts */
    /* ---------------------------------------------------- */
    public void updateEmployeeDocumentCounts(Employee employee) {
        List<OnboardingDocument> documents = documentRepository.findByEmployee(employee);

        int total = documents.size();
        int submitted = (int) documents.stream()
                .filter(doc -> doc.getFilePath() != null && !doc.getFilePath().trim().isEmpty())
                .count();
        int verified = (int) documents.stream()
                .filter(doc -> "VERIFIED".equals(doc.getStatus()))
                .count();

        employee.setTotalDocuments(total);
        employee.setSubmittedDocuments(submitted);
        employee.setVerifiedDocuments(verified);

        employeeRepository.save(employee);
    }
    public Map<String, Object> uploadMultipleDocuments(String employeeId,
                                                       Map<String, MultipartFile> files,
                                                       String uploadedBy) throws Exception {
        Map<String, Object> results = new HashMap<>();
        List<String> successDocs = new ArrayList<>();
        List<String> failedDocs = new ArrayList<>();

        Employee emp = employeeRepository.findByEmployeeId(employeeId)
                .orElseThrow(() -> new RuntimeException("Employee not found"));

        for (Map.Entry<String, MultipartFile> entry : files.entrySet()) {
            String docType = entry.getKey();
            MultipartFile file = entry.getValue();

            try {
                OnboardingDocument doc = uploadDocument(employeeId, docType, file, "Bulk upload by " + uploadedBy);
                successDocs.add(docType);
            } catch (Exception e) {
                failedDocs.add(docType + ": " + e.getMessage());
            }
        }

        results.put("success", failedDocs.isEmpty());
        results.put("uploaded", successDocs.size());
        results.put("failed", failedDocs.size());
        results.put("successDocuments", successDocs);
        results.put("failedDocuments", failedDocs);

        return results;
    }
    /* ---------------------------------------------------- */
    /* 11. NEW: Method to manually trigger permanent notification */
    /* ---------------------------------------------------- */
    public void sendPermanentAllDocumentsNotification(Employee employee) {
        if (hasAllDocumentsUploaded(employee)) {
            notificationService.notifyAllDocumentsUploaded(employee);

            // Also send temporary toast
            temporaryNotificationService.sendTemporaryNotification(
                    employee.getEmployeeId(),
                    "EMPLOYEE",
                    "ALL_DOCUMENTS_UPLOADED",
                    "🎉 All Documents Complete!",
                    "All documents uploaded successfully!",
                    employee.getEmployeeId(),
                    "EMPLOYEE",
                    "HR_SYSTEM"
            );
        }
    }

    /* ---------------------------------------------------- */
    /* 12. NEW: Method to manually trigger temporary toast */
    /* ---------------------------------------------------- */
    public void sendTestToast(String employeeId, String message) {
        temporaryNotificationService.sendTemporaryNotification(
                employeeId,
                "EMPLOYEE",
                "TEST",
                "📋 Test Notification",
                message,
                employeeId,
                "EMPLOYEE",
                "SYSTEM"
        );
    }
}