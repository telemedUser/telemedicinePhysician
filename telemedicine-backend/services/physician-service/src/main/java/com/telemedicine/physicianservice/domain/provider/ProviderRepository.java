package com.telemedicine.physicianservice.domain.provider;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ProviderRepository extends JpaRepository<ProviderEntity, String> {

    Optional<ProviderEntity> findByIdAndMarket(String id, String market);

    List<ProviderEntity> findByMarketOrderByDisplayNameAsc(String market);

    /**
     * Directory search filtered by optional status, specialty, mode and language, all market-scoped
     * (TM-DIR-002, TM-MKT-002). Null filters are ignored.
     */
    /**
     * Directory search filtered by optional status, specialty, mode and language, all market-scoped
     * (TM-DIR-002, TM-MKT-002). The caller passes {@code specialtyLike} already lowercased and
     * wrapped in {@code %...%} (or null to skip); comparing a column's {@code lower(...)} to a bound
     * string avoids applying {@code lower()} to a NULL bind parameter, which PostgreSQL rejects as
     * {@code lower(bytea)}.
     */
    @Query(
            """
            select distinct p from ProviderEntity p
              left join p.specialties s
              left join p.modes m
              left join p.languages l
            where p.market = :market
              and (:status is null or p.status = :status)
              and (:specialtyLike is null or lower(s.specialty) like :specialtyLike)
              and (:mode is null or m.mode = :mode)
              and (:language is null or l.language = :language)
            order by p.displayName asc
            """)
    List<ProviderEntity> search(
            @Param("market") String market,
            @Param("status") String status,
            @Param("specialtyLike") String specialtyLike,
            @Param("mode") String mode,
            @Param("language") String language);
}
