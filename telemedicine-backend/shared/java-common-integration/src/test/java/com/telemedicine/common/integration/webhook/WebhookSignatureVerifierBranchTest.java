package com.telemedicine.common.integration.webhook;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.telemedicine.common.core.error.DomainException;
import com.telemedicine.common.integration.TelemedicineIntegrationProperties;
import java.time.Duration;
import org.junit.jupiter.api.Test;

class WebhookSignatureVerifierBranchTest {

    @Test
    void rejectsFutureTimestamp() {
        WebhookSignatureVerifier verifier = verifier();
        long future = java.time.Instant.now().plus(Duration.ofMinutes(10)).getEpochSecond();
        String payload = "{}";
        String signature = verifier.sign(future, payload);

        assertThatThrownBy(() -> verifier.verify(payload, future, signature))
                .isInstanceOf(DomainException.class);
    }

    private static WebhookSignatureVerifier verifier() {
        TelemedicineIntegrationProperties.Webhook webhook =
                new TelemedicineIntegrationProperties.Webhook();
        webhook.setTimestampWindow(Duration.ofMinutes(5));
        return new WebhookSignatureVerifier("secret", webhook);
    }
}
