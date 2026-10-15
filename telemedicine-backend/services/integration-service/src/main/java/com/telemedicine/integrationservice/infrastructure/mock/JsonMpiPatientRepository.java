package com.telemedicine.integrationservice.infrastructure.mock;

import com.telemedicine.common.mockdata.AbstractJsonRepository;
import com.telemedicine.common.mockdata.MockDatastoreFactory;
import com.telemedicine.integrationservice.domain.MpiPatient;
import com.telemedicine.integrationservice.domain.MpiPatientRepository;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/** Delete with {@code infrastructure/mock} when switching to Postgres. */
@Profile("mock")
@Repository
public class JsonMpiPatientRepository extends AbstractJsonRepository<MpiPatient> implements MpiPatientRepository {

    public JsonMpiPatientRepository(MockDatastoreFactory datastore) {
        super(datastore, "integration-service", "mpi-patients.json", MpiPatient.class, MpiPatient::appointmentId);
    }

    @Override
    public Optional<MpiPatient> findByAppointmentId(String appointmentId) {
        return connection.query(p -> appointmentId.equals(p.appointmentId())).stream().findFirst();
    }
}
