package com.telemedicine.common.persistence.autoconfigure;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.telemedicine.common.core.time.TimeProvider;
import com.telemedicine.common.events.EventObjectMappers;
import com.telemedicine.common.events.consumer.ProcessedEventStore;
import com.telemedicine.common.persistence.TelemedicineOutboxProperties;
import com.telemedicine.common.persistence.TelemedicinePersistenceProperties;
import com.telemedicine.common.persistence.audit.TelemedicineAuditorAware;
import com.telemedicine.common.persistence.cache.TelemedicineCacheConfiguration;
import com.telemedicine.common.persistence.encryption.PhiEncryptionService;
import com.telemedicine.common.persistence.idempotency.IdempotencyKeyRepository;
import com.telemedicine.common.persistence.idempotency.JpaIdempotencyStore;
import com.telemedicine.common.persistence.idempotency.RedisIdempotencyStore;
import com.telemedicine.common.persistence.outbox.OutboxEventRepository;
import com.telemedicine.common.persistence.outbox.OutboxPublisher;
import com.telemedicine.common.persistence.outbox.OutboxRecorder;
import com.telemedicine.common.persistence.processed.JpaProcessedEventStore;
import com.telemedicine.common.persistence.processed.ProcessedEventRepository;
import com.telemedicine.common.security.access.SecurityContextAccessor;
import com.telemedicine.common.web.idempotency.IdempotencyStore;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.util.concurrent.ScheduledFuture;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.AuditorAware;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.transaction.PlatformTransactionManager;

@AutoConfiguration(after = HibernateJpaAutoConfiguration.class)
@ConditionalOnProperty(prefix = "telemedicine.persistence", name = "enabled", matchIfMissing = true)
@EnableConfigurationProperties({
    TelemedicinePersistenceProperties.class,
    TelemedicineOutboxProperties.class
})
@EnableJpaAuditing
@org.springframework.context.annotation.Import(TelemedicineCacheConfiguration.class)
public class PersistenceAutoConfiguration {

    private static final Logger log = LoggerFactory.getLogger(PersistenceAutoConfiguration.class);

    @Bean
    @ConditionalOnMissingBean(TimeProvider.class)
    TimeProvider telemedicineTimeProvider() {
        return TimeProvider.system();
    }

    @Bean
    @ConditionalOnMissingBean(MeterRegistry.class)
    MeterRegistry telemedicinePersistenceMeterRegistry() {
        return new SimpleMeterRegistry();
    }

    @Bean
    @ConditionalOnMissingBean(name = "outboxObjectMapper")
    ObjectMapper outboxObjectMapper() {
        return EventObjectMappers.create();
    }

    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnProperty(prefix = "telemedicine.persistence.phi-encryption", name = "key")
    PhiEncryptionService telemedicinePhiEncryptionService(
            TelemedicinePersistenceProperties properties) {
        return PhiEncryptionService.fromKeyMaterial(
                properties.getPhiEncryption().getKey(),
                properties.getPhiEncryption().getActiveKeyId());
    }

    @Configuration(proxyBeanMethods = false)
    @ConditionalOnClass(SecurityContextAccessor.class)
    static class AuditingConfiguration {

        @Bean
        @ConditionalOnMissingBean
        AuditorAware<String> telemedicineAuditorAware(
                ObjectProvider<SecurityContextAccessor> securityContextAccessor) {
            SecurityContextAccessor accessor =
                    securityContextAccessor.getIfAvailable(SecurityContextAccessor::new);
            return new TelemedicineAuditorAware(accessor);
        }
    }

    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnBean(OutboxEventRepository.class)
    OutboxRecorder telemedicineOutboxRecorder(
            OutboxEventRepository repository, ObjectMapper outboxObjectMapper) {
        return new OutboxRecorder(repository, outboxObjectMapper);
    }

    @Bean(destroyMethod = "shutdown")
    @ConditionalOnProperty(prefix = "telemedicine.outbox", name = "enabled", matchIfMissing = true)
    @ConditionalOnBean({OutboxEventRepository.class, KafkaTemplate.class})
    ThreadPoolTaskScheduler telemedicineOutboxTaskScheduler() {
        ThreadPoolTaskScheduler scheduler = new ThreadPoolTaskScheduler();
        scheduler.setPoolSize(1);
        scheduler.setThreadNamePrefix("outbox-");
        scheduler.setRemoveOnCancelPolicy(true);
        scheduler.initialize();
        return scheduler;
    }

    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnBean({OutboxEventRepository.class, KafkaTemplate.class})
    @ConditionalOnProperty(prefix = "telemedicine.outbox", name = "enabled", matchIfMissing = true)
    OutboxPublisher telemedicineOutboxPublisher(
            OutboxEventRepository repository,
            KafkaTemplate<String, String> kafkaTemplate,
            ObjectMapper outboxObjectMapper,
            TelemedicineOutboxProperties outboxProperties,
            TimeProvider timeProvider,
            PlatformTransactionManager transactionManager,
            MeterRegistry meterRegistry) {
        return new OutboxPublisher(
                repository,
                kafkaTemplate,
                outboxObjectMapper,
                outboxProperties,
                timeProvider,
                transactionManager,
                meterRegistry);
    }

    @Bean
    @ConditionalOnBean({OutboxPublisher.class, ThreadPoolTaskScheduler.class})
    @ConditionalOnProperty(prefix = "telemedicine.outbox", name = "enabled", matchIfMissing = true)
    ScheduledFuture<?> telemedicineOutboxPollingTask(
            OutboxPublisher publisher,
            ThreadPoolTaskScheduler telemedicineOutboxTaskScheduler,
            TelemedicineOutboxProperties outboxProperties) {
        return telemedicineOutboxTaskScheduler.scheduleWithFixedDelay(
                publisher::publishPending, outboxProperties.getPollInterval());
    }

    @Bean
    @ConditionalOnMissingBean(ProcessedEventStore.class)
    @ConditionalOnBean(ProcessedEventRepository.class)
    ProcessedEventStore telemedicineProcessedEventStore(
            ProcessedEventRepository repository, TimeProvider timeProvider) {
        return new JpaProcessedEventStore(repository, timeProvider);
    }

    @Bean
    @ConditionalOnMissingBean(IdempotencyStore.class)
    @ConditionalOnBean(IdempotencyKeyRepository.class)
    IdempotencyStore telemedicineIdempotencyStore(
            TelemedicinePersistenceProperties properties,
            IdempotencyKeyRepository repository,
            ObjectProvider<StringRedisTemplate> redisTemplate,
            ObjectMapper outboxObjectMapper,
            TimeProvider timeProvider) {
        String store = properties.getIdempotency().getStore();
        if (shouldUseRedis(store, redisTemplate.getIfAvailable())) {
            log.info("Using Redis IdempotencyStore");
            return new RedisIdempotencyStore(
                    redisTemplate.getObject(), outboxObjectMapper, timeProvider);
        }
        log.info("Using JPA IdempotencyStore");
        return new JpaIdempotencyStore(repository, timeProvider);
    }

    private static boolean shouldUseRedis(String store, StringRedisTemplate redisTemplate) {
        if (redisTemplate == null) {
            return false;
        }
        return "redis".equalsIgnoreCase(store) || "auto".equalsIgnoreCase(store);
    }
}
