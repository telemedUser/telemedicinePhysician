package com.telemedicine.physicianservice.domain.reference;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/** An orderable diagnostic test for the care-plan test dropdown (table {@code diagnostic_test}). */
@Entity
@Table(name = "diagnostic_test")
public class DiagnosticTestEntity {

    @Id
    private String id;

    @Column(nullable = false)
    private String name;

    private String category;

    private String specimen;

    @Column(nullable = false)
    private boolean active;

    protected DiagnosticTestEntity() {}

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getCategory() {
        return category;
    }

    public String getSpecimen() {
        return specimen;
    }

    public boolean isActive() {
        return active;
    }
}
