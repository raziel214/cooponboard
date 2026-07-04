package com.quimbaya.cooponboard.onboarding.domain.model;

import java.time.LocalDateTime;

public class DocumentMetadata {
    private String id;
    private String documentType;
    private String minioObjectName;
    private String contentType;
    private LocalDateTime uploadedAt;

    public DocumentMetadata() {
    }

    public DocumentMetadata(String id, String documentType, String minioObjectName, String contentType, LocalDateTime uploadedAt) {
        this.id = id;
        this.documentType = documentType;
        this.minioObjectName = minioObjectName;
        this.contentType = contentType;
        this.uploadedAt = uploadedAt;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getDocumentType() {
        return documentType;
    }

    public void setDocumentType(String documentType) {
        this.documentType = documentType;
    }

    public String getMinioObjectName() {
        return minioObjectName;
    }

    public void setMinioObjectName(String minioObjectName) {
        this.minioObjectName = minioObjectName;
    }

    public String getContentType() {
        return contentType;
    }

    public void setContentType(String contentType) {
        this.contentType = contentType;
    }

    public LocalDateTime getUploadedAt() {
        return uploadedAt;
    }

    public void setUploadedAt(LocalDateTime uploadedAt) {
        this.uploadedAt = uploadedAt;
    }
}
