package com.telemedicine.common.web.autoconfigure;

import static org.assertj.core.api.Assertions.assertThat;

import com.telemedicine.common.web.DataAccessExceptionHandler;
import com.telemedicine.common.web.GlobalExceptionHandler;
import com.telemedicine.common.web.TelemedicineWebProperties;
import com.telemedicine.common.web.idempotency.IdempotencyKeyFilter;
import com.telemedicine.common.web.idempotency.IdempotencyStore;
import com.telemedicine.common.web.idempotency.InMemoryIdempotencyStore;
import com.telemedicine.common.web.logging.RequestLoggingFilter;
import com.telemedicine.common.web.openapi.TelemedicineOpenApiCustomizer;
import com.telemedicine.common.web.page.PageRequestSupport;
import java.time.Duration;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.jackson.JacksonAutoConfiguration;
import org.springframework.boot.test.context.FilteredClassLoader;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.boot.test.context.runner.WebApplicationContextRunner;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.filter.ShallowEtagHeaderFilter;

class WebAutoConfigurationTest {

    private final WebApplicationContextRunner runner =
            new WebApplicationContextRunner()
                    .withConfiguration(
                            AutoConfigurations.of(
                                    JacksonAutoConfiguration.class, WebAutoConfiguration.class));

    @Test
    @DisplayName("the shared beans appear without the application scanning com.telemedicine.common")
    void registersSharedBeansByDefault() {
        runner.run(
                context ->
                        assertThat(context)
                                .hasSingleBean(GlobalExceptionHandler.class)
                                .hasSingleBean(PageRequestSupport.class)
                                .hasSingleBean(TelemedicineWebProperties.class)
                                .hasSingleBean(DataAccessExceptionHandler.class));
    }

    @Test
    void requestLoggingIsOnByDefaultAndCanBeDisabled() {
        runner.run(
                context -> assertThat(filterOf(context, RequestLoggingFilter.class)).isPresent());
        runner.withPropertyValues("telemedicine.web.request-logging.enabled=false")
                .run(
                        context ->
                                assertThat(filterOf(context, RequestLoggingFilter.class))
                                        .isEmpty());
    }

    @Test
    void etagIsOffByDefaultAndCanBeEnabled() {
        runner.run(
                context -> assertThat(filterOf(context, ShallowEtagHeaderFilter.class)).isEmpty());
        runner.withPropertyValues("telemedicine.web.etag.enabled=true")
                .run(
                        context ->
                                assertThat(filterOf(context, ShallowEtagHeaderFilter.class))
                                        .isPresent());
    }

    @Test
    @DisplayName("the master switch removes every bean this module contributes")
    void masterSwitchDisablesEverything() {
        runner.withPropertyValues("telemedicine.web.enabled=false")
                .run(
                        context ->
                                assertThat(context)
                                        .doesNotHaveBean(GlobalExceptionHandler.class)
                                        .doesNotHaveBean(PageRequestSupport.class));
    }

    @Test
    @DisplayName("nothing is registered in a non-servlet application")
    void backsOffOutsideAServletApplication() {
        new ApplicationContextRunner()
                .withConfiguration(AutoConfigurations.of(WebAutoConfiguration.class))
                .run(context -> assertThat(context).doesNotHaveBean(GlobalExceptionHandler.class));
    }

    @Test
    @DisplayName("a service that declares its own handler keeps it")
    void userBeansWin() {
        runner.withUserConfiguration(CustomHandlerConfiguration.class)
                .run(
                        context -> {
                            assertThat(context).hasSingleBean(GlobalExceptionHandler.class);
                            assertThat(context.getBean(GlobalExceptionHandler.class))
                                    .isInstanceOf(CustomHandler.class);
                        });
    }

    @Test
    void dataAccessAdviceBacksOffWithoutSpringTx() {
        runner.withClassLoader(
                        new FilteredClassLoader(org.springframework.dao.DataAccessException.class))
                .run(
                        context ->
                                assertThat(context)
                                        .doesNotHaveBean(DataAccessExceptionHandler.class));
    }

    @Test
    void openApiCustomizerBacksOffWithoutSpringdoc() {
        runner.run(
                context -> assertThat(context).hasSingleBean(TelemedicineOpenApiCustomizer.class));
        runner.withClassLoader(
                        new FilteredClassLoader(
                                org.springdoc.core.customizers.OpenApiCustomizer.class))
                .run(
                        context ->
                                assertThat(context)
                                        .doesNotHaveBean(TelemedicineOpenApiCustomizer.class));
    }

    @Test
    void idempotencyIsOptInAndFallsBackToTheInMemoryStore() {
        runner.run(context -> assertThat(context).doesNotHaveBean(IdempotencyStore.class));

        runner.withPropertyValues("telemedicine.web.idempotency.enabled=true")
                .run(
                        context -> {
                            assertThat(context).hasSingleBean(IdempotencyStore.class);
                            assertThat(context.getBean(IdempotencyStore.class))
                                    .isInstanceOf(InMemoryIdempotencyStore.class);
                            assertThat(filterOf(context, IdempotencyKeyFilter.class)).isPresent();
                        });
    }

    @Test
    void propertiesAreBound() {
        runner.withPropertyValues(
                        "telemedicine.web.pagination.default-size=25",
                        "telemedicine.web.pagination.max-size=250",
                        "telemedicine.web.expose-internal-errors=true",
                        "telemedicine.web.idempotency.ttl=2h",
                        "telemedicine.web.request-logging.sample-rate=0.1",
                        "telemedicine.web.request-logging.slow-request-threshold=500ms")
                .run(
                        context -> {
                            TelemedicineWebProperties properties =
                                    context.getBean(TelemedicineWebProperties.class);
                            assertThat(properties.getPagination().getDefaultSize()).isEqualTo(25);
                            assertThat(properties.getPagination().getMaxSize()).isEqualTo(250);
                            assertThat(properties.isExposeInternalErrors()).isTrue();
                            assertThat(properties.getIdempotency().getTtl())
                                    .isEqualTo(Duration.ofHours(2));
                            assertThat(properties.getRequestLogging().getSampleRate())
                                    .isEqualTo(0.1);
                            assertThat(properties.getRequestLogging().getSlowRequestThreshold())
                                    .isEqualTo(Duration.ofMillis(500));
                            assertThat(context.getBean(PageRequestSupport.class).maxSize())
                                    .isEqualTo(250);
                        });
    }

    private static Optional<?> filterOf(
            org.springframework.context.ApplicationContext context, Class<?> filterType) {
        return context.getBeansOfType(FilterRegistrationBean.class).values().stream()
                .filter(registration -> filterType.isInstance(registration.getFilter()))
                .findFirst();
    }

    @Configuration(proxyBeanMethods = false)
    static class CustomHandlerConfiguration {
        @Bean
        GlobalExceptionHandler customHandler(TelemedicineWebProperties properties) {
            return new CustomHandler(properties);
        }
    }

    static class CustomHandler extends GlobalExceptionHandler {
        CustomHandler(TelemedicineWebProperties properties) {
            super(properties);
        }
    }
}
