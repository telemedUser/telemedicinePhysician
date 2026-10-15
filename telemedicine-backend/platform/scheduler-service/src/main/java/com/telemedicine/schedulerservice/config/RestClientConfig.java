package com.telemedicine.schedulerservice.config;

import com.telemedicine.common.observability.mdc.MdcClientHttpRequestInterceptor;
import com.telemedicine.common.security.propagation.ClaimPropagationInterceptor;
import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.web.client.ClientHttpRequestFactories;
import org.springframework.boot.web.client.ClientHttpRequestFactorySettings;
import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

@Configuration
public class RestClientConfig {

    @Bean
    @LoadBalanced
    RestClient.Builder loadBalancedRestClientBuilder(
            ClaimPropagationInterceptor claimPropagationInterceptor,
            SchedulerProperties properties) {
        ClientHttpRequestFactorySettings settings =
                ClientHttpRequestFactorySettings.DEFAULTS
                        .withConnectTimeout(properties.downstreamTimeout())
                        .withReadTimeout(properties.downstreamTimeout());

        return RestClient.builder()
                .requestFactory(ClientHttpRequestFactories.get(settings))
                .requestInterceptor(claimPropagationInterceptor)
                .requestInterceptor(new MdcClientHttpRequestInterceptor());
    }

    @ConfigurationProperties(prefix = "telemedicine.scheduler")
    record SchedulerProperties(Duration downstreamTimeout) {
        SchedulerProperties {
            if (downstreamTimeout == null) {
                downstreamTimeout = Duration.ofSeconds(10);
            }
        }
    }
}
