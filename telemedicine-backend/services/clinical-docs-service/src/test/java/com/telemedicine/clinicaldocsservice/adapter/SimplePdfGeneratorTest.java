package com.telemedicine.clinicaldocsservice.adapter;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class SimplePdfGeneratorTest {

    @Test
    void generatesPdfBytes() {
        byte[] pdf = SimplePdfGenerator.generate("Consultation Summary", "Patient recovered");

        assertThat(pdf).isNotEmpty();
        assertThat(new String(pdf)).startsWith("%PDF-1.4");
    }
}
