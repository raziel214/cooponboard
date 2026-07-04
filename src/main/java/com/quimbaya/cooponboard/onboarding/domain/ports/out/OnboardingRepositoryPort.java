package com.quimbaya.cooponboard.onboarding.domain.ports.out;

import com.quimbaya.cooponboard.onboarding.domain.model.OnboardingRequest;

import java.util.Optional;

public interface OnboardingRepositoryPort {
    OnboardingRequest save(OnboardingRequest request);
    Optional<OnboardingRequest> findRequestById(String id);
    Optional<OnboardingRequest> findByTaxId(String taxId);
}
