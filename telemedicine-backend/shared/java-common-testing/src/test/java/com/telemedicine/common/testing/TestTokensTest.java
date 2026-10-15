package com.telemedicine.common.testing;

import static org.assertj.core.api.Assertions.assertThat;

import com.telemedicine.common.testing.jwt.TestJwts;
import org.junit.jupiter.api.Test;

class TestTokensTest {

    @Test
    @SuppressWarnings("deprecation")
    void deprecatedConstantsMatchTestJwts() {
        assertThat(TestTokens.PATIENT).isEqualTo(TestJwts.patient().getTokenValue());
        assertThat(TestTokens.PHYSICIAN).isEqualTo(TestJwts.physician().getTokenValue());
        assertThat(TestTokens.ADMIN).isEqualTo(TestJwts.admin().getTokenValue());
    }
}
