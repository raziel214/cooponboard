package com.quimbaya.cooponboard.onboarding.infrastructure.adapters.out.storage;

import com.quimbaya.cooponboard.onboarding.domain.ports.out.DocumentStoragePort;
import io.minio.BucketExistsArgs;
import io.minio.GetObjectArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import java.io.ByteArrayInputStream;
import java.io.InputStream;

@ApplicationScoped
public class MinioDocumentStorageAdapter implements DocumentStoragePort {

    private final MinioClient minioClient;
    private final String bucketName;

    @Inject
    public MinioDocumentStorageAdapter(
            MinioClient minioClient,
            @ConfigProperty(name = "onboarding.minio.bucket", defaultValue = "onboarding-documents") String bucketName) {
        this.minioClient = minioClient;
        this.bucketName = bucketName;
    }

    @Override
    public String upload(String objectName, String contentType, byte[] data) {
        try {
            // Ensure bucket exists
            boolean exists = minioClient.bucketExists(
                    BucketExistsArgs.builder().bucket(bucketName).build()
            );
            if (!exists) {
                minioClient.makeBucket(
                        MakeBucketArgs.builder().bucket(bucketName).build()
                );
            }

            // Upload object
            minioClient.putObject(
                    PutObjectArgs.builder()
                            .bucket(bucketName)
                            .object(objectName)
                            .contentType(contentType)
                            .stream(new ByteArrayInputStream(data), data.length, -1)
                            .build()
            );

            return String.format("/%s/%s", bucketName, objectName);
        } catch (Exception e) {
            throw new RuntimeException("Failed to upload document to MinIO storage", e);
        }
    }

    @Override
    public InputStream download(String objectName) {
        try {
            return minioClient.getObject(
                    GetObjectArgs.builder()
                            .bucket(bucketName)
                            .object(objectName)
                            .build()
            );
        } catch (Exception e) {
            throw new RuntimeException("Failed to download document from MinIO storage", e);
        }
    }
}
