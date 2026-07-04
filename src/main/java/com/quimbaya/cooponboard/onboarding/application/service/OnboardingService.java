package com.quimbaya.cooponboard.onboarding.application.service;

import com.quimbaya.cooponboard.onboarding.domain.model.DocumentMetadata;
import com.quimbaya.cooponboard.onboarding.domain.model.OnboardingRequest;
import com.quimbaya.cooponboard.onboarding.domain.model.OnboardingStatus;
import com.quimbaya.cooponboard.onboarding.domain.ports.in.EvaluateEligibilityUseCase;
import com.quimbaya.cooponboard.onboarding.domain.ports.in.GetOnboardingStatusUseCase;
import com.quimbaya.cooponboard.onboarding.domain.ports.in.InitiateOnboardingUseCase;
import com.quimbaya.cooponboard.onboarding.domain.ports.in.UploadDocumentUseCase;
import com.quimbaya.cooponboard.onboarding.domain.ports.out.DocumentStoragePort;
import com.quimbaya.cooponboard.onboarding.domain.ports.out.OnboardingRepositoryPort;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

@ApplicationScoped
public class OnboardingService implements InitiateOnboardingUseCase, UploadDocumentUseCase, GetOnboardingStatusUseCase, EvaluateEligibilityUseCase {

    private final OnboardingRepositoryPort repositoryPort;
    private final DocumentStoragePort storagePort;

    @Inject
    public OnboardingService(OnboardingRepositoryPort repositoryPort, DocumentStoragePort storagePort) {
        this.repositoryPort = repositoryPort;
        this.storagePort = storagePort;
    }

    @Override
    @Transactional
    public OnboardingRequest initiate(InitiateOnboardingUseCase.Command command) {
        // Business Rule: Cannot have two active onboarding requests with the same taxId
        Optional<OnboardingRequest> existing = repositoryPort.findByTaxId(command.taxId());
        if (existing.isPresent() && existing.get().getStatus() != OnboardingStatus.REJECTED) {
            throw new IllegalStateException("An active onboarding process already exists for Tax ID: " + command.taxId());
        }

        OnboardingRequest request = new OnboardingRequest();
        request.setId(UUID.randomUUID().toString());
        request.setTaxId(command.taxId());
        request.setFullName(command.fullName());
        request.setEmail(command.email());
        request.setPhoneNumber(command.phoneNumber());
        
        request.initiate();

        return repositoryPort.save(request);
    }

    @Override
    @Transactional
    public DocumentMetadata uploadDocument(UploadDocumentUseCase.Command command) {
        OnboardingRequest request = repositoryPort.findRequestById(command.onboardingRequestId())
                .orElseThrow(() -> new IllegalArgumentException("Onboarding request not found: " + command.onboardingRequestId()));

        // Upload to S3/MinIO
        String objectName = String.format("onboarding/%s/%s_%s", 
                request.getId(), 
                command.documentType().toLowerCase(), 
                command.fileName());
        
        String url = storagePort.upload(objectName, command.contentType(), command.fileContent());

        DocumentMetadata document = new DocumentMetadata();
        document.setId(UUID.randomUUID().toString());
        document.setDocumentType(command.documentType());
        document.setMinioObjectName(objectName);
        document.setContentType(command.contentType());
        document.setUploadedAt(LocalDateTime.now());

        request.addDocument(document);
        repositoryPort.save(request);

        return document;
    }

    @Override
    public Optional<OnboardingRequest> getOnboardingRequest(String id) {
        return repositoryPort.findRequestById(id);
    }

    @Override
    @Transactional
    public OnboardingRequest evaluate(EvaluateEligibilityUseCase.Command command) {
        OnboardingRequest request = repositoryPort.findRequestById(command.onboardingRequestId())
                .orElseThrow(() -> new IllegalArgumentException("Onboarding request not found: " + command.onboardingRequestId()));

        request.verifyEligibility();

        if (command.approved()) {
            request.approve();
        } else {
            request.reject(command.reason());
        }

        return repositoryPort.save(request);
    }
}
