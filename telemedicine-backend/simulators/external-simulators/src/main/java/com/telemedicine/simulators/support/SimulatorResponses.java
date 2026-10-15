package com.telemedicine.simulators.support;

import com.telemedicine.simulators.SimScenario;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.LinkedHashMap;
import java.util.Map;

public final class SimulatorResponses {

    private SimulatorResponses() {}

    public static Map<String, Object> hisAppointment(String appointmentId, String status) {
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("appointmentId", appointmentId);
        response.put("status", status);
        response.put("patientId", SimScenario.deterministicId("patient", appointmentId));
        response.put("specialty", "general");
        response.put("physicianId", SimScenario.deterministicId("phy", appointmentId));
        response.put("scheduledAt", Instant.now().toString());
        response.put("facilityCode", "CL-SCL-001");
        response.put("appointmentType", "TELEMEDICINE");
        return response;
    }

    public static Map<String, Object> hisPartial(String appointmentId) {
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("appointmentId", appointmentId);
        response.put("status", "PARTIAL");
        return response;
    }

    public static Map<String, String> hisStatusUpdate(String appointmentId, String status) {
        Map<String, String> response = new LinkedHashMap<>();
        response.put("appointmentId", appointmentId);
        response.put("status", status);
        return response;
    }

    public static Map<String, String> sapLinkRequest(String appointmentId, String linkToken) {
        Map<String, String> response = new LinkedHashMap<>();
        response.put("appointmentId", appointmentId);
        response.put("linkToken", linkToken);
        response.put("requestedBy", "SAP-BUPA-ENGINE");
        response.put("engineVersion", "2.1");
        response.put("verificationTimestamp", Instant.now().toString());
        return response;
    }

    public static Map<String, Object> sapVerify(String appointmentId, boolean valid) {
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("appointmentId", appointmentId);
        response.put("valid", valid);
        response.put("engineVersion", "2.1");
        response.put("verificationTimestamp", Instant.now().toString());
        response.put("requestedBy", "SAP-BUPA-ENGINE");
        return response;
    }

    public static Map<String, Object> paymentValidate(String appointmentId, boolean paid, String status) {
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("appointmentId", appointmentId);
        response.put("paid", paid);
        response.put("status", status);
        response.put("transactionId", SimScenario.deterministicId("txn", appointmentId));
        response.put("currency", "CLP");
        response.put("amount", 25000);
        response.put("paymentMethod", "FONASA");
        if (paid) {
            response.put("paidAt", Instant.now().toString());
        }
        return response;
    }

    public static Map<String, Object> fonasaChallenge(String challengeId, Instant expiresAt, int attemptsRemaining) {
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("challengeId", challengeId);
        response.put("maskedPhone", "+56****1234");
        response.put("expiresAt", expiresAt.toString());
        response.put("attemptsRemaining", attemptsRemaining);
        return response;
    }

    public static Map<String, Object> fonasaVerify(
            boolean verified, String patientId, String reason, int attemptsRemaining) {
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("verified", verified);
        if (patientId != null) {
            response.put("patientId", patientId);
        }
        if (reason != null) {
            response.put("reason", reason);
        }
        if (attemptsRemaining > 0) {
            response.put("attemptsRemaining", attemptsRemaining);
        }
        return response;
    }

    public static Map<String, Object> videoSession(
            String provider, String sessionId, String appointmentId) {
        Instant now = Instant.now();
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("sessionId", sessionId);
        response.put("provider", provider);
        response.put("appointmentId", appointmentId);
        response.put("apiKey", "sim-key");
        response.put("createdAt", now.toString());
        response.put("expiresAt", now.plus(24, ChronoUnit.HOURS).toString());
        response.put("maxParticipants", 2);
        return response;
    }

    public static Map<String, Object> videoToken(String provider, String role, String token) {
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("token", token);
        response.put("role", role);
        response.put("provider", provider);
        response.put("expiresAt", Instant.now().plus(1, ChronoUnit.HOURS).toString());
        response.put("connectionData", SimScenario.deterministicId("conn", role));
        return response;
    }

    public static Map<String, Object> mpiPatient(String patientRef) {
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("patientRef", patientRef);
        response.put(
                "name",
                "Patient " + patientRef.substring(0, Math.min(8, patientRef.length())));
        response.put("dateOfBirth", "1985-06-15");
        response.put("nationalIdHash", SimScenario.deterministicId("rut", patientRef));
        response.put("gender", "F");
        response.put("bloodType", "O+");
        response.put("insuranceType", "FONASA");
        response.put("contactPhone", "+56****5678");
        response.put("emergencyContact", "Contact ****9012");
        Map<String, String> address = new LinkedHashMap<>();
        address.put("region", "Metropolitana");
        address.put("city", "Santiago");
        response.put("address", address);
        return response;
    }

    public static Map<String, Object> anuraToken(String token) {
        return Map.of("token", token);
    }

    public static Map<String, Object> anuraResults(String token, double confidence) {
        Map<String, Object> metrics = new LinkedHashMap<>();
        metrics.put("heartRate", confidence > 0 ? 72 : 0);
        metrics.put("respiratoryRate", confidence > 0 ? 16 : 0);
        metrics.put("stress", confidence > 0 ? 0.31 : 0.0);
        metrics.put("bloodPressureEstimate", confidence > 0 ? "120/80" : null);
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("measurementId", SimScenario.deterministicId("measure", token));
        response.put("capturedAt", Instant.now().toString());
        response.put("confidence", confidence);
        response.put("metrics", metrics);
        response.put("rawToken", token);
        response.put("heartRate", metrics.get("heartRate"));
        response.put("stress", metrics.get("stress"));
        return response;
    }

    public static Map<String, String> recMedPrescription(String appointmentId) {
        Instant now = Instant.now();
        Map<String, String> response = new LinkedHashMap<>();
        response.put("prescriptionId", SimScenario.deterministicId("rx", appointmentId));
        response.put("status", "SUBMITTED");
        response.put("validatedAt", now.toString());
        response.put("expiresAt", now.plus(30, ChronoUnit.DAYS).toString());
        response.put(
                "documentUrl",
                "/sim/documents/" + SimScenario.deterministicId("rx", appointmentId));
        response.put("physicianRut", SimScenario.deterministicId("rut", "physician"));
        return response;
    }

    public static Map<String, String> recMedCertificate(String appointmentId) {
        Instant now = Instant.now();
        Map<String, String> response = new LinkedHashMap<>();
        response.put("certificateId", SimScenario.deterministicId("recmed-cert", appointmentId));
        response.put("status", "SUBMITTED");
        response.put("validatedAt", now.toString());
        response.put("expiresAt", now.plus(30, ChronoUnit.DAYS).toString());
        response.put(
                "documentUrl",
                "/sim/documents/" + SimScenario.deterministicId("recmed-cert", appointmentId));
        response.put("physicianRut", SimScenario.deterministicId("rut", "physician"));
        return response;
    }

    public static Map<String, String> medipassCertificate(String appointmentId) {
        Instant now = Instant.now();
        Map<String, String> response = new LinkedHashMap<>();
        response.put("certificateId", SimScenario.deterministicId("cert", appointmentId));
        response.put("status", "SUBMITTED");
        response.put("validatedAt", now.toString());
        response.put("expiresAt", now.plus(30, ChronoUnit.DAYS).toString());
        response.put(
                "documentUrl",
                "/sim/documents/" + SimScenario.deterministicId("cert", appointmentId));
        response.put("physicianRut", SimScenario.deterministicId("rut", "physician"));
        return response;
    }

    public static Map<String, String> notifySend(String channel, String messageId) {
        Map<String, String> response = new LinkedHashMap<>();
        response.put("channel", channel);
        response.put("messageId", messageId);
        response.put("status", "SENT");
        return response;
    }

    public static Map<String, String> zendeskTicket(String subject) {
        Map<String, String> response = new LinkedHashMap<>();
        response.put("ticketId", SimScenario.deterministicId("zd", subject));
        response.put("status", "OPEN");
        return response;
    }
}
