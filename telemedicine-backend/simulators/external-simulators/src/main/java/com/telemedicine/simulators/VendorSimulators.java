package com.telemedicine.simulators;

import com.telemedicine.simulators.engine.ProcessOutcome;
import com.telemedicine.simulators.engine.SimulatorEngine;
import com.telemedicine.simulators.engine.VendorNames;
import com.telemedicine.simulators.state.AppointmentState;
import com.telemedicine.simulators.state.AppointmentStateStore;
import com.telemedicine.simulators.state.FonasaChallengeStore;
import com.telemedicine.simulators.state.NotifyDeliveryLog;
import com.telemedicine.simulators.state.ProviderOutageState;
import com.telemedicine.simulators.state.VideoSessionStore;
import com.telemedicine.simulators.support.SimulatorResponses;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/sim/his")
class HisSimulatorController {

    private final SimulatorEngine engine;
    private final AppointmentStateStore appointmentStateStore;

    HisSimulatorController(SimulatorEngine engine, AppointmentStateStore appointmentStateStore) {
        this.engine = engine;
        this.appointmentStateStore = appointmentStateStore;
    }

    @GetMapping("/appointments/{id}")
    public Map<String, Object> getAppointment(
            @PathVariable String id,
            @RequestHeader(value = SimScenario.HEADER, required = false) String scenario) {
        String effective = SimScenario.scenarioOrDefault(scenario);
        if ("not-found".equalsIgnoreCase(effective)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Appointment not found in HIS");
        }
        ProcessOutcome outcome = engine.preProcess(VendorNames.HIS, scenario);
        AppointmentState state = appointmentStateStore.getOrCreate(id);
        if ("stale-status".equalsIgnoreCase(effective)) {
            return SimulatorResponses.hisAppointment(id, "SCHEDULED");
        }
        if (outcome == ProcessOutcome.PARTIAL) {
            return SimulatorResponses.hisPartial(id);
        }
        return SimulatorResponses.hisAppointment(id, state.status());
    }

    @PostMapping("/appointments/{id}/status")
    public Map<String, String> updateStatus(
            @PathVariable String id,
            @RequestBody Map<String, String> body,
            @RequestHeader(value = SimScenario.HEADER, required = false) String scenario) {
        engine.preProcess(VendorNames.HIS, scenario);
        String status = body.getOrDefault("status", "CHECKED_IN");
        AppointmentState updated = appointmentStateStore.transition(id, status);
        return SimulatorResponses.hisStatusUpdate(updated.appointmentId(), updated.status());
    }
}

@RestController
@RequestMapping("/sim/sap")
class SapSimulatorController {

    private final SimulatorEngine engine;
    private final AppointmentStateStore appointmentStateStore;

    SapSimulatorController(SimulatorEngine engine, AppointmentStateStore appointmentStateStore) {
        this.engine = engine;
        this.appointmentStateStore = appointmentStateStore;
    }

    @PostMapping("/link-request")
    public Map<String, String> linkRequest(
            @RequestBody Map<String, String> body,
            @RequestHeader(value = SimScenario.HEADER, required = false) String scenario) {
        String effective = SimScenario.scenarioOrDefault(scenario);
        if ("duplicate-link".equalsIgnoreCase(effective)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Link already created");
        }
        engine.preProcess(VendorNames.SAP, scenario);
        String appointmentId = body.get("appointmentId");
        appointmentStateStore.markLinked(appointmentId);
        return SimulatorResponses.sapLinkRequest(
                appointmentId, SimScenario.deterministicId("link", appointmentId));
    }

    @GetMapping("/appointments/{id}/verify")
    public Map<String, Object> verify(
            @PathVariable String id,
            @RequestHeader(value = SimScenario.HEADER, required = false) String scenario) {
        String effective = SimScenario.scenarioOrDefault(scenario);
        if ("invalid-appointment".equalsIgnoreCase(effective)) {
            engine.preProcess(VendorNames.SAP, scenario);
            return SimulatorResponses.sapVerify(id, false);
        }
        engine.preProcess(VendorNames.SAP, scenario);
        boolean valid = appointmentStateStore.exists(id);
        return SimulatorResponses.sapVerify(id, valid);
    }
}

@RestController
@RequestMapping("/sim/payment")
class PaymentSimulatorController {

    private final SimulatorEngine engine;
    private final AppointmentStateStore appointmentStateStore;

    PaymentSimulatorController(SimulatorEngine engine, AppointmentStateStore appointmentStateStore) {
        this.engine = engine;
        this.appointmentStateStore = appointmentStateStore;
    }

    @GetMapping("/validate/{appointmentId}")
    public Map<String, Object> validate(
            @PathVariable String appointmentId,
            @RequestHeader(value = SimScenario.HEADER, required = false) String scenario) {
        String effective = SimScenario.scenarioOrDefault(scenario);
        if ("unpaid".equalsIgnoreCase(effective)) {
            engine.preProcess(VendorNames.PAYMENT, scenario);
            return SimulatorResponses.paymentValidate(appointmentId, false, "UNPAID");
        }
        if ("payment-pending".equalsIgnoreCase(effective)) {
            engine.preProcess(VendorNames.PAYMENT, scenario);
            return SimulatorResponses.paymentValidate(appointmentId, false, "PENDING");
        }
        engine.preProcess(VendorNames.PAYMENT, scenario);
        AppointmentState state = appointmentStateStore.getOrCreate(appointmentId);
        boolean paid = "PAID".equals(state.paymentStatus());
        if (paid) {
            return SimulatorResponses.paymentValidate(appointmentId, true, "PAID");
        }
        appointmentStateStore.markPaid(appointmentId);
        return SimulatorResponses.paymentValidate(appointmentId, true, "PAID");
    }
}

@RestController
@RequestMapping("/sim/fonasa")
class FonasaSimulatorController {

    private final SimulatorEngine engine;
    private final FonasaChallengeStore fonasaChallengeStore;

    FonasaSimulatorController(SimulatorEngine engine, FonasaChallengeStore fonasaChallengeStore) {
        this.engine = engine;
        this.fonasaChallengeStore = fonasaChallengeStore;
    }

    @PostMapping("/2fa/challenge")
    public Map<String, Object> challenge(
            @RequestBody Map<String, String> body,
            @RequestHeader(value = SimScenario.HEADER, required = false) String scenario) {
        String effective = SimScenario.scenarioOrDefault(scenario);
        if ("invalid-rut".equalsIgnoreCase(effective)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Patient RUT not found in FONASA registry");
        }
        engine.preProcess(VendorNames.FONASA, scenario);
        String patientId = body.getOrDefault("patientId", "unknown");
        String challengeId = SimScenario.deterministicId("challenge", patientId);
        var created = fonasaChallengeStore.create(challengeId, patientId);
        return SimulatorResponses.fonasaChallenge(
                challengeId, created.expiresAt(), fonasaChallengeStore.attemptsRemaining(created));
    }

    @PostMapping("/2fa/verify")
    public Map<String, Object> verify(
            @RequestBody Map<String, String> body,
            @RequestHeader(value = SimScenario.HEADER, required = false) String scenario) {
        String effective = SimScenario.scenarioOrDefault(scenario);
        if ("locked-out".equalsIgnoreCase(effective)) {
            engine.preProcess(VendorNames.FONASA, scenario);
            return SimulatorResponses.fonasaVerify(false, body.get("patientId"), "LOCKED_OUT", 0);
        }
        engine.preProcess(VendorNames.FONASA, scenario);
        String challengeId = body.get("challengeId");
        if (challengeId == null || challengeId.isBlank()) {
            challengeId = SimScenario.deterministicId("challenge", body.getOrDefault("patientId", "unknown"));
        }
        FonasaChallengeStore.VerifyResult result =
                fonasaChallengeStore.verify(challengeId, body.get("code"));
        if ("123456".equals(body.get("code")) && result.reason() == null && !result.verified()) {
            return SimulatorResponses.fonasaVerify(true, body.get("patientId"), null, 0);
        }
        return SimulatorResponses.fonasaVerify(
                result.verified(),
                result.patientId() != null ? result.patientId() : body.get("patientId"),
                result.reason(),
                result.attemptsRemaining());
    }
}

@RestController
@RequestMapping("/sim/vonage")
class VonageSimulatorController {

    private final SimulatorEngine engine;
    private final VideoSessionStore videoSessionStore;
    private final ProviderOutageState providerOutageState;

    VonageSimulatorController(
            SimulatorEngine engine,
            VideoSessionStore videoSessionStore,
            ProviderOutageState providerOutageState) {
        this.engine = engine;
        this.videoSessionStore = videoSessionStore;
        this.providerOutageState = providerOutageState;
    }

    @PostMapping("/session")
    public Map<String, Object> session(
            @RequestBody Map<String, String> body,
            @RequestHeader(value = SimScenario.HEADER, required = false) String scenario) {
        if (providerOutageState.isVonageOutage()) {
            throw new RuntimeException("Vonage simulated outage");
        }
        String effective = SimScenario.scenarioOrDefault(scenario);
        if ("capacity-exceeded".equalsIgnoreCase(effective)) {
            throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS, "Too many concurrent sessions");
        }
        if ("session-expired".equalsIgnoreCase(effective)) {
            throw new ResponseStatusException(HttpStatus.GONE, "Session no longer valid");
        }
        engine.preProcess(VendorNames.VONAGE, scenario);
        String appointmentId = body.getOrDefault("appointmentId", "default");
        String sessionId = SimScenario.deterministicId("vonage", appointmentId);
        videoSessionStore.create(VendorNames.VONAGE, sessionId, appointmentId);
        return SimulatorResponses.videoSession(VendorNames.VONAGE, sessionId, appointmentId);
    }

    @PostMapping("/token")
    public Map<String, Object> token(
            @RequestBody Map<String, String> body,
            @RequestHeader(value = SimScenario.HEADER, required = false) String scenario) {
        engine.preProcess(VendorNames.VONAGE, scenario);
        String sessionId = body.get("sessionId");
        if (sessionId != null && videoSessionStore.requireSession(VendorNames.VONAGE, sessionId) == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Session not found");
        }
        String role = body.getOrDefault("role", "patient");
        return SimulatorResponses.videoToken(
                VendorNames.VONAGE, role, SimScenario.deterministicId("tok", role));
    }

    @PostMapping("/outage")
    public Map<String, Boolean> outage(@RequestBody Map<String, Boolean> body) {
        boolean enabled = providerOutageState.setVonageOutage(Boolean.TRUE.equals(body.get("enabled")));
        return Map.of("outage", enabled);
    }

    @DeleteMapping("/sessions/{sessionId}")
    public Map<String, String> deleteSession(@PathVariable String sessionId) {
        videoSessionStore.remove(VendorNames.VONAGE, sessionId);
        return Map.of("sessionId", sessionId, "status", "DISCONNECTED");
    }
}

@RestController
@RequestMapping("/sim/acs")
class AcsSimulatorController {

    private final SimulatorEngine engine;
    private final VideoSessionStore videoSessionStore;
    private final ProviderOutageState providerOutageState;

    AcsSimulatorController(
            SimulatorEngine engine,
            VideoSessionStore videoSessionStore,
            ProviderOutageState providerOutageState) {
        this.engine = engine;
        this.videoSessionStore = videoSessionStore;
        this.providerOutageState = providerOutageState;
    }

    @PostMapping("/session")
    public Map<String, Object> session(
            @RequestBody(required = false) Map<String, String> body,
            @RequestHeader(value = SimScenario.HEADER, required = false) String scenario) {
        if (providerOutageState.isAcsOutage()) {
            throw new RuntimeException("ACS simulated outage");
        }
        String effective = SimScenario.scenarioOrDefault(scenario);
        if ("capacity-exceeded".equalsIgnoreCase(effective)) {
            throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS, "Too many concurrent sessions");
        }
        if ("session-expired".equalsIgnoreCase(effective)) {
            throw new ResponseStatusException(HttpStatus.GONE, "Session no longer valid");
        }
        engine.preProcess(VendorNames.ACS, scenario);
        String appointmentId = body != null ? body.getOrDefault("appointmentId", "default") : "default";
        String sessionId = SimScenario.deterministicId("acs", appointmentId);
        videoSessionStore.create(VendorNames.ACS, sessionId, appointmentId);
        return SimulatorResponses.videoSession(VendorNames.ACS, sessionId, appointmentId);
    }

    @PostMapping("/token")
    public Map<String, Object> token(
            @RequestBody Map<String, String> body,
            @RequestHeader(value = SimScenario.HEADER, required = false) String scenario) {
        engine.preProcess(VendorNames.ACS, scenario);
        String sessionId = body.get("sessionId");
        if (sessionId != null && videoSessionStore.requireSession(VendorNames.ACS, sessionId) == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Session not found");
        }
        String role = body.getOrDefault("role", "patient");
        return SimulatorResponses.videoToken(
                VendorNames.ACS, role, SimScenario.deterministicId("acs-tok", role));
    }

    @PostMapping("/outage")
    public Map<String, Boolean> outage(@RequestBody Map<String, Boolean> body) {
        boolean enabled = providerOutageState.setAcsOutage(Boolean.TRUE.equals(body.get("enabled")));
        return Map.of("outage", enabled);
    }

    @DeleteMapping("/sessions/{sessionId}")
    public Map<String, String> deleteSession(@PathVariable String sessionId) {
        videoSessionStore.remove(VendorNames.ACS, sessionId);
        return Map.of("sessionId", sessionId, "status", "DISCONNECTED");
    }
}

@RestController
@RequestMapping("/sim/mpi")
class MpiSimulatorController {

    private final SimulatorEngine engine;

    MpiSimulatorController(SimulatorEngine engine) {
        this.engine = engine;
    }

    @GetMapping("/patients/{patientRef}")
    public ResponseEntity<Map<String, Object>> patient(
            @PathVariable String patientRef,
            @RequestHeader(value = SimScenario.HEADER, required = false) String scenario) {
        String effective = SimScenario.scenarioOrDefault(scenario);
        if ("not-found".equalsIgnoreCase(effective)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Patient not found in MPI registry");
        }
        if ("multiple-matches".equalsIgnoreCase(effective)) {
            engine.preProcess(VendorNames.MPI, scenario);
            Map<String, Object> candidateOne = SimulatorResponses.mpiPatient(patientRef + "-1");
            Map<String, Object> candidateTwo = SimulatorResponses.mpiPatient(patientRef + "-2");
            return ResponseEntity.status(HttpStatus.MULTIPLE_CHOICES)
                    .body(Map.of("candidates", List.of(candidateOne, candidateTwo)));
        }
        engine.preProcess(VendorNames.MPI, scenario);
        return ResponseEntity.ok(SimulatorResponses.mpiPatient(patientRef));
    }
}

@RestController
@RequestMapping("/sim/medipass")
class MedipassSimulatorController {

    private final SimulatorEngine engine;

    MedipassSimulatorController(SimulatorEngine engine) {
        this.engine = engine;
    }

    @PostMapping("/certificates")
    public Map<String, String> certificate(
            @RequestBody Map<String, Object> body,
            @RequestHeader(value = SimScenario.HEADER, required = false) String scenario) {
        String effective = SimScenario.scenarioOrDefault(scenario);
        if ("certificate-rejected".equalsIgnoreCase(effective)) {
            throw new ResponseStatusException(
                    HttpStatus.UNPROCESSABLE_ENTITY, "Certificate rejected by regulatory body");
        }
        engine.preProcess(VendorNames.MEDIPASS, scenario);
        String appointmentId = String.valueOf(body.getOrDefault("appointmentId", "default"));
        return SimulatorResponses.medipassCertificate(appointmentId);
    }
}

@RestController
@RequestMapping("/sim/notify")
class NotifySimulatorController {

    private final SimulatorEngine engine;
    private final NotifyDeliveryLog notifyDeliveryLog;

    NotifySimulatorController(SimulatorEngine engine, NotifyDeliveryLog notifyDeliveryLog) {
        this.engine = engine;
        this.notifyDeliveryLog = notifyDeliveryLog;
    }

    @PostMapping("/{channel}")
    public Map<String, String> send(
            @PathVariable String channel,
            @RequestBody Map<String, String> body,
            @RequestHeader(value = SimScenario.HEADER, required = false) String scenario) {
        String effective = SimScenario.scenarioOrDefault(scenario);
        if ("channel-unavailable".equalsIgnoreCase(effective)) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Notification channel unavailable");
        }
        if ("invalid-recipient".equalsIgnoreCase(effective)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid recipient format");
        }
        engine.preProcess(VendorNames.NOTIFY, scenario);
        String recipient = body.getOrDefault("recipientRef", "unknown");
        String appointmentId = body.get("appointmentId");
        String messageId = SimScenario.deterministicId("msg", channel + recipient);
        notifyDeliveryLog.append(
                new NotifyDeliveryLog.DeliveryEntry(
                        channel, messageId, "SENT", appointmentId, recipient, Instant.now()));
        return SimulatorResponses.notifySend(channel, messageId);
    }
}

@RestController
@RequestMapping("/sim/anura")
class AnuraSimulatorController {

    private final SimulatorEngine engine;

    AnuraSimulatorController(SimulatorEngine engine) {
        this.engine = engine;
    }

    @PostMapping("/token")
    public Map<String, Object> token(
            @RequestBody(required = false) Map<String, String> body,
            @RequestHeader(value = SimScenario.HEADER, required = false) String scenario) {
        String effective = SimScenario.scenarioOrDefault(scenario);
        if ("consent-missing".equalsIgnoreCase(effective)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Patient consent not recorded");
        }
        engine.preProcess(VendorNames.ANURA, scenario);
        String appointmentId = body != null ? body.getOrDefault("appointmentId", "default") : "default";
        return SimulatorResponses.anuraToken(SimScenario.deterministicId("anura", appointmentId));
    }

    @PostMapping("/decryptResults")
    public Map<String, Object> decrypt(
            @RequestBody Map<String, String> body,
            @RequestHeader(value = SimScenario.HEADER, required = false) String scenario) {
        String effective = SimScenario.scenarioOrDefault(scenario);
        if ("measurement-failed".equalsIgnoreCase(effective)) {
            engine.preProcess(VendorNames.ANURA, scenario);
            return SimulatorResponses.anuraResults(body.get("token"), 0.0);
        }
        engine.preProcess(VendorNames.ANURA, scenario);
        return SimulatorResponses.anuraResults(body.get("token"), 0.95);
    }
}

@RestController
@RequestMapping("/sim/recmed")
class RecMedSimulatorController {

    private final SimulatorEngine engine;

    RecMedSimulatorController(SimulatorEngine engine) {
        this.engine = engine;
    }

    @PostMapping("/prescriptions")
    public Map<String, String> prescription(
            @RequestBody Map<String, Object> body,
            @RequestHeader(value = SimScenario.HEADER, required = false) String scenario) {
        String effective = SimScenario.scenarioOrDefault(scenario);
        if ("validation-error".equalsIgnoreCase(effective)) {
            throw new ResponseStatusException(
                    HttpStatus.UNPROCESSABLE_ENTITY, "Prescription validation failed");
        }
        engine.preProcess(VendorNames.RECMED, scenario);
        String appointmentId = String.valueOf(body.getOrDefault("appointmentId", "default"));
        return SimulatorResponses.recMedPrescription(appointmentId);
    }

    @PostMapping("/certificates")
    public Map<String, String> certificate(
            @RequestBody Map<String, Object> body,
            @RequestHeader(value = SimScenario.HEADER, required = false) String scenario) {
        engine.preProcess(VendorNames.RECMED, scenario);
        String appointmentId = String.valueOf(body.getOrDefault("appointmentId", "default"));
        return SimulatorResponses.recMedCertificate(appointmentId);
    }
}

@RestController
@RequestMapping("/sim/zendesk")
class ZendeskSimulatorController {

    private final SimulatorEngine engine;

    ZendeskSimulatorController(SimulatorEngine engine) {
        this.engine = engine;
    }

    @PostMapping("/tickets")
    public ResponseEntity<Map<String, String>> ticket(
            @RequestBody Map<String, String> body,
            @RequestHeader(value = SimScenario.HEADER, required = false) String scenario) {
        String effective = SimScenario.scenarioOrDefault(scenario);
        if ("rate-limited".equalsIgnoreCase(effective)) {
            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                    .header(HttpHeaders.RETRY_AFTER, "30")
                    .body(Map.of("status", "RATE_LIMITED"));
        }
        engine.preProcess(VendorNames.ZENDESK, scenario);
        String subject = body.getOrDefault("subject", "support");
        return ResponseEntity.ok(SimulatorResponses.zendeskTicket(subject));
    }
}
