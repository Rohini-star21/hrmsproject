package com.mentis.hrms.service;

import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Base64;
import java.util.concurrent.TimeUnit;

@Service
public class FilePreviewService {

    public boolean isImageFile(String fileName) {
        if (fileName == null) return false;
        String ext = fileName.toLowerCase();
        return ext.endsWith(".jpg") || ext.endsWith(".jpeg") || ext.endsWith(".png");
    }

    public boolean isDocumentFile(String fileName) {
        if (fileName == null) return false;
        String ext = fileName.toLowerCase();
        return ext.endsWith(".doc") || ext.endsWith(".docx") || ext.endsWith(".pdf") || ext.endsWith(".txt");
    }

    public String getFilePreviewHtml(String filePath, Long applicationId) {
        if (filePath == null || filePath.trim().isEmpty()) {
            return getNoFileHtml();
        }

        String fileName = Paths.get(filePath).getFileName().toString();

        if (isImageFile(fileName)) {
            return getImagePreviewHtml(fileName, applicationId);
        } else if (fileName.toLowerCase().endsWith(".pdf")) {
            return getPdfPreviewHtml(fileName, applicationId);
        } else if (fileName.toLowerCase().endsWith(".txt")) {
            return getTextPreviewHtml(filePath, applicationId);
        } else if (fileName.toLowerCase().matches(".*\\.(doc|docx)$")) {
            return getDocumentPreviewHtml(fileName, applicationId);
        } else {
            return getUnsupportedFileHtml(fileName);
        }
    }

    private String getImagePreviewHtml(String fileName, Long applicationId) {
        return String.format("""
            <div class="image-preview-wrapper" style="height: 100%%; display: flex; align-items: center; justify-content: center; background: #f8f9fa;">
                <img src="/dashboard/preview-image/%d" 
                     alt="%s" 
                     style="max-width: 100%%; max-height: 100%%; object-fit: contain; border-radius: 8px; box-shadow: 0 4px 12px rgba(0,0,0,0.1);"
                     onerror="this.onerror=null; this.src='/static/images/file-icon.png';">
            </div>
            """, applicationId, fileName);
    }

    private String getPdfPreviewHtml(String fileName, Long applicationId) {
        return String.format("""
            <div class="pdf-preview-wrapper" style="height: 100%%;">
                <iframe class="resume-preview-iframe"
                        src="/dashboard/preview-resume/%d"
                        type="application/pdf"
                        style="width: 100%%; height: 100%%; border: none;">
                    Your browser does not support PDF preview.
                </iframe>
            </div>
            """, applicationId);
    }

    private String getTextPreviewHtml(String filePath, Long applicationId) {
        try {
            Path path = Paths.get("C:/hrms/uploads").resolve(filePath);
            if (Files.exists(path)) {
                String content = Files.readString(path);
                content = content.replace("<", "&lt;").replace(">", "&gt;");
                return String.format("""
                    <div class="text-preview-wrapper" style="height: 100%%; overflow: auto; padding: 20px; background: #ffffff;">
                        <pre style="font-family: 'Courier New', monospace; font-size: 14px; line-height: 1.5; margin: 0; white-space: pre-wrap;">%s</pre>
                    </div>
                    """, content);
            }
        } catch (IOException e) {
            // Fall through to unsupported file
        }
        return getUnsupportedFileHtml(filePath);
    }

    private String getDocumentPreviewHtml(String fileName, Long applicationId) {
        return String.format("""
            <div class="document-preview-wrapper" style="height: 100%%; display: flex; flex-direction: column; align-items: center; justify-content: center; background: #f8f9fa; padding: 20px;">
                <div style="text-align: center; background: white; padding: 40px; border-radius: 12px; box-shadow: 0 4px 12px rgba(0,0,0,0.1);">
                    <i class="fas fa-file-word" style="font-size: 4rem; color: #2b579a; margin-bottom: 20px;"></i>
                    <h4>%s</h4>
                    <p class="text-muted">Microsoft Word Document</p>
                    <div class="mt-3">
                        <a href="/dashboard/download-resume/%d" class="btn btn-primary">
                            <i class="fas fa-download me-2"></i>Download Document
                        </a>
                    </div>
                    <div class="mt-2">
                        <small class="text-muted">Preview not available for Word documents</small>
                    </div>
                </div>
            </div>
            """, fileName, applicationId);
    }

    private String getUnsupportedFileHtml(String fileName) {
        return String.format("""
            <div class="preview-unavailable">
                <i class="fas fa-file fa-3x mb-3"></i>
                <h4>File Preview Not Available</h4>
                <p>Cannot preview this file type</p>
                <p class="small text-muted">File: %s</p>
                <div class="mt-3">
                    <small class="text-muted">
                        <i class="fas fa-info-circle me-1"></i>
                        Download the file to view its contents
                    </small>
                </div>
            </div>
            """, fileName);
    }

    private String getNoFileHtml() {
        return """
            <div class="preview-unavailable">
                <i class="fas fa-file-upload fa-3x mb-3"></i>
                <h4>No File Uploaded</h4>
                <p>No resume or document has been uploaded for this application</p>
                <div class="mt-3">
                    <small class="text-muted">
                        <i class="fas fa-info-circle me-1"></i>
                        File preview is not available
                    </small>
                </div>
            </div>
            """;
    }
}