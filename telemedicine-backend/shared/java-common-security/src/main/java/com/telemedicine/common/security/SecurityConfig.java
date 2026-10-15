package com.telemedicine.common.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Minimal resource-server chain, superseded by {@link
 * com.telemedicine.common.security.autoconfigure.ResourceServerAutoConfiguration}.
 *
 * <p>Retained so that any service explicitly importing it keeps compiling. It is no longer worth
 * using: it performs no audience or issuer validation, maps no Keycloak roles (so every {@code
 * hasRole} check fails), sends no security headers, and answers 401 and 403 with an empty body
 * instead of a problem document.
 *
 * <p>Importing this class suppresses the auto-configured chain, which backs off whenever a {@link
 * SecurityFilterChain} bean already exists. Delete the import to adopt the shared one.
 *
 * @deprecated since 1.0.0, for removal in 2.0.0. Remove the import and let the auto-configuration
 *     register the chain, or copy this class into the service if the reduced behaviour is genuinely
 *     wanted.
 */
@Deprecated(since = "1.0.0", forRemoval = true)
@Configuration
@Profile("!dev & !mock")
public class SecurityConfig {

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http.csrf(csrf -> csrf.disable())
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(
                        auth ->
                                auth.requestMatchers("/actuator/**", "/health")
                                        .permitAll()
                                        .anyRequest()
                                        .authenticated())
                .oauth2ResourceServer(oauth2 -> oauth2.jwt(jwt -> {}));
        return http.build();
    }
}
