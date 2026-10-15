package com.telemedicine.common.security.consent;

import com.telemedicine.common.security.propagation.ClaimPropagationInterceptor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

@Configuration
@ConditionalOnProperty(
        prefix = "telemedicine.security.consent",
        name = "http-verifier-enabled",
        havingValue = "true")
public class HttpConsentVerifierConfiguration {

    @Bean
    RestClient waitingRoomConsentRestClient(
            RestClient.Builder builder,
            ClaimPropagationInterceptor claimPropagationInterceptor,
            @Value("${telemedicine.security.consent.waiting-room-url:http://waiting-room-service:8101}")
                    String waitingRoomUrl) {
        return builder
                .baseUrl(waitingRoomUrl)
                .requestInterceptor(claimPropagationInterceptor)
                .build();
    }

    @Bean
    ConsentVerifier httpConsentVerifier(
            @Qualifier("waitingRoomConsentRestClient") RestClient waitingRoomRestClient) {
        return new HttpConsentVerifier(waitingRoomRestClient);
    }
}
