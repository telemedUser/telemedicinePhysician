package com.telemedicine.common.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Permit-everything chain for local development, superseded by {@code
 * telemedicine.security.mode=DEV}.
 *
 * <p>The property is the safer mechanism: it is checked at startup by {@link SecurityModeGuard},
 * which refuses to boot if the bypass is active on a protected profile. This class has no such
 * guard — a service that ends up running with the {@code dev} profile in a real environment serves
 * every endpoint unauthenticated and looks perfectly healthy doing it.
 *
 * @deprecated since 1.0.0, for removal in 2.0.0. Set {@code telemedicine.security.mode=DEV}
 *     instead.
 */
@Deprecated(since = "1.0.0", forRemoval = true)
@Configuration
@Profile({"dev", "mock"})
public class DevSecurityConfig {

    @Bean
    SecurityFilterChain devSecurityFilterChain(HttpSecurity http) throws Exception {
        http.csrf(csrf -> csrf.disable())
                .authorizeHttpRequests(auth -> auth.anyRequest().permitAll());
        return http.build();
    }
}
