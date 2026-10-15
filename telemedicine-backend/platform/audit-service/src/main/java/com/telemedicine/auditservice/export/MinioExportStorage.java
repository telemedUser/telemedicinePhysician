package com.telemedicine.auditservice.export;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedGetObjectRequest;

public class MinioExportStorage {

    private final S3Client s3Client;
    private final S3Presigner presigner;
    private final MinioExportStorageProperties properties;

    public MinioExportStorage(
            S3Client s3Client, S3Presigner presigner, MinioExportStorageProperties properties) {
        this.s3Client = s3Client;
        this.presigner = presigner;
        this.properties = properties;
    }

    public StoredObject store(String objectKey, String contentType, byte[] body) {
        s3Client.putObject(
                PutObjectRequest.builder()
                        .bucket(properties.bucket())
                        .key(objectKey)
                        .contentType(contentType)
                        .build(),
                RequestBody.fromBytes(body));
        Instant expiresAt = Instant.now().plus(properties.urlTtl());
        PresignedGetObjectRequest presigned =
                presigner.presignGetObject(
                        GetObjectPresignRequest.builder()
                                .signatureDuration(properties.urlTtl())
                                .getObjectRequest(
                                        GetObjectRequest.builder()
                                                .bucket(properties.bucket())
                                                .key(objectKey)
                                                .build())
                                .build());
        return new StoredObject(objectKey, presigned.url().toString(), expiresAt);
    }

    public StoredObject storeText(String objectKey, String contentType, String content) {
        return store(objectKey, contentType, content.getBytes(StandardCharsets.UTF_8));
    }

    public record StoredObject(String objectKey, String downloadUrl, Instant expiresAt) {}
}
