package com.quimbaya.cooponboard.onboarding.infrastructure.adapters.in.rest;

import com.quimbaya.cooponboard.onboarding.domain.model.DocumentMetadata;
import com.quimbaya.cooponboard.onboarding.domain.model.OnboardingRequest;
import com.quimbaya.cooponboard.onboarding.domain.ports.in.EvaluateEligibilityUseCase;
import com.quimbaya.cooponboard.onboarding.domain.ports.in.GetOnboardingStatusUseCase;
import com.quimbaya.cooponboard.onboarding.domain.ports.in.InitiateOnboardingUseCase;
import com.quimbaya.cooponboard.onboarding.domain.ports.in.UploadDocumentUseCase;
import io.quarkus.security.Authenticated;
import org.jboss.resteasy.reactive.server.ServerExceptionMapper;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.media.Content;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;
import org.jboss.resteasy.reactive.RestForm;
import org.jboss.resteasy.reactive.multipart.FileUpload;

import java.io.IOException;
import java.nio.file.Files;

@Path("/api/v1/onboarding")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Tag(name = "Customer Onboarding", description = "BIAN-aligned operations for cooperative member onboarding")
@Authenticated
public class OnboardingResource {

    private final InitiateOnboardingUseCase initiateOnboardingUseCase;
    private final UploadDocumentUseCase uploadDocumentUseCase;
    private final GetOnboardingStatusUseCase getOnboardingStatusUseCase;
    private final EvaluateEligibilityUseCase evaluateEligibilityUseCase;

    @Inject
    public OnboardingResource(
            InitiateOnboardingUseCase initiateOnboardingUseCase,
            UploadDocumentUseCase uploadDocumentUseCase,
            GetOnboardingStatusUseCase getOnboardingStatusUseCase,
            EvaluateEligibilityUseCase evaluateEligibilityUseCase) {
        this.initiateOnboardingUseCase = initiateOnboardingUseCase;
        this.uploadDocumentUseCase = uploadDocumentUseCase;
        this.getOnboardingStatusUseCase = getOnboardingStatusUseCase;
        this.evaluateEligibilityUseCase = evaluateEligibilityUseCase;
    }

    @POST
    @Operation(summary = "Initiate onboarding request", description = "Registers a new cooperative onboarding process for a potential member.")
    @APIResponse(responseCode = "201", description = "Onboarding request successfully created")
    @APIResponse(responseCode = "400", description = "Invalid request details")
    @APIResponse(responseCode = "409", description = "Onboarding request already active for this Tax ID")
    public Response initiate(InitiateRequest request) {
        OnboardingRequest domainRequest = initiateOnboardingUseCase.initiate(
                new InitiateOnboardingUseCase.Command(
                        request.taxId(),
                        request.fullName(),
                        request.email(),
                        request.phoneNumber()
                )
        );
        return Response.status(Response.Status.CREATED).entity(domainRequest).build();
    }

    @GET
    @Path("/{id}")
    @Operation(summary = "Retrieve onboarding status", description = "Gets the detailed status and uploaded documents metadata of an onboarding request.")
    @APIResponse(responseCode = "200", description = "Onboarding request found")
    @APIResponse(responseCode = "404", description = "Onboarding request not found")
    public Response getStatus(@PathParam("id") String id) {
        return getOnboardingStatusUseCase.getOnboardingRequest(id)
                .map(request -> Response.ok(request).build())
                .orElseGet(() -> Response.status(Response.Status.NOT_FOUND).build());
    }

    @POST
    @Path("/{id}/documents")
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @Operation(summary = "Upload supporting document", description = "Uploads identification or proof of income files to storage (MinIO) and associates it with the onboarding process.")
    @APIResponse(responseCode = "200", description = "Document uploaded successfully")
    @APIResponse(responseCode = "404", description = "Onboarding request not found")
    public Response uploadDocument(
            @PathParam("id") String id,
            @RestForm("documentType") String documentType,
            @RestForm("file") FileUpload file) throws IOException {
        
        if (file == null) {
            return Response.status(Response.Status.BAD_REQUEST).entity("File is missing").build();
        }

        byte[] content = Files.readAllBytes(file.uploadedFile());
        DocumentMetadata document = uploadDocumentUseCase.uploadDocument(
                new UploadDocumentUseCase.Command(
                        id,
                        documentType,
                        file.fileName(),
                        file.contentType(),
                        content
                )
        );
        return Response.ok(document).build();
    }

    @POST
    @Path("/{id}/evaluate")
    @Operation(summary = "Evaluate & Complete onboarding", description = "Checks eligibility criteria and marks the onboarding request as APPROVED or REJECTED.")
    @APIResponse(responseCode = "200", description = "Evaluation processed successfully")
    @APIResponse(responseCode = "400", description = "Invalid request or evaluation state transition error")
    @APIResponse(responseCode = "404", description = "Onboarding request not found")
    public Response evaluate(@PathParam("id") String id, EvaluateRequest request) {
        OnboardingRequest domainRequest = evaluateEligibilityUseCase.evaluate(
                new EvaluateEligibilityUseCase.Command(
                        id,
                        request.approved(),
                        request.reason()
                )
        );
        return Response.ok(domainRequest).build();
    }

    // --- REST DTOs ---
    public record InitiateRequest(
            @Schema(required = true, example = "900.123.456-7") String taxId,
            @Schema(required = true, example = "Juan Pérez") String fullName,
            @Schema(required = true, example = "juan.perez@example.com") String email,
            @Schema(example = "+573001234567") String phoneNumber
    ) {}

    public record EvaluateRequest(
            @Schema(required = true, example = "true") boolean approved,
            @Schema(example = "Documents and income requirements verified successfully") String reason
    ) {}

    // --- Exception Mappings ---
    @ServerExceptionMapper
    public Response mapIllegalArgumentException(IllegalArgumentException x) {
        return Response.status(Response.Status.BAD_REQUEST)
                .entity(new ErrorResponse(x.getMessage()))
                .build();
    }

    @ServerExceptionMapper
    public Response mapIllegalStateException(IllegalStateException x) {
        // Map unique constraint / active request to 409 Conflict
        if (x.getMessage().contains("already exists")) {
            return Response.status(Response.Status.CONFLICT)
                    .entity(new ErrorResponse(x.getMessage()))
                    .build();
        }
        return Response.status(Response.Status.BAD_REQUEST)
                .entity(new ErrorResponse(x.getMessage()))
                .build();
    }

    public record ErrorResponse(String message) {}
}
