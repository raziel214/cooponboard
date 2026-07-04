package com.quimbaya.cooponboard.onboarding.infrastructure.adapters.out.database;

import com.quimbaya.cooponboard.onboarding.domain.model.DocumentMetadata;
import com.quimbaya.cooponboard.onboarding.domain.model.OnboardingRequest;
import com.quimbaya.cooponboard.onboarding.domain.ports.out.OnboardingRepositoryPort;
import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@ApplicationScoped
public class PanacheOnboardingRepository implements PanacheRepositoryBase<OnboardingRequestEntity, String>, OnboardingRepositoryPort {

    @Override
    public OnboardingRequest save(OnboardingRequest request) {
        OnboardingRequestEntity entity = findByIdOptional(request.getId())
                .orElseGet(() -> {
                    OnboardingRequestEntity newEntity = new OnboardingRequestEntity();
                    newEntity.setId(request.getId());
                    return newEntity;
                });

        entity.setTaxId(request.getTaxId());
        entity.setFullName(request.getFullName());
        entity.setEmail(request.getEmail());
        entity.setPhoneNumber(request.getPhoneNumber());
        entity.setStatus(request.getStatus());
        entity.setCreatedAt(request.getCreatedAt());
        entity.setUpdatedAt(request.getUpdatedAt());

        // Update documents collection
        entity.getDocuments().clear();
        if (request.getDocuments() != null) {
            for (DocumentMetadata docDomain : request.getDocuments()) {
                DocumentEntity docEntity = new DocumentEntity();
                docEntity.setId(docDomain.getId());
                docEntity.setDocumentType(docDomain.getDocumentType());
                docEntity.setMinioObjectName(docDomain.getMinioObjectName());
                docEntity.setContentType(docDomain.getContentType());
                docEntity.setUploadedAt(docDomain.getUploadedAt());
                entity.addDocument(docEntity);
            }
        }

        persist(entity);
        return toDomain(entity);
    }

    @Override
    public Optional<OnboardingRequest> findRequestById(String id) {
        return findByIdOptional(id).map(this::toDomain);
    }

    @Override
    public Optional<OnboardingRequest> findByTaxId(String taxId) {
        return find("taxId", taxId).firstResultOptional().map(this::toDomain);
    }

    private OnboardingRequest toDomain(OnboardingRequestEntity entity) {
        OnboardingRequest request = new OnboardingRequest(
                entity.getId(),
                entity.getTaxId(),
                entity.getFullName(),
                entity.getEmail(),
                entity.getPhoneNumber(),
                entity.getStatus(),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );

        if (entity.getDocuments() != null) {
            List<DocumentMetadata> docDomainList = entity.getDocuments().stream()
                    .map(docEntity -> new DocumentMetadata(
                            docEntity.getId(),
                            docEntity.getDocumentType(),
                            docEntity.getMinioObjectName(),
                            docEntity.getContentType(),
                            docEntity.getUploadedAt()
                    ))
                    .collect(Collectors.toList());
            request.setDocuments(docDomainList);
        }

        return request;
    }
}
