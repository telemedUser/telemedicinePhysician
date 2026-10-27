package com.telemedicine.physicianservice.domain.worklist;

import java.time.Instant;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface WorklistItemRepository extends JpaRepository<WorklistItemEntity, String> {

    /** Worklist for a physician, scoped to market (TM-MKT-002), ordered oldest-waiting first. */
    List<WorklistItemEntity> findByProviderIdAndMarketOrderByEnteredAtAsc(
            String providerId, String market);

    /**
     * Atomically stamp the waiting-room arrival on an existing row. A direct UPDATE avoids the
     * read-modify-write lost-update race between the PatientEnteredWaitingRoom handler and the
     * AppointmentCreated / DeviceTestCompleted handlers, which run on independent consumer threads
     * (different topics) and were otherwise clobbering each other's fields via @Version optimistic
     * locking. Readiness is only advanced to IN_PROGRESS when it is not already READY. Returns the
     * number of rows updated (0 when the row does not exist yet).
     */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(
            "update WorklistItemEntity w set w.enteredAt = :enteredAt, "
                + "w.patientRef = coalesce(:patientRef, w.patientRef), "
                + "w.readiness = case when w.readiness = 'READY' then w.readiness else 'IN_PROGRESS' end, "
                + "w.updatedAt = :now where w.appointmentId = :appointmentId")
    int markEntered(
            @Param("appointmentId") String appointmentId,
            @Param("enteredAt") Instant enteredAt,
            @Param("patientRef") String patientRef,
            @Param("now") Instant now);

    /**
     * Atomically set the device-check outcome without touching enteredAt — so it cannot clobber the
     * arrival stamp written by the concurrent PatientEnteredWaitingRoom handler (same row, other
     * consumer thread). Returns rows updated (0 when the row does not exist yet).
     */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(
            "update WorklistItemEntity w set w.readiness = :readiness, "
                + "w.technicalStatus = :technicalStatus, w.updatedAt = :now "
                + "where w.appointmentId = :appointmentId")
    int markDeviceTest(
            @Param("appointmentId") String appointmentId,
            @Param("readiness") String readiness,
            @Param("technicalStatus") String technicalStatus,
            @Param("now") Instant now);

    /** Atomically set the consultation status without touching other fields. Returns rows updated. */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(
            "update WorklistItemEntity w set w.consultationStatus = :status, w.updatedAt = :now "
                + "where w.appointmentId = :appointmentId")
    int markStatus(
            @Param("appointmentId") String appointmentId,
            @Param("status") String status,
            @Param("now") Instant now);
}
