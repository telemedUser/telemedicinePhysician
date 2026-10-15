package com.telemedicine.integrationservice.domain;

import java.util.List;
import java.util.Optional;

/** Persistence port — keep when removing mock; add Jpa*Repository @Profile("postgres"). */
public interface AppointmentRepository {
    Optional<Appointment> findById(String id);

    List<Appointment> findAll();

    Appointment save(Appointment appointment);
}
