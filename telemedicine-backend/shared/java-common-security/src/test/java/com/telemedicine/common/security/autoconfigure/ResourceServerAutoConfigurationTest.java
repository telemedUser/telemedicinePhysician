package com.telemedicine.common.security.autoconfigure;

import static org.assertj.core.api.Assertions.assertThat;

import com.telemedicine.common.security.SecurityConfig;
import com.telemedicine.common.security.SecurityMode;
import com.telemedicine.common.security.SecurityModeGuard;
import com.telemedicine.common.security.TelemedicineSecurityProperties;
import com.telemedicine.common.security.access.SecurityContextAccessor;
import com.telemedicine.common.security.consent.ConsentDecision;
import com.telemedicine.common.security.consent.ConsentGuardAspect;
import com.telemedicine.common.security.consent.ConsentVerifier;
import com.telemedicine.common.security.consent.PermissiveConsentVerifier;
import com.telemedicine.common.security.problem.ProblemAccessDeniedHandler;
import com.telemedicine.common.security.problem.ProblemAuthenticationEntryPoint;
import com.telemedicine.common.security.propagation.ClaimPropagationExchangeFilter;
import com.telemedicine.common.security.propagation.ClaimPropagationInterceptor;
import java.time.Duration;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.jackson.JacksonAutoConfiguration;
import org.springframework.boot.autoconfigure.security.oauth2.resource.servlet.OAuth2ResourceServerAutoConfiguration;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.boot.test.context.runner.WebApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.web.SecurityFilterChain;

class ResourceServerAutoConfigurationTest {

    private final WebApplicationContextRunner runner =
            new WebApplicationContextRunner()
                    .withConfiguration(
                            AutoConfigurations.of(
                                    JacksonAutoConfiguration.class,
                                    ResourceServerAutoConfiguration.class,
                                    SecurityAutoConfiguration.class,
                                    OAuth2ResourceServerAutoConfiguration.class));

    @Test
    @DisplayName("a service gets the whole security layer without importing anything")
    void registersEverythingByDefault() {
        runner.run(
                context ->
                        assertThat(context)
                                .hasSingleBean(SecurityFilterChain.class)
                                .hasSingleBean(SecurityContextAccessor.class)
                                .hasSingleBean(SecurityModeGuard.class)
                                .hasSingleBean(ProblemAuthenticationEntryPoint.class)
                                .hasSingleBean(ProblemAccessDeniedHandler.class)
                                .hasSingleBean(ClaimPropagationInterceptor.class)
                                .hasSingleBean(ConsentGuardAspect.class)
                                .hasSingleBean(ConsentVerifier.class));
    }

    @Test
    void bindsItsProperties() {
        runner.withPropertyValues(
                        "telemedicine.security.mode=DEV",
                        "telemedicine.security.jwt.jwk-set-uri=http://kc/realms/x/certs",
                        "telemedicine.security.jwt.issuer=http://kc/realms/x",
                        "telemedicine.security.jwt.audiences=telemedicine-api,gateway",
                        "telemedicine.security.jwt.clock-skew=15s",
                        "telemedicine.security.headers.frame-ancestors='self',https://his.example",
                        "telemedicine.security.propagation.forward-authorization=false")
                .run(
                        context -> {
                            TelemedicineSecurityProperties properties =
                                    context.getBean(TelemedicineSecurityProperties.class);
                            assertThat(properties.getMode()).isEqualTo(SecurityMode.DEV);
                            assertThat(properties.getJwt().getJwkSetUri())
                                    .isEqualTo("http://kc/realms/x/certs");
                            assertThat(properties.getJwt().getIssuer())
                                    .isEqualTo("http://kc/realms/x");
                            assertThat(properties.getJwt().getAudiences())
                                    .containsExactly("telemedicine-api", "gateway");
                            assertThat(properties.getJwt().getClockSkew())
                                    .isEqualTo(Duration.ofSeconds(15));
                            assertThat(properties.getHeaders().getFrameAncestors())
                                    .containsExactly("'self'", "https://his.example");
                            assertThat(properties.getPropagation().isForwardAuthorization())
                                    .isFalse();
                        });
    }

    @Test
    void canBeTurnedOffEntirely() {
        runner.withPropertyValues("telemedicine.security.enabled=false")
                .run(
                        context ->
                                assertThat(context)
                                        .doesNotHaveBean(SecurityContextAccessor.class)
                                        .doesNotHaveBean(SecurityModeGuard.class));
    }

    @Test
    @DisplayName("a service that declares its own filter chain keeps it")
    void backsOffFromAnApplicationSuppliedChain() {
        runner.withUserConfiguration(OwnChainConfiguration.class)
                .run(
                        context -> {
                            assertThat(context).hasSingleBean(SecurityFilterChain.class);
                            assertThat(context.getBeanNamesForType(SecurityFilterChain.class))
                                    .containsExactly("applicationChain");
                        });
    }

    @Test
    void backsOffFromAnApplicationSuppliedConsentVerifier() {
        runner.withUserConfiguration(OwnConsentConfiguration.class)
                .run(
                        context ->
                                assertThat(context.getBean(ConsentVerifier.class))
                                        .isNotInstanceOf(PermissiveConsentVerifier.class));
    }

    @Test
    void consentEnforcementCanBeDisabled() {
        runner.withPropertyValues("telemedicine.security.consent.enabled=false")
                .run(
                        context ->
                                assertThat(context)
                                        .doesNotHaveBean(ConsentGuardAspect.class)
                                        .doesNotHaveBean(ConsentVerifier.class));
    }

    @Test
    @DisplayName("the reactive propagation filter appears only where WebFlux is on the classpath")
    void registersTheReactiveFilterOnlyWithWebflux() {
        runner.run(
                context -> assertThat(context).hasSingleBean(ClaimPropagationExchangeFilter.class));

        runner.withClassLoader(
                        new org.springframework.boot.test.context.FilteredClassLoader(
                                org.springframework.web.reactive.function.client.WebClient.class))
                .run(
                        context ->
                                assertThat(context)
                                        .doesNotHaveBean(ClaimPropagationExchangeFilter.class));
    }

    @Test
    @DisplayName("nothing is registered outside a servlet application, such as in a batch job")
    void staysOutOfNonWebApplications() {
        new ApplicationContextRunner()
                .withConfiguration(
                        AutoConfigurations.of(
                                JacksonAutoConfiguration.class,
                                ResourceServerAutoConfiguration.class))
                .run(context -> assertThat(context).doesNotHaveBean(SecurityFilterChain.class));
    }

    @Test
    @DisplayName("the deprecated SecurityConfig still suppresses the shared chain, as documented")
    void legacyConfigurationStillWins() {
        // That class matches paths with Spring Security's MVC matcher, which needs the MVC
        // infrastructure present. The shared chain uses AntPathRequestMatcher precisely so it does
        // not impose that coupling on services.
        runner.withConfiguration(
                        AutoConfigurations.of(
                                org.springframework.boot.autoconfigure.web.servlet
                                        .WebMvcAutoConfiguration.class))
                .withUserConfiguration(SecurityConfig.class)
                .run(
                        context -> {
                            assertThat(context).hasSingleBean(SecurityFilterChain.class);
                            assertThat(context.getBeanNamesForType(SecurityFilterChain.class))
                                    .containsExactly("securityFilterChain");
                        });
    }

    @Configuration(proxyBeanMethods = false)
    static class OwnChainConfiguration {

        @Bean
        SecurityFilterChain applicationChain(
                org.springframework.security.config.annotation.web.builders.HttpSecurity http)
                throws Exception {
            return http.authorizeHttpRequests(requests -> requests.anyRequest().permitAll())
                    .build();
        }
    }

    @Configuration(proxyBeanMethods = false)
    static class OwnConsentConfiguration {

        @Bean
        ConsentVerifier applicationConsentVerifier() {
            return (subjectId, consentType, version) -> ConsentDecision.missing();
        }
    }
}
