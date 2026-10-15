package com.telemedicine.physicianservice.domain.provider;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "provider_specialty")
public class ProviderSpecialtyEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "provider_id", nullable = false)
    private ProviderEntity provider;

    @Column(nullable = false)
    private String specialty;

    @Column(name = "sub_specialty")
    private String subSpecialty;

    protected ProviderSpecialtyEntity() {}

    public ProviderSpecialtyEntity(ProviderEntity provider, String specialty, String subSpecialty) {
        this.provider = provider;
        this.specialty = specialty;
        this.subSpecialty = subSpecialty;
    }

    public String getSpecialty() {
        return specialty;
    }

    public String getSubSpecialty() {
        return subSpecialty;
    }
}
