package com.telemedicine.common.security.jwt;

import java.util.List;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtClaimNames;

/**
 * Rejects a token that was not issued for this platform.
 *
 * <p>Without this check any token the realm signs is accepted, so a token minted for an unrelated
 * client in the same realm authenticates here. Signature validity is not authorisation to call this
 * API.
 */
public class AudienceValidator implements OAuth2TokenValidator<Jwt> {

    private final List<String> accepted;

    public AudienceValidator(List<String> accepted) {
        if (accepted == null || accepted.isEmpty()) {
            throw new IllegalArgumentException(
                    "At least one accepted audience is required; omit the validator entirely to"
                            + " skip the check.");
        }
        this.accepted = List.copyOf(accepted);
    }

    @Override
    public OAuth2TokenValidatorResult validate(Jwt token) {
        List<String> audience = token.getAudience();
        if (audience != null && audience.stream().anyMatch(accepted::contains)) {
            return OAuth2TokenValidatorResult.success();
        }
        return OAuth2TokenValidatorResult.failure(
                new OAuth2Error(
                        "invalid_token",
                        "The required audience " + accepted + " is missing",
                        "https://datatracker.ietf.org/doc/html/rfc6750#section-3.1"));
    }

    /** Audiences this validator accepts. */
    public List<String> accepted() {
        return accepted;
    }

    /** Claim name being validated, exposed for diagnostics. */
    public static String claimName() {
        return JwtClaimNames.AUD;
    }
}
