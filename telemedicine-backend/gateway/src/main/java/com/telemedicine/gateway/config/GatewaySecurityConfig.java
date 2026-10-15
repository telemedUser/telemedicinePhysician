package com.telemedicine.gateway.config;

import com.telemedicine.common.security.Roles;
import com.telemedicine.common.security.SecurityMode;
import com.telemedicine.common.security.SecurityModeGuard;
import com.telemedicine.common.security.TelemedicineSecurityProperties;
import com.telemedicine.common.security.jwt.AudienceValidator;
import com.telemedicine.common.security.jwt.KeycloakRoleConverter;
import java.util.ArrayList;
import java.util.List;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.core.env.Environment;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusReactiveJwtDecoder;
import org.springframework.security.oauth2.jwt.ReactiveJwtDecoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.ReactiveJwtAuthenticationConverterAdapter;
import org.springframework.security.web.server.SecurityWebFilterChain;

@Configuration
@EnableWebFluxSecurity
@EnableConfigurationProperties(TelemedicineSecurityProperties.class)
@Profile("!dev")
public class GatewaySecurityConfig {

    @Bean
    SecurityModeGuard gatewaySecurityModeGuard(
            TelemedicineSecurityProperties properties, Environment environment) {
        return new SecurityModeGuard(properties, environment);
    }

    @Bean
    SecurityWebFilterChain springSecurityFilterChain(
            ServerHttpSecurity http, TelemedicineSecurityProperties properties) {
        if (properties.getMode() == SecurityMode.DEV) {
            return http.csrf(ServerHttpSecurity.CsrfSpec::disable)
                    .authorizeExchange(exchange -> exchange.anyExchange().permitAll())
                    .build();
        }

        JwtAuthenticationConverter jwtConverter = new JwtAuthenticationConverter();
        jwtConverter.setJwtGrantedAuthoritiesConverter(
                new KeycloakRoleConverter(properties.getJwt().getRoleClients()));

        return http.csrf(ServerHttpSecurity.CsrfSpec::disable)
                .authorizeExchange(
                        exchange ->
                                exchange.pathMatchers(
                                                "/actuator/health",
                                                "/actuator/health/**",
                                                "/actuator/info",
                                                "/v3/api-docs",
                                                "/v3/api-docs/**",
                                                "/swagger-ui.html",
                                                "/swagger-ui/**")
                                        .permitAll()
                                        .pathMatchers("/bff/patient/**")
                                        .hasRole(Roles.REALM_PATIENT)
                                        .pathMatchers("/bff/physician/**")
                                        .hasRole(Roles.REALM_PHYSICIAN)
                                        .pathMatchers("/bff/admin/**")
                                        .hasRole(Roles.REALM_ADMIN)
                                        .pathMatchers("/api/v1/config/**")
                                        .hasRole(Roles.REALM_ADMIN)
                                        .pathMatchers("/api/v1/audit/**")
                                        .hasRole(Roles.REALM_ADMIN)
                                        .pathMatchers("/api/v1/scheduler/**")
                                        .hasRole(Roles.REALM_ADMIN)
                                        .pathMatchers("/sim/**")
                                        .authenticated()
                                        .anyExchange()
                                        .authenticated())
                .oauth2ResourceServer(
                        oauth2 ->
                                oauth2.jwt(
                                        jwt ->
                                                jwt.jwtAuthenticationConverter(
                                                        new ReactiveJwtAuthenticationConverterAdapter(
                                                                jwtConverter))))
                .build();
    }

    @Bean
    ReactiveJwtDecoder reactiveJwtDecoder(TelemedicineSecurityProperties properties) {
        TelemedicineSecurityProperties.Jwt jwt = properties.getJwt();
        NimbusReactiveJwtDecoder decoder =
                NimbusReactiveJwtDecoder.withJwkSetUri(jwt.getJwkSetUri()).build();
        List<OAuth2TokenValidator<Jwt>> validators = new ArrayList<>();
        if (jwt.getIssuer() != null && !jwt.getIssuer().isBlank()) {
            validators.add(JwtValidators.createDefaultWithIssuer(jwt.getIssuer()));
        }
        if (jwt.getAudiences() != null && !jwt.getAudiences().isEmpty()) {
            validators.add(new AudienceValidator(jwt.getAudiences()));
        }
        if (!validators.isEmpty()) {
            decoder.setJwtValidator(new DelegatingOAuth2TokenValidator<>(validators));
        }
        return decoder;
    }
}
