package com.telemedicine.physicianservice.domain.reference;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/** An ICD-10 diagnosis code for the care-plan diagnosis dropdown (table {@code diagnosis_code}). */
@Entity
@Table(name = "diagnosis_code")
public class DiagnosisCodeEntity {

    @Id
    private String code;

    @Column(nullable = false)
    private String title;

    @Column(nullable = false)
    private String system;

    @Column(nullable = false)
    private boolean active;

    protected DiagnosisCodeEntity() {}

    public String getCode() {
        return code;
    }

    public String getTitle() {
        return title;
    }

    public String getSystem() {
        return system;
    }

    public boolean isActive() {
        return active;
    }
}
