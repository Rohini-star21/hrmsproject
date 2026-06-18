package com.mentis.hrms.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "offer_letters")
public class OfferLetter {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "application_id")
    private JobApplication application;

    // ADD THIS: Employee ID field for employee-based offers
    @Column(name = "employee_id")
    private String employeeId;

    private String candidateName;
    private String candidateEmail;
    private String designation;
    private String department;
    private String joiningDate;
    private String workLocation;
    private String employmentType;
    private String annualSalary;
    private String currency;
    private String reportingManager;
    private String probationPeriod;

    @Column(length = 2000)
    private String additionalNotes;

    private String offerType; // OFFER or APPOINTMENT
    private String status; // DRAFT, GENERATED, SENT, SIGNED

    @Column(name = "offer_file_path")
    private String offerFilePath;

    @Column(name = "signed_file_path")
    private String signedFilePath;

    @Column(name = "signature_data_uri", columnDefinition = "TEXT")
    private String signatureDataUri;

    @Column(name = "created_date")
    private LocalDateTime createdDate;

    @Column(name = "sent_date")
    private LocalDateTime sentDate;

    @Column(name = "signed_date")
    private LocalDateTime signedDate;

    // Constructors
    public OfferLetter() {
        this.createdDate = LocalDateTime.now();
        this.status = "DRAFT";
    }

    public OfferLetter(JobApplication application) {
        this();
        this.application = application;
        this.candidateName = application.getFirstName() + " " + application.getLastName();
        this.candidateEmail = application.getEmail();
        this.offerType = "OFFER";
    }

    // ADD THIS: Constructor for employee offers
    public OfferLetter(String employeeId, String candidateName, String candidateEmail) {
        this();
        this.employeeId = employeeId;
        this.candidateName = candidateName;
        this.candidateEmail = candidateEmail;
        this.offerType = "OFFER";
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public JobApplication getApplication() { return application; }
    public void setApplication(JobApplication application) { this.application = application; }

    // ADD THIS: Employee ID getter and setter
    public String getEmployeeId() { return employeeId; }
    public void setEmployeeId(String employeeId) { this.employeeId = employeeId; }

    public String getCandidateName() { return candidateName; }
    public void setCandidateName(String candidateName) { this.candidateName = candidateName; }

    public String getCandidateEmail() { return candidateEmail; }
    public void setCandidateEmail(String candidateEmail) {
        this.candidateEmail = candidateEmail;  // Fix this line
    }
    public String getDesignation() { return designation; }
    public void setDesignation(String designation) { this.designation = designation; }

    public String getDepartment() { return department; }
    public void setDepartment(String department) { this.department = department; }

    public String getJoiningDate() { return joiningDate; }
    public void setJoiningDate(String joiningDate) { this.joiningDate = joiningDate; }

    public String getWorkLocation() { return workLocation; }
    public void setWorkLocation(String workLocation) { this.workLocation = workLocation; }

    public String getEmploymentType() { return employmentType; }
    public void setEmploymentType(String employmentType) { this.employmentType = employmentType; }

    public String getAnnualSalary() { return annualSalary; }
    public void setAnnualSalary(String annualSalary) { this.annualSalary = annualSalary; }

    public String getCurrency() { return currency; }
    public void setCurrency(String currency) { this.currency = currency; }

    public String getReportingManager() { return reportingManager; }
    public void setReportingManager(String reportingManager) { this.reportingManager = reportingManager; }

    public String getProbationPeriod() { return probationPeriod; }
    public void setProbationPeriod(String probationPeriod) { this.probationPeriod = probationPeriod; }

    public String getAdditionalNotes() { return additionalNotes; }
    public void setAdditionalNotes(String additionalNotes) { this.additionalNotes = additionalNotes; }

    public String getOfferType() { return offerType; }
    public void setOfferType(String offerType) { this.offerType = offerType; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getOfferFilePath() { return offerFilePath; }
    public void setOfferFilePath(String offerFilePath) { this.offerFilePath = offerFilePath; }

    public String getSignedFilePath() { return signedFilePath; }
    public void setSignedFilePath(String signedFilePath) { this.signedFilePath = signedFilePath; }

    public String getSignatureDataUri() { return signatureDataUri; }
    public void setSignatureDataUri(String signatureDataUri) { this.signatureDataUri = signatureDataUri; }

    public LocalDateTime getCreatedDate() { return createdDate; }
    public void setCreatedDate(LocalDateTime createdDate) { this.createdDate = createdDate; }

    public LocalDateTime getSentDate() { return sentDate; }
    public void setSentDate(LocalDateTime sentDate) { this.sentDate = sentDate; }

    public LocalDateTime getSignedDate() { return signedDate; }
    public void setSignedDate(LocalDateTime signedDate) { this.signedDate = signedDate; }

    // ADD THIS: Helper method to check if offer is for employee
    public boolean isEmployeeOffer() {
        return employeeId != null && !employeeId.trim().isEmpty();
    }
}