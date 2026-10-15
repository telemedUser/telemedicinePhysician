package com.telemedicine.identityservice.service;

import com.telemedicine.common.core.constants.MdcKeys;
import com.telemedicine.common.core.error.IdentityValidationException;
import com.telemedicine.common.core.id.IdGenerator;
import com.telemedicine.common.events.payload.IdentityValidated;
import com.telemedicine.common.events.payload.IdentityValidationFailed;
import com.telemedicine.common.persistence.outbox.OutboxRecorder;
import com.telemedicine.identityservice.api.dto.FonasaChallengeRequest;
import com.telemedicine.identityservice.api.dto.FonasaChallengeResponse;
import com.telemedicine.identityservice.api.dto.FonasaVerifyRequest;
import com.telemedicine.identityservice.api.dto.FonasaVerifyResponse;
import com.telemedicine.identityservice.api.dto.ValidateIdentityRequest;
import com.telemedicine.identityservice.api.dto.ValidateIdentityResponse;
import com.telemedicine.identityservice.client.IntegrationServiceClient;
import com.telemedicine.identityservice.client.IntegrationServiceClient.MpiPatientResponse;
import com.telemedicine.identityservice.config.IdentityServiceProperties;
import com.telemedicine.identityservice.domain.FonasaChallengeEntity;
import com.telemedicine.identityservice.domain.FonasaChallengeRepository;
import com.telemedicine.identityservice.domain.IdentityVerificationEntity;
import com.telemedicine.identityservice.domain.IdentityVerificationRepository;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import org.slf4j.MDC;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class IdentityService {

    private static final String METHOD_MPI = "MPI";
    private static final String METHOD_FONASA = "FONASA_2FA";

    private final IntegrationServiceClient integrationClient;
    private final IdentityVerificationRepository verificationRepository;
    private final FonasaChallengeRepository challengeRepository;
    private final OutboxRecorder outboxRecorder;
    private final IdentityServiceProperties properties;

    public IdentityService(
            IntegrationServiceClient integrationClient,
            IdentityVerificationRepository verificationRepository,
            FonasaChallengeRepository challengeRepository,
            OutboxRecorder outboxRecorder,
            IdentityServiceProperties properties) {
        this.integrationClient = integrationClient;
        this.verificationRepository = verificationRepository;
        this.challengeRepository = challengeRepository;
        this.outboxRecorder = outboxRecorder;
        this.properties = properties;
    }

    @Transactional(readOnly = true)
    public Map<String, Object> status(String appointmentId) {
        return verificationRepository
                .findFirstByAppointmentIdAndValidatedTrueOrderByValidatedAtDesc(appointmentId)
                .map(
                        verification ->
                                Map.<String, Object>of(
                                        "verificationStatus",
                                        "VALIDATED",
                                        "method",
                                        verification.getMethod()))
                .orElseGet(() -> Map.of("verificationStatus", "PENDING"));
    }

    @Transactional
    public ValidateIdentityResponse validate(ValidateIdentityRequest request) {
        MpiPatientResponse mpi = integrationClient.mpiLookup(request.appointmentId());
        if (!request.patientId().equals(mpi.patientId())) {
            recordValidationFailed(
                    request.appointmentId(),
                    request.patientId(),
                    METHOD_MPI,
                    "PATIENT_MISMATCH",
                    1);
            throw new IdentityValidationException("Patient identity does not match MPI record");
        }

        Instant now = Instant.now();
        IdentityVerificationEntity verification =
                new IdentityVerificationEntity(
                        IdGenerator.newUlid(),
                        request.appointmentId(),
                        mpi.patientId(),
                        METHOD_MPI,
                        true,
                        now,
                        now);
        verificationRepository.save(verification);

        outboxRecorder.record(
                "identity",
                request.appointmentId(),
                new IdentityValidated(request.appointmentId(), mpi.patientId(), METHOD_MPI, now),
                correlationId());

        return new ValidateIdentityResponse(
                request.appointmentId(), mpi.patientId(), METHOD_MPI, true, now);
    }

    @Transactional
    public FonasaChallengeResponse startFonasaChallenge(FonasaChallengeRequest request) {
        assertNotLockedOut(request.appointmentId());

        IntegrationServiceClient.FonasaChallengeResponse challenge =
                integrationClient.fonasaChallenge(
                        request.appointmentId(), request.patientId(), request.rut(), request.phone());

        Instant now = Instant.now();
        FonasaChallengeEntity entity =
                new FonasaChallengeEntity(
                        challenge.challengeId(),
                        request.appointmentId(),
                        request.patientId(),
                        challenge.maskedPhone(),
                        request.rut(),
                        request.phone(),
                        0,
                        null,
                        false,
                        now,
                        now);
        challengeRepository.save(entity);

        return new FonasaChallengeResponse(challenge.challengeId(), challenge.maskedPhone());
    }

    @Transactional
    public FonasaVerifyResponse verifyFonasa(FonasaVerifyRequest request) {
        FonasaChallengeEntity challenge =
                challengeRepository
                        .findById(request.challengeId())
                        .orElseThrow(IdentityValidationException::twoFactorRequired);

        if (challenge.isVerified()) {
            return new FonasaVerifyResponse(true, METHOD_FONASA);
        }

        if (isLockedOut(challenge)) {
            long retryAfter = Duration.between(Instant.now(), challenge.getLockoutUntil()).getSeconds();
            throw IdentityValidationException.lockedOut(Math.max(retryAfter, 1));
        }

        IntegrationServiceClient.FonasaVerifyResponse verifyResponse =
                integrationClient.fonasaVerify(
                        request.challengeId(), request.code(), request.patientId());

        if (verifyResponse.verified()) {
            challenge.setVerified(true);
            challenge.setUpdatedAt(Instant.now());
            challengeRepository.save(challenge);

            Instant now = Instant.now();
            verificationRepository.save(
                    new IdentityVerificationEntity(
                            IdGenerator.newUlid(),
                            challenge.getAppointmentId(),
                            challenge.getPatientRef(),
                            METHOD_FONASA,
                            true,
                            now,
                            now));

            outboxRecorder.record(
                    "identity",
                    challenge.getAppointmentId(),
                    new IdentityValidated(
                            challenge.getAppointmentId(),
                            challenge.getPatientRef(),
                            METHOD_FONASA,
                            now),
                    correlationId());

            return new FonasaVerifyResponse(true, METHOD_FONASA);
        }

        int attempt = challenge.getAttempts() + 1;
        challenge.setAttempts(attempt);
        challenge.setUpdatedAt(Instant.now());

        if (attempt >= properties.getMaxAttempts()) {
            Instant lockoutUntil = Instant.now().plus(properties.getLockoutDuration());
            challenge.setLockoutUntil(lockoutUntil);
            challengeRepository.save(challenge);
            recordValidationFailed(
                    challenge.getAppointmentId(),
                    challenge.getPatientRef(),
                    METHOD_FONASA,
                    "LOCKED_OUT",
                    attempt);
            long retryAfter = properties.getLockoutDuration().getSeconds();
            throw IdentityValidationException.lockedOut(retryAfter);
        }

        challengeRepository.save(challenge);
        recordValidationFailed(
                challenge.getAppointmentId(),
                challenge.getPatientRef(),
                METHOD_FONASA,
                "INVALID_CODE",
                attempt);

        int attemptsRemaining = properties.getMaxAttempts() - attempt;
        throw IdentityValidationException.twoFactorInvalid(attemptsRemaining);
    }

    private void assertNotLockedOut(String appointmentId) {
        challengeRepository
                .findFirstByAppointmentIdOrderByCreatedAtDesc(appointmentId)
                .filter(this::isLockedOut)
                .ifPresent(
                        challenge -> {
                            long retryAfter =
                                    Duration.between(Instant.now(), challenge.getLockoutUntil())
                                            .getSeconds();
                            throw IdentityValidationException.lockedOut(Math.max(retryAfter, 1));
                        });
    }

    private boolean isLockedOut(FonasaChallengeEntity challenge) {
        return challenge.getLockoutUntil() != null
                && challenge.getLockoutUntil().isAfter(Instant.now());
    }

    private void recordValidationFailed(
            String appointmentId,
            String patientRef,
            String method,
            String reasonCode,
            int attempt) {
        outboxRecorder.record(
                "identity",
                appointmentId,
                new IdentityValidationFailed(
                        appointmentId,
                        patientRef,
                        method,
                        reasonCode,
                        attempt,
                        Instant.now()),
                correlationId());
    }

    private static String correlationId() {
        String fromMdc = MDC.get(MdcKeys.CORRELATION_ID);
        return fromMdc != null ? fromMdc : IdGenerator.correlationId();
    }
}
