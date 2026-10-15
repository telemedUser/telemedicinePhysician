package com.telemedicine.common.integration.webhook;

import static org.assertj.core.api.Assertions.assertThat;

import com.telemedicine.common.integration.TelemedicineIntegrationProperties;
import java.time.Duration;
import org.junit.jupiter.api.Test;

class WebhookSignatureVerifierExtendedTest {

    @Test
    void acceptsSha256Prefix() {
        WebhookSignatureVerifier verifier = verifier();
        long timestamp = java.time.Instant.now().getEpochSecond();
        String payload = "{}";
        String signature = "sha256=" + verifier.sign(timestamp, payload);

        verifier.verify(payload, timestamp, signature);
        assertThat(signature).startsWith("sha256=");
    }

    private static WebhookSignatureVerifier verifier() {
        TelemedicineIntegrationProperties.Webhook webhook =
                new TelemedicineIntegrationProperties.Webhook();
        webhook.setTimestampWindow(Duration.ofMinutes(5));
        webhook.setReplayCacheTtl(Duration.ofMinutes(30));
        return new WebhookSignatureVerifier("secret", webhook);
    }
}
