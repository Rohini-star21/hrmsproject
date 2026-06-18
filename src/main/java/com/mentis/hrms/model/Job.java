package com.mentis.hrms.model;

import jakarta.persistence.*;
import com.fasterxml.jackson.annotation.JsonIgnore;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Arrays;
import java.util.stream.Collectors;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Transient;
@Entity
@Table(name = "jobs")
public class Job {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String title;
    private String department;
    private String jobType;
    // REPLACE WITH these two fields:
    private String location; // Keep for backward compatibility

    @ManyToOne
    @JoinColumn(name = "location_id")
    private Location jobLocation; // New relationship field

    @Transient
    private String locationDisplayName;
    private String experienceLevel;
    private String salaryRange;
    private LocalDate applicationDeadline;

    @Column(length = 2000)
    private String description;

    @Column(length = 2000)
    private String requirements;

    private String applicationInstructions;
    private LocalDate postedDate;
    private Boolean active;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "job_requirements", joinColumns = @JoinColumn(name = "job_id"))
    @Column(name = "requirement")
    private List<String> requirementList = new ArrayList<>();

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "job_responsibilities", joinColumns = @JoinColumn(name = "job_id"))
    @Column(name = "responsibility")
    private List<String> responsibilities = new ArrayList<>();

    // Prevent circular reference
    @OneToMany(mappedBy = "job", fetch = FetchType.LAZY)
    @JsonIgnore
    private List<JobApplication> applications = new ArrayList<>();

    public Job() {
        this.postedDate = LocalDate.now();
        this.active = true;
        this.responsibilities = new ArrayList<>();
        this.requirementList = new ArrayList<>();
        this.applications = new ArrayList<>();
    }
// ========== NEW LOCATION RELATIONSHIP METHODS ==========

    public Location getJobLocation() {
        return jobLocation;
    }

    public void setJobLocation(Location jobLocation) {
        this.jobLocation = jobLocation;
        // Update the legacy location field for backward compatibility
        if (jobLocation != null) {
            this.location = jobLocation.getName() + " (" +
                    (jobLocation.getCity() != null ? jobLocation.getCity() : "") +
                    (jobLocation.getCity() != null && jobLocation.getCountry() != null ? ", " : "") +
                    (jobLocation.getCountry() != null ? jobLocation.getCountry() : "") + ")";
        }
    }

    // Helper method to get location ID (for form binding)
    public Long getJobLocationId() {
        return jobLocation != null ? jobLocation.getId() : null;
    }

    // Helper method to set location by ID (will be used in controller)
    public void setJobLocationId(Long locationId) {
        // This will be handled in controller
        // Don't implement here
    }

    // Get display name for location
    public String getLocationDisplayName() {
        if (jobLocation != null) {
            return jobLocation.getName() + " (" +
                    (jobLocation.getCity() != null ? jobLocation.getCity() : "") +
                    (jobLocation.getCity() != null && jobLocation.getCountry() != null ? ", " : "") +
                    (jobLocation.getCountry() != null ? jobLocation.getCountry() : "") + ")";
        }
        return location;
    }


    // Getters and setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDepartment() {
        return department;
    }

    public void setDepartment(String department) {
        this.department = department;
    }

    public String getJobType() {
        return jobType;
    }

    public void setJobType(String jobType) {
        this.jobType = jobType;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public String getExperienceLevel() {
        return experienceLevel;
    }

    public void setExperienceLevel(String experienceLevel) {
        this.experienceLevel = experienceLevel;
    }

    public String getSalaryRange() {
        return salaryRange;
    }

    public void setSalaryRange(String salaryRange) {
        this.salaryRange = salaryRange;
    }

    public LocalDate getApplicationDeadline() {
        return applicationDeadline;
    }

    public void setApplicationDeadline(LocalDate applicationDeadline) {
        this.applicationDeadline = applicationDeadline;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getRequirements() {
        return requirements;
    }

    public void setRequirements(String requirements) {
        this.requirements = requirements;
        if (requirements != null && !requirements.trim().isEmpty()) {
            this.requirementList.clear();
            String[] reqArray = requirements.split(",");
            for (String req : reqArray) {
                String trimmedReq = req.trim();
                if (!trimmedReq.isEmpty()) {
                    this.requirementList.add(trimmedReq);
                }
            }
        }
    }

    public String getApplicationInstructions() {
        return applicationInstructions;
    }

    public void setApplicationInstructions(String applicationInstructions) {
        this.applicationInstructions = applicationInstructions;
    }

    public LocalDate getPostedDate() {
        return postedDate;
    }

    public void setPostedDate(LocalDate postedDate) {
        this.postedDate = postedDate;
    }

    public Boolean getActive() {
        return active != null ? active : true;
    }

    public void setActive(Boolean active) {
        this.active = active;
    }

    public List<String> getRequirementList() {
        if (requirementList == null) {
            requirementList = new ArrayList<>();
        }
        return requirementList;
    }

    public void setRequirementList(List<String> requirementList) {
        this.requirementList = requirementList;
        if (requirementList != null && !requirementList.isEmpty()) {
            this.requirements = String.join(", ", requirementList);
        } else {
            this.requirements = "";
        }
    }

    public List<String> getResponsibilities() {
        if (responsibilities == null) {
            responsibilities = new ArrayList<>();
        }
        return responsibilities;
    }

    public void setResponsibilities(List<String> responsibilities) {
        this.responsibilities = responsibilities;
    }

    // Add this method to handle responsibilities from string
    public void setResponsibilitiesFromString(String responsibilitiesStr) {
        if (responsibilitiesStr != null && !responsibilitiesStr.trim().isEmpty()) {
            this.responsibilities = Arrays.stream(responsibilitiesStr.split("\n"))
                    .map(String::trim)
                    .filter(s -> !s.isEmpty())
                    .collect(Collectors.toList());
        } else {
            this.responsibilities = new ArrayList<>();
        }
    }

    public List<JobApplication> getApplications() {
        if (applications == null) {
            applications = new ArrayList<>();
        }
        return applications;
    }

    public void setApplications(List<JobApplication> applications) {
        this.applications = applications;
    }

    public String getFormattedApplicationDeadline() {
        return applicationDeadline != null ? applicationDeadline.toString() : "Not specified";
    }

    public String getFormattedPostedDate() {
        return postedDate != null ? postedDate.toString() : "Not specified";
    }

    public boolean isActive() {
        return getActive() && (applicationDeadline == null || applicationDeadline.isAfter(LocalDate.now()));
    }

    public int getApplicationCount() {
        return applications != null ? applications.size() : 0;
    }

    @Override
    public String toString() {
        return "Job{" +
                "id=" + id +
                ", title='" + title + '\'' +
                ", department='" + department + '\'' +
                ", jobType='" + jobType + '\'' +
                ", location='" + location + '\'' +
                ", experienceLevel='" + experienceLevel + '\'' +
                ", applicationDeadline=" + applicationDeadline +
                ", postedDate=" + postedDate +
                ", active=" + active +
                ", applicationCount=" + getApplicationCount() +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Job job = (Job) o;

        return id != null ? id.equals(job.id) : job.id == null;
    }

    @Override
    public int hashCode() {
        return id != null ? id.hashCode() : 0;
    }
}