package com.telemedicine.physicianservice.domain.worklist;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface WorklistItemRepository extends JpaRepository<WorklistItemEntity, String> {

    /** Worklist for a physician, scoped to market (TM-MKT-002), ordered oldest-waiting first. */
    List<WorklistItemEntity> findByProviderIdAndMarketOrderByEnteredAtAsc(
            String providerId, String market);
}
