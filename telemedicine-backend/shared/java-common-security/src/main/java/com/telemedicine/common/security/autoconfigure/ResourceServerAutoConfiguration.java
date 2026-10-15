package com.telemedicine.common.security.autoconfigure;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.telemedicine.common.security.SecurityMode;
import com.telemedicine.common.security.SecurityModeGuard;
import com.telemedicine.common.security.TelemedicineSecurityProperties;
import com.telemedicine.common.security.access.SecurityContextAccessor;
import com.telemedicine.common.security.annotation.RequiresRole;
import com.telemedicine.common.security.consent.ConsentGuardAspect;
import com.telemedicine.common.security.consent.ConsentVerifier;
import com.telemedicine.common.security.consent.PermissiveConsentVerifier;
import com.telemedicine.common.security.jwt.AudienceValidator;
import com.telemedicine.common.security.jwt.KeycloakRoleConverter;
import com.telemedicine.common.security.method.RequiresRoleAuthorizationManager;
import com.telemedicine.common.security.problem.ProblemAccessDeniedHandler;
import com.telemedicine.common.security.problem.ProblemAuthenticationEntryPoint;
import com.telemedicine.common.security.problem.ProblemHttpWriter;
import com.telemedicine.common.security.propagation.ClaimPropagationInterceptor;
import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.aop.Pointcut;
import org.springframework.aop.support.Pointcuts;
import org.springframework.aop.support.annotation.AnnotationMatchingPointcut;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.autoconfigure.jackson.JacksonAutoConfiguration;
import org.springframework.boot.autoconfigure.security.oauth2.resource.servlet.OAuth2ResourceServerAutoConfiguration;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Role;
import org.springframework.core.Ordered;
import org.springframework.core.env.Environment;
import org.springframework.security.authorization.method.AuthorizationManagerBeforeMethodInterceptor;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.HeadersConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtIssuerValidator;
import org.springframework.security.oauth2.jwt.JwtTimestampValidator;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.header.writers.StaticHeadersWriter;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;

/**
 * Wires the Keycloak resource server, method security, security headers and the consent gate.
 *
 * <p>Registered through {@code AutoConfiguration.imports}, because the libraries sit in {@code
 * com.telemedicine.common.*} and no service component-scans that package: before this existed,
 * {@code SecurityConfig} was on the classpath of seventeen services and active in none.
 *
 * <p>Every bean backs off when the service declares its own, and the whole filter chain backs off
 * when a service already publishes a {@link SecurityFilterChain}.
 */
@AutoConfiguration(
        before = {SecurityAutoConfiguration.class, OAuth2ResourceServerAutoConfiguration.class},
        after = JacksonAutoConfiguration.class)
@ConditionalOnClass({SecurityFilterChain.class, JwtDecoder.class})
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
@ConditionalOnProperty(prefix = "telemedicine.security", name = "enabled", matchIfMissing = true)
@EnableConfigurationProperties(TelemedicineSecurityProperties.class)
@EnableMethodSecurity
public class ResourceServerAutoConfiguration {

    private static final Logger log =
            LoggerFactory.getLogger(ResourceServerAutoConfiguration.class);

    @Bean
    public SecurityModeGuard telemedicineSecurityModeGuard(
            TelemedicineSecurityProperties properties, Environment environment) {
        return new SecurityModeGuard(properties, environment);
    }

    @Bean
    @ConditionalOnMissingBean
    public SecurityContextAccessor telemedicineSecurityContextAccessor(
            TelemedicineSecurityProperties properties) {
        return new SecurityContextAccessor(properties.getJwt());
    }

    @Bean
    @ConditionalOnMissingBean
    public ProblemHttpWriter telemedicineProblemHttpWriter(ObjectMapper objectMapper) {
        return new ProblemHttpWriter(objectMapper);
    }

    @Bean
    @ConditionalOnMissingBean(name = "telemedicineAuthenticationEntryPoint")
    public ProblemAuthenticationEntryPoint telemedicineAuthenticationEntryPoint(
            ProblemHttpWriter writer,
            @Value("${spring.application.name:telemedicine}") String applicationName) {
        return new ProblemAuthenticationEntryPoint(writer, applicationName);
    }

    @Bean
    @ConditionalOnMissingBean(name = "telemedicineAccessDeniedHandler")
    public ProblemAccessDeniedHandler telemedicineAccessDeniedHandler(ProblemHttpWriter writer) {
        return new ProblemAccessDeniedHandler(writer);
    }

    @Bean
    @ConditionalOnMissingBean
    public JwtAuthenticationConverter telemedicineJwtAuthenticationConverter(
            TelemedicineSecurityProperties properties) {
        JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(
                new KeycloakRoleConverter(properties.getJwt().getRoleClients()));
        converter.setPrincipalClaimName(properties.getJwt().getPrincipalClaim());
        return converter;
    }

    /**
     * Built explicitly rather than left to Spring Boot so the Phase 0 JWKS URI is the default and
     * so audience, issuer and clock skew are validated the same way in every service.
     *
     * <p>Constructing the decoder does not contact Keycloak — the key set is fetched on first use —
     * so this is registered in dev mode too, where the filter chain simply never consults it.
     */
    @Bean
    @ConditionalOnMissingBean
    public JwtDecoder telemedicineJwtDecoder(TelemedicineSecurityProperties properties) {
        TelemedicineSecurityProperties.Jwt jwt = properties.getJwt();
        NimbusJwtDecoder decoder = NimbusJwtDecoder.withJwkSetUri(jwt.getJwkSetUri()).build();

        List<OAuth2TokenValidator<Jwt>> validators = new ArrayList<>();
        validators.add(new JwtTimestampValidator(jwt.getClockSkew()));
        if (jwt.getIssuer() != null && !jwt.getIssuer().isBlank()) {
            validators.add(new JwtIssuerValidator(jwt.getIssuer()));
        }
        if (jwt.getAudiences() != null && !jwt.getAudiences().isEmpty()) {
            validators.add(new AudienceValidator(jwt.getAudiences()));
        } else if (properties.getMode() == SecurityMode.JWT) {
            log.warn(
                    "telemedicine.security.jwt.audiences is empty, so any token this realm signs is"
                        + " accepted, including one issued to an unrelated client. Set the expected"
                        + " audience before exposing the service outside development.");
        }
        decoder.setJwtValidator(new DelegatingOAuth2TokenValidator<>(validators));
        return decoder;
    }

    @Bean
    @ConditionalOnMissingBean(SecurityFilterChain.class)
    public SecurityFilterChain telemedicineSecurityFilterChain(
            HttpSecurity http,
            TelemedicineSecurityProperties properties,
            JwtAuthenticationConverter jwtAuthenticationConverter,
            ProblemAuthenticationEntryPoint entryPoint,
            ProblemAccessDeniedHandler accessDeniedHandler)
            throws Exception {

        // Token-authenticated APIs keep no session, so there is no cookie for a cross-site request
        // to ride on and CSRF protection would only reject legitimate clients.
        http.csrf(csrf -> csrf.disable())
                .sessionManagement(
                        session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .headers(headers -> applyHeaders(headers, properties.getHeaders()));

        if (properties.getMode() == SecurityMode.DEV) {
            http.authorizeHttpRequests(requests -> requests.anyRequest().permitAll());
            return http.build();
        }

        http.authorizeHttpRequests(
                        requests -> {
                            for (String path : properties.getPermitAllPaths()) {
                                requests.requestMatchers(new AntPathRequestMatcher(path))
                                        .permitAll();
                            }
                            requests.anyRequest().authenticated();
                        })
                .oauth2ResourceServer(
                        oauth2 ->
                                oauth2.authenticationEntryPoint(entryPoint)
                                        .accessDeniedHandler(accessDeniedHandler)
                                        .jwt(
                                                jwt ->
                                                        jwt.jwtAuthenticationConverter(
                                                                jwtAuthenticationConverter)))
                .exceptionHandling(
                        exceptions ->
                                exceptions
                                        .authenticationEntryPoint(entryPoint)
                                        .accessDeniedHandler(accessDeniedHandler));
        return http.build();
    }

    private static void applyHeaders(
            HeadersConfigurer<HttpSecurity> headers,
            TelemedicineSecurityProperties.Headers properties) {
        if (!properties.isEnabled()) {
            headers.disable();
            return;
        }
        // X-Frame-Options is deliberately off: it cannot express an allow-list, and the physician
        // UI is embedded in the hospital information system in picture-in-picture mode. The CSP
        // frame-ancestors directive below is what actually constrains embedding.
        headers.frameOptions(frame -> frame.disable())
                .contentSecurityPolicy(
                        csp -> csp.policyDirectives(properties.resolvedContentSecurityPolicy()))
                .addHeaderWriter(
                        new StaticHeadersWriter("Referrer-Policy", properties.getReferrerPolicy()))
                .addHeaderWriter(
                        new StaticHeadersWriter(
                                "Permissions-Policy", properties.getPermissionsPolicy()));
        if (properties.isHstsEnabled()) {
            headers.httpStrictTransportSecurity(
                    hsts ->
                            hsts.includeSubDomains(true)
                                    .maxAgeInSeconds(properties.getHstsMaxAge().toSeconds()));
        } else {
            headers.httpStrictTransportSecurity(hsts -> hsts.disable());
        }
    }

    @Bean
    @ConditionalOnMissingBean
    public ClaimPropagationInterceptor telemedicineClaimPropagationInterceptor(
            SecurityContextAccessor securityContext, TelemedicineSecurityProperties properties) {
        return new ClaimPropagationInterceptor(securityContext, properties.getPropagation());
    }

    /** Enforcement for {@code @RequiresRole}, which carries no SpEL expression to evaluate. */
    @Bean
    @Role(BeanDefinition.ROLE_INFRASTRUCTURE)
    @ConditionalOnMissingBean(name = "telemedicineRequiresRoleInterceptor")
    public AuthorizationManagerBeforeMethodInterceptor telemedicineRequiresRoleInterceptor() {
        Pointcut pointcut =
                Pointcuts.union(
                        AnnotationMatchingPointcut.forMethodAnnotation(RequiresRole.class),
                        AnnotationMatchingPointcut.forClassAnnotation(RequiresRole.class));
        AuthorizationManagerBeforeMethodInterceptor interceptor =
                new AuthorizationManagerBeforeMethodInterceptor(
                        pointcut, new RequiresRoleAuthorizationManager());
        // Runs after @PreAuthorize so that a method carrying both is refused by the stricter rule.
        interceptor.setOrder(Ordered.LOWEST_PRECEDENCE - 100);
        return interceptor;
    }

    /**
     * The reactive counterpart of the propagation interceptor, registered only where WebFlux is
     * present so servlet-only services are not made to carry Reactor.
     */
    @Configuration(proxyBeanMethods = false)
    @ConditionalOnClass(name = "org.springframework.web.reactive.function.client.WebClient")
    static class ReactivePropagationConfiguration {

        @Bean
        @ConditionalOnMissingBean
        com.telemedicine.common.security.propagation.ClaimPropagationExchangeFilter
                telemedicineClaimPropagationExchangeFilter(
                        SecurityContextAccessor securityContext,
                        TelemedicineSecurityProperties properties) {
            return new com.telemedicine.common.security.propagation.ClaimPropagationExchangeFilter(
                    securityContext, properties.getPropagation());
        }
    }

    /** The consent gate needs AOP, which this module bundles; the condition keeps it honest. */
    @Configuration(proxyBeanMethods = false)
    @ConditionalOnClass(name = "org.aspectj.lang.annotation.Aspect")
    @ConditionalOnProperty(
            prefix = "telemedicine.security.consent",
            name = "enabled",
            matchIfMissing = true)
    static class ConsentConfiguration {

        @Bean
        @ConditionalOnMissingBean
        ConsentVerifier telemedicineConsentVerifier() {
            return new PermissiveConsentVerifier();
        }

        @Bean
        @ConditionalOnMissingBean
        ConsentGuardAspect telemedicineConsentGuardAspect(
                ConsentVerifier verifier, SecurityContextAccessor securityContext) {
            return new ConsentGuardAspect(verifier, securityContext);
        }
    }
}
