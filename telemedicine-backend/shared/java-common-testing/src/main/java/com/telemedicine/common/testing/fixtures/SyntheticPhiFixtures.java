package com.telemedicine.common.testing.fixtures;

/**
 * Obviously synthetic PHI for tests. Values are invalid or reserved so they cannot be mistaken for
 * real patient data.
 */
public final class SyntheticPhiFixtures {

    public static final String PATIENT_ID = "patient-synthetic-001";
    public static final String PHYSICIAN_ID = "physician-synthetic-001";
    public static final String APPOINTMENT_ID = "apt-synthetic-001";

    /** Reserved documentation RUT; not a valid checksum. */
    public static final String RUT = "11.111.111-1";

    public static final String EMAIL = "synthetic.patient@example.invalid";
    public static final String PHONE_E164 = "+56900000000";
    public static final String PATIENT_NAME = "Synthetic Patient";
    public static final String ADDRESS = "123 Test Street, Synthetic City";

    private SyntheticPhiFixtures() {}
}
