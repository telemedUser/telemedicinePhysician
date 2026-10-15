package com.telemedicine.integrationservice.infrastructure.mock;

import com.telemedicine.common.mockdata.AbstractJsonRepository;
import com.telemedicine.common.mockdata.MockDatastoreFactory;
import com.telemedicine.integrationservice.domain.Appointment;
import com.telemedicine.integrationservice.domain.AppointmentRepository;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Mock adapter — requests data via {@link com.telemedicine.common.mockdata.MockDatastoreConnection}.
 * Delete with {@code infrastructure/mock} when Postgres is ready.
 */
@Profile("mock")
@Repository
public class JsonAppointmentRepository extends AbstractJsonRepository<Appointment> implements AppointmentRepository {

    public JsonAppointmentRepository(MockDatastoreFactory datastore) {
        super(datastore, "integration-service", "appointments.json", Appointment.class, Appointment::id);
    }

    @Override
    public Optional<Appointment> findById(String id) {
        return super.findById(id);
    }

    @Override
    public List<Appointment> findAll() {
        return super.findAll();
    }

    @Override
    public Appointment save(Appointment appointment) {
        return super.save(appointment);
    }
}
