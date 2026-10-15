package com.telemedicine.reportingservice.config;

import com.telemedicine.reportingservice.export.MinioExportStorage;
import com.telemedicine.reportingservice.export.MinioExportStorageProperties;
import java.net.URI;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3Configuration;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;

@Configuration
@EnableConfigurationProperties({ReportingServiceProperties.class, MinioProperties.class})
public class ReportingServiceConfiguration {

    @Bean
    S3Client reportingS3Client(MinioProperties minio) {
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
    S3Presigner reportingS3Presigner(MinioProperties minio) {
        return S3Presigner.builder()
                .endpointOverride(URI.create(minio.endpoint()))
                .region(Region.US_EAST_1)
                .credentialsProvider(
                        StaticCredentialsProvider.create(
                                AwsBasicCredentials.create(minio.accessKey(), minio.secretKey())))
                .serviceConfiguration(S3Configuration.builder().pathStyleAccessEnabled(true).build())
                .build();
    }

    @Bean
    MinioExportStorage minioExportStorage(
            S3Client reportingS3Client,
            S3Presigner reportingS3Presigner,
            ReportingServiceProperties properties) {
        return new MinioExportStorage(
                reportingS3Client,
                reportingS3Presigner,
                new MinioExportStorageProperties(
                        properties.getExportBucket(), properties.getExportUrlTtl()));
    }
}
