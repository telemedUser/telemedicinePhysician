package com.telemedicine.common.events;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

import com.telemedicine.common.events.payload.LinkGenerated;
import java.time.Instant;
import org.junit.jupiter.api.Test;

class EventEnvelopeTest {

    @Test
    void builderFillsDefaults() {
        LinkGenerated payload =
                new LinkGenerated("APT-1", "LNK-1", Instant.parse("2026-06-01T12:00:00Z"), "SMS");

        EventEnvelope<LinkGenerated> envelope =
                EventEnvelope.<LinkGenerated>builder()
                        .type(EventTypes.LINK_GENERATED)
                        .subject("APT-1")
                        .payload(payload)
                        .build();

        assertThat(envelope.eventId()).isNotBlank();
        assertThat(envelope.version()).isEqualTo("1.0");
        assertThat(envelope.occurredAt()).isNotNull();
        assertThat(envelope.type()).isEqualTo(EventTypes.LINK_GENERATED);
        assertThat(envelope.subject()).isEqualTo("APT-1");
        assertThat(envelope.payload()).isEqualTo(payload);
    }

    @Test
    void blankVersionFallsBackToDefault() {
        EventEnvelope<String> envelope =
                EventEnvelope.<String>builder()
                        .type(EventTypes.STATUS_CHANGED)
                        .version("   ")
                        .subject("APT-1")
                        .payload("x")
                        .build();

        assertThat(envelope.version()).isEqualTo("1.0");
    }

    @Test
    void builderHonoursExplicitValues() {
        Instant when = Instant.parse("2026-03-01T10:00:00Z");
        EventEnvelope<String> envelope =
                EventEnvelope.<String>builder()
                        .eventId("fixed-id")
                        .type(EventTypes.STATUS_CHANGED)
                        .version("2.0")
                        .occurredAt(when)
                        .correlationId("01JCORR")
                        .causationId("01JCAUSE")
                        .subject("APT-9")
                        .payload("x")
                        .build();

        assertThat(envelope.eventId()).isEqualTo("fixed-id");
        assertThat(envelope.version()).isEqualTo("2.0");
        assertThat(envelope.occurredAt()).isEqualTo(when);
        assertThat(envelope.correlationId()).isEqualTo("01JCORR");
    }

    @Test
    void rejectsUnknownTypesAndMissingSubjects() {
        assertThatIllegalArgumentException()
                .isThrownBy(
                        () ->
                                EventEnvelope.builder()
                                        .type("Unknown")
                                        .subject("APT-1")
                                        .payload("x")
                                        .build());
        assertThatIllegalArgumentException()
                .isThrownBy(
                        () ->
                                EventEnvelope.builder()
                                        .type(EventTypes.LINK_GENERATED)
                                        .subject("  ")
                                        .payload("x")
                                        .build());
    }
}
