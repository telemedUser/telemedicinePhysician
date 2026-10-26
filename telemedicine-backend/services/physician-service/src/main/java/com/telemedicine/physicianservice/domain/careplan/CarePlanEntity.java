package com.telemedicine.physicianservice.domain.careplan;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

/**
 * The physician's care plan for one appointment (D-08). The clinical note is four text sections;
 * the lists (diagnoses, prescriptions, diagnostic tests) are stored as JSON text so the shape can
 * evolve. This is the physician-side system of record for the plan later surfaced to the patient.
 */
@Entity
@Table(name = "care_plan")
public class CarePlanEntity {

    @Id
    @Column(name = "appointment_id")
    private String appointmentId;

    @Column(name = "provider_id")
    private String providerId;

    @Column(nullable = false)
    private String market;

    @Column(name = "note_history", columnDefinition = "text")
    private String noteHistory;

    @Column(name = "note_findings", columnDefinition = "text")
    private String noteFindings;

    @Column(name = "note_assessment", columnDefinition = "text")
    private String noteAssessment;

    @Column(name = "note_plan", columnDefinition = "text")
    private String notePlan;

    @Column(columnDefinition = "text")
    private String diagnoses;

    @Column(columnDefinition = "text")
    private String prescriptions;

    @Column(name = "diagnostic_tests", columnDefinition = "text")
    private String diagnosticTests;

    @Column(name = "follow_up_enabled", nullable = false)
    private boolean followUpEnabled;

    @Column(name = "follow_up_mode")
    private String followUpMode;

    @Column(name = "follow_up_in_days")
    private Integer followUpInDays;

    @Column(name = "follow_up_date")
    private String followUpDate;

    @Column(name = "follow_up_instructions", columnDefinition = "text")
    private String followUpInstructions;

    @Column(name = "leave_enabled", nullable = false)
    private boolean leaveEnabled;

    @Column(name = "leave_reason")
    private String leaveReason;

    @Column(name = "leave_start_date")
    private String leaveStartDate;

    @Column(name = "leave_end_date")
    private String leaveEndDate;

    @Column(name = "summary_text", columnDefinition = "text")
    private String summaryText;

    @Column(name = "summary_instructions", columnDefinition = "text")
    private String summaryInstructions;

    @Column(nullable = false)
    private String status;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected CarePlanEntity() {}

    public CarePlanEntity(String appointmentId, String market) {
        this.appointmentId = appointmentId;
        this.market = market != null ? market : "IN";
        this.status = "DRAFT";
        this.updatedAt = Instant.now();
    }

    public String getAppointmentId() {
        return appointmentId;
    }

    public String getProviderId() {
        return providerId;
    }

    public void setProviderId(String providerId) {
        this.providerId = providerId;
    }

    public String getMarket() {
        return market;
    }

    public void setMarket(String market) {
        this.market = market;
    }

    public String getNoteHistory() {
        return noteHistory;
    }

    public void setNoteHistory(String noteHistory) {
        this.noteHistory = noteHistory;
    }

    public String getNoteFindings() {
        return noteFindings;
    }

    public void setNoteFindings(String noteFindings) {
        this.noteFindings = noteFindings;
    }

    public String getNoteAssessment() {
        return noteAssessment;
    }

    public void setNoteAssessment(String noteAssessment) {
        this.noteAssessment = noteAssessment;
    }

    public String getNotePlan() {
        return notePlan;
    }

    public void setNotePlan(String notePlan) {
        this.notePlan = notePlan;
    }

    public String getDiagnoses() {
        return diagnoses;
    }

    public void setDiagnoses(String diagnoses) {
        this.diagnoses = diagnoses;
    }

    public String getPrescriptions() {
        return prescriptions;
    }

    public void setPrescriptions(String prescriptions) {
        this.prescriptions = prescriptions;
    }

    public String getDiagnosticTests() {
        return diagnosticTests;
    }

    public void setDiagnosticTests(String diagnosticTests) {
        this.diagnosticTests = diagnosticTests;
    }

    public boolean isFollowUpEnabled() {
        return followUpEnabled;
    }

    public void setFollowUpEnabled(boolean followUpEnabled) {
        this.followUpEnabled = followUpEnabled;
    }

    public String getFollowUpMode() {
        return followUpMode;
    }

    public void setFollowUpMode(String followUpMode) {
        this.followUpMode = followUpMode;
    }

    public Integer getFollowUpInDays() {
        return followUpInDays;
    }

    public void setFollowUpInDays(Integer followUpInDays) {
        this.followUpInDays = followUpInDays;
    }

    public String getFollowUpDate() {
        return followUpDate;
    }

    public void setFollowUpDate(String followUpDate) {
        this.followUpDate = followUpDate;
    }

    public String getFollowUpInstructions() {
        return followUpInstructions;
    }

    public void setFollowUpInstructions(String followUpInstructions) {
        this.followUpInstructions = followUpInstructions;
    }

    public boolean isLeaveEnabled() {
        return leaveEnabled;
    }

    public void setLeaveEnabled(boolean leaveEnabled) {
        this.leaveEnabled = leaveEnabled;
    }

    public String getLeaveReason() {
        return leaveReason;
    }

    public void setLeaveReason(String leaveReason) {
        this.leaveReason = leaveReason;
    }

    public String getLeaveStartDate() {
        return leaveStartDate;
    }

    public void setLeaveStartDate(String leaveStartDate) {
        this.leaveStartDate = leaveStartDate;
    }

    public String getLeaveEndDate() {
        return leaveEndDate;
    }

    public void setLeaveEndDate(String leaveEndDate) {
        this.leaveEndDate = leaveEndDate;
    }

    public String getSummaryText() {
        return summaryText;
    }

    public void setSummaryText(String summaryText) {
        this.summaryText = summaryText;
    }

    public String getSummaryInstructions() {
        return summaryInstructions;
    }

    public void setSummaryInstructions(String summaryInstructions) {
        this.summaryInstructions = summaryInstructions;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }
}
