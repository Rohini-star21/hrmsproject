package com.mentis.hrms.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.core.io.FileSystemResource;

import jakarta.mail.internet.MimeMessage;
import jakarta.mail.MessagingException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;

import java.io.File;
import java.nio.file.Paths;

@Service
public class EmailService {
    private static final Logger logger = LoggerFactory.getLogger(EmailService.class);

    private final JavaMailSender mailSender;

    @Value("${spring.mail.username}")
    private String fromEmail;

    // ADD THIS: Add basePath for file access
    @Value("${app.upload.base-path:C:/hrms/uploads}")
    private String basePath;

    public EmailService(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    @Async
    public void sendApplicationConfirmation(String toEmail, String candidateName, String jobTitle) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true);

            helper.setFrom(fromEmail);
            helper.setTo(toEmail);
            helper.setSubject("Application Received - " + jobTitle);

            String emailContent = """
                <!DOCTYPE html>
                <html>
                <head>
                    <style>
                        body { font-family: Arial, sans-serif; line-height: 1.6; }
                        .container { max-width: 600px; margin: 0 auto; padding: 20px; }
                        .header { background: #f8f9fa; padding: 20px; text-align: center; }
                        .content { padding: 20px; }
                        .footer { background: #f8f9fa; padding: 10px; text-align: center; font-size: 12px; color: #666; }
                    </style>
                </head>
                <body>
                    <div class="container">
                        <div class="header">
                            <h2>Mentis IT Solutions</h2>
                        </div>
                        <div class="content">
                            <h3>Dear %s,</h3>
                            <p>Thank you for applying for the <strong>%s</strong> position at Mentis IT Solutions.</p>
                            <p>We have received your application and will review it carefully. Our HR team will contact you if your qualifications match our requirements.</p>
                            <p>Best regards,<br>HR Team<br>Mentis IT Solutions</p>
                        </div>
                        <div class="footer">
                            <p>This is an automated message. Please do not reply to this email.</p>
                        </div>
                    </div>
                </body>
                </html>
                """.formatted(candidateName, jobTitle);

            helper.setText(emailContent, true);
            mailSender.send(message);

            logger.info("Application confirmation email sent to: {}", toEmail);
        } catch (MessagingException e) {
            logger.error("Failed to send application confirmation email to {}: {}", toEmail, e.getMessage());
        }
    }

    @Async
    public void sendApplicationNotification(String candidateName, String jobTitle, String candidateEmail) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true);

            helper.setFrom(fromEmail);
            helper.setTo("hr@mentisit.com"); // HR email
            helper.setSubject("New Application Received - " + jobTitle);

            String emailContent = """
                <!DOCTYPE html>
                <html>
                <head>
                    <style>
                        body { font-family: Arial, sans-serif; line-height: 1.6; }
                        .container { max-width: 600px; margin: 0 auto; padding: 20px; }
                        .header { background: #f8f9fa; padding: 20px; text-align: center; }
                        .content { padding: 20px; }
                        .info-box { background: #e3f2fd; padding: 15px; border-radius: 5px; margin: 10px 0; }
                    </style>
                </head>
                <body>
                    <div class="container">
                        <div class="header">
                            <h2>New Job Application</h2>
                        </div>
                        <div class="content">
                            <h3>Application Details:</h3>
                            <div class="info-box">
                                <p><strong>Candidate:</strong> %s</p>
                                <p><strong>Position:</strong> %s</p>
                                <p><strong>Email:</strong> %s</p>
                                <p><strong>Date:</strong> %s</p>
                            </div>
                            <p>Please review this application in the HRMS dashboard.</p>
                        </div>
                    </div>
                </body>
                </html>
                """.formatted(candidateName, jobTitle, candidateEmail, java.time.LocalDateTime.now().toString());

            helper.setText(emailContent, true);
            mailSender.send(message);

            logger.info("Application notification email sent to HR");
        } catch (MessagingException e) {
            logger.error("Failed to send application notification email: {}", e.getMessage());
        }
    }

    @Async
    public void sendOfferLetter(String toEmail, String candidateName, String filePath, String offerType) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true);

            helper.setFrom(fromEmail);
            helper.setTo(toEmail);
            helper.setSubject(offerType + " Letter - Mentis IT Solutions");

            String emailContent = """
                <!DOCTYPE html>
                <html>
                <head>
                    <style>
                        body { font-family: Arial, sans-serif; line-height: 1.6; }
                        .container { max-width: 600px; margin: 0 auto; padding: 20px; }
                        .header { background: #f8f9fa; padding: 20px; text-align: center; }
                        .content { padding: 20px; }
                        .footer { background: #f8f9fa; padding: 10px; text-align: center; font-size: 12px; color: #666; }
                    </style>
                </head>
                <body>
                    <div class="container">
                        <div class="header">
                            <h2>Mentis IT Solutions</h2>
                        </div>
                        <div class="content">
                            <h3>Dear %s,</h3>
                            <p>Congratulations! We are pleased to extend your %s letter.</p>
                            <p>Please find your %s letter attached with this email.</p>
                            <p>If you have any questions, please don't hesitate to contact our HR department.</p>
                            <p>Best regards,<br>HR Team<br>Mentis IT Solutions</p>
                        </div>
                        <div class="footer">
                            <p>This is an automated message. Please do not reply to this email.</p>
                        </div>
                    </div>
                </body>
                </html>
                """.formatted(candidateName, offerType, offerType);

            helper.setText(emailContent, true);

            // Attach the offer letter file (HTML or PDF)
            File file = new File(Paths.get(basePath).resolve(filePath).toString());
            if (file.exists()) {
                FileSystemResource resource = new FileSystemResource(file);

                // Use PDF if available, otherwise HTML
                String pdfFilePath = filePath.replace(".html", ".pdf");
                File pdfFile = new File(Paths.get(basePath).resolve(pdfFilePath).toString());

                if (pdfFile.exists()) {
                    // Attach PDF
                    FileSystemResource pdfResource = new FileSystemResource(pdfFile);
                    helper.addAttachment(offerType + "_Letter.pdf", pdfResource);
                    logger.info("PDF offer letter attached: {}", pdfFilePath);
                } else {
                    // Fallback to HTML
                    helper.addAttachment(offerType + "_Letter.html", resource);
                    logger.info("HTML offer letter attached: {}", filePath);
                }
            }

            mailSender.send(message);

            logger.info("{} letter sent to: {}", offerType, toEmail);
        } catch (MessagingException e) {
            logger.error("Failed to send {} letter to {}: {}", offerType, toEmail, e.getMessage());
        }
    }
}