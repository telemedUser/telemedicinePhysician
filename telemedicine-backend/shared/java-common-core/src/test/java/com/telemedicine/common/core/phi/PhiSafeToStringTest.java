package com.telemedicine.common.core.phi;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class PhiSafeToStringTest {

    @Test
    @DisplayName("annotated members are masked by their declared kind")
    void masksAnnotatedFields() {
        String rendered = PhiSafeToString.of(new Patient("APT-1", "12.345.678-5", "Maria Soto"));

        assertThat(rendered)
                .startsWith("Patient{")
                .contains("appointmentId=APT-1")
                .contains("rut=[REDACTED]")
                .contains("name=M. S.")
                .doesNotContain("12.345.678")
                .doesNotContain("Maria");
    }

    @Test
    @DisplayName("unannotated free text still passes through pattern redaction")
    void redactsUnannotatedFieldsAsWell() {
        String rendered = PhiSafeToString.of(new Note("contact maria@bupa.cl"));

        assertThat(rendered).contains("m***@bupa.cl").doesNotContain("maria@bupa.cl");
    }

    @Test
    void walksTheSuperclassChain() {
        String rendered = PhiSafeToString.of(new Physician("DOC-7", "Ana Diaz"));

        assertThat(rendered).contains("licenceId=DOC-7").contains("name=A. D.");
    }

    @Test
    void handlesNullTargetAndNullFields() {
        assertThat(PhiSafeToString.of(null)).isEqualTo("null");
        assertThat(PhiSafeToString.render(new Patient("APT-2", null, null)))
                .contains("rut=null")
                .contains("name=null");
    }

    @Test
    void staticAndSyntheticMembersAreSkipped() {
        assertThat(PhiSafeToString.of(new Patient("APT-3", "x", "y"))).doesNotContain("CONSTANT");
    }

    @SuppressWarnings("unused")
    private static class Person {
        @Phi(PhiKind.NAME)
        private final String name;

        Person(String name) {
            this.name = name;
        }
    }

    @SuppressWarnings("unused")
    private static final class Physician extends Person {
        private final String licenceId;

        Physician(String licenceId, String name) {
            super(name);
            this.licenceId = licenceId;
        }
    }

    @SuppressWarnings("unused")
    private static final class Patient {
        static final String CONSTANT = "not-rendered";

        private final String appointmentId;

        @Phi(PhiKind.RUT)
        private final String rut;

        @Phi(PhiKind.NAME)
        private final String name;

        Patient(String appointmentId, String rut, String name) {
            this.appointmentId = appointmentId;
            this.rut = rut;
            this.name = name;
        }
    }

    @SuppressWarnings("unused")
    private static final class Note {
        private final String text;

        Note(String text) {
            this.text = text;
        }
    }
}
