package com.telemedicine.waitingroomservice.repository;

import com.telemedicine.waitingroomservice.domain.QueueEntryEntity;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface QueueEntryRepository extends JpaRepository<QueueEntryEntity, String> {

    Optional<QueueEntryEntity> findByAppointmentId(String appointmentId);

    List<QueueEntryEntity> findAllByOrderByEnteredAtAsc();
}
