package com.mentis.hrms.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "onboarding_documents")
public class OnboardingDocument {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER) // CHANGE FROM LAZY TO EAGER
    @JoinColumn(name = "employee_id")
    private Employee employee;

    private String documentType;
    private String documentName;
    private String filePath;
    private String status = "PENDING"; // PENDING, SUBMITTED, VERIFIED, REJECTED
    private String verificationNotes;

    private LocalDateTime submittedDate;
    private LocalDateTime verifiedDate;
    private String verifiedBy;

    private boolean mandatory = true;
    private Integer documentOrder = 0;




    @Column(length = 1000)
    private String description;

    // Constructors
    public OnboardingDocument() {}

    // Getters and Setters (generate all)
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    /*  notes field already exists – just the accessors were missing  */
    public Employee getEmployee() { return employee; }
    public void setEmployee(Employee employee) { this.employee = employee; }

    public String getDocumentType() { return documentType; }
    public void setDocumentType(String documentType) { this.documentType = documentType; }

    public String getDocumentName() { return documentName; }
    public void setDocumentName(String documentName) { this.documentName = documentName; }

    public String getFilePath() { return filePath; }
    public void setFilePath(String filePath) { this.filePath = filePath; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getVerificationNotes() { return verificationNotes; }
    public void setVerificationNotes(String verificationNotes) { this.verificationNotes = verificationNotes; }

    public LocalDateTime getSubmittedDate() { return submittedDate; }
    public void setSubmittedDate(LocalDateTime submittedDate) { this.submittedDate = submittedDate; }

    public LocalDateTime getVerifiedDate() { return verifiedDate; }
    public void setVerifiedDate(LocalDateTime verifiedDate) { this.verifiedDate = verifiedDate; }

    public String getVerifiedBy() { return verifiedBy; }
    public void setVerifiedBy(String verifiedBy) { this.verifiedBy = verifiedBy; }

    public boolean isMandatory() { return mandatory; }
    public void setMandatory(boolean mandatory) { this.mandatory = mandatory; }

    public Integer getDocumentOrder() { return documentOrder; }
    public void setDocumentOrder(Integer documentOrder) { this.documentOrder = documentOrder; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
}