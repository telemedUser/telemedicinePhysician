package com.telemedicine.common.persistence.audit;

import static org.assertj.core.api.Assertions.assertThat;

import com.telemedicine.common.security.access.SecurityContextAccessor;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

class TelemedicineAuditorAwareTest {

    @AfterEach
    void clearContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void resolvesCurrentUserIdFromSecurityContext() {
        Jwt jwt =
                Jwt.withTokenValue("token").header("alg", "none").claim("sub", "user-123").build();
        SecurityContextHolder.getContext().setAuthentication(new JwtAuthenticationToken(jwt));

        TelemedicineAuditorAware auditorAware =
                new TelemedicineAuditorAware(new SecurityContextAccessor());

        assertThat(auditorAware.getCurrentAuditor()).isEqualTo(Optional.of("user-123"));
    }

    @Test
    void returnsEmptyWhenUnauthenticated() {
        TelemedicineAuditorAware auditorAware =
                new TelemedicineAuditorAware(new SecurityContextAccessor());

        assertThat(auditorAware.getCurrentAuditor()).isEmpty();
    }
}
