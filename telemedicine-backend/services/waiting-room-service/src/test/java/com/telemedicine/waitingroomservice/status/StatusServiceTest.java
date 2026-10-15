package com.telemedicine.waitingroomservice.status;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.telemedicine.common.core.error.ConflictException;
import com.telemedicine.common.observability.metrics.JourneyMetrics;
import com.telemedicine.common.persistence.outbox.OutboxRecorder;
import com.telemedicine.common.security.access.SecurityContextAccessor;
import com.telemedicine.waitingroomservice.config.WaitingRoomServiceProperties;
import com.telemedicine.waitingroomservice.consent.ConsentService;
import com.telemedicine.waitingroomservice.devicetest.DeviceTestService;
import com.telemedicine.waitingroomservice.domain.AppointmentStatusEntity;
import com.telemedicine.waitingroomservice.domain.WaitingRoomStatus;
import com.telemedicine.waitingroomservice.integration.IntegrationGateway;
import com.telemedicine.waitingroomservice.repository.AppointmentStatusRepository;
import com.telemedicine.waitingroomservice.web.dto.StatusUpdateRequest;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class StatusServiceTest {

    @Mock private AppointmentStatusRepository appointmentStatusRepository;
    @Mock private ConsentService consentService;
    @Mock private DeviceTestService deviceTestService;
    @Mock private IntegrationGateway integrationGateway;
    @Mock private OutboxRecorder outboxRecorder;

    private StatusService statusService;

    @BeforeEach
    void setUp() {
        WaitingRoomServiceProperties properties = new WaitingRoomServiceProperties();
        statusService =
                new StatusService(
                        appointmentStatusRepository,
                        consentService,
                        deviceTestService,
                        integrationGateway,
                        outboxRecorder,
                        new SecurityContextAccessor(),
                        new JourneyMetrics(new SimpleMeterRegistry()),
                        properties);
    }

    @Test
    void rejectsInvalidTransition() {
        AppointmentStatusEntity entity = new AppointmentStatusEntity();
        entity.setAppointmentId("appt-1");
        entity.setStatusEnum(WaitingRoomStatus.OPEN_PATIENT);
        when(appointmentStatusRepository.findById("appt-1")).thenReturn(Optional.of(entity));

        assertThatThrownBy(
                        () ->
                                statusService.update(
                                        "appt-1", new StatusUpdateRequest("OPEN_DOCTOR")))
                .isInstanceOf(ConflictException.class);
    }
}
