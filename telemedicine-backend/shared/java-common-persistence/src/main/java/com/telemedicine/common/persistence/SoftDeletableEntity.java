package com.telemedicine.common.persistence;

import jakarta.persistence.MappedSuperclass;
import java.time.Instant;
import org.hibernate.annotations.SQLRestriction;

/**
 * Soft-deletable entity base class.
 *
 * <p>Rows are retained with a {@code deleted_at} timestamp; {@link SQLRestriction} hides them from
 * normal queries.
 */
@MappedSuperclass
@SQLRestriction("deleted_at IS NULL")
public abstract class SoftDeletableEntity extends BaseEntity {

    private Instant deletedAt;

    public Instant getDeletedAt() {
        return deletedAt;
    }

    public void setDeletedAt(Instant deletedAt) {
        this.deletedAt = deletedAt;
    }

    public void softDelete(Instant when) {
        deletedAt = when;
    }

    public boolean isDeleted() {
        return deletedAt != null;
    }
}
