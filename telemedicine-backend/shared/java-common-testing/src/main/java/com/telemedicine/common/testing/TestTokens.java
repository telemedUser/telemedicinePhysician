package com.telemedicine.common.testing;

import com.telemedicine.common.testing.jwt.TestJwts;

/**
 * Legacy bearer-token placeholders.
 *
 * @deprecated use {@link TestJwts#patientBearer()}, {@link TestJwts#physicianBearer()} and {@link
 *     TestJwts#adminBearer()} instead. Plain strings cannot be validated by the resource server.
 */
@Deprecated(since = "1.0.0", forRemoval = true)
public final class TestTokens {

    @Deprecated(since = "1.0.0", forRemoval = true)
    public static final String PATIENT = TestJwts.patient().getTokenValue();

    @Deprecated(since = "1.0.0", forRemoval = true)
    public static final String PHYSICIAN = TestJwts.physician().getTokenValue();

    @Deprecated(since = "1.0.0", forRemoval = true)
    public static final String ADMIN = TestJwts.admin().getTokenValue();

    private TestTokens() {}
}
