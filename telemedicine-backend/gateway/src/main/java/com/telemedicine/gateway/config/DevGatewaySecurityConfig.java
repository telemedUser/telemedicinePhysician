package com.telemedicine.gateway.config;

import com.telemedicine.common.security.SecurityModeGuard;
import com.telemedicine.common.security.TelemedicineSecurityProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.core.env.Environment;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.web.server.SecurityWebFilterChain;

@Configuration
@EnableWebFluxSecurity
@EnableConfigurationProperties(TelemedicineSecurityProperties.class)
@Profile("dev")
public class DevGatewaySecurityConfig {

    @Bean
    SecurityModeGuard devGatewaySecurityModeGuard(
            TelemedicineSecurityProperties properties, Environment environment) {
        return new SecurityModeGuard(properties, environment);
    }

    @Bean
    SecurityWebFilterChain devSecurity(ServerHttpSecurity http) {
        return http.csrf(ServerHttpSecurity.CsrfSpec::disable)
                .authorizeExchange(exchange -> exchange.anyExchange().permitAll())
                .build();
    }
}
