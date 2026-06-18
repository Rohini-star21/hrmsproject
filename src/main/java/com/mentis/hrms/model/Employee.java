package com.mentis.hrms.model;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import com.mentis.hrms.model.enums.UserRole;

@Entity
@Table(name = "employees")
public class Employee {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // ============================================
    // BASIC INFORMATION
    // ============================================

    @Column(name = "employee_id", nullable = false, unique = true)
    private String employeeId;

    @Column(name = "employee_name")
    private String employeeName;

    @Column(name = "first_name")
    private String firstName;

    @Column(name = "last_name")
    private String lastName;

    @Column(name = "personal_email")
    private String personalEmail;

    @Column(name = "email", nullable = false)
    private String email;

    @Column(name = "phone")
    private String phone;

    // ✅ FIXED: Changed from LocalDateTime to LocalDate to match EmployeeFormDTO
    // Database column is DATETIME, but we only store date part
    @Column(name = "date_of_birth")
    private LocalDate dateOfBirth;

    @Column(name = "gender")
    private String gender;

    // ============================================
    // LEGACY ADDRESS FIELDS (Backward Compatibility)
    // ============================================

    @Column(name = "address")
    private String address;

    @Column(name = "city")
    private String city;

    @Column(name = "state")
    private String state;

    @Column(name = "zip_code")
    private String zipCode;

    @Column(name = "country")
    private String country;

    // ============================================
    // PERMANENT ADDRESS FIELDS
    // ============================================

    @Column(name = "permanent_address")
    private String permanentAddress;

    @Column(name = "permanent_city")
    private String permanentCity;

    @Column(name = "permanent_state")
    private String permanentState;

    @Column(name = "permanent_zip_code")
    private String permanentZipCode;

    @Column(name = "permanent_country")
    private String permanentCountry;

    // ============================================
    // DOCUMENT DEADLINE FIELDS
    // ============================================

    @Column(name = "document_deadline")
    private LocalDateTime documentDeadline;

    @Column(name = "deadline_warning_sent", nullable = false, columnDefinition = "BOOLEAN DEFAULT FALSE")
    private boolean deadlineWarningSent = false;

    @Column(name = "deadline_final_sent", nullable = false, columnDefinition = "BOOLEAN DEFAULT FALSE")
    private boolean deadlineFinalSent = false;

    // ============================================
    // RESIDENTIAL ADDRESS FIELDS
    // ============================================

    @Column(name = "residential_address")
    private String residentialAddress;

    @Column(name = "residential_city")
    private String residentialCity;

    @Column(name = "residential_state")
    private String residentialState;

    @Column(name = "residential_zip_code")
    private String residentialZipCode;

    @Column(name = "residential_country")
    private String residentialCountry;

    // ============================================
    // ADDRESS TYPE (NEW FIELD - was missing)
    // ============================================

    @Column(name = "address_type", length = 20)
    private String addressType;  // "same" or "different"

    // ============================================
    // PRESENCE STATUS FIELDS
    // ============================================

    @Column(name = "presence_status", length = 20)
    private String presenceStatus = "OFFLINE";

    @Column(name = "last_presence_update")
    private LocalDateTime lastPresenceUpdate;

    // ============================================
    // ROLE FIELD
    // ============================================

    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false, length = 20)
    private UserRole role = UserRole.EMPLOYEE;

    // ============================================
    // JOB INFORMATION
    // ============================================

    @Column(name = "department")
    private String department;

    @Column(name = "designation")
    private String designation;

    @Column(name = "work_location")
    private String workLocation;

    @Column(name = "employment_type")
    private String employmentType;

    @Column(name = "work_type")
    private String workType;

    // ✅ FIXED: Changed from LocalDateTime to LocalDate to match EmployeeFormDTO
    @Column(name = "start_date")
    private LocalDate startDate;

    @Column(name = "manager")
    private String manager;

    // ============================================
    // COMPENSATION
    // ============================================

    @Column(name = "salary")
    private String salary;

    @Column(name = "pay_frequency")
    private String payFrequency;

    @Column(name = "currency")
    private String currency;

    // ============================================
    // BENEFITS
    // ============================================

    @ElementCollection
    @CollectionTable(name = "employee_benefits", joinColumns = @JoinColumn(name = "employee_id"))
    @Column(name = "benefit")
    private List<String> benefits = new ArrayList<>();

    // ============================================
    // PROFILE
    // ============================================

    @Column(name = "profile_picture")
    private String profilePicture;

    @Column(name = "profile_color", length = 7)
    private String profileColor;

    @Column(name = "status")
    private String status = "Active";

    @Column(name = "created_date")
    private LocalDateTime createdDate = LocalDateTime.now();

    @Column(name = "updated_date")
    private LocalDateTime updatedDate = LocalDateTime.now();

    // ============================================
    // CONTACT INFORMATION
    // ============================================

    @Column(name = "emergency_contact")
    private String emergencyContact;

    @Column(name = "linkedin")
    private String linkedin;

    // ============================================
    // ONBOARDING FIELDS
    // ============================================

    @Column(name = "onboarding_status")
    private String onboardingStatus = "NOT_STARTED";

    @Column(name = "onboarding_start_date")
    private LocalDateTime onboardingStartDate;

    @Column(name = "onboarding_completed_date")
    private LocalDateTime onboardingCompletedDate;

    @Column(name = "total_documents")
    private Integer totalDocuments = 0;

    @Column(name = "submitted_documents")
    private Integer submittedDocuments = 0;

    @Column(name = "verified_documents")
    private Integer verifiedDocuments = 0;

    // ============================================
    // LOGIN CREDENTIALS
    // ============================================

    @Column(name = "password")
    private String password;

    @Column(name = "reset_token")
    private String resetToken;

    @Column(name = "token_expiry")
    private LocalDateTime tokenExpiry;

    @Column(name = "credentials_created")
    private boolean credentialsCreated = false;

    @Column(name = "active")
    private boolean active = true;

    @Column(name = "is_active")
    private Boolean isActive = true;

    // ============================================
    // ONBOARDING DOCUMENTS RELATIONSHIP
    // ============================================

    @OneToMany(mappedBy = "employee", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<OnboardingDocument> documents = new ArrayList<>();

    // ============================================
    // TRANSIENT HELPER METHODS
    // ============================================

    @Transient
    public String getInitials() {
        String initials = "";
        if (firstName != null && !firstName.isEmpty()) {
            initials += firstName.substring(0, 1).toUpperCase();
        }
        if (lastName != null && !lastName.isEmpty()) {
            initials += lastName.substring(0, 1).toUpperCase();
        }
        return !initials.isEmpty() ? initials : "U";
    }

    @Transient
    public boolean hasProfilePicture() {
        return profilePicture != null && !profilePicture.trim().isEmpty();
    }

    @Transient
    public String getFullName() {
        return (firstName != null ? firstName : "") + " " + (lastName != null ? lastName : "");
    }

    // ============================================
    // CONSTRUCTORS
    // ============================================

    public Employee() {}

    public Employee(String firstName, String lastName, String personalEmail) {
        this.firstName = firstName;
        this.lastName = lastName;
        this.personalEmail = personalEmail;
        this.email = personalEmail;
        this.employeeName = firstName + " " + lastName;
    }

    // ============================================
    // GETTERS AND SETTERS - BASIC INFO
    // ============================================

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getEmployeeId() { return employeeId; }
    public void setEmployeeId(String employeeId) { this.employeeId = employeeId; }

    public String getEmployeeName() { return employeeName; }
    public void setEmployeeName(String employeeName) { this.employeeName = employeeName; }

    public String getFirstName() { return firstName; }
    public void setFirstName(String firstName) { this.firstName = firstName; }

    public String getLastName() { return lastName; }
    public void setLastName(String lastName) { this.lastName = lastName; }

    public String getPersonalEmail() { return personalEmail; }
    public void setPersonalEmail(String personalEmail) { this.personalEmail = personalEmail; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }

    // ✅ FIXED: LocalDate instead of LocalDateTime
    public LocalDate getDateOfBirth() { return dateOfBirth; }
    public void setDateOfBirth(LocalDate dateOfBirth) { this.dateOfBirth = dateOfBirth; }

    public String getGender() { return gender; }
    public void setGender(String gender) { this.gender = gender; }

    // ============================================
    // GETTERS AND SETTERS - LEGACY ADDRESS
    // ============================================

    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }

    public String getCity() { return city; }
    public void setCity(String city) { this.city = city; }

    public String getState() { return state; }
    public void setState(String state) { this.state = state; }

    public String getZipCode() { return zipCode; }
    public void setZipCode(String zipCode) { this.zipCode = zipCode; }

    public String getCountry() { return country; }
    public void setCountry(String country) { this.country = country; }

    // ============================================
    // GETTERS AND SETTERS - PERMANENT ADDRESS
    // ============================================

    public String getPermanentAddress() { return permanentAddress; }
    public void setPermanentAddress(String permanentAddress) { this.permanentAddress = permanentAddress; }

    public String getPermanentCity() { return permanentCity; }
    public void setPermanentCity(String permanentCity) { this.permanentCity = permanentCity; }

    public String getPermanentState() { return permanentState; }
    public void setPermanentState(String permanentState) { this.permanentState = permanentState; }

    public String getPermanentZipCode() { return permanentZipCode; }
    public void setPermanentZipCode(String permanentZipCode) { this.permanentZipCode = permanentZipCode; }

    public String getPermanentCountry() { return permanentCountry; }
    public void setPermanentCountry(String permanentCountry) { this.permanentCountry = permanentCountry; }

    // ============================================
    // GETTERS AND SETTERS - DOCUMENT DEADLINE
    // ============================================

    public LocalDateTime getDocumentDeadline() { return documentDeadline; }
    public void setDocumentDeadline(LocalDateTime documentDeadline) { this.documentDeadline = documentDeadline; }

    public boolean isDeadlineWarningSent() { return deadlineWarningSent; }
    public void setDeadlineWarningSent(boolean deadlineWarningSent) { this.deadlineWarningSent = deadlineWarningSent; }

    public boolean isDeadlineFinalSent() { return deadlineFinalSent; }
    public void setDeadlineFinalSent(boolean deadlineFinalSent) { this.deadlineFinalSent = deadlineFinalSent; }

    // ============================================
    // GETTERS AND SETTERS - RESIDENTIAL ADDRESS
    // ============================================

    public String getResidentialAddress() { return residentialAddress; }
    public void setResidentialAddress(String residentialAddress) { this.residentialAddress = residentialAddress; }

    public String getResidentialCity() { return residentialCity; }
    public void setResidentialCity(String residentialCity) { this.residentialCity = residentialCity; }

    public String getResidentialState() { return residentialState; }
    public void setResidentialState(String residentialState) { this.residentialState = residentialState; }

    public String getResidentialZipCode() { return residentialZipCode; }
    public void setResidentialZipCode(String residentialZipCode) { this.residentialZipCode = residentialZipCode; }

    public String getResidentialCountry() { return residentialCountry; }
    public void setResidentialCountry(String residentialCountry) { this.residentialCountry = residentialCountry; }

    // ============================================
    // GETTERS AND SETTERS - ADDRESS TYPE (NEW)
    // ============================================

    public String getAddressType() { return addressType; }
    public void setAddressType(String addressType) { this.addressType = addressType; }

    // ============================================
    // GETTERS AND SETTERS - PRESENCE STATUS
    // ============================================

    public String getPresenceStatus() { return presenceStatus; }
    public void setPresenceStatus(String presenceStatus) { this.presenceStatus = presenceStatus; }

    public LocalDateTime getLastPresenceUpdate() { return lastPresenceUpdate; }
    public void setLastPresenceUpdate(LocalDateTime lastPresenceUpdate) { this.lastPresenceUpdate = lastPresenceUpdate; }

    // ============================================
    // GETTERS AND SETTERS - ROLE
    // ============================================

    public UserRole getRole() { return role; }
    public void setRole(UserRole role) { this.role = role; }

    // ============================================
    // GETTERS AND SETTERS - JOB INFO
    // ============================================

    public String getDepartment() { return department; }
    public void setDepartment(String department) { this.department = department; }

    public String getDesignation() { return designation; }
    public void setDesignation(String designation) { this.designation = designation; }

    public String getWorkLocation() { return workLocation; }
    public void setWorkLocation(String workLocation) { this.workLocation = workLocation; }

    public String getEmploymentType() { return employmentType; }
    public void setEmploymentType(String employmentType) { this.employmentType = employmentType; }

    public String getWorkType() { return workType; }
    public void setWorkType(String workType) { this.workType = workType; }

    // ✅ FIXED: LocalDate instead of LocalDateTime
    public LocalDate getStartDate() { return startDate; }
    public void setStartDate(LocalDate startDate) { this.startDate = startDate; }

    public String getManager() { return manager; }
    public void setManager(String manager) { this.manager = manager; }

    // ============================================
    // GETTERS AND SETTERS - COMPENSATION
    // ============================================

    public String getSalary() { return salary; }
    public void setSalary(String salary) { this.salary = salary; }

    public String getPayFrequency() { return payFrequency; }
    public void setPayFrequency(String payFrequency) { this.payFrequency = payFrequency; }

    public String getCurrency() { return currency; }
    public void setCurrency(String currency) { this.currency = currency; }

    public List<String> getBenefits() { return benefits; }
    public void setBenefits(List<String> benefits) { this.benefits = benefits; }

    // ============================================
    // GETTERS AND SETTERS - PROFILE
    // ============================================

    public String getProfilePicture() { return profilePicture; }
    public void setProfilePicture(String profilePicture) { this.profilePicture = profilePicture; }

    public String getProfileColor() { return profileColor; }
    public void setProfileColor(String profileColor) { this.profileColor = profileColor; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public LocalDateTime getCreatedDate() { return createdDate; }
    public void setCreatedDate(LocalDateTime createdDate) { this.createdDate = createdDate; }

    public LocalDateTime getUpdatedDate() { return updatedDate; }
    public void setUpdatedDate(LocalDateTime updatedDate) { this.updatedDate = updatedDate; }

    // ============================================
    // GETTERS AND SETTERS - CONTACT
    // ============================================

    public String getEmergencyContact() { return emergencyContact; }
    public void setEmergencyContact(String emergencyContact) { this.emergencyContact = emergencyContact; }

    public String getLinkedin() { return linkedin; }
    public void setLinkedin(String linkedin) { this.linkedin = linkedin; }

    // ============================================
    // GETTERS AND SETTERS - ONBOARDING
    // ============================================

    public String getOnboardingStatus() { return onboardingStatus != null ? onboardingStatus : "NOT_STARTED"; }
    public void setOnboardingStatus(String onboardingStatus) { this.onboardingStatus = onboardingStatus; }

    public LocalDateTime getOnboardingStartDate() { return onboardingStartDate; }
    public void setOnboardingStartDate(LocalDateTime onboardingStartDate) { this.onboardingStartDate = onboardingStartDate; }

    public LocalDateTime getOnboardingCompletedDate() { return onboardingCompletedDate; }
    public void setOnboardingCompletedDate(LocalDateTime onboardingCompletedDate) { this.onboardingCompletedDate = onboardingCompletedDate; }

    public Integer getTotalDocuments() { return totalDocuments != null ? totalDocuments : 0; }
    public void setTotalDocuments(Integer totalDocuments) { this.totalDocuments = totalDocuments; }

    public Integer getSubmittedDocuments() { return submittedDocuments != null ? submittedDocuments : 0; }
    public void setSubmittedDocuments(Integer submittedDocuments) { this.submittedDocuments = submittedDocuments; }

    public Integer getVerifiedDocuments() { return verifiedDocuments != null ? verifiedDocuments : 0; }
    public void setVerifiedDocuments(Integer verifiedDocuments) { this.verifiedDocuments = verifiedDocuments; }

    // ============================================
    // GETTERS AND SETTERS - LOGIN CREDENTIALS
    // ============================================

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }

    public String getResetToken() { return resetToken; }
    public void setResetToken(String resetToken) { this.resetToken = resetToken; }

    public LocalDateTime getTokenExpiry() { return tokenExpiry; }
    public void setTokenExpiry(LocalDateTime tokenExpiry) { this.tokenExpiry = tokenExpiry; }

    public boolean isCredentialsCreated() { return credentialsCreated; }
    public void setCredentialsCreated(boolean credentialsCreated) { this.credentialsCreated = credentialsCreated; }

    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }

    public Boolean getIsActive() { return isActive; }
    public void setIsActive(Boolean isActive) { this.isActive = isActive; }

    // ============================================
    // GETTERS AND SETTERS - DOCUMENTS
    // ============================================

    public List<OnboardingDocument> getDocuments() { return documents; }
    public void setDocuments(List<OnboardingDocument> documents) { this.documents = documents; }
}