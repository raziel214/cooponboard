package com.quimbaya.cooponboard.onboarding.domain.model;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class OnboardingRequest {
    private String id;
    private String taxId;
    private String fullName;
    private String email;
    private String phoneNumber;
    private OnboardingStatus status;
    private List<DocumentMetadata> documents = new ArrayList<>();
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public OnboardingRequest() {
    }

    public OnboardingRequest(String id, String taxId, String fullName, String email, String phoneNumber, OnboardingStatus status, LocalDateTime createdAt, LocalDateTime updatedAt) {
        this.id = id;
        this.taxId = taxId;
        this.fullName = fullName;
        this.email = email;
        this.phoneNumber = phoneNumber;
        this.status = status;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    // Domain business logic
    public void initiate() {
        if (this.taxId == null || this.taxId.trim().isEmpty()) {
            throw new IllegalArgumentException("Tax ID is required to initiate onboarding");
        }
        if (this.fullName == null || this.fullName.trim().isEmpty()) {
            throw new IllegalArgumentException("Full name is required to initiate onboarding");
        }
        if (this.email == null || !this.email.contains("@")) {
            throw new IllegalArgumentException("A valid email is required to initiate onboarding");
        }
        this.status = OnboardingStatus.INITIATED;
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    public void addDocument(DocumentMetadata document) {
        this.documents.add(document);
        this.status = OnboardingStatus.DOCUMENTS_UPLOADED;
        this.updatedAt = LocalDateTime.now();
    }

    public void verifyEligibility() {
        if (this.status != OnboardingStatus.DOCUMENTS_UPLOADED) {
            throw new IllegalStateException("Documents must be uploaded before eligibility can be verified");
        }
        // Basic domain validation logic
        this.status = OnboardingStatus.ELIGIBILITY_CHECKED;
        this.updatedAt = LocalDateTime.now();
    }

    public void approve() {
        if (this.status != OnboardingStatus.ELIGIBILITY_CHECKED) {
            throw new IllegalStateException("Eligibility must be checked before approval");
        }
        this.status = OnboardingStatus.APPROVED;
        this.updatedAt = LocalDateTime.now();
    }

    public void reject(String reason) {
        this.status = OnboardingStatus.REJECTED;
        this.updatedAt = LocalDateTime.now();
    }

    // Getters and Setters
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getTaxId() {
        return taxId;
    }

    public void setTaxId(String taxId) {
        this.taxId = taxId;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public void setPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }

    public OnboardingStatus getStatus() {
        return status;
    }

    public void setStatus(OnboardingStatus status) {
        this.status = status;
    }

    public List<DocumentMetadata> getDocuments() {
        return documents;
    }

    public void setDocuments(List<DocumentMetadata> documents) {
        this.documents = documents;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
