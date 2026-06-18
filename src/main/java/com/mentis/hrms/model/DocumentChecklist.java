

        package com.mentis.hrms.model;

import jakarta.persistence.*;

@Entity
@Table(name = "document_checklists")
public class DocumentChecklist {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String documentType;
    private String documentName;
    private String category; // PERSONAL, EDUCATIONAL, PROFESSIONAL, IDENTITY, OFFER
    private boolean mandatory = true;
    private Integer displayOrder = 0;
    private String description;
    private String fileFormats = "pdf,jpg,jpeg,png";
    private Long maxFileSize = 5242880L; // 5MB default

    // Constructors
    public DocumentChecklist() {}

    public DocumentChecklist(String documentType, String documentName, String category,
                             boolean mandatory, Integer displayOrder, String description) {
        this.documentType = documentType;
        this.documentName = documentName;
        this.category = category;
        this.mandatory = mandatory;
        this.displayOrder = displayOrder;
        this.description = description;
    }

    // Getters and Setters (generate all)
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getDocumentType() { return documentType; }
    public void setDocumentType(String documentType) { this.documentType = documentType; }

    public String getDocumentName() { return documentName; }
    public void setDocumentName(String documentName) { this.documentName = documentName; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public boolean isMandatory() { return mandatory; }
    public void setMandatory(boolean mandatory) { this.mandatory = mandatory; }

    public Integer getDisplayOrder() { return displayOrder; }
    public void setDisplayOrder(Integer displayOrder) { this.displayOrder = displayOrder; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getFileFormats() { return fileFormats; }
    public void setFileFormats(String fileFormats) { this.fileFormats = fileFormats; }

    public Long getMaxFileSize() { return maxFileSize; }
    public void setMaxFileSize(Long maxFileSize) { this.maxFileSize = maxFileSize; }
}