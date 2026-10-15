package com.telemedicine.common.integration.webhook;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.telemedicine.common.core.error.DomainException;
import com.telemedicine.common.core.error.ErrorCode;
import com.telemedicine.common.integration.TelemedicineIntegrationProperties;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Duration;
import java.time.Instant;
import java.util.HexFormat;
import java.util.Objects;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

/**
 * Validates inbound vendor webhooks using HMAC-SHA256, a timestamp freshness window and a replay
 * cache.
 */
public class WebhookSignatureVerifier {

    private static final String HMAC_SHA256 = "HmacSHA256";

    private final byte[] secret;
    private final Duration timestampWindow;
    private final Cache<String, Boolean> replayCache;

    public WebhookSignatureVerifier(
            String secret, TelemedicineIntegrationProperties.Webhook webhookProperties) {
        this.secret = Objects.requireNonNull(secret, "secret").getBytes(StandardCharsets.UTF_8);
        TelemedicineIntegrationProperties.Webhook webhook =
                webhookProperties == null
                        ? new TelemedicineIntegrationProperties.Webhook()
                        : webhookProperties;
        this.timestampWindow =
                webhook.getTimestampWindow() == null
                        ? Duration.ofMinutes(5)
                        : webhook.getTimestampWindow();
        Duration replayTtl =
                webhook.getReplayCacheTtl() == null
                        ? Duration.ofHours(1)
                        : webhook.getReplayCacheTtl();
        this.replayCache =
                Caffeine.newBuilder().expireAfterWrite(replayTtl).maximumSize(10_000).build();
    }

    /**
     * Validates the webhook signature.
     *
     * @param payload raw request body
     * @param timestamp Unix epoch seconds from the vendor header
     * @param signature hex-encoded HMAC-SHA256 of {@code timestamp + "." + payload}
     */
    public void verify(String payload, long timestamp, String signature) {
        Objects.requireNonNull(payload, "payload");
        Objects.requireNonNull(signature, "signature");
        Instant requestTime = Instant.ofEpochSecond(timestamp);
        Instant now = Instant.now();
        if (requestTime.isBefore(now.minus(timestampWindow))
                || requestTime.isAfter(now.plus(timestampWindow))) {
            throw new DomainException(
                    ErrorCode.WEBHOOK_SIGNATURE_INVALID,
                    "Webhook timestamp outside allowed window");
        }
        String replayKey = digest(payload, timestamp, signature);
        if (replayCache.getIfPresent(replayKey) != null) {
            throw new DomainException(
                    ErrorCode.WEBHOOK_SIGNATURE_INVALID, "Webhook replay detected");
        }
        String expected = sign(timestamp, payload);
        if (!constantTimeEquals(expected, normalize(signature))) {
            throw new DomainException(
                    ErrorCode.WEBHOOK_SIGNATURE_INVALID, "Webhook signature mismatch");
        }
        replayCache.put(replayKey, Boolean.TRUE);
    }

    String sign(long timestamp, String payload) {
        try {
            Mac mac = Mac.getInstance(HMAC_SHA256);
            mac.init(new SecretKeySpec(secret, HMAC_SHA256));
            String content = timestamp + "." + payload;
            byte[] digest = mac.doFinal(content.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (Exception exception) {
            throw new IllegalStateException("Unable to compute webhook signature", exception);
        }
    }

    private static String digest(String payload, long timestamp, String signature) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash =
                    digest.digest(
                            (timestamp + ":" + signature + ":" + payload)
                                    .getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (Exception exception) {
            throw new IllegalStateException("Unable to hash replay key", exception);
        }
    }

    private static String normalize(String signature) {
        String trimmed = signature.trim();
        if (trimmed.startsWith("sha256=")) {
            return trimmed.substring("sha256=".length());
        }
        return trimmed;
    }

    private static boolean constantTimeEquals(String left, String right) {
        if (left.length() != right.length()) {
            return false;
        }
        int result = 0;
        for (int index = 0; index < left.length(); index++) {
            result |= left.charAt(index) ^ right.charAt(index);
        }
        return result == 0;
    }
}
