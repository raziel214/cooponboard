package com.quimbaya.cooponboard.onboarding.domain.ports.in;

import com.quimbaya.cooponboard.onboarding.domain.model.DocumentMetadata;

public interface UploadDocumentUseCase {
    DocumentMetadata uploadDocument(Command command);

    record Command(String onboardingRequestId, String documentType, String fileName, String contentType, byte[] fileContent) {}
}
