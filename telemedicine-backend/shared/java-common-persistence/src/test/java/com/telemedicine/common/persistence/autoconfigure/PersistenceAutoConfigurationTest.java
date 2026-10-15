package com.telemedicine.common.persistence.autoconfigure;

import static org.assertj.core.api.Assertions.assertThat;

import com.telemedicine.common.persistence.TelemedicineOutboxProperties;
import com.telemedicine.common.persistence.TelemedicinePersistenceProperties;
import com.telemedicine.common.persistence.encryption.PhiEncryptionService;
import com.telemedicine.common.persistence.outbox.OutboxRecorder;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

class PersistenceAutoConfigurationTest {

    private final ApplicationContextRunner runner =
            new ApplicationContextRunner()
                    .withConfiguration(
                            AutoConfigurations.of(
                                    DataSourceAutoConfiguration.class,
                                    HibernateJpaAutoConfiguration.class,
                                    PersistenceAutoConfiguration.class))
                    .withPropertyValues(
                            "spring.datasource.url=jdbc:h2:mem:autoconfig;MODE=PostgreSQL;DB_CLOSE_DELAY=-1",
                            "spring.jpa.hibernate.ddl-auto=create-drop",
                            "telemedicine.outbox.enabled=false");

    @Test
    void bindsPersistenceProperties() {
        runner.withPropertyValues("telemedicine.persistence.jpa.open-in-view=true")
                .run(
                        context ->
                                assertThat(
                                                context.getBean(
                                                                TelemedicinePersistenceProperties
                                                                        .class)
                                                        .getJpa()
                                                        .isOpenInView())
                                        .isTrue());
    }

    @Test
    void bindsOutboxProperties() {
        runner.withPropertyValues("telemedicine.outbox.batch-size=25")
                .run(
                        context ->
                                assertThat(
                                                context.getBean(TelemedicineOutboxProperties.class)
                                                        .getBatchSize())
                                        .isEqualTo(25));
    }

    @Test
    void canBeDisabled() {
        runner.withPropertyValues("telemedicine.persistence.enabled=false")
                .run(context -> assertThat(context).doesNotHaveBean(OutboxRecorder.class));
    }

    @Test
    void registersPhiEncryptionWhenKeyConfigured() {
        runner.withPropertyValues(
                        "telemedicine.persistence.phi-encryption.key="
                                + "v1:AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA=")
                .run(context -> assertThat(context).hasSingleBean(PhiEncryptionService.class));
    }
}
