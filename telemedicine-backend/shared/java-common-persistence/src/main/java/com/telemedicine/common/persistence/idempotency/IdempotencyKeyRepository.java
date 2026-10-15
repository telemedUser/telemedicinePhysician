package com.telemedicine.common.persistence.idempotency;

import java.time.Instant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface IdempotencyKeyRepository extends JpaRepository<IdempotencyKeyEntity, String> {

    @Modifying
    @Query("DELETE FROM IdempotencyKeyEntity e WHERE e.expiresAt < :cutoff")
    int deleteExpired(@Param("cutoff") Instant cutoff);
}
