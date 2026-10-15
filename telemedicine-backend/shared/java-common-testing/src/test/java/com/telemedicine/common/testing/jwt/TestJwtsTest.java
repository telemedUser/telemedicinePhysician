package com.telemedicine.common.testing.jwt;

import static org.assertj.core.api.Assertions.assertThat;

import com.telemedicine.common.security.Roles;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jwt.Jwt;

class TestJwtsTest {

    @Test
    void buildsRoleSpecificTokens() {
        Jwt patient = TestJwts.patient();
        assertThat(patient.getSubject()).isEqualTo("patient.demo");
        assertThat(TestJwts.authorities(patient))
                .extracting(Object::toString)
                .containsExactly(Roles.PATIENT);

        Jwt physician = TestJwts.physician();
        assertThat(TestJwts.authorities(physician))
                .extracting(Object::toString)
                .containsExactly(Roles.PHYSICIAN);

        Jwt admin = TestJwts.admin();
        assertThat(TestJwts.authorities(admin))
                .extracting(Object::toString)
                .containsExactly(Roles.ADMIN);
    }

    @Test
    void decoderValidatesSignedToken() {
        Jwt jwt = TestJwts.patient();
        Jwt decoded = TestJwts.decoder().decode(jwt.getTokenValue());
        assertThat(decoded.getSubject()).isEqualTo("patient.demo");
    }

    @Test
    void buildsServiceToken() {
        assertThat(TestJwts.authorities(TestJwts.service()))
                .extracting(Object::toString)
                .containsExactly(Roles.SERVICE);
    }

    @Test
    void bearerPrefixIsPresent() {
        assertThat(TestJwts.patientBearer()).startsWith("Bearer ");
    }
}
