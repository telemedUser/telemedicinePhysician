package com.telemedicine.common.audit.autoconfigure;

import com.telemedicine.common.audit.AuditChainState;
import com.telemedicine.common.audit.AuditEventDispatcher;
import com.telemedicine.common.audit.AuditedAspect;
import com.telemedicine.common.audit.LoggingAuditDispatcher;
import com.telemedicine.common.audit.OutboxAuditDispatcher;
import com.telemedicine.common.audit.TelemedicineAuditProperties;
import com.telemedicine.common.persistence.outbox.OutboxRecorder;
import com.telemedicine.common.security.access.SecurityContextAccessor;
import io.micrometer.core.instrument.MeterRegistry;
import java.util.Optional;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

@AutoConfiguration
@ConditionalOnClass(AuditedAspect.class)
@ConditionalOnProperty(prefix = "telemedicine.audit", name = "enabled", matchIfMissing = true)
@EnableConfigurationProperties(TelemedicineAuditProperties.class)
public class AuditAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public AuditChainState telemedicineAuditChainState() {
        return new AuditChainState();
    }

    @Bean
    @ConditionalOnMissingBean(AuditEventDispatcher.class)
    @ConditionalOnBean(OutboxRecorder.class)
    public AuditEventDispatcher telemedicineOutboxAuditDispatcher(OutboxRecorder outboxRecorder) {
        return new OutboxAuditDispatcher(outboxRecorder);
    }

    @Bean
    @ConditionalOnMissingBean(AuditEventDispatcher.class)
    public AuditEventDispatcher telemedicineLoggingAuditDispatcher() {
        return new LoggingAuditDispatcher();
    }

    @Bean
    @ConditionalOnMissingBean
    public AuditedAspect telemedicineAuditedAspect(
            AuditEventDispatcher dispatcher,
            SecurityContextAccessor securityContext,
            AuditChainState chainState,
            TelemedicineAuditProperties properties,
            ObjectProvider<MeterRegistry> meterRegistry) {
        return new AuditedAspect(
                dispatcher,
                securityContext,
                chainState,
                properties,
                Optional.ofNullable(meterRegistry.getIfAvailable()));
    }
}
