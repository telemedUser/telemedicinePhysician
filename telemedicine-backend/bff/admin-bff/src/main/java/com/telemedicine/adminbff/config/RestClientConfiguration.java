package com.telemedicine.adminbff.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.telemedicine.common.integration.TelemedicineIntegrationProperties;
import com.telemedicine.common.integration.config.PlatformConfigClient;
import com.telemedicine.common.observability.mdc.MdcClientHttpRequestInterceptor;
import com.telemedicine.common.security.propagation.ClaimPropagationInterceptor;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

@Configuration
@EnableConfigurationProperties(AdminBffProperties.class)
public class RestClientConfiguration {

    @Bean
    @LoadBalanced
    RestClient.Builder loadBalancedRestClientBuilder(
            AdminBffProperties properties,
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
    @Primary
    PlatformConfigClient platformConfigClient(
            RestClient.Builder loadBalancedRestClientBuilder,
            TelemedicineIntegrationProperties integrationProperties,
            ObjectMapper objectMapper) {
        RestClient restClient =
                loadBalancedRestClientBuilder
                        .clone()
                        .baseUrl(toHttpBase(integrationProperties.getConfigServiceBaseUrl()))
                        .build();
        return new PlatformConfigClient(
                restClient, objectMapper, integrationProperties.getConfigCacheTtl());
    }

    @Bean
    RestClient configRestClient(
            RestClient.Builder loadBalancedRestClientBuilder, AdminBffProperties properties) {
        return loadBalancedRestClientBuilder
                .clone()
                .baseUrl(toHttpBase(properties.getDownstream().getConfigService()))
                .build();
    }

    @Bean
    RestClient auditRestClient(
            RestClient.Builder loadBalancedRestClientBuilder, AdminBffProperties properties) {
        return loadBalancedRestClientBuilder
                .clone()
                .baseUrl(toHttpBase(properties.getDownstream().getAuditService()))
                .build();
    }

    @Bean
    RestClient schedulerRestClient(
            RestClient.Builder loadBalancedRestClientBuilder, AdminBffProperties properties) {
        return loadBalancedRestClientBuilder
                .clone()
                .baseUrl(toHttpBase(properties.getDownstream().getSchedulerService()))
                .build();
    }

    @Bean
    RestClient monitoringRestClient(
            RestClient.Builder loadBalancedRestClientBuilder, AdminBffProperties properties) {
        return loadBalancedRestClientBuilder
                .clone()
                .baseUrl(toHttpBase(properties.getDownstream().getMonitoringService()))
                .build();
    }

    @Bean
    RestClient reportingRestClient(
            RestClient.Builder loadBalancedRestClientBuilder, AdminBffProperties properties) {
        return loadBalancedRestClientBuilder
                .clone()
                .baseUrl(toHttpBase(properties.getDownstream().getReportingService()))
                .build();
    }

    static String toHttpBase(String url) {
        if (url.startsWith("lb://")) {
            return "http://" + url.substring("lb://".length());
        }
        return url;
    }
}
