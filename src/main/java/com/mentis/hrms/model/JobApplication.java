package com.mentis.hrms.model;

import jakarta.persistence.*;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Objects;

/**
 * JobApplication entity represents a candidate's application for a specific job position.
 * This entity maintains all application-related information including personal details,
 * application status, and relationship to the job posting.
 *
 * @author Menti's IT Solutions
 * @version 2.0
 */
@Entity
@Table(name = "job_applications")
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class JobApplication {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "first_name", nullable = false, length = 100)
    private String firstName;

    @Column(name = "last_name", nullable = false, length = 100)
    private String lastName;

    @Column(name = "email", nullable = false, length = 255)
    private String email;

    @Column(name = "phone", length = 20)
    private String phone;

    @Column(name = "linkedin_profile", length = 500)
    private String linkedinProfile;

    @Column(name = "cover_letter", length = 2000)
    private String coverLetter;

    @Column(name = "resume_path", length = 500)
    private String resumePath;

    @Column(name = "application_date", nullable = false)
    private LocalDateTime applicationDate;

    @Column(name = "experience", length = 50)
    private String experience;

    @Column(name = "status", nullable = false, length = 50)
    private String status;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "job_id", nullable = false)
    @JsonIgnoreProperties({"applications", "requirementList", "responsibilities"})
    private Job job;

    // ==================== CONSTRUCTORS ====================

    /**
     * Default constructor that initializes the application with default values.
     * Sets the application date to current timestamp and status to "Applied".
     */
    public JobApplication() {
        this.applicationDate = LocalDateTime.now();
        this.status = "Applied";
        this.firstName = "";
        this.lastName = "";
    }

    /**
     * Parameterized constructor for creating a job application with basic information.
     *
     * @param firstName  The candidate's first name
     * @param lastName   The candidate's last name
     * @param email      The candidate's email address
     * @param phone      The candidate's phone number
     * @param experience The candidate's years of experience
     */
    public JobApplication(String firstName, String lastName, String email, String phone, String experience) {
        this();
        this.firstName = firstName != null ? firstName : "";
        this.lastName = lastName != null ? lastName : "";
        this.email = email;
        this.phone = phone;
        this.experience = experience;
    }

    // ==================== GETTERS AND SETTERS ====================

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName != null ? firstName.trim() : "";
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName != null ? lastName.trim() : "";
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email != null ? email.trim().toLowerCase() : null;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone != null ? phone.trim() : null;
    }

    public String getLinkedinProfile() {
        return linkedinProfile;
    }

    public void setLinkedinProfile(String linkedinProfile) {
        this.linkedinProfile = linkedinProfile != null ? linkedinProfile.trim() : null;
    }

    public String getCoverLetter() {
        return coverLetter;
    }

    public void setCoverLetter(String coverLetter) {
        this.coverLetter = coverLetter;
    }

    public String getResumePath() {
        return resumePath;
    }

    public void setResumePath(String resumePath) {
        this.resumePath = resumePath;
    }

    public LocalDateTime getApplicationDate() {
        return applicationDate;
    }

    public void setApplicationDate(LocalDateTime applicationDate) {
        this.applicationDate = applicationDate != null ? applicationDate : LocalDateTime.now();
    }

    public String getExperience() {
        return experience;
    }

    public void setExperience(String experience) {
        this.experience = experience;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status != null ? status : "Applied";
    }

    public Job getJob() {
        return job;
    }

    public void setJob(Job job) {
        this.job = job;
    }

    // ==================== BUSINESS LOGIC METHODS ====================

    /**
     * Returns the candidate's full name by combining first and last names.
     *
     * @return The full name of the candidate
     */
    public String getFullName() {
        String first = firstName != null ? firstName.trim() : "";
        String last = lastName != null ? lastName.trim() : "";

        if (first.isEmpty() && last.isEmpty()) {
            return "Unknown Candidate";
        }

        return (first + " " + last).trim();
    }

    /**
     * Returns a formatted application date string in a user-friendly format.
     * Format: "MMM dd, yyyy HH:mm" (e.g., "Oct 24, 2025 08:05")
     *
     * @return Formatted application date string
     */
    public String getFormattedApplicationDate() {
        if (applicationDate == null) {
            return "Date not available";
        }

        try {
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MMM dd, yyyy HH:mm");
            return applicationDate.format(formatter);
        } catch (Exception e) {
            return "Invalid date format";
        }
    }

    /**
     * Returns a display-friendly application date string.
     * This method provides consistent date formatting across the application.
     *
     * @return Display-formatted application date string
     */
    public String getDisplayApplicationDate() {
        if (applicationDate == null) {
            return "Date not available";
        }

        try {
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MMM dd, yyyy HH:mm");
            return applicationDate.format(formatter);
        } catch (Exception e) {
            return "Invalid date";
        }
    }

    /**
     * Returns a detailed application date string with day of week.
     * Format: "EEEE, MMMM dd, yyyy 'at' hh:mm a"
     *
     * @return Detailed formatted application date string
     */
    public String getDetailedApplicationDate() {
        if (applicationDate == null) {
            return "Date not available";
        }

        try {
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("EEEE, MMMM dd, yyyy 'at' hh:mm a");
            return applicationDate.format(formatter);
        } catch (Exception e) {
            return "Invalid date format";
        }
    }

    /**
     * Returns the job title associated with this application.
     *
     * @return Job title or "Position Not Found" if job is null
     */
    public String getJobTitle() {
        return job != null && job.getTitle() != null ? job.getTitle() : "Position Not Found";
    }

    /**
     * Returns the department associated with this application's job.
     *
     * @return Department name or "N/A" if not available
     */
    public String getJobDepartment() {
        return job != null && job.getDepartment() != null ? job.getDepartment() : "N/A";
    }

    /**
     * Returns the job type associated with this application.
     *
     * @return Job type or "N/A" if not available
     */
    public String getJobType() {
        return job != null && job.getJobType() != null ? job.getJobType() : "N/A";
    }

    /**
     * Returns the job location associated with this application.
     *
     * @return Job location or "N/A" if not available
     */
    public String getJobLocation() {
        return job != null && job.getLocation() != null ? job.getLocation() : "N/A";
    }

    /**
     * Checks if this application has an associated job.
     *
     * @return true if job exists and has an ID, false otherwise
     */
    public boolean hasJob() {
        return job != null && job.getId() != null;
    }

    /**
     * Returns the Bootstrap color class for the status badge based on application status.
     *
     * @return CSS class name for status badge styling
     */
    public String getStatusColor() {
        if (status == null) {
            return "secondary";
        }

        switch (status.toLowerCase()) {
            case "applied":
                return "secondary";
            case "in review":
                return "info";
            case "interview":
                return "warning";
            case "interviewed":
                return "warning";
            case "on hold":
                return "info";
            case "hired":
                return "success";
            case "rejected":
                return "danger";
            default:
                return "secondary";
        }
    }

    /**
     * Returns a shortened version of the cover letter for preview purposes.
     *
     * @return Abbreviated cover letter (first 100 characters) or default message
     */
    public String getShortCoverLetter() {
        if (coverLetter == null || coverLetter.trim().isEmpty()) {
            return "No cover letter provided";
        }

        return coverLetter.length() > 100 ? coverLetter.substring(0, 100) + "..." : coverLetter;
    }

    /**
     * Validates if the application has all required fields populated.
     *
     * @return true if all required fields are valid, false otherwise
     */
    public boolean isValid() {
        return firstName != null && !firstName.trim().isEmpty() &&
                lastName != null && !lastName.trim().isEmpty() &&
                email != null && !email.trim().isEmpty() &&
                phone != null && !phone.trim().isEmpty() &&
                experience != null && !experience.trim().isEmpty() &&
                job != null;
    }

    /**
     * Returns the ID of the associated job.
     *
     * @return Job ID or null if job is not set
     */
    public Long getJobId() {
        return job != null ? job.getId() : null;
    }

    /**
     * Checks if the associated job is currently active.
     *
     * @return true if job exists and is active, false otherwise
     */
    public boolean isJobActive() {
        return job != null && job.isActive();
    }

    /**
     * Returns the candidate's initials for display in avatar components.
     *
     * @return Two-letter initials (e.g., "JD" for John Doe)
     */
    public String getInitials() {
        String firstInitial = firstName != null && !firstName.isEmpty() ?
                firstName.substring(0, 1).toUpperCase() : "U";
        String lastInitial = lastName != null && !lastName.isEmpty() ?
                lastName.substring(0, 1).toUpperCase() : "C";
        return firstInitial + lastInitial;
    }

    /**
     * Returns the file extension of the uploaded resume.
     *
     * @return File extension (e.g., "pdf", "docx") or "unknown" if not available
     */
    public String getResumeFileExtension() {
        if (resumePath == null || resumePath.isEmpty()) {
            return "unknown";
        }

        int lastDot = resumePath.lastIndexOf('.');
        if (lastDot > 0 && lastDot < resumePath.length() - 1) {
            return resumePath.substring(lastDot + 1).toLowerCase();
        }

        return "unknown";
    }

    /**
     * Returns a user-friendly resume file type description.
     *
     * @return File type description (e.g., "PDF Document", "Word Document")
     */
    public String getResumeFileType() {
        String extension = getResumeFileExtension();

        switch (extension.toLowerCase()) {
            case "pdf":
                return "PDF Document";
            case "doc":
            case "docx":
                return "Word Document";
            case "txt":
                return "Text Document";
            default:
                return "Document";
        }
    }

    /**
     * Checks if the application date is within the last specified number of days.
     *
     * @param days Number of days to check against
     * @return true if application is within the specified timeframe
     */
    public boolean isRecentApplication(int days) {
        if (applicationDate == null) {
            return false;
        }

        LocalDateTime cutoffDate = LocalDateTime.now().minusDays(days);
        return applicationDate.isAfter(cutoffDate);
    }

    // ==================== OVERRIDE METHODS ====================

    @Override
    public String toString() {
        return "JobApplication{" +
                "id=" + id +
                ", firstName='" + firstName + '\'' +
                ", lastName='" + lastName + '\'' +
                ", email='" + email + '\'' +
                ", job=" + (job != null ? "Job{id=" + job.getId() + ", title='" + job.getTitle() + "'}" : "null") +
                ", applicationDate=" + applicationDate +
                ", status='" + status + '\'' +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        JobApplication that = (JobApplication) o;

        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    // ==================== PRE-PERSIST AND PRE-UPDATE HOOKS ====================

    /**
     * JPA callback method executed before persisting a new entity.
     * Ensures that essential fields have valid default values.
     */
    @PrePersist
    protected void onCreate() {
        if (applicationDate == null) {
            applicationDate = LocalDateTime.now();
        }

        if (status == null || status.trim().isEmpty()) {
            status = "Applied";
        }

        if (firstName == null) {
            firstName = "";
        }

        if (lastName == null) {
            lastName = "";
        }
    }

    /**
     * JPA callback method executed before updating an existing entity.
     * Ensures data consistency during updates.
     */
    @PreUpdate
    protected void onUpdate() {
        if (status == null || status.trim().isEmpty()) {
            status = "Applied";
        }

        if (firstName == null) {
            firstName = "";
        }

        if (lastName == null) {
            lastName = "";
        }
    }
}