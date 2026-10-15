package com.telemedicine.physicianservice.config;

import java.util.List;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Local-development security ONLY (active on the {@code dev} profile). Declaring a {@link
 * SecurityFilterChain} bean here suppresses the shared Keycloak resource-server chain
 * (its auto-config backs off on {@code @ConditionalOnMissingBean(SecurityFilterChain.class)}).
 *
 * <p>It permits every request and installs an <em>anonymous</em> authentication that carries the
 * PHYSICIAN and ADMIN roles. That matters because the platform's method security
 * ({@code @IsPhysician} / {@code @RequiresRole}) still runs in dev and needs an authenticated
 * principal with the right authorities — the standalone physician app signs in with a dev bypass
 * and sends no real token. In any real environment the {@code dev} profile is absent and the
 * normal JWT chain applies.
 */
@Configuration
@Profile("dev")
public class DevSecurityConfiguration {

    @Bean
    SecurityFilterChain devSecurityFilterChain(HttpSecurity http) throws Exception {
        http.csrf(csrf -> csrf.disable())
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .headers(headers -> headers.frameOptions(frame -> frame.disable()))
                .authorizeHttpRequests(auth -> auth.anyRequest().permitAll())
                .anonymous(
                        anon ->
                                anon.principal("dev-physician")
                                        .authorities(
                                                List.of(
                                                        new SimpleGrantedAuthority("ROLE_PHYSICIAN"),
                                                        new SimpleGrantedAuthority("ROLE_ADMIN"))));
        return http.build();
    }
}
