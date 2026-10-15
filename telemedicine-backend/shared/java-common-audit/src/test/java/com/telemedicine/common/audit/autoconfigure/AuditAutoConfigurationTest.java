package com.telemedicine.common.audit.autoconfigure;

import static org.assertj.core.api.Assertions.assertThat;

import com.telemedicine.common.audit.AuditChainState;
import com.telemedicine.common.audit.AuditEventDispatcher;
import com.telemedicine.common.audit.AuditedAspect;
import com.telemedicine.common.audit.LoggingAuditDispatcher;
import com.telemedicine.common.audit.OutboxAuditDispatcher;
import com.telemedicine.common.audit.TelemedicineAuditProperties;
import com.telemedicine.common.persistence.outbox.OutboxRecorder;
import com.telemedicine.common.security.access.SecurityContextAccessor;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

class AuditAutoConfigurationTest {

    private final ApplicationContextRunner runner =
            new ApplicationContextRunner()
                    .withConfiguration(AutoConfigurations.of(AuditAutoConfiguration.class));

    @Test
    void registersAspectAndLoggingFallbackByDefault() {
        runner.withUserConfiguration(SecurityContextConfiguration.class)
                .run(
                        context ->
                                assertThat(context)
                                        .hasSingleBean(AuditedAspect.class)
                                        .hasSingleBean(AuditChainState.class)
                                        .hasSingleBean(TelemedicineAuditProperties.class)
                                        .hasSingleBean(LoggingAuditDispatcher.class));
    }

    @Test
    void prefersOutboxDispatcherWhenOutboxRecorderPresent() {
        runner.withUserConfiguration(SecurityContextConfiguration.class, OutboxConfiguration.class)
                .run(
                        context ->
                                assertThat(context.getBean(AuditEventDispatcher.class))
                                        .isInstanceOf(OutboxAuditDispatcher.class));
    }

    @Test
    void masterSwitchDisablesBeans() {
        runner.withPropertyValues("telemedicine.audit.enabled=false")
                .run(context -> assertThat(context).doesNotHaveBean(AuditedAspect.class));
    }

    @Test
    void propertiesAreBound() {
        runner.withUserConfiguration(SecurityContextConfiguration.class)
                .withPropertyValues(
                        "telemedicine.audit.system-actor-id=scheduler",
                        "telemedicine.audit.system-actor-role=SCHEDULER",
                        "telemedicine.audit.phi-safe-by-default=false")
                .run(
                        context -> {
                            TelemedicineAuditProperties properties =
                                    context.getBean(TelemedicineAuditProperties.class);
                            assertThat(properties.getSystemActorId()).isEqualTo("scheduler");
                            assertThat(properties.getSystemActorRole()).isEqualTo("SCHEDULER");
                            assertThat(properties.isPhiSafeByDefault()).isFalse();
                        });
    }

    @Configuration(proxyBeanMethods = false)
    static class SecurityContextConfiguration {
        @Bean
        SecurityContextAccessor securityContextAccessor() {
            return new SecurityContextAccessor();
        }
    }

    @Configuration(proxyBeanMethods = false)
    static class OutboxConfiguration {
        @Bean
        OutboxRecorder outboxRecorder() {
            return org.mockito.Mockito.mock(OutboxRecorder.class);
        }
    }
}
