package com.mentis.hrms.dto;

import org.springframework.web.multipart.MultipartFile;

public class OfferRequest {
    private Long applicationId;
    private String employeeId; // ADD THIS: For employee offers
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
    private String additionalNotes;
    private String offerType; // OFFER or APPOINTMENT

    // NEW: Signature file field
    private MultipartFile signatureFile;

    // ADD THIS: To track the source type
    private String type; // "APPLICATION" or "EMPLOYEE"

    // Default constructor
    public OfferRequest() {}

    // Getters and Setters
    public Long getApplicationId() { return applicationId; }
    public void setApplicationId(Long applicationId) { this.applicationId = applicationId; }

    // ADD THIS: Employee ID getter and setter
    public String getEmployeeId() { return employeeId; }
    public void setEmployeeId(String employeeId) { this.employeeId = employeeId; }

    public String getCandidateName() { return candidateName; }
    public void setCandidateName(String candidateName) { this.candidateName = candidateName; }

    public String getCandidateEmail() { return candidateEmail; }
    public void setCandidateEmail(String candidateEmail) { this.candidateEmail = candidateEmail; }

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

    // NEW: Signature file getter and setter
    public MultipartFile getSignatureFile() { return signatureFile; }
    public void setSignatureFile(MultipartFile signatureFile) { this.signatureFile = signatureFile; }

    // ADD THIS: Type getter and setter
    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
}