package com.telemedicine.clinicaldocsservice.config;

import com.telemedicine.clinicaldocsservice.storage.ClinicalDocumentStorage;
import java.net.URI;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3Configuration;

@Configuration
@EnableConfigurationProperties({ClinicalDocsServiceProperties.class, MinioProperties.class})
public class ClinicalDocsServiceConfiguration {

    @Bean
    S3Client clinicalDocsS3Client(MinioProperties minio) {
        return S3Client.builder()
                .endpointOverride(URI.create(minio.endpoint()))
                .region(Region.US_EAST_1)
                .credentialsProvider(
                        StaticCredentialsProvider.create(
                                AwsBasicCredentials.create(minio.accessKey(), minio.secretKey())))
                .serviceConfiguration(S3Configuration.builder().pathStyleAccessEnabled(true).build())
                .build();
    }

    @Bean
    ClinicalDocumentStorage clinicalDocumentStorage(
            S3Client clinicalDocsS3Client, ClinicalDocsServiceProperties properties) {
        return new ClinicalDocumentStorage(clinicalDocsS3Client, properties);
    }
}
