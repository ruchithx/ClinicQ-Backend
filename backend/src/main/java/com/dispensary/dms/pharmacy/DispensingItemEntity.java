package com.dispensary.dms.pharmacy;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.Immutable;

import java.math.BigDecimal;
import java.util.UUID;

/** One handed-over line within a dispensing (table {@code dispensing_items}). */
@Entity
@Immutable
@Table(name = "dispensing_items")
public class DispensingItemEntity {

    @Id
    @Column(name = "id")
    private UUID id;

    @Column(name = "dispensing_id", nullable = false, insertable = false, updatable = false)
    private UUID dispensingId;

    @Column(name = "prescription_id", nullable = false)
    private UUID prescriptionId;

    @Column(name = "prescription_item_id", nullable = false)
    private UUID prescriptionItemId;

    @Column(name = "quantity_dispensed", nullable = false, precision = 7, scale = 2)
    private BigDecimal quantityDispensed;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private DispensedItemStatus status;

    @Column(name = "note")
    private String note;

    protected DispensingItemEntity() {
    }

    public DispensingItemEntity(UUID id, UUID prescriptionId, UUID prescriptionItemId,
                                BigDecimal quantityDispensed, DispensedItemStatus status, String note) {
        this.id = id;
        this.prescriptionId = prescriptionId;
        this.prescriptionItemId = prescriptionItemId;
        this.quantityDispensed = quantityDispensed;
        this.status = status;
        this.note = note;
    }

    public UUID getId() {
        return id;
    }

    public UUID getDispensingId() {
        return dispensingId;
    }

    /** Mirrors the join column in memory; the column itself is written by the parent's collection. */
    void setDispensingId(UUID dispensingId) {
        this.dispensingId = dispensingId;
    }

    public UUID getPrescriptionId() {
        return prescriptionId;
    }

    public UUID getPrescriptionItemId() {
        return prescriptionItemId;
    }

    public BigDecimal getQuantityDispensed() {
        return quantityDispensed;
    }

    public DispensedItemStatus getStatus() {
        return status;
    }

    public String getNote() {
        return note;
    }
}
