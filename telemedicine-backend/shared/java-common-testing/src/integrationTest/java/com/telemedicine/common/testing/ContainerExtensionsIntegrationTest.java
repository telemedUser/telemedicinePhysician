package com.telemedicine.common.testing;

import static org.assertj.core.api.Assertions.assertThat;

import com.telemedicine.common.testing.containers.KafkaContainerExtension;
import com.telemedicine.common.testing.containers.PostgresContainerExtension;
import com.telemedicine.common.testing.containers.TelemedicineContainers;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@Tag("integration")
@ExtendWith({PostgresContainerExtension.class, KafkaContainerExtension.class})
class ContainerExtensionsIntegrationTest {

    @Test
    void postgresAndKafkaContainersStart() {
        assertThat(TelemedicineContainers.postgres().isRunning()).isTrue();
        assertThat(TelemedicineContainers.kafka().isRunning()).isTrue();
        assertThat(TelemedicineContainers.postgres().getJdbcUrl()).contains("jdbc:postgresql://");
        assertThat(TelemedicineContainers.kafka().getBootstrapServers()).isNotBlank();
    }
}
