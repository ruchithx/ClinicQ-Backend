package com.dispensary.dms.pharmacy;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import org.hibernate.annotations.Immutable;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/** The single dispensing event of a prescription (table {@code dispensings}). */
@Entity
@Immutable
@Table(name = "dispensings")
public class DispensingEntity {

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
    @JoinColumn(name = "dispensing_id")
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

    public UUID getId() {
        return id;
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
