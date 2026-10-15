package com.telemedicine.clinicaldocsservice.storage;

import com.telemedicine.clinicaldocsservice.config.ClinicalDocsServiceProperties;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

public class ClinicalDocumentStorage {

    private final S3Client s3Client;
    private final ClinicalDocsServiceProperties properties;

    public ClinicalDocumentStorage(S3Client s3Client, ClinicalDocsServiceProperties properties) {
        this.s3Client = s3Client;
        this.properties = properties;
    }

    public void storeClinicalDocument(String objectKey, byte[] body) {
        putObject(properties.getClinicalDocumentsBucket(), objectKey, "application/pdf", body);
    }

    public void storePrescription(String objectKey, byte[] body) {
        putObject(properties.getPrescriptionsBucket(), objectKey, "application/pdf", body);
    }

    public byte[] read(String bucket, String objectKey) {
        return s3Client.getObjectAsBytes(
                        GetObjectRequest.builder().bucket(bucket).key(objectKey).build())
                .asByteArray();
    }

    private void putObject(String bucket, String objectKey, String contentType, byte[] body) {
        s3Client.putObject(
                PutObjectRequest.builder()
                        .bucket(bucket)
                        .key(objectKey)
                        .contentType(contentType)
                        .build(),
                RequestBody.fromBytes(body));
    }
}
