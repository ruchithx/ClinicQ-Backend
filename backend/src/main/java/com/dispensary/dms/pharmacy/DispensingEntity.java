package com.dispensary.dms.pharmacy;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToMany;
import jakarta.persistence.PostLoad;
import jakarta.persistence.PostPersist;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import org.hibernate.annotations.Immutable;
import org.springframework.data.domain.Persistable;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * The single dispensing event of a prescription (table {@code dispensings}). Implements
 * {@link Persistable} because the id is assigned up front and there is no {@code @Version}, so
 * Spring Data would otherwise merge instead of persist and fail to cascade the new items.
 */
@Entity
@Immutable
@Table(name = "dispensings")
public class DispensingEntity implements Persistable<UUID> {

    @Transient
    private boolean persisted;

    @Id
    @Column(name = "id")
    private UUID id;

    @Column(name = "prescription_id", nullable = false)
    private UUID prescriptionId;

    @Column(name = "pharmacist_id", nullable = false)
    private UUID pharmacistId;

    @Column(name = "dispensed_at", nullable = false)
    private Instant dispensedAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "outcome", nullable = false)
    private DispensingOutcome outcome;

    @Column(name = "notes")
    private String notes;

    @OneToMany(cascade = CascadeType.PERSIST)
    @JoinColumn(name = "dispensing_id", nullable = false, updatable = false)
    private List<DispensingItemEntity> items = new ArrayList<>();

    protected DispensingEntity() {
    }

    public DispensingEntity(UUID id, UUID prescriptionId, UUID pharmacistId, Instant dispensedAt,
                            DispensingOutcome outcome, String notes) {
        this.id = id;
        this.prescriptionId = prescriptionId;
        this.pharmacistId = pharmacistId;
        this.dispensedAt = dispensedAt;
        this.outcome = outcome;
        this.notes = notes;
    }

    @Override
    public UUID getId() {
        return id;
    }

    @Override
    public boolean isNew() {
        return !persisted;
    }

    @PostLoad
    @PostPersist
    void markPersisted() {
        this.persisted = true;
    }

    @PrePersist
    void linkItems() {
        items.forEach(i -> i.setDispensingId(id));
    }

    public UUID getPrescriptionId() {
        return prescriptionId;
    }

    public UUID getPharmacistId() {
        return pharmacistId;
    }

    public Instant getDispensedAt() {
        return dispensedAt;
    }

    public DispensingOutcome getOutcome() {
        return outcome;
    }

    public String getNotes() {
        return notes;
    }

    public List<DispensingItemEntity> getItems() {
        return items;
    }

    public void setItems(List<DispensingItemEntity> items) {
        this.items = items;
    }
}
