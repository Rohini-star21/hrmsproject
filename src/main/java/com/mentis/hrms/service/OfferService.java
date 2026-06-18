package com.mentis.hrms.service;

import com.mentis.hrms.dto.OfferRequest;
import com.mentis.hrms.model.Employee;
import com.mentis.hrms.model.JobApplication;
import com.mentis.hrms.model.OfferLetter;
import com.mentis.hrms.repository.OfferLetterRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring6.SpringTemplateEngine;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;

@Service
public class OfferService {
    private static final Logger logger = LoggerFactory.getLogger(OfferService.class);

    private final OfferLetterRepository offerLetterRepository;
    private final JobApplicationService jobApplicationService;
    private final SpringTemplateEngine templateEngine;
    private final EmailService emailService;
    private final PdfGenerationService pdfGenerationService;
    private final EmployeeService employeeService;

    @Value("${app.upload.base-path:C:/hrms/uploads}")
    private String basePath;

    @Autowired
    public OfferService(OfferLetterRepository offerLetterRepository,
                        JobApplicationService jobApplicationService,
                        SpringTemplateEngine templateEngine,
                        EmailService emailService,
                        PdfGenerationService pdfGenerationService,
                        EmployeeService employeeService) {
        this.offerLetterRepository = offerLetterRepository;
        this.jobApplicationService = jobApplicationService;
        this.templateEngine = templateEngine;
        this.emailService = emailService;
        this.pdfGenerationService = pdfGenerationService;
        this.employeeService = employeeService;
    }

    // === EXISTING METHODS ===

    public List<OfferLetter> getAllOfferLetters() {
        try {
            return offerLetterRepository.findAllByOrderByCreatedDateDesc();
        } catch (Exception e) {
            logger.error("Error getting all offer letters: {}", e.getMessage());
            return List.of();
        }
    }

    public long getOfferCount() {
        try {
            return offerLetterRepository.count();
        } catch (Exception e) {
            logger.error("Error getting offer count: {}", e.getMessage());
            return 0;
        }
    }

    public long getSentOfferCount() {
        try {
            return offerLetterRepository.countByStatus("SENT");
        } catch (Exception e) {
            logger.error("Error getting sent offer count: {}", e.getMessage());
            return 0;
        }
    }

    public OfferLetter createAndGenerateOffer(OfferRequest request) throws Exception {
        logger.info("Creating offer letter for application ID: {}", request.getApplicationId());

        try {
            JobApplication application = jobApplicationService.getApplicationById(request.getApplicationId());
            if (application == null) {
                throw new RuntimeException("Application not found with ID: " + request.getApplicationId());
            }

            OfferLetter offer = new OfferLetter(application);
            populateOfferData(offer, request);

            offer = offerLetterRepository.save(offer);
            logger.info("Offer letter created with ID: {}", offer.getId());

            String signaturePath = handleSignatureUpload(offer.getId(), request.getSignatureFile());
            String filePath = generateOfferDocument(offer, signaturePath);
            offer.setOfferFilePath(filePath);
            offer.setStatus("GENERATED");

            return offerLetterRepository.save(offer);

        } catch (Exception e) {
            logger.error("Error creating offer: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to create offer: " + e.getMessage());
        }
    }

    private void populateOfferData(OfferLetter offer, OfferRequest request) {
        offer.setDesignation(safeValue(request.getDesignation(), "Not Specified"));
        offer.setDepartment(safeValue(request.getDepartment(), "Not Specified"));
        offer.setJoiningDate(safeValue(request.getJoiningDate(), LocalDate.now().plusDays(14).toString()));
        offer.setWorkLocation(safeValue(request.getWorkLocation(), "Not Specified"));
        offer.setEmploymentType(safeValue(request.getEmploymentType(), "Full-time"));
        offer.setAnnualSalary(safeValue(request.getAnnualSalary(), "To be discussed"));
        offer.setCurrency(safeValue(request.getCurrency(), "USD"));
        offer.setReportingManager(safeValue(request.getReportingManager(), "Not Specified"));
        offer.setProbationPeriod(safeValue(request.getProbationPeriod(), "3 months"));
        offer.setAdditionalNotes(safeValue(request.getAdditionalNotes(), ""));
        offer.setOfferType(safeValue(request.getOfferType(), "OFFER"));
        offer.setStatus("DRAFT");

        if (request.getCandidateName() != null) {
            String cleanName = cleanDuplicateData(request.getCandidateName());
            offer.setCandidateName(cleanName);
            logger.info("POPULATE - Set cleaned name: {}", cleanName);
        } else {
            offer.setCandidateName("Candidate");
        }

        if (request.getCandidateEmail() != null) {
            String cleanEmail = cleanDuplicateData(request.getCandidateEmail());
            cleanEmail = cleanEmail.replaceAll("\\s+", "");
            offer.setCandidateEmail(cleanEmail);
            logger.info("POPULATE - Set cleaned email: {}", cleanEmail);
        } else {
            offer.setCandidateEmail("candidate@example.com");
        }
    }

    private String cleanDuplicateData(String input) {
        if (input == null || input.trim().isEmpty()) {
            return input;
        }
        String trimmed = input.trim();
        if (trimmed.contains(",")) {
            return trimmed.substring(0, trimmed.indexOf(",")).trim();
        }
        String[] parts = trimmed.split("\\s+");
        if (parts.length > 1 && parts[0].equals(parts[1])) {
            return parts[0];
        }
        return trimmed;
    }

    private String safeValue(String value, String defaultValue) {
        return (value != null && !value.trim().isEmpty()) ? value : defaultValue;
    }

    private String handleSignatureUpload(Long offerId, MultipartFile file) throws Exception {
        if (file == null || file.isEmpty()) {
            return null;
        }
        Path signaturesDir = Paths.get(basePath, "signatures");
        Files.createDirectories(signaturesDir);
        String fileName = "signature-" + offerId + ".png";
        Path targetPath = signaturesDir.resolve(fileName);
        try (InputStream is = file.getInputStream()) {
            Files.copy(is, targetPath, StandardCopyOption.REPLACE_EXISTING);
        }
        logger.info("Signature saved: {}", targetPath);
        return "/signatures/" + fileName;
    }

    private String generateOfferDocument(OfferLetter offer, String signaturePath) throws Exception {
        logger.info("Generating offer document for ID: {}", offer.getId());

        // Clean data
        if (offer.getCandidateName() != null) {
            String cleanName = cleanDuplicateData(offer.getCandidateName());
            offer.setCandidateName(cleanName);
        }
        if (offer.getCandidateEmail() != null) {
            String cleanEmail = cleanDuplicateData(offer.getCandidateEmail());
            cleanEmail = cleanEmail.replaceAll("\\s+", "");
            offer.setCandidateEmail(cleanEmail);
        }

        Context context = new Context();
        context.setVariable("offer", offer);
        context.setVariable("today", LocalDateTime.now().format(DateTimeFormatter.ofPattern("MMMM dd, yyyy")));
        context.setVariable("currentYear", LocalDateTime.now().getYear());

        if (signaturePath != null) {
            try {
                Path signatureFilePath = Paths.get(basePath).resolve(signaturePath.substring(1));
                String signatureUrl = "file:///" + signatureFilePath.toString().replace("\\", "/");
                context.setVariable("signature", signatureUrl);
                context.setVariable("showSignature", true);
            } catch (Exception e) {
                logger.warn("Signature processing failed: {}", e.getMessage());
                context.setVariable("showSignature", false);
            }
        } else {
            context.setVariable("showSignature", false);
        }

        // USE THE CLEAN TEMPLATE
        String templateName = "OFFER".equalsIgnoreCase(offer.getOfferType()) ?
                "clean-offer-template" : "appointment-letter-template";

        String htmlContent = templateEngine.process(templateName, context);
        logger.info("Template processed successfully");

        return pdfGenerationService.generateOfferPdf(
                htmlContent,
                offer.getCandidateName(),
                offer.getOfferType(),
                offer.getId()
        );
    }

    /**
     * CORRECTED DELETE METHOD - Use this one
     */
    public void deleteOffer(Long offerId) {
        try {
            logger.info("Attempting to delete offer with id: {}", offerId);

            // Find the offer
            OfferLetter offer = offerLetterRepository.findById(offerId)
                    .orElseThrow(() -> new RuntimeException("Offer not found with id: " + offerId));

            // Optional: Add validation - don't delete if already sent
            if ("SENT".equals(offer.getStatus())) {
                logger.warn("Attempted to delete a sent offer: {}", offerId);
                throw new RuntimeException("Cannot delete an offer that has already been sent to candidate");
            }

            // Delete associated PDF file if it exists - USING CORRECT GETTER
            if (offer.getOfferFilePath() != null && !offer.getOfferFilePath().isEmpty()) {
                try {
                    Path filePath = Paths.get(offer.getOfferFilePath());
                    boolean deleted = Files.deleteIfExists(filePath);
                    if (deleted) {
                        logger.info("Deleted offer file: {}", offer.getOfferFilePath());
                    } else {
                        logger.warn("Offer file not found: {}", offer.getOfferFilePath());
                    }
                } catch (IOException e) {
                    logger.warn("Could not delete offer file: {} - {}", offer.getOfferFilePath(), e.getMessage());
                    // Continue with database deletion even if file delete fails
                }
            }

            // Delete the offer from database
            offerLetterRepository.delete(offer);
            logger.info("Offer deleted successfully with id: {}", offerId);

        } catch (Exception e) {
            logger.error("Error in deleteOffer service: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to delete offer: " + e.getMessage());
        }
    }

    /**
     * SIMPLER ALTERNATIVE - Use this if you don't need file deletion
     */
    /*
    public void deleteOffer(Long offerId) {
        try {
            logger.info("Attempting to delete offer with id: {}", offerId);

            // Check if offer exists
            if (!offerLetterRepository.existsById(offerId)) {
                throw new RuntimeException("Offer not found with id: " + offerId);
            }

            // Simple delete by ID
            offerLetterRepository.deleteById(offerId);

            logger.info("Offer deleted successfully with id: {}", offerId);

        } catch (Exception e) {
            logger.error("Error in deleteOffer service: {}", e.getMessage());
            throw new RuntimeException("Failed to delete offer: " + e.getMessage());
        }
    }
    */

    public OfferLetter createAndGenerateOfferForEmployee(OfferRequest request) throws Exception {
        logger.info("Creating offer for employee ID: {}", request.getEmployeeId());

        Optional<Employee> employeeOpt = employeeService.getEmployeeByEmployeeId(request.getEmployeeId());
        if (employeeOpt.isEmpty()) {
            throw new RuntimeException("Employee not found with ID: " + request.getEmployeeId());
        }

        Employee employee = employeeOpt.get();

        OfferLetter offer = new OfferLetter();
        offer.setEmployeeId(request.getEmployeeId());
        offer.setCandidateName(request.getCandidateName());
        offer.setCandidateEmail(request.getCandidateEmail());
        offer.setDesignation(request.getDesignation());
        offer.setDepartment(request.getDepartment());
        offer.setJoiningDate(request.getJoiningDate());
        offer.setWorkLocation(request.getWorkLocation());
        offer.setEmploymentType(request.getEmploymentType());
        offer.setAnnualSalary(request.getAnnualSalary());
        offer.setCurrency(request.getCurrency());
        offer.setReportingManager(request.getReportingManager());
        offer.setProbationPeriod(request.getProbationPeriod());
        offer.setAdditionalNotes(request.getAdditionalNotes());
        offer.setOfferType(request.getOfferType());
        offer.setStatus("DRAFT");
        offer.setCreatedDate(LocalDateTime.now());

        offer = offerLetterRepository.save(offer);
        logger.info("Offer letter saved with ID: {}", offer.getId());

        String signaturePath = null;
        if (request.getSignatureFile() != null && !request.getSignatureFile().isEmpty()) {
            signaturePath = handleSignatureUpload(offer.getId(), request.getSignatureFile());
        }

        String filePath = generateOfferDocument(offer, signaturePath);
        offer.setOfferFilePath(filePath);
        offer.setStatus("GENERATED");

        return offerLetterRepository.save(offer);
    }

    public Optional<OfferLetter> getOfferById(Long id) {
        return offerLetterRepository.findById(id);
    }

    public List<OfferLetter> getOffersByApplication(Long applicationId) {
        return offerLetterRepository.findByApplicationId(applicationId);
    }

    public OfferLetter createOffer(OfferRequest request) throws Exception {
        logger.info("Creating offer for type: {}", request.getType());

        if (request.getCandidateName() != null) {
            String cleanName = cleanDuplicateData(request.getCandidateName());
            request.setCandidateName(cleanName);
        }
        if (request.getCandidateEmail() != null) {
            String cleanEmail = cleanDuplicateData(request.getCandidateEmail());
            cleanEmail = cleanEmail.replaceAll("\\s+", "");
            request.setCandidateEmail(cleanEmail);
        }

        if ("EMPLOYEE".equalsIgnoreCase(request.getType())) {
            return createAndGenerateOfferForEmployee(request);
        } else {
            return createAndGenerateOffer(request);
        }
    }

    // SIMPLE SAVE METHOD - NO WEB ANNOTATIONS!
    public OfferLetter saveOfferLetter(OfferLetter offer) {
        return offerLetterRepository.save(offer);
    }

    public OfferLetter sendOfferToCandidate(Long offerId) throws Exception {
        logger.info("Sending offer ID: {}", offerId);

        OfferLetter offer = offerLetterRepository.findById(offerId)
                .orElseThrow(() -> new RuntimeException("Offer letter not found: " + offerId));

        if (!"GENERATED".equals(offer.getStatus())) {
            throw new RuntimeException("Offer must be in GENERATED status before sending. Current status: " + offer.getStatus());
        }

        emailService.sendOfferLetter(
                offer.getCandidateEmail(),
                offer.getCandidateName(),
                offer.getOfferFilePath(),
                offer.getOfferType()
        );

        offer.setStatus("SENT");
        offer.setSentDate(LocalDateTime.now());

        return offerLetterRepository.save(offer);
    }
}