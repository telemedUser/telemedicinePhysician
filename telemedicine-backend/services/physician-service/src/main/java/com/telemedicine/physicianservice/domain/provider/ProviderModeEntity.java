package com.telemedicine.physicianservice.domain.provider;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

/** A consultation mode a provider supports: video | audio | chat. */
@Entity
@Table(name = "provider_mode")
public class ProviderModeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "provider_id", nullable = false)
    private ProviderEntity provider;

    @Column(nullable = false)
    private String mode;

    protected ProviderModeEntity() {}

    public ProviderModeEntity(ProviderEntity provider, String mode) {
        this.provider = provider;
        this.mode = mode;
    }

    public String getMode() {
        return mode;
    }
}
