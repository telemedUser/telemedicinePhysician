package com.telemedicine.integrationservice.infrastructure.persistence;

import com.telemedicine.integrationservice.domain.Appointment;
import com.telemedicine.integrationservice.domain.AppointmentRepository;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Repository;

@Profile("postgres")
@Repository
public class JpaAppointmentRepository implements AppointmentRepository {

    private final AppointmentJpaRepository jpaRepository;

    public JpaAppointmentRepository(AppointmentJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public Optional<Appointment> findById(String id) {
        return jpaRepository.findById(id).map(AppointmentMapper::toDomain);
    }

    @Override
    public List<Appointment> findAll() {
        return jpaRepository.findAll().stream().map(AppointmentMapper::toDomain).toList();
    }

    @Override
    public Appointment save(Appointment appointment) {
        Instant now = Instant.now();
        AppointmentEntity entity =
                jpaRepository
                        .findById(appointment.id())
                        .map(existing -> AppointmentMapper.merge(existing, appointment, now))
                        .orElseGet(() -> AppointmentMapper.toNewEntity(appointment, now));
        return AppointmentMapper.toDomain(jpaRepository.save(entity));
    }
}
