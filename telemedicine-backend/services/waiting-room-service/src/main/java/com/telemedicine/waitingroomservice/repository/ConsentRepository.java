package com.telemedicine.waitingroomservice.repository;

import com.telemedicine.waitingroomservice.domain.ConsentEntity;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ConsentRepository extends JpaRepository<ConsentEntity, String> {

    @Query(
            """
            SELECT c FROM ConsentEntity c
            WHERE c.subjectId = :subjectId
              AND c.consentType = :consentType
              AND c.withdrawnAt IS NULL
            ORDER BY c.grantedAt DESC
            """)
    Optional<ConsentEntity> findActiveBySubjectAndType(
            @Param("subjectId") String subjectId, @Param("consentType") String consentType);

    List<ConsentEntity> findByAppointmentIdAndWithdrawnAtIsNull(String appointmentId);

    @Query(
            """
            SELECT c FROM ConsentEntity c
            WHERE c.subjectId = :subjectId
              AND c.consentType = :consentType
            ORDER BY c.grantedAt DESC
            """)
    List<ConsentEntity> findHistoryBySubjectAndType(
            @Param("subjectId") String subjectId, @Param("consentType") String consentType);
}
