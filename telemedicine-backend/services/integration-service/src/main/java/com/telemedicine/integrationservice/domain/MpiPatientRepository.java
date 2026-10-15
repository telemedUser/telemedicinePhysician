package com.telemedicine.integrationservice.domain;

import java.util.Optional;

/** Persistence port — keep when removing mock; add Jpa*Repository @Profile("postgres"). */
public interface MpiPatientRepository {
    Optional<MpiPatient> findByAppointmentId(String appointmentId);
}
