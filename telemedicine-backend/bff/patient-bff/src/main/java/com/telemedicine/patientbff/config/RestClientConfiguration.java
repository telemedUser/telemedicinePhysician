package com.telemedicine.patientbff.config;

import com.telemedicine.common.observability.mdc.MdcClientHttpRequestInterceptor;
import com.telemedicine.common.security.propagation.ClaimPropagationInterceptor;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

@Configuration
@EnableConfigurationProperties(PatientBffProperties.class)
public class RestClientConfiguration {

    @Bean
    @LoadBalanced
    RestClient.Builder loadBalancedRestClientBuilder(
            PatientBffProperties properties,
            ClaimPropagationInterceptor claimPropagationInterceptor,
            MdcClientHttpRequestInterceptor mdcClientHttpRequestInterceptor) {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(
                (int) properties.getRestClient().getConnectTimeout().toMillis());
        requestFactory.setReadTimeout((int) properties.getRestClient().getReadTimeout().toMillis());
        return RestClient.builder()
                .requestFactory(requestFactory)
                .requestInterceptor(claimPropagationInterceptor)
                .requestInterceptor(mdcClientHttpRequestInterceptor);
    }

    @Bean
    RestClient waitingRoomRestClient(
            RestClient.Builder loadBalancedRestClientBuilder, PatientBffProperties properties) {
        return loadBalancedRestClientBuilder
                .clone()
                .baseUrl(toHttpBase(properties.getDownstream().getWaitingRoomService()))
                .build();
    }

    @Bean
    RestClient integrationRestClient(
            RestClient.Builder loadBalancedRestClientBuilder, PatientBffProperties properties) {
        return loadBalancedRestClientBuilder
                .clone()
                .baseUrl(toHttpBase(properties.getDownstream().getIntegrationService()))
                .build();
    }

    @Bean
    RestClient clinicalDocsRestClient(
            RestClient.Builder loadBalancedRestClientBuilder, PatientBffProperties properties) {
        return loadBalancedRestClientBuilder
                .clone()
                .baseUrl(toHttpBase(properties.getDownstream().getClinicalDocsService()))
                .build();
    }

    @Bean
    RestClient identityRestClient(
            RestClient.Builder loadBalancedRestClientBuilder, PatientBffProperties properties) {
        return loadBalancedRestClientBuilder
                .clone()
                .baseUrl(toHttpBase(properties.getDownstream().getIdentityService()))
                .build();
    }

    @Bean
    RestClient videoRestClient(
            RestClient.Builder loadBalancedRestClientBuilder, PatientBffProperties properties) {
        return loadBalancedRestClientBuilder
                .clone()
                .baseUrl(toHttpBase(properties.getDownstream().getVideoService()))
                .build();
    }

    @Bean
    RestClient surveyRestClient(
            RestClient.Builder loadBalancedRestClientBuilder, PatientBffProperties properties) {
        return loadBalancedRestClientBuilder
                .clone()
                .baseUrl(toHttpBase(properties.getDownstream().getSurveyService()))
                .build();
    }

    static String toHttpBase(String url) {
        if (url.startsWith("lb://")) {
            return "http://" + url.substring("lb://".length());
        }
        return url;
    }
}
