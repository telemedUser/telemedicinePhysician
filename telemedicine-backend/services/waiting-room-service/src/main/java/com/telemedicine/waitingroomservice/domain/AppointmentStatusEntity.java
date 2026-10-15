package com.telemedicine.waitingroomservice.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.Instant;

@Entity
@Table(name = "appointment_status")
public class AppointmentStatusEntity {

    @Id
    @Column(name = "appointment_id", length = 128, nullable = false)
    private String appointmentId;

    @Column(name = "patient_ref", length = 128)
    private String patientRef;

    @Column(length = 32, nullable = false)
    private String status = WaitingRoomStatus.OPEN_PATIENT.name();

    @Column(name = "identity_validated", nullable = false)
    private boolean identityValidated;

    @Column(name = "payment_confirmed", nullable = false)
    private boolean paymentConfirmed;

    @Column(name = "checked_in_at")
    private Instant checkedInAt;

    @Column(name = "open_doctor_at")
    private Instant openDoctorAt;

    @Column(name = "no_show_at")
    private Instant noShowAt;

    @Column(name = "timed_out_at")
    private Instant timedOutAt;

    @Version
    @Column(nullable = false)
    private long version;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public String getAppointmentId() {
        return appointmentId;
    }

    public void setAppointmentId(String appointmentId) {
        this.appointmentId = appointmentId;
    }

    public String getPatientRef() {
        return patientRef;
    }

    public void setPatientRef(String patientRef) {
        this.patientRef = patientRef;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public WaitingRoomStatus statusEnum() {
        return WaitingRoomStatus.fromWire(status);
    }

    public void setStatusEnum(WaitingRoomStatus status) {
        this.status = status.name();
    }

    public boolean isIdentityValidated() {
        return identityValidated;
    }

    public void setIdentityValidated(boolean identityValidated) {
        this.identityValidated = identityValidated;
    }

    public boolean isPaymentConfirmed() {
        return paymentConfirmed;
    }

    public void setPaymentConfirmed(boolean paymentConfirmed) {
        this.paymentConfirmed = paymentConfirmed;
    }

    public Instant getCheckedInAt() {
        return checkedInAt;
    }

    public void setCheckedInAt(Instant checkedInAt) {
        this.checkedInAt = checkedInAt;
    }

    public Instant getOpenDoctorAt() {
        return openDoctorAt;
    }

    public void setOpenDoctorAt(Instant openDoctorAt) {
        this.openDoctorAt = openDoctorAt;
    }

    public Instant getNoShowAt() {
        return noShowAt;
    }

    public void setNoShowAt(Instant noShowAt) {
        this.noShowAt = noShowAt;
    }

    public Instant getTimedOutAt() {
        return timedOutAt;
    }

    public void setTimedOutAt(Instant timedOutAt) {
        this.timedOutAt = timedOutAt;
    }

    public long getVersion() {
        return version;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }
}
