package com.telemedicine.common.testing.wiremock;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.absent;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.matching;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathMatching;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.telemedicine.common.core.constants.Headers;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

/**
 * WireMock stubs shaped like the Phase 0 {@code external-simulators} controllers, honouring {@link
 * Headers#SIM_SCENARIO}.
 */
public final class SimulatorStubs {

    private SimulatorStubs() {}

    public static void his(WireMockServer server) {
        String successBody = SimulatorResponseBodies.hisAppointmentBody("apt-001");
        stubGet(server, "/sim/his/appointments/[^/]+$", successBody);
        stubPost(
                server,
                "/sim/his/appointments/[^/]+/status",
                "{\"appointmentId\":\"apt-001\",\"status\":\"CHECKED_IN\"}");
    }

    public static void hisNotFound(WireMockServer server) {
        server.stubFor(
                get(urlPathMatching("/sim/his/appointments/[^/]+$"))
                        .withHeader(Headers.SIM_SCENARIO, equalTo("not-found"))
                        .willReturn(aResponse().withStatus(404).withBody("{\"message\":\"not found\"}")));
    }

    public static void fonasa(WireMockServer server) {
        stubPost(
                server,
                "/sim/fonasa/2fa/challenge",
                SimulatorResponseBodies.fonasaChallengeBody("patient-synthetic-001"));
        stubPost(
                server,
                "/sim/fonasa/2fa/verify",
                """
                {"verified":true,"patientId":"patient-synthetic-001"}
                """);
    }

    public static void fonasaLockedOut(WireMockServer server) {
        server.stubFor(
                post(urlPathEqualTo("/sim/fonasa/2fa/verify"))
                        .withHeader(Headers.SIM_SCENARIO, equalTo("locked-out"))
                        .willReturn(
                                json(
                                        """
                                        {"verified":false,"patientId":"patient-synthetic-001","reason":"LOCKED_OUT"}
                                        """)));
    }

    public static void vonage(WireMockServer server) {
        stubPost(server, "/sim/vonage/session", SimulatorResponseBodies.vonageSessionBody());
        stubPost(server, "/sim/vonage/token", SimulatorResponseBodies.vonageTokenBody());
    }

    public static void vonageSessionExpired(WireMockServer server) {
        server.stubFor(
                post(urlPathEqualTo("/sim/vonage/session"))
                        .withHeader(Headers.SIM_SCENARIO, equalTo("session-expired"))
                        .willReturn(aResponse().withStatus(410).withBody("{\"message\":\"expired\"}")));
    }

    public static void registerAll(WireMockServer server) {
        his(server);
        sap(server);
        payment(server);
        fonasa(server);
        vonage(server);
        acs(server);
        anura(server);
        recmed(server);
        medipass(server);
        mpi(server);
        zendesk(server);
        notify(server);
    }

    public static void registerAllWithScenarios(WireMockServer server) {
        registerAll(server);
        hisNotFound(server);
        paymentUnpaid(server);
        fonasaLockedOut(server);
        vonageSessionExpired(server);
        mpiNotFound(server);
    }

    public static void withLatency(WireMockServer server, String pathPattern, int delayMs) {
        server.stubFor(
                get(urlPathMatching(pathPattern))
                        .atPriority(1)
                        .willReturn(json(SimulatorResponseBodies.hisAppointmentBody("apt-001")).withFixedDelay(delayMs)));
    }

    public static void paymentUnpaid(WireMockServer server) {
        server.stubFor(
                get(urlPathMatching("/sim/payment/validate/[^/]+$"))
                        .withHeader(Headers.SIM_SCENARIO, equalTo("unpaid"))
                        .willReturn(
                                json(SimulatorResponseBodies.paymentValidateBody("apt-001", false, "UNPAID"))));
    }

    public static void sap(WireMockServer server) {
        stubPost(
                server,
                "/sim/sap/link-request",
                """
                {"appointmentId":"apt-001","linkToken":"%s","requestedBy":"SAP-BUPA-ENGINE","engineVersion":"2.1","verificationTimestamp":"2026-01-01T10:00:00Z"}
                """
                        .formatted(deterministicId("link", "apt-001")));
        stubGet(
                server,
                "/sim/sap/appointments/[^/]+/verify",
                """
                {"appointmentId":"apt-001","valid":true,"engineVersion":"2.1","verificationTimestamp":"2026-01-01T10:00:00Z","requestedBy":"SAP-BUPA-ENGINE"}
                """);
    }

    public static void payment(WireMockServer server) {
        stubGet(
                server,
                "/sim/payment/validate/[^/]+$",
                SimulatorResponseBodies.paymentValidateBody("apt-001", true, "PAID"));
    }

    public static void acs(WireMockServer server) {
        stubPost(
                server,
                "/sim/acs/session",
                """
                {"sessionId":"%s","provider":"acs","appointmentId":"apt-001","apiKey":"sim-key","createdAt":"2026-01-01T10:00:00Z","expiresAt":"2026-01-02T10:00:00Z","maxParticipants":2}
                """
                        .formatted(deterministicId("acs", "default")));
        stubPost(
                server,
                "/sim/acs/token",
                """
                {"token":"%s","role":"patient","provider":"acs","expiresAt":"2026-01-01T11:00:00Z","connectionData":"%s"}
                """
                        .formatted(deterministicId("acs-tok", "patient"), deterministicId("conn", "patient")));
    }

    public static void anura(WireMockServer server) {
        stubPost(
                server,
                "/sim/anura/token",
                """
                {"token":"%s"}
                """
                        .formatted(deterministicId("anura", "default")));
        stubPost(server, "/sim/anura/decryptResults", SimulatorResponseBodies.anuraResultsBody());
    }

    public static void recmed(WireMockServer server) {
        stubPost(server, "/sim/recmed/prescriptions", SimulatorResponseBodies.recMedPrescriptionBody());
        stubPost(
                server,
                "/sim/recmed/certificates",
                """
                {"certificateId":"%s","status":"SUBMITTED","validatedAt":"2026-01-01T10:00:00Z","expiresAt":"2026-01-31T10:00:00Z","documentUrl":"/sim/documents/%s","physicianRut":"%s"}
                """
                        .formatted(
                                deterministicId("recmed-cert", "apt-001"),
                                deterministicId("recmed-cert", "apt-001"),
                                deterministicId("rut", "physician")));
    }

    public static void medipass(WireMockServer server) {
        stubPost(
                server,
                "/sim/medipass/certificates",
                """
                {"certificateId":"%s","status":"SUBMITTED","validatedAt":"2026-01-01T10:00:00Z","expiresAt":"2026-01-31T10:00:00Z","documentUrl":"/sim/documents/%s","physicianRut":"%s"}
                """
                        .formatted(
                                deterministicId("cert", "apt-001"),
                                deterministicId("cert", "apt-001"),
                                deterministicId("rut", "physician")));
    }

    public static void mpi(WireMockServer server) {
        stubGet(
                server,
                "/sim/mpi/patients/[^/]+$",
                SimulatorResponseBodies.mpiPatientBody("patient-synthetic-001"));
    }

    public static void mpiNotFound(WireMockServer server) {
        server.stubFor(
                get(urlPathMatching("/sim/mpi/patients/[^/]+$"))
                        .withHeader(Headers.SIM_SCENARIO, equalTo("not-found"))
                        .willReturn(aResponse().withStatus(404).withBody("{\"message\":\"not found\"}")));
    }

    public static void zendesk(WireMockServer server) {
        stubPost(
                server,
                "/sim/zendesk/tickets",
                """
                {"ticketId":"%s","status":"OPEN"}
                """
                        .formatted(deterministicId("zd", "support")));
    }

    public static void notify(WireMockServer server) {
        stubPost(
                server,
                "/sim/notify/email",
                """
                {"channel":"email","messageId":"%s","status":"SENT"}
                """
                        .formatted(deterministicId("msg", "email")));
        stubPost(
                server,
                "/sim/notify/sms",
                """
                {"channel":"sms","messageId":"%s","status":"SENT"}
                """
                        .formatted(deterministicId("msg", "sms")));
        stubPost(
                server,
                "/sim/notify/whatsapp",
                """
                {"channel":"whatsapp","messageId":"%s","status":"SENT"}
                """
                        .formatted(deterministicId("msg", "whatsapp")));
        stubPost(
                server,
                "/sim/notify/push",
                """
                {"channel":"push","messageId":"%s","status":"SENT"}
                """
                        .formatted(deterministicId("msg", "push")));
    }

    private static void stubGet(WireMockServer server, String pathPattern, String successBody) {
        server.stubFor(
                get(urlPathMatching(pathPattern))
                        .withHeader(Headers.SIM_SCENARIO, absent())
                        .willReturn(json(successBody)));
        server.stubFor(
                get(urlPathMatching(pathPattern))
                        .withHeader(Headers.SIM_SCENARIO, equalTo("success"))
                        .willReturn(json(successBody)));
        server.stubFor(
                get(urlPathMatching(pathPattern))
                        .withHeader(Headers.SIM_SCENARIO, matching("(?i)(failure|error)"))
                        .willReturn(failureResponse()));
        server.stubFor(
                get(urlPathMatching(pathPattern))
                        .withHeader(Headers.SIM_SCENARIO, matching("(?i)timeout"))
                        .willReturn(timeoutResponse()));
    }

    private static void stubPost(WireMockServer server, String path, String successBody) {
        server.stubFor(
                post(urlPathEqualTo(path))
                        .withHeader(Headers.SIM_SCENARIO, absent())
                        .willReturn(json(successBody)));
        server.stubFor(
                post(urlPathEqualTo(path))
                        .withHeader(Headers.SIM_SCENARIO, equalTo("success"))
                        .willReturn(json(successBody)));
        server.stubFor(
                post(urlPathEqualTo(path))
                        .withHeader(Headers.SIM_SCENARIO, matching("(?i)(failure|error)"))
                        .willReturn(failureResponse()));
        server.stubFor(
                post(urlPathEqualTo(path))
                        .withHeader(Headers.SIM_SCENARIO, matching("(?i)timeout"))
                        .willReturn(timeoutResponse()));
    }

    private static com.github.tomakehurst.wiremock.client.ResponseDefinitionBuilder json(String body) {
        return aResponse().withHeader("Content-Type", "application/json").withBody(body);
    }

    private static com.github.tomakehurst.wiremock.client.ResponseDefinitionBuilder failureResponse() {
        return aResponse()
                .withStatus(503)
                .withHeader("Content-Type", "application/json")
                .withBody("{\"message\":\"Simulated vendor failure\"}");
    }

    private static com.github.tomakehurst.wiremock.client.ResponseDefinitionBuilder timeoutResponse() {
        return aResponse()
                .withStatus(504)
                .withHeader("Content-Type", "application/json")
                .withBody("{\"message\":\"Simulated vendor timeout\"}");
    }

    static String deterministicId(String prefix, String seed) {
        UUID uuid = UUID.nameUUIDFromBytes((prefix + ":" + seed).getBytes(StandardCharsets.UTF_8));
        return prefix + "-" + uuid.toString().substring(0, 8);
    }
}
