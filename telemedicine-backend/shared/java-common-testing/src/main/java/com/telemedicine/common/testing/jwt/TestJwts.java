package com.telemedicine.common.testing.jwt;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.gen.RSAKeyGenerator;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import com.telemedicine.common.security.Roles;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.Map;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;

/**
 * Factory for RSA-signed JWTs shaped like Keycloak tokens from the Phase 0 realm.
 *
 * <p>Replaces the placeholder {@link com.telemedicine.common.testing.TestTokens} string constants
 * with verifiable tokens and a matching {@link JwtDecoder} for {@code @WebMvcTest} and integration
 * tests.
 */
public final class TestJwts {

    private static final String ISSUER = "http://localhost/realms/telemedicine";

    private static final RSAKey RSA_KEY;

    static {
        try {
            RSA_KEY = new RSAKeyGenerator(2048).keyID("telemedicine-test-key").generate();
        } catch (JOSEException ex) {
            throw new IllegalStateException("Failed to generate test RSA key pair", ex);
        }
    }

    private TestJwts() {}

    public static Jwt patient() {
        return build(
                "patient.demo",
                "patient.demo",
                List.of(Roles.REALM_PATIENT),
                Map.of("patient_id", "patient-synthetic-001"));
    }

    public static Jwt physician() {
        return build(
                "physician.demo",
                "physician.demo",
                List.of(Roles.REALM_PHYSICIAN),
                Map.of("physician_id", "physician-synthetic-001"));
    }

    public static Jwt admin() {
        return build("admin.demo", "admin.demo", List.of(Roles.REALM_ADMIN), Map.of());
    }

    public static Jwt service() {
        return build(
                "service-account-gateway",
                "service-account-gateway",
                List.of("SERVICE"),
                Map.of("client_id", "gateway"));
    }

    /** Bearer value for HTTP {@code Authorization} headers. */
    public static String bearer(Jwt jwt) {
        return "Bearer " + jwt.getTokenValue();
    }

    public static String patientBearer() {
        return bearer(patient());
    }

    public static String physicianBearer() {
        return bearer(physician());
    }

    public static String adminBearer() {
        return bearer(admin());
    }

    public static JwtDecoder decoder() {
        try {
            return NimbusJwtDecoder.withPublicKey(RSA_KEY.toRSAPublicKey()).build();
        } catch (JOSEException ex) {
            throw new IllegalStateException("Failed to build test JwtDecoder", ex);
        }
    }

    public static List<SimpleGrantedAuthority> authorities(Jwt jwt) {
        @SuppressWarnings("unchecked")
        Map<String, Object> realmAccess = jwt.getClaim("realm_access");
        if (realmAccess == null) {
            return List.of();
        }
        @SuppressWarnings("unchecked")
        List<String> roles = (List<String>) realmAccess.get("roles");
        if (roles == null) {
            return List.of();
        }
        return roles.stream().map(Roles::authority).map(SimpleGrantedAuthority::new).toList();
    }

    private static Jwt build(
            String subject,
            String preferredUsername,
            List<String> realmRoles,
            Map<String, Object> extraClaims) {
        Instant now = Instant.parse("2099-01-01T00:00:00Z");
        JWTClaimsSet.Builder claims =
                new JWTClaimsSet.Builder()
                        .issuer(ISSUER)
                        .subject(subject)
                        .issueTime(Date.from(now))
                        .expirationTime(Date.from(now.plusSeconds(3600)))
                        .claim("preferred_username", preferredUsername)
                        .claim("realm_access", Map.of("roles", realmRoles));
        extraClaims.forEach(claims::claim);

        try {
            SignedJWT signed =
                    new SignedJWT(
                            new JWSHeader.Builder(JWSAlgorithm.RS256)
                                    .keyID(RSA_KEY.getKeyID())
                                    .build(),
                            claims.build());
            signed.sign(new RSASSASigner(RSA_KEY.toRSAPrivateKey()));
            String tokenValue = signed.serialize();
            Jwt.Builder jwtBuilder =
                    Jwt.withTokenValue(tokenValue)
                            .header("alg", "RS256")
                            .header("kid", RSA_KEY.getKeyID())
                            .issuer(ISSUER)
                            .subject(subject)
                            .issuedAt(now)
                            .expiresAt(now.plusSeconds(3600))
                            .claim("preferred_username", preferredUsername)
                            .claim("realm_access", Map.of("roles", realmRoles));
            extraClaims.forEach(jwtBuilder::claim);
            return jwtBuilder.build();
        } catch (JOSEException ex) {
            throw new IllegalStateException("Failed to sign test JWT for " + subject, ex);
        }
    }
}
