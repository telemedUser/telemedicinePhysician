package com.telemedicine.common.integration.webhook;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.telemedicine.common.core.error.DomainException;
import com.telemedicine.common.integration.TelemedicineIntegrationProperties;
import java.time.Duration;
import org.junit.jupiter.api.Test;

class WebhookSignatureVerifierTest {

    private static final String SECRET = "test-secret";

    @Test
    void acceptsValidSignature() {
        WebhookSignatureVerifier verifier = verifier();
        long timestamp = java.time.Instant.now().getEpochSecond();
        String payload = "{\"event\":\"payment\"}";
        String signature = verifier.sign(timestamp, payload);

        assertThatCode(() -> verifier.verify(payload, timestamp, signature))
                .doesNotThrowAnyException();
    }

    @Test
    void rejectsReplay() {
        WebhookSignatureVerifier verifier = verifier();
        long timestamp = java.time.Instant.now().getEpochSecond();
        String payload = "{\"event\":\"payment\"}";
        String signature = verifier.sign(timestamp, payload);
        verifier.verify(payload, timestamp, signature);

        assertThatThrownBy(() -> verifier.verify(payload, timestamp, signature))
                .isInstanceOf(DomainException.class)
                .hasMessageContaining("replay");
    }

    @Test
    void rejectsStaleTimestamp() {
        WebhookSignatureVerifier verifier = verifier();
        long timestamp = java.time.Instant.now().minus(Duration.ofMinutes(10)).getEpochSecond();
        String payload = "{}";
        String signature = verifier.sign(timestamp, payload);

        assertThatThrownBy(() -> verifier.verify(payload, timestamp, signature))
                .isInstanceOf(DomainException.class)
                .hasMessageContaining("timestamp");
    }

    @Test
    void rejectsInvalidSignature() {
        WebhookSignatureVerifier verifier = verifier();
        long timestamp = java.time.Instant.now().getEpochSecond();

        assertThatThrownBy(() -> verifier.verify("{}", timestamp, "deadbeef"))
                .isInstanceOf(DomainException.class)
                .hasMessageContaining("mismatch");
    }

    private static WebhookSignatureVerifier verifier() {
        TelemedicineIntegrationProperties.Webhook webhook =
                new TelemedicineIntegrationProperties.Webhook();
        webhook.setTimestampWindow(Duration.ofMinutes(5));
        webhook.setReplayCacheTtl(Duration.ofMinutes(30));
        return new WebhookSignatureVerifier(SECRET, webhook);
    }
}
