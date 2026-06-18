package com.mentis.hrms.dto;

import java.time.LocalDate;

public class HolidayDTO {

    private Long id;
    private String holidayName;
    private String holidayDate;
    private String holidayType;
    private String description;
    private String createdBy;

    // Constructors
    public HolidayDTO() {}

    public HolidayDTO(Long id, String holidayName, String holidayDate, String holidayType, String description) {
        this.id = id;
        this.holidayName = holidayName;
        this.holidayDate = holidayDate;
        this.holidayType = holidayType;
        this.description = description;
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getHolidayName() { return holidayName; }
    public void setHolidayName(String holidayName) { this.holidayName = holidayName; }

    public String getHolidayDate() { return holidayDate; }
    public void setHolidayDate(String holidayDate) { this.holidayDate = holidayDate; }

    public String getHolidayType() { return holidayType; }
    public void setHolidayType(String holidayType) { this.holidayType = holidayType; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getCreatedBy() { return createdBy; }
    public void setCreatedBy(String createdBy) { this.createdBy = createdBy; }
}