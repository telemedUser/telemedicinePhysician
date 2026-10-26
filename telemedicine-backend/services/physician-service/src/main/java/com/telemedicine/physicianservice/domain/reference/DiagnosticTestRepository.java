package com.telemedicine.physicianservice.domain.reference;

import java.util.List;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface DiagnosticTestRepository extends JpaRepository<DiagnosticTestEntity, String> {

    @Query(
            "SELECT t FROM DiagnosticTestEntity t WHERE t.active = true AND "
                + "LOWER(t.name) LIKE LOWER(CONCAT('%', :q, '%')) "
                + "ORDER BY CASE WHEN LOWER(t.name) LIKE LOWER(CONCAT(:q, '%')) THEN 0 ELSE 1 END, t.name")
    List<DiagnosticTestEntity> search(@Param("q") String q, Pageable pageable);

    List<DiagnosticTestEntity> findByActiveTrueOrderByName(Pageable pageable);
}
