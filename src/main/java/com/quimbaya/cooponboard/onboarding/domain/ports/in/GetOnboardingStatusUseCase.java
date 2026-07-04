package com.quimbaya.cooponboard.onboarding.domain.ports.in;

import com.quimbaya.cooponboard.onboarding.domain.model.OnboardingRequest;

import java.util.Optional;

public interface GetOnboardingStatusUseCase {
    Optional<OnboardingRequest> getOnboardingRequest(String id);
}
