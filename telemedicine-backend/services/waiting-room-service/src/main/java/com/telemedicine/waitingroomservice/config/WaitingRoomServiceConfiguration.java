package com.telemedicine.waitingroomservice.config;

import com.telemedicine.common.observability.mdc.MdcClientHttpRequestInterceptor;
import com.telemedicine.common.security.propagation.ClaimPropagationInterceptor;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

@Configuration
@EnableConfigurationProperties(WaitingRoomServiceProperties.class)
public class WaitingRoomServiceConfiguration {

    @Bean
    RestClient integrationRestClient(
            RestClient.Builder builder,
            WaitingRoomServiceProperties properties,
            ClaimPropagationInterceptor claimPropagationInterceptor,
            MdcClientHttpRequestInterceptor mdcClientHttpRequestInterceptor) {
        return builder
                .baseUrl(normalizeBaseUrl(properties.getIntegrationServiceUrl()))
                .requestInterceptor(claimPropagationInterceptor)
                .requestInterceptor(mdcClientHttpRequestInterceptor)
                .build();
    }

    static String normalizeBaseUrl(String url) {
        if (url.startsWith("lb://")) {
            return "http://" + url.substring("lb://".length());
        }
        return url;
    }
}
