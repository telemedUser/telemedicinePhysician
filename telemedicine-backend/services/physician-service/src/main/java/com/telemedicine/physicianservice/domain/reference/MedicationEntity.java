package com.telemedicine.physicianservice.domain.reference;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/** A medication in the formulary for the prescription dropdown (table {@code medication}). */
@Entity
@Table(name = "medication")
public class MedicationEntity {

    @Id
    private String id;

    @Column(nullable = false)
    private String name;

    private String strength;

    private String form;

    @Column(nullable = false)
    private boolean active;

    protected MedicationEntity() {}

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getStrength() {
        return strength;
    }

    public String getForm() {
        return form;
    }

    public boolean isActive() {
        return active;
    }
}
