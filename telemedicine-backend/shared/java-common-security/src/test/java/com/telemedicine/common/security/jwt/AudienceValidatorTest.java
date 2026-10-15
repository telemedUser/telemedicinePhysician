package com.telemedicine.common.security.jwt;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jwt.Jwt;

class AudienceValidatorTest {

    private final AudienceValidator validator =
            new AudienceValidator(List.of("telemedicine-api", "gateway"));

    @Test
    void acceptsATokenCarryingOneOfTheExpectedAudiences() {
        assertThat(validator.validate(withAudience(List.of("account", "gateway"))).hasErrors())
                .isFalse();
    }

    @Test
    @DisplayName("a token signed by the realm but issued to another client is rejected")
    void rejectsAnUnrelatedAudience() {
        OAuth2TokenValidatorResult result = validator.validate(withAudience(List.of("account")));

        assertThat(result.hasErrors()).isTrue();
        assertThat(result.getErrors())
                .singleElement()
                .satisfies(
                        error -> {
                            assertThat(error.getErrorCode()).isEqualTo("invalid_token");
                            assertThat(error.getDescription()).contains("telemedicine-api");
                        });
    }

    @Test
    void rejectsATokenWithNoAudienceAtAll() {
        assertThat(validator.validate(withAudience(null)).hasErrors()).isTrue();
        assertThat(validator.validate(withAudience(List.of())).hasErrors()).isTrue();
    }

    @Test
    @DisplayName("constructing with no audiences fails rather than silently accepting everything")
    void refusesAnEmptyConfiguration() {
        assertThatIllegalArgumentException().isThrownBy(() -> new AudienceValidator(List.of()));
        assertThatIllegalArgumentException().isThrownBy(() -> new AudienceValidator(null));
    }

    @Test
    void exposesItsConfigurationForDiagnostics() {
        assertThat(validator.accepted()).containsExactly("telemedicine-api", "gateway");
        assertThat(AudienceValidator.claimName()).isEqualTo("aud");
    }

    private static Jwt withAudience(List<String> audience) {
        Jwt.Builder builder = Jwt.withTokenValue("token").header("alg", "RS256").subject("user");
        if (audience != null) {
            builder.audience(audience);
        }
        return builder.build();
    }
}
