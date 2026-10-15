package com.telemedicine.waitingroomservice.sessionlink;

import com.telemedicine.common.core.id.IdGenerator;
import com.telemedicine.waitingroomservice.config.WaitingRoomServiceProperties;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.HexFormat;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.stereotype.Component;

@Component
public class SessionLinkTokenService {

    private final WaitingRoomServiceProperties properties;

    public SessionLinkTokenService(WaitingRoomServiceProperties properties) {
        this.properties = properties;
    }

    public IssuedToken issue(String appointmentId) {
        String linkId = IdGenerator.newId();
        Instant expiresAt = Instant.now().plus(properties.getSessionLinkTtl());
        String payload = appointmentId + "|" + linkId + "|" + expiresAt.toEpochMilli();
        String signature = sign(payload);
        String token = payload + "|" + signature;
        return new IssuedToken(linkId, token, hash(token), expiresAt);
    }

    public String hash(String token) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashed = digest.digest(token.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hashed);
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 unavailable", exception);
        }
    }

    private String sign(String payload) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(
                    new SecretKeySpec(
                            properties.getSessionLinkSecret().getBytes(StandardCharsets.UTF_8),
                            "HmacSHA256"));
            byte[] signature = mac.doFinal(payload.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(signature);
        } catch (Exception exception) {
            throw new IllegalStateException("Unable to sign session link token", exception);
        }
    }

    public record IssuedToken(String linkId, String token, String tokenHash, Instant expiresAt) {}
}
