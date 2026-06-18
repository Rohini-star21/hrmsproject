package com.mentis.hrms.seeders;

import com.mentis.hrms.model.DocumentChecklist;
import com.mentis.hrms.repository.DocumentChecklistRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.List;

@Component
@Transactional
public class DocumentChecklistSeeder implements CommandLineRunner {

    private static final Logger logger = LoggerFactory.getLogger(DocumentChecklistSeeder.class);

    @Autowired
    private DocumentChecklistRepository documentChecklistRepository;

    @Override
    public void run(String... args) throws Exception {
        seedDocumentChecklist();
    }

    private void seedDocumentChecklist() {
        long count = documentChecklistRepository.count();

        if (count == 0) {
            logger.info("📄 Initializing document checklist...");

            List<DocumentChecklist> defaultChecklist = Arrays.asList(
                    // PERSONAL INFORMATION DOCUMENTS
                    createDocument("PAN_CARD", "PAN Card", "IDENTITY", true, 1,
                            "Permanent Account Number card issued by Income Tax Department",
                            "jpg,jpeg,png,pdf", 2097152L), // 2MB

                    createDocument("AADHAAR_CARD", "Aadhaar Card", "IDENTITY", true, 2,
                            "Aadhaar card with 12-digit unique identity number",
                            "jpg,jpeg,png,pdf", 2097152L), // 2MB

                    createDocument("PASSPORT", "Passport", "IDENTITY", false, 3,
                            "Valid passport for international identification",
                            "jpg,jpeg,png,pdf", 5242880L), // 5MB

                    createDocument("DRIVING_LICENSE", "Driving License", "IDENTITY", false, 4,
                            "Valid driving license",
                            "jpg,jpeg,png,pdf", 2097152L), // 2MB

                    createDocument("VOTER_ID", "Voter ID Card", "IDENTITY", false, 5,
                            "Voter identification card",
                            "jpg,jpeg,png,pdf", 2097152L), // 2MB

                    // EPMPLOYMENT DOCUMENTS
                    createDocument("OFFER_LETTER", "Signed Offer Letter", "OFFER", true, 6,
                            "Signed and accepted copy of offer letter",
                            "pdf,jpg,jpeg,png", 5242880L), // 5MB

                    createDocument("APPOINTMENT_LETTER", "Appointment Letter", "OFFER", true, 7,
                            "Official appointment letter from HR",
                            "pdf", 5242880L), // 5MB

                    createDocument("RELIEVING_LETTER", "Previous Relieving Letter", "PROFESSIONAL", true, 8,
                            "Relieving letter from previous employer",
                            "pdf,jpg,jpeg,png", 5242880L), // 5MB

                    createDocument("EXPERIENCE_LETTERS", "Experience Certificates", "PROFESSIONAL", true, 9,
                            "Experience certificates from previous employers",
                            "pdf,jpg,jpeg,png", 10485760L), // 10MB

                    // EDUCATIONAL DOCUMENTS
                    createDocument("TENTH_MARKSHEET", "10th Marksheet", "EDUCATIONAL", true, 10,
                            "Secondary school (10th) marksheet/certificate",
                            "pdf,jpg,jpeg,png", 5242880L), // 5MB

                    createDocument("TWELFTH_MARKSHEET", "12th Marksheet", "EDUCATIONAL", true, 11,
                            "Higher secondary (12th) marksheet/certificate",
                            "pdf,jpg,jpeg,png", 5242880L), // 5MB

                    createDocument("DEGREE_CERTIFICATE", "Degree Certificate", "EDUCATIONAL", true, 12,
                            "Graduation degree certificate",
                            "pdf,jpg,jpeg,png", 5242880L), // 5MB

                    createDocument("POST_GRADUATION", "Post Graduation Certificate", "EDUCATIONAL", false, 13,
                            "Post graduation degree/diploma certificate",
                            "pdf,jpg,jpeg,png", 5242880L), // 5MB

                    // FINANCIAL DOCUMENTS
                    createDocument("BANK_ACCOUNT", "Bank Account Details", "FINANCIAL", true, 14,
                            "Cancelled cheque or bank passbook copy",
                            "jpg,jpeg,png,pdf", 2097152L), // 2MB

                    createDocument("PAN_CARD_COPY", "PAN Card Copy for Finance", "FINANCIAL", true, 15,
                            "PAN card copy for salary processing",
                            "jpg,jpeg,png,pdf", 2097152L), // 2MB

                    // PERSONAL DOCUMENTS
                    createDocument("PASSPORT_PHOTO", "Passport Size Photograph", "PERSONAL", true, 16,
                            "Recent passport size photograph (white background)",
                            "jpg,jpeg,png", 2097152L), // 2MB

                    createDocument("SIGNATURE", "Digital Signature", "PERSONAL", true, 17,
                            "Scanned copy of signature",
                            "jpg,jpeg,png", 1048576L), // 1MB

                    // MEDICAL DOCUMENTS
                    createDocument("MEDICAL_CERTIFICATE", "Medical Fitness Certificate", "MEDICAL", false, 18,
                            "Medical fitness certificate from registered practitioner",
                            "pdf,jpg,jpeg,png", 5242880L), // 5MB

                    // ADDITIONAL DOCUMENTS
                    createDocument("NDA", "Non-Disclosure Agreement", "LEGAL", true, 19,
                            "Signed Non-Disclosure Agreement",
                            "pdf", 5242880L), // 5MB

                    createDocument("POLICY_ACKNOWLEDGMENT", "Company Policy Acknowledgment", "LEGAL", true, 20,
                            "Signed acknowledgment of company policies",
                            "pdf", 5242880L) // 5MB
            );

            documentChecklistRepository.saveAll(defaultChecklist);
            logger.info("✅ Document checklist initialized with {} default documents", defaultChecklist.size());
        } else {
            logger.info("📄 Document checklist already exists with {} items", count);
        }
    }

    // Also update the createChecklistItem method:
    private DocumentChecklist createChecklistItem(String type, String name, String category,
                                                  boolean mandatory, int order, String description,
                                                  String formats, Long maxSize) {
        DocumentChecklist item = new DocumentChecklist();
        item.setDocumentType(type);
        item.setDocumentName(name);
        item.setCategory(category);
        item.setMandatory(mandatory);
        item.setDisplayOrder(order);
        item.setDescription(description);
        item.setFileFormats(formats);
        item.setMaxFileSize(maxSize != null ? maxSize : 5242880L); // Default 5MB
        return item;
    }

    private DocumentChecklist createDocument(String type, String name, String category,
                                             boolean mandatory, int order,
                                             String description, String formats, Long maxSize) {
        DocumentChecklist document = new DocumentChecklist();
        document.setDocumentType(type);
        document.setDocumentName(name);
        document.setCategory(category);
        document.setMandatory(mandatory);
        document.setDisplayOrder(order);
        document.setDescription(description);
        document.setFileFormats(formats);
        document.setMaxFileSize(maxSize != null ? maxSize : 5242880L); // Default 5MB
        return document;
    }
}