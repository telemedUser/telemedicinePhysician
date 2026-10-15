package com.telemedicine.integrationservice;

import com.telemedicine.integrationservice.domain.AppointmentStatus;
import com.telemedicine.integrationservice.service.AppointmentService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@ActiveProfiles("mock")
class AppointmentServiceMockTest {

    @Autowired
    private AppointmentService appointmentService;

    @Test
    void loadsSeedAppointment() {
        var appointment = appointmentService.consult("apt-001");
        assertEquals("apt-001", appointment.id());
        assertEquals(AppointmentStatus.SCHEDULED, appointment.status());
        assertEquals("CONFIRMED", appointment.paymentStatus());
    }

    @Test
    void createsAndPersistsInOverlay() {
        var created = appointmentService.create(java.util.Map.of("patientId", "pat-new"));
        assertTrue(created.id().startsWith("apt-"));
        assertEquals(AppointmentStatus.CREATED, created.status());
        assertEquals(created.id(), appointmentService.consult(created.id()).id());
    }

    @Test
    void mpiLookupForSeedAppointment() {
        var patient = appointmentService.mpiLookup("apt-demo");
        assertEquals("pat-demo", patient.patientId());
        assertEquals("Demo Patient", patient.name());
    }
}
