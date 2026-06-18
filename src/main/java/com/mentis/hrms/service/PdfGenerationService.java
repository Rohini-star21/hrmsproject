package com.mentis.hrms.service;

import com.itextpdf.html2pdf.HtmlConverter;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.Paragraph;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.FileOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Service
public class PdfGenerationService {
    private static final Logger logger = LoggerFactory.getLogger(PdfGenerationService.class);

    @Value("${app.upload.base-path:C:/hrms/uploads}")
    private String basePath;

    public String generateOfferPdf(String htmlContent, String candidateName, String offerType, Long offerId) throws Exception {
        logger.info("Generating PDF for offer ID: {}", offerId);

        try {
            // Create offers directory if not exists
            Path offersDir = Paths.get(basePath, "offers");
            Files.createDirectories(offersDir);

            // Generate file name
            String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
            String safeName = candidateName.replaceAll("[^a-zA-Z0-9.-]", "_");
            String fileName = String.format("%s_%s_%d_%s.pdf",
                    safeName, offerType, offerId, timestamp);

            Path pdfPath = offersDir.resolve(fileName);

            // Convert HTML to PDF
            try (FileOutputStream fos = new FileOutputStream(pdfPath.toFile())) {
                HtmlConverter.convertToPdf(htmlContent, fos);
                logger.info("PDF generated successfully: {}", pdfPath);
            } catch (Exception e) {
                logger.error("HTML to PDF conversion failed: {}", e.getMessage());

                // Fallback to simple PDF if HTML conversion fails
                return generateSimplePdf(candidateName, offerType, offerId, htmlContent);
            }

            // Save HTML backup (optional)
            try {
                Path htmlPath = offersDir.resolve(fileName.replace(".pdf", ".html"));
                Files.write(htmlPath, htmlContent.getBytes());
            } catch (Exception e) {
                logger.warn("Could not save HTML backup: {}", e.getMessage());
            }

            return "offers/" + fileName;

        } catch (Exception e) {
            logger.error("Error generating PDF: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to generate PDF: " + e.getMessage());
        }
    }

    private String generateSimplePdf(String candidateName, String offerType, Long offerId, String htmlContent) throws Exception {
        logger.info("Generating simple PDF for offer ID: {}", offerId);

        Path offersDir = Paths.get(basePath, "offers");
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
        String safeName = candidateName.replaceAll("[^a-zA-Z0-9.-]", "_");
        String fileName = String.format("%s_%s_%d_%s_simple.pdf",
                safeName, offerType, offerId, timestamp);
        Path pdfPath = offersDir.resolve(fileName);

        try (PdfWriter writer = new PdfWriter(pdfPath.toFile());
             PdfDocument pdf = new PdfDocument(writer);
             Document document = new Document(pdf)) {

            // Extract text from HTML (simple approach)
            String textContent = htmlContent.replaceAll("<[^>]*>", " ")
                    .replaceAll("\\s+", " ")
                    .trim();

            document.add(new Paragraph("OFFER LETTER")
                    .setFontSize(20)
                    .setBold());
            document.add(new Paragraph("\n"));
            document.add(new Paragraph("Candidate: " + candidateName));
            document.add(new Paragraph("Date: " + LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd MMMM yyyy"))));
            document.add(new Paragraph("\n"));

            // Add extracted content (limit to avoid huge documents)
            if (textContent.length() > 2000) {
                document.add(new Paragraph(textContent.substring(0, 2000) + "..."));
            } else {
                document.add(new Paragraph(textContent));
            }
        }

        logger.info("Simple PDF generated: {}", pdfPath);
        return "offers/" + fileName;
    }
}