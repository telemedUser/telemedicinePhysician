package com.telemedicine.waitingroomservice.sessionlink;

import static org.assertj.core.api.Assertions.assertThat;

import com.telemedicine.waitingroomservice.config.WaitingRoomServiceProperties;
import org.junit.jupiter.api.Test;

class SessionLinkTokenServiceTest {

    @Test
    void issuesDeterministicLengthTokenAndHash() {
        WaitingRoomServiceProperties properties = new WaitingRoomServiceProperties();
        properties.setSessionLinkSecret("unit-test-secret");
        SessionLinkTokenService tokenService = new SessionLinkTokenService(properties);

        SessionLinkTokenService.IssuedToken issued = tokenService.issue("appt-42");

        assertThat(issued.token()).contains("appt-42");
        assertThat(issued.tokenHash()).hasSize(64);
        assertThat(tokenService.hash(issued.token())).isEqualTo(issued.tokenHash());
    }
}
