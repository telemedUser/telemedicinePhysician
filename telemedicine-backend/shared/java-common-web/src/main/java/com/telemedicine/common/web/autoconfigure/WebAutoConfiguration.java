package com.telemedicine.common.web.autoconfigure;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.telemedicine.common.web.DataAccessExceptionHandler;
import com.telemedicine.common.web.GlobalExceptionHandler;
import com.telemedicine.common.web.TelemedicineWebProperties;
import com.telemedicine.common.web.idempotency.IdempotencyKeyFilter;
import com.telemedicine.common.web.idempotency.IdempotencyStore;
import com.telemedicine.common.web.idempotency.InMemoryIdempotencyStore;
import com.telemedicine.common.web.logging.RequestLoggingFilter;
import com.telemedicine.common.web.openapi.TelemedicineOpenApiCustomizer;
import com.telemedicine.common.web.page.PageRequestSupport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.autoconfigure.jackson.JacksonAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.web.filter.ShallowEtagHeaderFilter;

/**
 * Registers the shared web layer in every service without touching the service's own code.
 *
 * <p>This class is the fix for the defect that motivated the whole phase. The libraries live in
 * {@code com.telemedicine.common.*} while the applications live in {@code
 * com.telemedicine.<service>}, so component scanning never reached them: {@code
 * GlobalExceptionHandler} sat on the classpath of seventeen services and was instantiated by none
 * of them. Declaring the configuration in {@code AutoConfiguration.imports} means Spring Boot picks
 * it up from the jar itself.
 *
 * <p>Every bean is {@code @ConditionalOnMissingBean}, so a service that needs different behaviour
 * declares its own bean and this one steps aside.
 */
@AutoConfiguration(after = JacksonAutoConfiguration.class)
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
@ConditionalOnProperty(prefix = "telemedicine.web", name = "enabled", matchIfMissing = true)
@EnableConfigurationProperties(TelemedicineWebProperties.class)
public class WebAutoConfiguration {

    private static final Logger log = LoggerFactory.getLogger(WebAutoConfiguration.class);

    @Bean
    @ConditionalOnMissingBean
    public GlobalExceptionHandler telemedicineGlobalExceptionHandler(
            TelemedicineWebProperties properties) {
        if (properties.isExposeInternalErrors()) {
            log.warn(
                    "telemedicine.web.expose-internal-errors is enabled: 5xx responses will include"
                        + " exception types and messages. Never enable this outside development.");
        }
        return new GlobalExceptionHandler(properties);
    }

    @Bean
    @ConditionalOnMissingBean
    public PageRequestSupport telemedicinePageRequestSupport(TelemedicineWebProperties properties) {
        return new PageRequestSupport(properties);
    }

    // Conditions on FilterRegistrationBean beans are keyed by name: the generic parameter is
    // erased, so a plain @ConditionalOnMissingBean would make the second filter back off merely
    // because the first one already registered.
    @Bean
    @ConditionalOnMissingBean(name = "telemedicineRequestLoggingFilter")
    @ConditionalOnProperty(
            prefix = "telemedicine.web.request-logging",
            name = "enabled",
            matchIfMissing = true)
    public FilterRegistrationBean<RequestLoggingFilter> telemedicineRequestLoggingFilter(
            TelemedicineWebProperties properties) {
        FilterRegistrationBean<RequestLoggingFilter> registration =
                new FilterRegistrationBean<>(
                        new RequestLoggingFilter(properties.getRequestLogging()));
        // Runs just inside the correlation filter so every log line already has the MDC populated.
        registration.setOrder(Ordered.HIGHEST_PRECEDENCE + 20);
        return registration;
    }

    @Bean
    @ConditionalOnMissingBean(name = "telemedicineEtagFilter")
    @ConditionalOnProperty(prefix = "telemedicine.web.etag", name = "enabled", havingValue = "true")
    public FilterRegistrationBean<ShallowEtagHeaderFilter> telemedicineEtagFilter() {
        FilterRegistrationBean<ShallowEtagHeaderFilter> registration =
                new FilterRegistrationBean<>(new ShallowEtagHeaderFilter());
        registration.setOrder(Ordered.HIGHEST_PRECEDENCE + 30);
        return registration;
    }

    /**
     * Only registered when Spring's DAO exception hierarchy is present, so services without a
     * datasource are not forced to carry spring-tx.
     */
    @Configuration(proxyBeanMethods = false)
    @ConditionalOnClass(name = "org.springframework.dao.DataAccessException")
    static class DataAccessAdviceConfiguration {

        @Bean
        @ConditionalOnMissingBean
        DataAccessExceptionHandler telemedicineDataAccessExceptionHandler() {
            return new DataAccessExceptionHandler();
        }
    }

    /** Idempotency is opt-in; it becomes mandatory for the payment-adjacent flows in Phase 3. */
    @Configuration(proxyBeanMethods = false)
    @ConditionalOnProperty(
            prefix = "telemedicine.web.idempotency",
            name = "enabled",
            havingValue = "true")
    static class IdempotencyConfiguration {

        @Bean
        @ConditionalOnMissingBean(IdempotencyStore.class)
        IdempotencyStore telemedicineIdempotencyStore() {
            log.warn(
                    "No shared IdempotencyStore bean found; falling back to the in-memory store."
                        + " This guarantees nothing across replicas. Add java-common-persistence"
                        + " for the JPA or Redis implementation before running more than one"
                        + " instance.");
            return new InMemoryIdempotencyStore();
        }

        @Bean
        @ConditionalOnMissingBean(name = "telemedicineIdempotencyKeyFilter")
        FilterRegistrationBean<IdempotencyKeyFilter> telemedicineIdempotencyKeyFilter(
                IdempotencyStore store,
                TelemedicineWebProperties properties,
                ObjectMapper objectMapper) {
            FilterRegistrationBean<IdempotencyKeyFilter> registration =
                    new FilterRegistrationBean<>(
                            new IdempotencyKeyFilter(
                                    store, properties.getIdempotency(), objectMapper));
            registration.setOrder(Ordered.HIGHEST_PRECEDENCE + 40);
            return registration;
        }
    }

    /** springdoc is an optional dependency; the customizer only exists where it is on the path. */
    @Configuration(proxyBeanMethods = false)
    @ConditionalOnClass(name = "org.springdoc.core.customizers.OpenApiCustomizer")
    static class OpenApiConfiguration {

        @Bean
        @ConditionalOnMissingBean
        TelemedicineOpenApiCustomizer telemedicineOpenApiCustomizer(
                @org.springframework.beans.factory.annotation.Value(
                                "${spring.security.oauth2.resourceserver.jwt.jwk-set-uri:"
                                    + "http://keycloak:8080/realms/telemedicine/protocol/openid-connect/certs}")
                        String jwkSetUri) {
            return new TelemedicineOpenApiCustomizer(jwkSetUri);
        }
    }
}
