package com.quimbaya.cooponboard.onboarding.domain.ports.in;

import com.quimbaya.cooponboard.onboarding.domain.model.OnboardingRequest;

public interface InitiateOnboardingUseCase {
    OnboardingRequest initiate(Command command);

    record Command(String taxId, String fullName, String email, String phoneNumber) {}
}
