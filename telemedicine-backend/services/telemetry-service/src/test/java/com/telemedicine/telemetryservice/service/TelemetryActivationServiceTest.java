package com.telemedicine.telemetryservice.service;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.telemedicine.common.core.error.ErrorCode;
import com.telemedicine.common.core.error.ForbiddenException;
import com.telemedicine.common.integration.config.PlatformConfigClient;
import com.telemedicine.common.integration.config.PlatformConfigSnapshot;
import java.time.Instant;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class TelemetryActivationServiceTest {

    @Mock private PlatformConfigClient configClient;

    private TelemetryActivationService activationService;

    @BeforeEach
    void setUp() {
        activationService = new TelemetryActivationService(configClient, new ObjectMapper());
    }

    @Test
    void rejectsWhenTelemetryDisabled() {
        when(configClient.snapshot())
                .thenReturn(
                        new PlatformConfigSnapshot(
                                "simulator",
                                "simulator",
                                false,
                                Map.of(),
                                Map.of(),
                                "{}",
                                Instant.now()));

        assertThatThrownBy(() -> activationService.requireEnabled(null, null))
                .isInstanceOf(ForbiddenException.class)
                .extracting(ex -> ((ForbiddenException) ex).getCode())
                .isEqualTo(ErrorCode.TELEMETRY_NOT_ENABLED);
    }

    @Test
    void allowsWhenEnabledWithoutRules() {
        when(configClient.snapshot())
                .thenReturn(
                        new PlatformConfigSnapshot(
                                "simulator",
                                "simulator",
                                true,
                                Map.of(),
                                Map.of(),
                                "{}",
                                Instant.now()));

        activationService.requireEnabled("GENERAL", "CARDIOLOGY");
    }
}
