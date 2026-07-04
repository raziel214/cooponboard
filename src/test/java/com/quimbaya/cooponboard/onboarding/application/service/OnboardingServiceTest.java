package com.quimbaya.cooponboard.onboarding.application.service;

import com.quimbaya.cooponboard.onboarding.domain.model.DocumentMetadata;
import com.quimbaya.cooponboard.onboarding.domain.model.OnboardingRequest;
import com.quimbaya.cooponboard.onboarding.domain.model.OnboardingStatus;
import com.quimbaya.cooponboard.onboarding.domain.ports.in.EvaluateEligibilityUseCase;
import com.quimbaya.cooponboard.onboarding.domain.ports.in.InitiateOnboardingUseCase;
import com.quimbaya.cooponboard.onboarding.domain.ports.in.UploadDocumentUseCase;
import com.quimbaya.cooponboard.onboarding.domain.ports.out.DocumentStoragePort;
import com.quimbaya.cooponboard.onboarding.domain.ports.out.OnboardingRepositoryPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class OnboardingServiceTest {

    private OnboardingRepositoryPort repositoryPort;
    private DocumentStoragePort storagePort;
    private OnboardingService onboardingService;

    @BeforeEach
    void setUp() {
        repositoryPort = Mockito.mock(OnboardingRepositoryPort.class);
        storagePort = Mockito.mock(DocumentStoragePort.class);
        onboardingService = new OnboardingService(repositoryPort, storagePort);
    }

    @Test
    void shouldInitiateOnboardingSuccessfully() {
        InitiateOnboardingUseCase.Command command = new InitiateOnboardingUseCase.Command(
                "123-456", "John Doe", "john.doe@example.com", "555-0199"
        );

        when(repositoryPort.findByTaxId("123-456")).thenReturn(Optional.empty());
        when(repositoryPort.save(any(OnboardingRequest.class))).thenAnswer(invocation -> invocation.getArgument(0));

        OnboardingRequest result = onboardingService.initiate(command);

        assertNotNull(result);
        assertEquals("123-456", result.getTaxId());
        assertEquals(OnboardingStatus.INITIATED, result.getStatus());
        verify(repositoryPort).save(any(OnboardingRequest.class));
    }

    @Test
    void shouldThrowExceptionWhenInitiatingDuplicateTaxId() {
        InitiateOnboardingUseCase.Command command = new InitiateOnboardingUseCase.Command(
                "123-456", "John Doe", "john.doe@example.com", "555-0199"
        );
        OnboardingRequest existing = new OnboardingRequest();
        existing.setStatus(OnboardingStatus.INITIATED);

        when(repositoryPort.findByTaxId("123-456")).thenReturn(Optional.of(existing));

        assertThrows(IllegalStateException.class, () -> onboardingService.initiate(command));
    }

    @Test
    void shouldUploadDocumentSuccessfully() {
        UploadDocumentUseCase.Command command = new UploadDocumentUseCase.Command(
                "req-id", "IDENTITY_CARD", "id_card.pdf", "application/pdf", new byte[]{1, 2, 3}
        );
        OnboardingRequest existing = new OnboardingRequest();
        existing.setId("req-id");
        existing.setStatus(OnboardingStatus.INITIATED);

        when(repositoryPort.findRequestById("req-id")).thenReturn(Optional.of(existing));
        when(storagePort.upload(any(), any(), any())).thenReturn("/bucket/path");
        when(repositoryPort.save(any(OnboardingRequest.class))).thenAnswer(invocation -> invocation.getArgument(0));

        DocumentMetadata document = onboardingService.uploadDocument(command);

        assertNotNull(document);
        assertEquals("IDENTITY_CARD", document.getDocumentType());
        assertEquals("application/pdf", document.getContentType());
        assertEquals(OnboardingStatus.DOCUMENTS_UPLOADED, existing.getStatus());
        verify(storagePort).upload(any(), any(), any());
    }

    @Test
    void shouldApproveOnboardingSuccessfully() {
        EvaluateEligibilityUseCase.Command command = new EvaluateEligibilityUseCase.Command(
                "req-id", true, "All checks passed"
        );
        OnboardingRequest existing = new OnboardingRequest();
        existing.setId("req-id");
        existing.setStatus(OnboardingStatus.DOCUMENTS_UPLOADED);

        when(repositoryPort.findRequestById("req-id")).thenReturn(Optional.of(existing));
        when(repositoryPort.save(any(OnboardingRequest.class))).thenAnswer(invocation -> invocation.getArgument(0));

        OnboardingRequest result = onboardingService.evaluate(command);

        assertNotNull(result);
        assertEquals(OnboardingStatus.APPROVED, result.getStatus());
        verify(repositoryPort).save(any(OnboardingRequest.class));
    }
}
