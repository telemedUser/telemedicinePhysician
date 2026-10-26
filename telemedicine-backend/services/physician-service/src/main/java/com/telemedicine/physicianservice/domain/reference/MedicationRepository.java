package com.telemedicine.physicianservice.domain.reference;

import java.util.List;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface MedicationRepository extends JpaRepository<MedicationEntity, String> {

    @Query(
            "SELECT m FROM MedicationEntity m WHERE m.active = true AND "
                + "LOWER(m.name) LIKE LOWER(CONCAT('%', :q, '%')) "
                + "ORDER BY CASE WHEN LOWER(m.name) LIKE LOWER(CONCAT(:q, '%')) THEN 0 ELSE 1 END, m.name")
    List<MedicationEntity> search(@Param("q") String q, Pageable pageable);

    List<MedicationEntity> findByActiveTrueOrderByName(Pageable pageable);
}
