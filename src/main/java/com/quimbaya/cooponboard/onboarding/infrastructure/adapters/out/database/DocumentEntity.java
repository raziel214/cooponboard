package com.quimbaya.cooponboard.onboarding.infrastructure.adapters.out.database;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

@Entity
@Table(name = "documents")
public class DocumentEntity {

    @Id
    @Column(name = "id", length = 36)
    private String id;

    @Column(name = "document_type", nullable = false)
    private String documentType;

    @Column(name = "minio_object_name", nullable = false)
    private String minioObjectName;

    @Column(name = "content_type", nullable = false)
    private String contentType;

    @Column(name = "uploaded_at", nullable = false)
    private LocalDateTime uploadedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "onboarding_request_id", nullable = false)
    private OnboardingRequestEntity onboardingRequest;

    public DocumentEntity() {
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

    public OnboardingRequestEntity getOnboardingRequest() {
        return onboardingRequest;
    }

    public void setOnboardingRequest(OnboardingRequestEntity onboardingRequest) {
        this.onboardingRequest = onboardingRequest;
    }
}
