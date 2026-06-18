package com.mentis.hrms.dto;

import org.springframework.web.multipart.MultipartFile;
import java.time.LocalDate;
import java.util.List;

public class EmployeeFormDTO {

    // Basic Information
    private String firstName;
    private String lastName;
    private String personalEmail;
    private String phone;
    private LocalDate dateOfBirth;
    private String gender;

    // Job Information
    private String employeeId;
    private String department;
    private String designation;
    private String workLocation;
    private String employmentType;
    private String workType;
    private LocalDate startDate;
    private String manager;

    // Compensation
    private String salary;
    private String payFrequency;
    private String currency;

    // Address Information
    private String permanentAddress;
    private String permanentCity;
    private String permanentState;
    private String permanentZipCode;
    private String permanentCountry;

    private String residentialAddress;
    private String residentialCity;
    private String residentialState;
    private String residentialZipCode;
    private String residentialCountry;
    private String addressType;

    // Contact
    private String emergencyContact;
    private String linkedin;

    // File Upload
    private MultipartFile profilePicture;

    // Benefits
    private List<String> benefits;

    // Getters and Setters
    public String getFirstName() { return firstName; }
    public void setFirstName(String firstName) { this.firstName = firstName; }

    public String getLastName() { return lastName; }
    public void setLastName(String lastName) { this.lastName = lastName; }

    public String getPersonalEmail() { return personalEmail; }
    public void setPersonalEmail(String personalEmail) { this.personalEmail = personalEmail; }

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }

    public LocalDate getDateOfBirth() { return dateOfBirth; }
    public void setDateOfBirth(LocalDate dateOfBirth) { this.dateOfBirth = dateOfBirth; }

    public String getGender() { return gender; }
    public void setGender(String gender) { this.gender = gender; }

    public String getEmployeeId() { return employeeId; }
    public void setEmployeeId(String employeeId) { this.employeeId = employeeId; }

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

    public LocalDate getStartDate() { return startDate; }
    public void setStartDate(LocalDate startDate) { this.startDate = startDate; }

    public String getManager() { return manager; }
    public void setManager(String manager) { this.manager = manager; }

    public String getSalary() { return salary; }
    public void setSalary(String salary) { this.salary = salary; }

    public String getPayFrequency() { return payFrequency; }
    public void setPayFrequency(String payFrequency) { this.payFrequency = payFrequency; }

    public String getCurrency() { return currency; }
    public void setCurrency(String currency) { this.currency = currency; }

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

    public String getAddressType() { return addressType; }
    public void setAddressType(String addressType) { this.addressType = addressType; }

    public String getEmergencyContact() { return emergencyContact; }
    public void setEmergencyContact(String emergencyContact) { this.emergencyContact = emergencyContact; }

    public String getLinkedin() { return linkedin; }
    public void setLinkedin(String linkedin) { this.linkedin = linkedin; }

    public MultipartFile getProfilePicture() { return profilePicture; }
    public void setProfilePicture(MultipartFile profilePicture) { this.profilePicture = profilePicture; }

    public List<String> getBenefits() { return benefits; }
    public void setBenefits(List<String> benefits) { this.benefits = benefits; }
}