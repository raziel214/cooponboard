package com.quimbaya.cooponboard.onboarding.domain.ports.in;

import com.quimbaya.cooponboard.onboarding.domain.model.OnboardingRequest;

public interface EvaluateEligibilityUseCase {
    OnboardingRequest evaluate(Command command);

    record Command(String onboardingRequestId, boolean approved, String reason) {}
}
