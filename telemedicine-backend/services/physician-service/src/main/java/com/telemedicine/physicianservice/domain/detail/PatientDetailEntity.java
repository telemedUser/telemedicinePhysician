package com.telemedicine.physicianservice.domain.detail;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

/**
 * Patient-detail read model for the physician "review &amp; join" page (prototype D-03). Keyed by
 * appointment id and maintained from the {@code ConsultationContextPublished} event. It stores only
 * the fields the event carries — the physician service holds no clinical data of its own — with the
 * variable-length detail kept verbatim in {@code contextJson}.
 */
@Entity
@Table(name = "patient_detail")
public class PatientDetailEntity {

    @Id
    @Column(name = "appointment_id")
    private String appointmentId;

    @Column(nullable = false)
    private String market;

    @Column(name = "patient_display")
    private String patientDisplay;

    @Column(name = "age_years", nullable = false)
    private int ageYears;

    @Column(name = "sex_code")
    private String sexCode;

    private String city;

    @Column(name = "insurance_scheme")
    private String insuranceScheme;

    @Column(name = "national_id_masked")
    private String nationalIdMasked;

    private String modality;

    @Column(name = "visit_type")
    private String visitType;

    @Column(name = "scheduled_at")
    private Instant scheduledAt;

    @Column(name = "severity_score", nullable = false)
    private int severityScore;

    @Column(name = "symptom_duration_code")
    private String symptomDurationCode;

    @Column(name = "symptom_count", nullable = false)
    private int symptomCount;

    @Column(name = "attachment_count", nullable = false)
    private int attachmentCount;

    @Column(name = "red_flag", nullable = false)
    private boolean redFlag;

    @Column(name = "consultation_fee_minor", nullable = false)
    private long consultationFeeMinor;

    @Column(name = "voucher_minor", nullable = false)
    private long voucherMinor;

    @Column(name = "amount_paid_minor", nullable = false)
    private long amountPaidMinor;

    @Column(nullable = false)
    private String currency;

    @Column(name = "payment_status")
    private String paymentStatus;

    @Column(name = "context_json", columnDefinition = "text")
    private String contextJson;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected PatientDetailEntity() {}

    public PatientDetailEntity(String appointmentId, String market) {
        this.appointmentId = appointmentId;
        this.market = market != null ? market : "IN";
        this.currency = "INR";
        this.updatedAt = Instant.now();
    }

    public String getAppointmentId() {
        return appointmentId;
    }

    public String getMarket() {
        return market;
    }

    public void setMarket(String market) {
        this.market = market;
    }

    public String getPatientDisplay() {
        return patientDisplay;
    }

    public void setPatientDisplay(String patientDisplay) {
        this.patientDisplay = patientDisplay;
    }

    public int getAgeYears() {
        return ageYears;
    }

    public void setAgeYears(int ageYears) {
        this.ageYears = ageYears;
    }

    public String getSexCode() {
        return sexCode;
    }

    public void setSexCode(String sexCode) {
        this.sexCode = sexCode;
    }

    public String getCity() {
        return city;
    }

    public void setCity(String city) {
        this.city = city;
    }

    public String getInsuranceScheme() {
        return insuranceScheme;
    }

    public void setInsuranceScheme(String insuranceScheme) {
        this.insuranceScheme = insuranceScheme;
    }

    public String getNationalIdMasked() {
        return nationalIdMasked;
    }

    public void setNationalIdMasked(String nationalIdMasked) {
        this.nationalIdMasked = nationalIdMasked;
    }

    public String getModality() {
        return modality;
    }

    public void setModality(String modality) {
        this.modality = modality;
    }

    public String getVisitType() {
        return visitType;
    }

    public void setVisitType(String visitType) {
        this.visitType = visitType;
    }

    public Instant getScheduledAt() {
        return scheduledAt;
    }

    public void setScheduledAt(Instant scheduledAt) {
        this.scheduledAt = scheduledAt;
    }

    public int getSeverityScore() {
        return severityScore;
    }

    public void setSeverityScore(int severityScore) {
        this.severityScore = severityScore;
    }

    public String getSymptomDurationCode() {
        return symptomDurationCode;
    }

    public void setSymptomDurationCode(String symptomDurationCode) {
        this.symptomDurationCode = symptomDurationCode;
    }

    public int getSymptomCount() {
        return symptomCount;
    }

    public void setSymptomCount(int symptomCount) {
        this.symptomCount = symptomCount;
    }

    public int getAttachmentCount() {
        return attachmentCount;
    }

    public void setAttachmentCount(int attachmentCount) {
        this.attachmentCount = attachmentCount;
    }

    public boolean isRedFlag() {
        return redFlag;
    }

    public void setRedFlag(boolean redFlag) {
        this.redFlag = redFlag;
    }

    public long getConsultationFeeMinor() {
        return consultationFeeMinor;
    }

    public void setConsultationFeeMinor(long consultationFeeMinor) {
        this.consultationFeeMinor = consultationFeeMinor;
    }

    public long getVoucherMinor() {
        return voucherMinor;
    }

    public void setVoucherMinor(long voucherMinor) {
        this.voucherMinor = voucherMinor;
    }

    public long getAmountPaidMinor() {
        return amountPaidMinor;
    }

    public void setAmountPaidMinor(long amountPaidMinor) {
        this.amountPaidMinor = amountPaidMinor;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    public String getPaymentStatus() {
        return paymentStatus;
    }

    public void setPaymentStatus(String paymentStatus) {
        this.paymentStatus = paymentStatus;
    }

    public String getContextJson() {
        return contextJson;
    }

    public void setContextJson(String contextJson) {
        this.contextJson = contextJson;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }
}
