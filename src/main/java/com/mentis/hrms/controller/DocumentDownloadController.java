package com.mentis.hrms.controller;

import com.mentis.hrms.model.Employee;
import com.mentis.hrms.model.OnboardingDocument;
import com.mentis.hrms.service.DocumentService;
import com.mentis.hrms.service.EmployeeService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Optional;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

@Controller
@RequestMapping("/download")
public class DocumentDownloadController {

    private static final Logger logger = LoggerFactory.getLogger(DocumentDownloadController.class);

    @Autowired
    private DocumentService documentService;

    @Autowired
    private EmployeeService employeeService;

    @Value("${app.upload.base-path:C:/hrms/uploads}")
    private String basePath;

    /* ==================== DOWNLOAD SINGLE DOCUMENT ==================== */
    @GetMapping("/document/{documentId}")
    public ResponseEntity<Resource> downloadDocument(@PathVariable Long documentId) {
        try {
            Optional<OnboardingDocument> documentOpt = documentService.getDocumentRepository().findById(documentId);

            if (documentOpt.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
            }

            OnboardingDocument document = documentOpt.get();
            String filePath = basePath + "/" + document.getFilePath();
            Path path = Paths.get(filePath);

            if (!Files.exists(path)) {
                logger.error("File not found: {}", filePath);
                return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
            }

            Resource resource = new UrlResource(path.toUri());

            if (!resource.exists() || !resource.isReadable()) {
                logger.error("File is not readable: {}", filePath);
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
            }

            // Determine content type
            String contentType = determineContentType(document.getFilePath());

            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType(contentType))
                    .header(HttpHeaders.CONTENT_DISPOSITION,
                            "attachment; filename=\"" + getFileName(document) + "\"")
                    .body(resource);

        } catch (Exception e) {
            logger.error("Error downloading document {}: {}", documentId, e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /* ==================== DOWNLOAD ALL DOCUMENTS FOR EMPLOYEE (ZIP) ==================== */
    @GetMapping("/employee/{employeeId}/all")
    public ResponseEntity<Resource> downloadAllDocuments(@PathVariable String employeeId) {
        try {
            Optional<Employee> employeeOpt = employeeService.getEmployeeByEmployeeId(employeeId);

            if (employeeOpt.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
            }

            Employee employee = employeeOpt.get();
            List<OnboardingDocument> documents = documentService.getDocumentsByEmployee(employee);

            // Filter only documents with files
            List<OnboardingDocument> documentsWithFiles = documents.stream()
                    .filter(doc -> doc.getFilePath() != null && !doc.getFilePath().isEmpty())
                    .toList();

            if (documentsWithFiles.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
            }

            // Create temporary zip file
            String zipFileName = "documents_" + employeeId + "_" + System.currentTimeMillis() + ".zip";
            String tempDir = System.getProperty("java.io.tmpdir");
            File zipFile = new File(tempDir, zipFileName);

            try (FileOutputStream fos = new FileOutputStream(zipFile);
                 ZipOutputStream zos = new ZipOutputStream(fos)) {

                for (OnboardingDocument document : documentsWithFiles) {
                    String filePath = basePath + "/" + document.getFilePath();
                    File file = new File(filePath);

                    if (file.exists()) {
                        try (FileInputStream fis = new FileInputStream(file)) {
                            ZipEntry zipEntry = new ZipEntry(getFileName(document));
                            zos.putNextEntry(zipEntry);

                            byte[] buffer = new byte[1024];
                            int length;
                            while ((length = fis.read(buffer)) > 0) {
                                zos.write(buffer, 0, length);
                            }
                            zos.closeEntry();
                        }
                    }
                }
            }

            Path path = zipFile.toPath();
            Resource resource = new UrlResource(path.toUri());

            // Clean up temp file after download
            zipFile.deleteOnExit();

            return ResponseEntity.ok()
                    .contentType(MediaType.APPLICATION_OCTET_STREAM)
                    .header(HttpHeaders.CONTENT_DISPOSITION,
                            "attachment; filename=\"" + zipFileName + "\"")
                    .header(HttpHeaders.CONTENT_LENGTH, String.valueOf(zipFile.length()))
                    .body(resource);

        } catch (Exception e) {
            logger.error("Error creating zip for employee {}: {}", employeeId, e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /* ==================== HR SIDE: DOWNLOAD ALL DOCUMENTS (FROM ONBOARDING) ==================== */
    @GetMapping("/onboarding/employee/{employeeId}/all")
    public ResponseEntity<Resource> hrDownloadAllDocuments(@PathVariable String employeeId) {
        return downloadAllDocuments(employeeId);
    }

    /* ==================== HELPER METHODS ==================== */
    private String getFileName(OnboardingDocument document) {
        String extension = getFileExtension(document.getFilePath());
        return document.getDocumentType() + "_" +
                document.getEmployee().getEmployeeId() +
                (extension.isEmpty() ? "" : "." + extension);
    }

    private String getFileExtension(String filePath) {
        if (filePath == null || filePath.lastIndexOf(".") == -1) {
            return "";
        }
        return filePath.substring(filePath.lastIndexOf(".") + 1);
    }

    private String determineContentType(String filePath) {
        String extension = getFileExtension(filePath).toLowerCase();

        return switch (extension) {
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