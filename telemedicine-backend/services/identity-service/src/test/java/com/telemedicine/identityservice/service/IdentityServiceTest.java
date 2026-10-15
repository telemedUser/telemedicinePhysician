package com.telemedicine.identityservice.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.telemedicine.common.core.error.ErrorCode;
import com.telemedicine.common.core.error.IdentityValidationException;
import com.telemedicine.common.core.error.NotFoundException;
import com.telemedicine.common.events.payload.IdentityValidated;
import com.telemedicine.common.events.payload.IdentityValidationFailed;
import com.telemedicine.common.persistence.outbox.OutboxRecorder;
import com.telemedicine.identityservice.api.dto.FonasaChallengeRequest;
import com.telemedicine.identityservice.api.dto.FonasaVerifyRequest;
import com.telemedicine.identityservice.api.dto.ValidateIdentityRequest;
import com.telemedicine.identityservice.client.IntegrationServiceClient;
import com.telemedicine.identityservice.client.IntegrationServiceClient.FonasaChallengeResponse;
import com.telemedicine.identityservice.client.IntegrationServiceClient.FonasaVerifyResponse;
import com.telemedicine.identityservice.client.IntegrationServiceClient.MpiPatientResponse;
import com.telemedicine.identityservice.config.IdentityServiceProperties;
import com.telemedicine.identityservice.domain.FonasaChallengeEntity;
import com.telemedicine.identityservice.domain.FonasaChallengeRepository;
import com.telemedicine.identityservice.domain.IdentityVerificationRepository;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class IdentityServiceTest {

    @Mock private IntegrationServiceClient integrationClient;
    @Mock private IdentityVerificationRepository verificationRepository;
    @Mock private FonasaChallengeRepository challengeRepository;
    @Mock private OutboxRecorder outboxRecorder;

    private IdentityService identityService;

    @BeforeEach
    void setUp() {
        IdentityServiceProperties properties = new IdentityServiceProperties();
        properties.setMaxAttempts(3);
        properties.setLockoutDuration(Duration.ofMinutes(15));
        identityService =
                new IdentityService(
                        integrationClient,
                        verificationRepository,
                        challengeRepository,
                        outboxRecorder,
                        properties);
    }

    @Test
    void validateEmitsIdentityValidatedWhenMpiMatches() {
        when(integrationClient.mpiLookup("apt-1"))
                .thenReturn(new MpiPatientResponse("apt-1", "pat-1", "Patient", "1985-01-01"));

        var response =
                identityService.validate(new ValidateIdentityRequest("apt-1", "pat-1"));

        assertThat(response.validated()).isTrue();
        verify(verificationRepository).save(any());
        verify(outboxRecorder).record(eq("identity"), eq("apt-1"), any(IdentityValidated.class), any());
    }

    @Test
    void validateFailsWhenPatientMismatch() {
        when(integrationClient.mpiLookup("apt-1"))
                .thenReturn(new MpiPatientResponse("apt-1", "pat-1", "Patient", "1985-01-01"));

        assertThatThrownBy(
                        () -> identityService.validate(new ValidateIdentityRequest("apt-1", "pat-other")))
                .isInstanceOf(IdentityValidationException.class);

        verify(outboxRecorder).record(eq("identity"), eq("apt-1"), any(IdentityValidationFailed.class), any());
    }

    @Test
    void mpiNotFoundMapsToMpiPatientNotFound() {
        when(integrationClient.mpiLookup("apt-missing"))
                .thenThrow(new NotFoundException(ErrorCode.MPI_PATIENT_NOT_FOUND, "missing"));

        assertThatThrownBy(() -> identityService.validate(new ValidateIdentityRequest("apt-missing", "pat-1")))
                .extracting(ex -> ((NotFoundException) ex).getCode())
                .isEqualTo(ErrorCode.MPI_PATIENT_NOT_FOUND);
    }

    @Test
    void fonasaVerifyDoesNotUseHardcodedCode() {
        FonasaChallengeEntity challenge =
                new FonasaChallengeEntity(
                        "chal-1",
                        "apt-1",
                        "pat-1",
                        "+56****1234",
                        "11111111-1",
                        "+56912345678",
                        0,
                        null,
                        false,
                        Instant.now(),
                        Instant.now());
        when(challengeRepository.findById("chal-1")).thenReturn(Optional.of(challenge));
        when(integrationClient.fonasaVerify("chal-1", "000000", "pat-1"))
                .thenReturn(new FonasaVerifyResponse(false, "pat-1"));

        assertThatThrownBy(
                        () ->
                                identityService.verifyFonasa(
                                        new FonasaVerifyRequest("apt-1", "chal-1", "000000", "pat-1")))
                .isInstanceOf(IdentityValidationException.class);

        verify(integrationClient).fonasaVerify("chal-1", "000000", "pat-1");
        verify(integrationClient, never()).fonasaVerify(eq("chal-1"), eq("123456"), any());
    }

    @Test
    void fonasaVerifyLocksOutAfterMaxAttempts() {
        FonasaChallengeEntity challenge =
                new FonasaChallengeEntity(
                        "chal-1",
                        "apt-1",
                        "pat-1",
                        "+56****1234",
                        "11111111-1",
                        "+56912345678",
                        2,
                        null,
                        false,
                        Instant.now(),
                        Instant.now());
        when(challengeRepository.findById("chal-1")).thenReturn(Optional.of(challenge));
        when(integrationClient.fonasaVerify("chal-1", "bad", "pat-1"))
                .thenReturn(new FonasaVerifyResponse(false, "pat-1"));

        assertThatThrownBy(
                        () ->
                                identityService.verifyFonasa(
                                        new FonasaVerifyRequest("apt-1", "chal-1", "bad", "pat-1")))
                .isInstanceOf(IdentityValidationException.class)
                .extracting(ex -> ((IdentityValidationException) ex).getCode())
                .isEqualTo(ErrorCode.IDENTITY_LOCKED_OUT);

        ArgumentCaptor<FonasaChallengeEntity> saved = ArgumentCaptor.forClass(FonasaChallengeEntity.class);
        verify(challengeRepository, times(1)).save(saved.capture());
        assertThat(saved.getValue().getLockoutUntil()).isNotNull();
    }

    @Test
    void fonasaChallengeDelegatesToIntegration() {
        when(integrationClient.fonasaChallenge("apt-1", "pat-1", "11111111-1", "+56912345678"))
                .thenReturn(new FonasaChallengeResponse("chal-99", "+56****1234"));
        when(challengeRepository.findFirstByAppointmentIdOrderByCreatedAtDesc("apt-1"))
                .thenReturn(Optional.empty());

        var response =
                identityService.startFonasaChallenge(
                        new FonasaChallengeRequest("apt-1", "pat-1", "11111111-1", "+56912345678"));

        assertThat(response.challengeId()).isEqualTo("chal-99");
        verify(challengeRepository).save(any());
    }
}
