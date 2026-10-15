package com.telemedicine.integrationservice.config;

import com.telemedicine.common.core.constants.MdcKeys;
import com.telemedicine.common.integration.acl.IntegrationFailureReporter;
import com.telemedicine.common.persistence.outbox.OutboxRecorder;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import org.slf4j.MDC;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.web.client.RestClient;

@Configuration
@EnableConfigurationProperties(IntegrationServiceProperties.class)
public class IntegrationServiceConfiguration {

    @Bean
    @Primary
    @Profile("postgres")
    IntegrationFailureReporter outboxIntegrationFailureReporter(OutboxRecorder outboxRecorder) {
        IntegrationFailureReporter.IntegrationEventPublisher publisher =
                (subject, payload) -> {
                    outboxRecorder.record(
                            "Integration",
                            subject,
                            payload,
                            correlationId(),
                            subject);
                    return CompletableFuture.completedFuture(null);
                };
        return new IntegrationFailureReporter(Optional.of(publisher));
    }

    @Bean
    RestClient.Builder integrationRestClientBuilder() {
        return RestClient.builder();
    }

    private static String correlationId() {
        String correlationId = MDC.get(MdcKeys.CORRELATION_ID);
        return correlationId != null ? correlationId : "integration-service";
    }
}
