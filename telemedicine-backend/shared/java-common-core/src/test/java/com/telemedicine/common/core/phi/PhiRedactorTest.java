package com.telemedicine.common.core.phi;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;

class PhiRedactorTest {

    @Nested
    @DisplayName("identifiers are removed")
    class Positive {

        @ParameterizedTest
        @ValueSource(
                strings = {
                    "Patient 12.345.678-5 checked in",
                    "rut=12345678-5",
                    "RUT 9.876.543-K arrived",
                    "identifier 1.234.567-8 verified"
                })
        void redactsChileanRuts(String input) {
            assertThat(PhiRedactor.redact(input)).contains("[RUT[REDACTED]]").doesNotContain("-5");
        }

        @Test
        void masksEmailToInitialPlusDomain() {
            assertThat(PhiRedactor.redact("contact maria.soto@bupa.cl about it"))
                    .isEqualTo("contact m***@bupa.cl about it");
        }

        @Test
        void masksE164PhoneNumbers() {
            assertThat(PhiRedactor.redact("sms sent to +56912345678"))
                    .isEqualTo("sms sent to +56*******78");
        }

        @Test
        void removesBearerTokensEntirely() {
            String jwt =
                    "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9"
                            + ".eyJzdWIiOiIxMjM0NTY3ODkwIiwibmFtZSI6IkpvaG4ifQ"
                            + ".dozjgNryP4J3jVmNHl0w5N_XgL0n3I9PlFUP0THsR8U";

            assertThat(PhiRedactor.redact("Authorization: Bearer " + jwt))
                    .isEqualTo("Authorization: Bearer [JWT[REDACTED]]");
        }

        @Test
        void removesLuhnValidCardNumbers() {
            assertThat(PhiRedactor.redact("card 4111111111111111 charged"))
                    .isEqualTo("card [PAN[REDACTED]] charged");
        }

        @Test
        void redactsEveryIdentifierInOnePass() {
            String input = "12.345.678-5 / maria@bupa.cl / +56912345678";

            String redacted = PhiRedactor.redact(input);

            assertThat(redacted)
                    .contains("[RUT[REDACTED]]")
                    .contains("m***@bupa.cl")
                    .contains("+56*******78")
                    .doesNotContain("maria@")
                    .doesNotContain("12.345.678");
        }
    }

    @Nested
    @DisplayName("clinical detail survives")
    class NegativeControls {

        /**
         * Over-redaction is the failure mode that makes engineers turn redaction off. Clinical text
         * is dense with numbers, and none of these may be touched.
         */
        @ParameterizedTest
        @ValueSource(
                strings = {
                    "BP 120/80 mmHg, HR 72, SpO2 98%",
                    "Paracetamol 500 mg every 8 hours for 5 days",
                    "ICD-10 J06.9 acute upper respiratory infection",
                    "Appointment APT-2025-0001 rescheduled to 2025-03-14",
                    "Consultation lasted 1800 seconds",
                    "Waiting room position 3 of 12",
                    "Version 1.0.0 of the clinical consent text",
                    "Measurement id 1700000000000 recorded"
                })
        void leavesClinicalFreeTextUntouched(String input) {
            assertThat(PhiRedactor.redact(input)).isEqualTo(input);
        }

        @Test
        @DisplayName("a long digit run that fails the Luhn check is not a card number")
        void doesNotRedactNonLuhnDigitRuns() {
            assertThat(PhiRedactor.redact("reference 4111111111111112 stored"))
                    .isEqualTo("reference 4111111111111112 stored");
        }

        @Test
        void handlesNullAndEmpty() {
            assertThat(PhiRedactor.redact(null)).isNull();
            assertThat(PhiRedactor.redact("")).isEmpty();
        }

        @Test
        void returnsTheSameTextWhenThereIsNothingToRedact() {
            String input = "queue advanced";

            assertThat(PhiRedactor.redact(input)).isSameAs(input);
        }
    }

    @Nested
    class Masking {

        @ParameterizedTest
        @EnumSource(PhiKind.class)
        void everyKindHasARule(PhiKind kind) {
            assertThat(PhiRedactor.mask("Maria Soto Perez", kind)).isNotNull().isNotEmpty();
        }

        @Test
        void strongIdentifiersAreRemovedOutright() {
            assertThat(PhiRedactor.mask("12.345.678-5", PhiKind.RUT)).isEqualTo("[REDACTED]");
            assertThat(PhiRedactor.mask("Av. Providencia 123", PhiKind.ADDRESS))
                    .isEqualTo("[REDACTED]");
            assertThat(PhiRedactor.mask("MRN-99", PhiKind.MRN)).isEqualTo("[REDACTED]");
            assertThat(PhiRedactor.mask("4111111111111111", PhiKind.PAN)).isEqualTo("[REDACTED]");
            assertThat(PhiRedactor.mask("s3cret", PhiKind.SECRET)).isEqualTo("[REDACTED]");
            assertThat(PhiRedactor.mask("Reports chest pain", PhiKind.CLINICAL_NOTE))
                    .isEqualTo("[REDACTED]");
            assertThat(PhiRedactor.maskRut("12.345.678-5")).isEqualTo("[REDACTED]");
            assertThat(PhiRedactor.maskRut(null)).isNull();
        }

        @Test
        void contactDetailsKeepASupportableFragment() {
            assertThat(PhiRedactor.mask("maria.soto@bupa.cl", PhiKind.EMAIL))
                    .isEqualTo("m***@bupa.cl");
            assertThat(PhiRedactor.mask("+56912345678", PhiKind.PHONE)).isEqualTo("+56*******78");
        }

        @ParameterizedTest
        @CsvSource({"Maria Fernanda Soto, 'M. F. S.'", "Soto, 'S.'", "'  Ana  Diaz  ', 'A. D.'"})
        void namesReduceToInitials(String input, String expected) {
            assertThat(PhiRedactor.maskName(input)).isEqualTo(expected);
        }

        @Test
        void maskingHandlesDegenerateInput() {
            assertThat(PhiRedactor.mask(null, PhiKind.EMAIL)).isNull();
            assertThat(PhiRedactor.maskGeneric(null)).isEqualTo("****");
            assertThat(PhiRedactor.maskGeneric("ab")).isEqualTo("****");
            assertThat(PhiRedactor.maskGeneric("abcdef")).isEqualTo("ab****ef");
            assertThat(PhiRedactor.maskEmail(null)).isNull();
            assertThat(PhiRedactor.maskEmail("not-an-email")).isEqualTo("no****il");
            assertThat(PhiRedactor.maskEmail("@nolocal.cl")).isEqualTo("@n****cl");
            assertThat(PhiRedactor.maskEmail("trailing@")).isEqualTo("tr****g@");
            assertThat(PhiRedactor.maskPhone(null)).isNull();
            assertThat(PhiRedactor.maskPhone("12")).isEqualTo("****");
            assertThat(PhiRedactor.maskPhone("0229876543")).isEqualTo("********43");
            assertThat(PhiRedactor.maskName(null)).isEqualTo("****");
            assertThat(PhiRedactor.maskName("   ")).isEqualTo("****");
        }
    }

    @Nested
    class Detection {

        @Test
        void containsPhiFlagsOnlyRealIdentifiers() {
            assertThat(PhiRedactor.containsPhi("patient 12.345.678-5")).isTrue();
            assertThat(PhiRedactor.containsPhi("BP 120/80 mmHg")).isFalse();
            assertThat(PhiRedactor.containsPhi(null)).isFalse();
            assertThat(PhiRedactor.containsPhi("")).isFalse();
        }

        @ParameterizedTest
        @CsvSource({
            "patientRut, RUT",
            "nationalId, RUT",
            "contactEmail, EMAIL",
            "mobilePhone, PHONE",
            "msisdn, PHONE",
            "fullName, NAME",
            "homeAddress, ADDRESS",
            "accessToken, SECRET",
            "passwordHash, SECRET",
            "appointmentId, GENERIC"
        })
        void inferrsKindFromFieldName(String fieldName, PhiKind expected) {
            assertThat(PhiRedactor.kindForFieldName(fieldName)).isEqualTo(expected);
        }

        @Test
        void inferenceDefaultsSafelyForNull() {
            assertThat(PhiRedactor.kindForFieldName(null)).isEqualTo(PhiKind.GENERIC);
        }

        @Test
        void luhnRejectsShortAndLongRuns() {
            assertThat(PhiRedactor.isLuhnValid("4111111111111111")).isTrue();
            assertThat(PhiRedactor.isLuhnValid("411111111111")).isFalse();
            assertThat(PhiRedactor.isLuhnValid("41111111111111111111")).isFalse();
        }
    }
}
