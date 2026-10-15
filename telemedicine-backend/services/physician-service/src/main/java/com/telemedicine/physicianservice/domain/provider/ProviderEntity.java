package com.telemedicine.physicianservice.domain.provider;

import com.telemedicine.common.persistence.BaseEntity;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import java.util.ArrayList;
import java.util.List;

/**
 * Provider / physician record (PRD §21 Provider & Physician Management, TM-PRV-001..008).
 *
 * <p>Authoritative provider data lives in the HIS and is reached only through the integration
 * service (TM-INT-001); this row is a market-scoped projection plus the platform-owned operational
 * overlay (status, availability, capacity). It carries a {@code sourceRef} and {@code
 * lastRefreshedAt} so a consumer never treats a stale projection as authoritative (TM-ARC-020).
 */
@Entity
@Table(name = "provider")
public class ProviderEntity extends BaseEntity {

    @Column(nullable = false)
    private String market;

    @Column(name = "display_name", nullable = false)
    private String displayName;

    /** ACTIVE | SUSPENDED | ON_LEAVE | OFFBOARDED (see {@link ProviderStatus}). */
    @Column(nullable = false)
    private String status;

    /** Routing attributes (years of experience, rating, education) as a JSON string. */
    @Column(name = "routing_attributes", columnDefinition = "text")
    private String routingAttributes;

    @Column(name = "source_ref")
    private String sourceRef;

    @Column(name = "last_refreshed_at")
    private java.time.Instant lastRefreshedAt;

    @Column(name = "staleness_seconds", nullable = false)
    private int stalenessSeconds = 900;

    // LAZY (not EAGER): a provider has five child collections, and eagerly fetching more than one
    // List at once triggers Hibernate's MultipleBagFetchException. They are read inside the
    // read-only transaction in ProviderService, so lazy initialisation resolves fine.
    @OneToMany(mappedBy = "provider", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<ProviderSpecialtyEntity> specialties = new ArrayList<>();

    @OneToMany(mappedBy = "provider", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<ProviderModeEntity> modes = new ArrayList<>();

    @OneToMany(mappedBy = "provider", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<ProviderLanguageEntity> languages = new ArrayList<>();

    @OneToMany(mappedBy = "provider", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<ProviderCredentialEntity> credentials = new ArrayList<>();

    @OneToMany(mappedBy = "provider", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<AvailabilityEntity> availability = new ArrayList<>();

    protected ProviderEntity() {}

    public ProviderEntity(String providerId, String market, String displayName, ProviderStatus status) {
        setId(providerId);
        this.market = market;
        this.displayName = displayName;
        this.status = status.name();
    }

    public String getMarket() {
        return market;
    }

    public void setMarket(String market) {
        this.market = market;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public ProviderStatus getStatus() {
        return ProviderStatus.valueOf(status);
    }

    public void setStatus(ProviderStatus status) {
        this.status = status.name();
    }

    public String getRoutingAttributes() {
        return routingAttributes;
    }

    public void setRoutingAttributes(String routingAttributes) {
        this.routingAttributes = routingAttributes;
    }

    public String getSourceRef() {
        return sourceRef;
    }

    public void setSourceRef(String sourceRef) {
        this.sourceRef = sourceRef;
    }

    public java.time.Instant getLastRefreshedAt() {
        return lastRefreshedAt;
    }

    public void setLastRefreshedAt(java.time.Instant lastRefreshedAt) {
        this.lastRefreshedAt = lastRefreshedAt;
    }

    public int getStalenessSeconds() {
        return stalenessSeconds;
    }

    public void setStalenessSeconds(int stalenessSeconds) {
        this.stalenessSeconds = stalenessSeconds;
    }

    public List<ProviderSpecialtyEntity> getSpecialties() {
        return specialties;
    }

    public List<ProviderModeEntity> getModes() {
        return modes;
    }

    public List<ProviderLanguageEntity> getLanguages() {
        return languages;
    }

    public List<ProviderCredentialEntity> getCredentials() {
        return credentials;
    }

    public List<AvailabilityEntity> getAvailability() {
        return availability;
    }

    /** Whether the projection has exceeded its staleness tolerance (TM-ARC-020). */
    public boolean isStale() {
        if (lastRefreshedAt == null) {
            return true;
        }
        return lastRefreshedAt.plusSeconds(stalenessSeconds).isBefore(java.time.Instant.now());
    }
}
