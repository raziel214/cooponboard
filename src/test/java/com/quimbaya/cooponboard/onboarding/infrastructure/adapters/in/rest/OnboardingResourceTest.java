package com.quimbaya.cooponboard.onboarding.infrastructure.adapters.in.rest;

import com.quimbaya.cooponboard.onboarding.domain.model.OnboardingRequest;
import com.quimbaya.cooponboard.onboarding.domain.model.OnboardingStatus;
import com.quimbaya.cooponboard.onboarding.domain.ports.in.EvaluateEligibilityUseCase;
import com.quimbaya.cooponboard.onboarding.domain.ports.in.GetOnboardingStatusUseCase;
import com.quimbaya.cooponboard.onboarding.domain.ports.in.InitiateOnboardingUseCase;
import com.quimbaya.cooponboard.onboarding.domain.ports.in.UploadDocumentUseCase;
import io.quarkus.test.InjectMock;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.security.TestSecurity;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@QuarkusTest
class OnboardingResourceTest {

    @InjectMock
    InitiateOnboardingUseCase initiateOnboardingUseCase;

    @InjectMock
    GetOnboardingStatusUseCase getOnboardingStatusUseCase;

    @InjectMock
    UploadDocumentUseCase uploadDocumentUseCase;

    @InjectMock
    EvaluateEligibilityUseCase evaluateEligibilityUseCase;

    @Test
    void shouldReturn401WhenUnauthenticated() {
        RestAssured.given()
                .contentType(ContentType.JSON)
                .body(new OnboardingResource.InitiateRequest("123-456", "John Doe", "john@test.com", "123"))
                .post("/api/v1/onboarding")
                .then()
                .statusCode(401);
    }

    @Test
    @TestSecurity(user = "alice", roles = {"user"})
    void shouldInitiateOnboardingSuccessfullyWhenAuthenticated() {
        OnboardingRequest request = new OnboardingRequest(
                "req-id", "123-456", "John Doe", "john@test.com", "123",
                OnboardingStatus.INITIATED, LocalDateTime.now(), LocalDateTime.now()
        );

        when(initiateOnboardingUseCase.initiate(any())).thenReturn(request);

        RestAssured.given()
                .contentType(ContentType.JSON)
                .body(new OnboardingResource.InitiateRequest("123-456", "John Doe", "john@test.com", "123"))
                .post("/api/v1/onboarding")
                .then()
                .statusCode(201)
                .body("id", Matchers.equalTo("req-id"))
                .body("taxId", Matchers.equalTo("123-456"))
                .body("status", Matchers.equalTo("INITIATED"));
    }

    @Test
    @TestSecurity(user = "alice", roles = {"user"})
    void shouldReturn404WhenOnboardingNotFound() {
        when(getOnboardingStatusUseCase.getOnboardingRequest("invalid-id")).thenReturn(Optional.empty());

        RestAssured.given()
                .get("/api/v1/onboarding/invalid-id")
                .then()
                .statusCode(404);
    }

    @Test
    @TestSecurity(user = "alice", roles = {"user"})
    void shouldReturnOnboardingStatusWhenFound() {
        OnboardingRequest request = new OnboardingRequest(
                "req-id", "123-456", "John Doe", "john@test.com", "123",
                OnboardingStatus.INITIATED, LocalDateTime.now(), LocalDateTime.now()
        );

        when(getOnboardingStatusUseCase.getOnboardingRequest("req-id")).thenReturn(Optional.of(request));

        RestAssured.given()
                .get("/api/v1/onboarding/req-id")
                .then()
                .statusCode(200)
                .body("id", Matchers.equalTo("req-id"))
                .body("status", Matchers.equalTo("INITIATED"));
    }
}
