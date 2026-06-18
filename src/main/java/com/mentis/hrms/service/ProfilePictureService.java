package com.mentis.hrms.service;

import com.mentis.hrms.model.Employee;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.*;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

@Service
public class ProfilePictureService {

    private static final Logger logger = LoggerFactory.getLogger(ProfilePictureService.class);

    @Value("${profile.pictures.directory:uploads/profile-pictures}")
    private String uploadDir;

    @Autowired
    private EmployeeService employeeService;

    public String uploadProfilePicture(String employeeId, MultipartFile file) throws IOException {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("Please select a file to upload");
        }

        // Validate file type
        String contentType = file.getContentType();
        if (contentType == null ||
                !(contentType.equals("image/jpeg") ||
                        contentType.equals("image/png") ||
                        contentType.equals("image/jpg") ||
                        contentType.equals("image/gif"))) {
            throw new IllegalArgumentException("Only JPG, PNG, and GIF images are allowed");
        }

        // Validate file size (max 5MB)
        if (file.getSize() > 5 * 1024 * 1024) {
            throw new IllegalArgumentException("File size must be less than 5MB");
        }

        Optional<Employee> employeeOpt = employeeService.getEmployeeByEmployeeId(employeeId);
        if (employeeOpt.isEmpty()) {
            throw new IllegalArgumentException("Employee not found: " + employeeId);
        }

        Employee employee = employeeOpt.get();

        // Create upload directory if it doesn't exist
        Path uploadPath = Paths.get(uploadDir);
        if (!Files.exists(uploadPath)) {
            Files.createDirectories(uploadPath);
        }

        // Generate unique filename
        String originalFilename = file.getOriginalFilename();
        String fileExtension = "";
        if (originalFilename != null && originalFilename.contains(".")) {
            fileExtension = originalFilename.substring(originalFilename.lastIndexOf("."));
        } else {
            fileExtension = ".jpg"; // default extension
        }

        String uniqueFilename = employeeId + "_" + UUID.randomUUID().toString() + fileExtension;

        // Save the file
        Path filePath = uploadPath.resolve(uniqueFilename);
        Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);

        // Delete old profile picture if exists
        if (employee.getProfilePicture() != null && !employee.getProfilePicture().isEmpty()) {
            try {
                Path oldFilePath = Paths.get(employee.getProfilePicture());
                if (Files.exists(oldFilePath)) {
                    Files.delete(oldFilePath);
                }
            } catch (Exception e) {
                logger.warn("Could not delete old profile picture: {}", e.getMessage());
            }
        }

        // Update employee record
        String relativePath = uploadDir + "/" + uniqueFilename;
        employee.setProfilePicture(relativePath);
        employee.setUpdatedDate(LocalDateTime.now());
        employeeService.saveEmployee(employee);

        logger.info("Profile picture uploaded for employee {}: {}", employeeId, relativePath);
        return relativePath;
    }

    public boolean deleteProfilePicture(String employeeId) throws IOException {
        Optional<Employee> employeeOpt = employeeService.getEmployeeByEmployeeId(employeeId);
        if (employeeOpt.isEmpty()) {
            throw new IllegalArgumentException("Employee not found: " + employeeId);
        }

        Employee employee = employeeOpt.get();

        if (employee.getProfilePicture() == null || employee.getProfilePicture().isEmpty()) {
            return false;
        }

        // Delete file
        Path filePath = Paths.get(employee.getProfilePicture());
        if (Files.exists(filePath)) {
            Files.delete(filePath);
        }

        // Update employee record
        employee.setProfilePicture(null);
        employee.setUpdatedDate(LocalDateTime.now());
        employeeService.saveEmployee(employee);

        logger.info("Profile picture deleted for employee {}", employeeId);
        return true;
    }

    public byte[] getProfilePicture(String employeeId) throws IOException {
        Optional<Employee> employeeOpt = employeeService.getEmployeeByEmployeeId(employeeId);
        if (employeeOpt.isEmpty()) {
            throw new IllegalArgumentException("Employee not found: " + employeeId);
        }

        Employee employee = employeeOpt.get();

        if (employee.getProfilePicture() == null || employee.getProfilePicture().isEmpty()) {
            return null;
        }

        Path filePath = Paths.get(employee.getProfilePicture());
        if (!Files.exists(filePath)) {
            return null;
        }

        return Files.readAllBytes(filePath);
    }
}