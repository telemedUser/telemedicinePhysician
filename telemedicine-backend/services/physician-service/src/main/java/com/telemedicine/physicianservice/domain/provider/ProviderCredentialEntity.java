package com.telemedicine.physicianservice.domain.provider;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.LocalDate;

/**
 * Provider credential (PRD §21, TM-PRV-006). The platform flags expiry so routing can block an
 * expired credential; it is not the credential-verification system of record (PRD §5 out of scope).
 */
@Entity
@Table(name = "provider_credential")
public class ProviderCredentialEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "provider_id", nullable = false)
    private ProviderEntity provider;

    @Column(nullable = false)
    private String type;

    private String number;

    @Column(name = "valid_from")
    private LocalDate validFrom;

    @Column(name = "valid_until")
    private LocalDate validUntil;

    @Column(nullable = false)
    private boolean verified;

    protected ProviderCredentialEntity() {}

    public ProviderCredentialEntity(
            ProviderEntity provider,
            String type,
            String number,
            LocalDate validFrom,
            LocalDate validUntil,
            boolean verified) {
        this.provider = provider;
        this.type = type;
        this.number = number;
        this.validFrom = validFrom;
        this.validUntil = validUntil;
        this.verified = verified;
    }

    public String getType() {
        return type;
    }

    public String getNumber() {
        return number;
    }

    public LocalDate getValidFrom() {
        return validFrom;
    }

    public LocalDate getValidUntil() {
        return validUntil;
    }

    public boolean isVerified() {
        return verified;
    }

    /** Credential is expired when its validity end date is in the past (TM-PRV-006). */
    public boolean isExpired() {
        return validUntil != null && validUntil.isBefore(LocalDate.now());
    }
}
