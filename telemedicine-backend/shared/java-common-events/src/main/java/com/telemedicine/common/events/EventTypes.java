package com.telemedicine.common.events;

import java.util.List;

/**
 * Every domain event the platform publishes.
 *
 * <p>This class, {@code shared/contracts/events/event-catalogue.yaml} and {@code
 * telemedicine-platform/infra/kafka/topics.txt} describe the same set of events, and a contract
 * test fails the build when they diverge. Before that test existed the three had drifted to eleven,
 * twenty-eight and seven entries respectively, which meant a service could publish to a topic that
 * was never created and the message would simply vanish.
 *
 * <p>A constant's value is the wire type and the topic name. Renaming one is a breaking change for
 * every consumer and for the Kafka topics themselves.
 */
public final class EventTypes {

    // --- Appointment and payment (integration-service) ---
    public static final String APPOINTMENT_CREATED = "AppointmentCreated";
    public static final String APPOINTMENT_RESCHEDULED = "AppointmentRescheduled";
    public static final String PAYMENT_CONFIRMED = "PaymentConfirmed";

    // --- Access and consent (waiting-room-service) ---
    public static final String LINK_GENERATED = "LinkGenerated";
    public static final String CONSENT_GRANTED = "ConsentGranted";
    public static final String CONSENT_WITHDRAWN = "ConsentWithdrawn";

    // --- Identity (identity-service) ---
    public static final String IDENTITY_VALIDATED = "IdentityValidated";
    public static final String IDENTITY_VALIDATION_FAILED = "IdentityValidationFailed";

    // --- Waiting room (waiting-room-service) ---
    public static final String DEVICE_TEST_COMPLETED = "DeviceTestCompleted";
    public static final String PATIENT_ENTERED_WAITING_ROOM = "PatientEnteredWaitingRoom";
    public static final String STATUS_CHANGED = "StatusChanged";

    // --- Consultation context for physician review (patient-booking-service) ---
    public static final String CONSULTATION_CONTEXT_PUBLISHED = "ConsultationContextPublished";

    // --- Video consultation (video-service) ---
    public static final String VIDEO_SESSION_CREATED = "VideoSessionCreated";
    public static final String VIDEO_SESSION_STARTED = "VideoSessionStarted";
    public static final String VIDEO_SESSION_ENDED = "VideoSessionEnded";
    public static final String CHAT_MESSAGE_SENT = "ChatMessageSent";

    // --- Telemetry and quality ---
    public static final String TELEMETRY_MEASURED = "TelemetryMeasured";
    public static final String CONNECTION_QUALITY_DEGRADED = "ConnectionQualityDegraded";

    // --- Clinical documents (clinical-docs-service) ---
    public static final String CLINICAL_DOCUMENT_ISSUED = "ClinicalDocumentIssued";
    public static final String PRESCRIPTION_ISSUED = "PrescriptionIssued";
    public static final String MEDICAL_LEAVE_ISSUED = "MedicalLeaveIssued";

    // --- Consultation closure and survey ---
    public static final String CONSULTATION_CLOSED = "ConsultationClosed";
    public static final String SURVEY_REQUESTED = "SurveyRequested";
    public static final String SURVEY_COMPLETED = "SurveyCompleted";

    // --- Notification (notification-service) ---
    public static final String NOTIFICATION_SENT = "NotificationSent";
    public static final String NOTIFICATION_FAILED = "NotificationFailed";

    // --- Platform ---
    public static final String INTEGRATION_FAILED = "IntegrationFailed";
    public static final String CONFIGURATION_CHANGED = "ConfigurationChanged";
    public static final String AUDIT_RECORDED = "AuditRecorded";

    /** The complete catalogue, in the order the events occur across the patient journey. */
    public static final List<String> ALL =
            List.of(
                    APPOINTMENT_CREATED,
                    APPOINTMENT_RESCHEDULED,
                    PAYMENT_CONFIRMED,
                    LINK_GENERATED,
                    CONSENT_GRANTED,
                    CONSENT_WITHDRAWN,
                    IDENTITY_VALIDATED,
                    IDENTITY_VALIDATION_FAILED,
                    DEVICE_TEST_COMPLETED,
                    PATIENT_ENTERED_WAITING_ROOM,
                    STATUS_CHANGED,
                    CONSULTATION_CONTEXT_PUBLISHED,
                    VIDEO_SESSION_CREATED,
                    VIDEO_SESSION_STARTED,
                    VIDEO_SESSION_ENDED,
                    CHAT_MESSAGE_SENT,
                    TELEMETRY_MEASURED,
                    CONNECTION_QUALITY_DEGRADED,
                    CLINICAL_DOCUMENT_ISSUED,
                    PRESCRIPTION_ISSUED,
                    MEDICAL_LEAVE_ISSUED,
                    CONSULTATION_CLOSED,
                    SURVEY_REQUESTED,
                    SURVEY_COMPLETED,
                    NOTIFICATION_SENT,
                    NOTIFICATION_FAILED,
                    INTEGRATION_FAILED,
                    CONFIGURATION_CHANGED,
                    AUDIT_RECORDED);

    private EventTypes() {}

    /** Whether the value names an event in the catalogue. */
    public static boolean isKnown(String eventType) {
        return ALL.contains(eventType);
    }

    /**
     * @throws IllegalArgumentException when the type is not in the catalogue, which is how a typo
     *     in a publisher is caught before the message is written rather than after
     */
    public static String require(String eventType) {
        if (!isKnown(eventType)) {
            throw new IllegalArgumentException(
                    "Unknown event type '"
                            + eventType
                            + "'. Add it to EventTypes, event-catalogue.yaml and topics.txt"
                            + " together.");
        }
        return eventType;
    }
}
