package com.telemedicine.physicianservice.domain.reference;

import java.util.List;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface DiagnosisCodeRepository extends JpaRepository<DiagnosisCodeEntity, String> {

    /** Case-insensitive search on code OR title; active only; code-prefix matches rank first. */
    @Query(
            "SELECT d FROM DiagnosisCodeEntity d WHERE d.active = true AND ("
                + "LOWER(d.code) LIKE LOWER(CONCAT('%', :q, '%')) OR "
                + "LOWER(d.title) LIKE LOWER(CONCAT('%', :q, '%'))) "
                + "ORDER BY CASE WHEN LOWER(d.code) LIKE LOWER(CONCAT(:q, '%')) THEN 0 ELSE 1 END, d.code")
    List<DiagnosisCodeEntity> search(@Param("q") String q, Pageable pageable);

    List<DiagnosisCodeEntity> findByActiveTrueOrderByCode(Pageable pageable);
}
