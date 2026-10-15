package com.telemedicine.common.testing.wiremock;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

/**
 * Canonical simulator response bodies shared between WireMock stubs and contract-sync tests.
 */
public final class SimulatorResponseBodies {

    private SimulatorResponseBodies() {}

    public static String hisAppointmentBody(String appointmentId) {
        return """
               {"appointmentId":"%s","status":"SCHEDULED","patientId":"%s","specialty":"general","physicianId":"%s","scheduledAt":"2026-01-01T10:00:00Z","facilityCode":"CL-SCL-001","appointmentType":"TELEMEDICINE"}
               """
                .formatted(
                        appointmentId,
                        deterministicId("patient", appointmentId),
                        deterministicId("phy", appointmentId));
    }

    public static String paymentValidateBody(String appointmentId, boolean paid, String status) {
        if (paid) {
            return """
                   {"appointmentId":"%s","paid":true,"status":"%s","transactionId":"%s","currency":"CLP","amount":25000,"paymentMethod":"FONASA","paidAt":"2026-01-01T10:00:00Z"}
                   """
                    .formatted(appointmentId, status, deterministicId("txn", appointmentId));
        }
        return """
               {"appointmentId":"%s","paid":false,"status":"%s","transactionId":"%s","currency":"CLP","amount":25000,"paymentMethod":"FONASA"}
               """
                .formatted(appointmentId, status, deterministicId("txn", appointmentId));
    }

    public static String fonasaChallengeBody(String patientId) {
        return """
               {"challengeId":"%s","maskedPhone":"+56****1234","expiresAt":"2026-01-01T10:05:00Z","attemptsRemaining":3}
               """
                .formatted(deterministicId("challenge", patientId));
    }

    public static String mpiPatientBody(String patientRef) {
        return """
               {"patientRef":"%s","name":"Patient Synthetic","dateOfBirth":"1985-06-15","nationalIdHash":"%s","gender":"F","bloodType":"O+","insuranceType":"FONASA","contactPhone":"+56****5678","emergencyContact":"Contact ****9012","address":{"region":"Metropolitana","city":"Santiago"}}
               """
                .formatted(patientRef, deterministicId("rut", patientRef));
    }

    public static String vonageSessionBody() {
        return """
               {"sessionId":"%s","provider":"vonage","appointmentId":"apt-001","apiKey":"sim-key","createdAt":"2026-01-01T10:00:00Z","expiresAt":"2026-01-02T10:00:00Z","maxParticipants":2}
               """
                .formatted(deterministicId("vonage", "default"));
    }

    public static String vonageTokenBody() {
        return """
               {"token":"%s","role":"patient","provider":"vonage","expiresAt":"2026-01-01T11:00:00Z","connectionData":"%s"}
               """
                .formatted(deterministicId("tok", "patient"), deterministicId("conn", "patient"));
    }

    public static String anuraResultsBody() {
        return """
               {"measurementId":"%s","capturedAt":"2026-01-01T10:00:00Z","confidence":0.95,"metrics":{"heartRate":72,"respiratoryRate":16,"stress":0.31,"bloodPressureEstimate":"120/80"},"rawToken":"token","heartRate":72,"stress":0.31}
               """
                .formatted(deterministicId("measure", "token"));
    }

    public static String recMedPrescriptionBody() {
        return """
               {"prescriptionId":"%s","status":"SUBMITTED","validatedAt":"2026-01-01T10:00:00Z","expiresAt":"2026-01-31T10:00:00Z","documentUrl":"/sim/documents/%s","physicianRut":"%s"}
               """
                .formatted(
                        deterministicId("rx", "apt-001"),
                        deterministicId("rx", "apt-001"),
                        deterministicId("rut", "physician"));
    }

    static String deterministicId(String prefix, String seed) {
        UUID uuid = UUID.nameUUIDFromBytes((prefix + ":" + seed).getBytes(StandardCharsets.UTF_8));
        return prefix + "-" + uuid.toString().substring(0, 8);
    }
}
