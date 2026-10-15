package com.dispensary.dms.prescription;

import com.dispensary.dms.medicine.DosageForm;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.Immutable;

import java.math.BigDecimal;
import java.util.UUID;

/** A prescribed line (table {@code prescription_items}); immutable snapshot. */
@Entity
@Immutable
@Table(name = "prescription_items")
public class PrescriptionItemEntity {

    @Id
    @Column(name = "id")
    private UUID id;

    @Column(name = "prescription_id", nullable = false, insertable = false, updatable = false)
    private UUID prescriptionId;

    @Column(name = "line_number", nullable = false)
    private short lineNumber;

    @Column(name = "medicine_id")
    private UUID medicineId;

    @Column(name = "medicine_name", nullable = false)
    private String medicineName;

    @Column(name = "strength")
    private String strength;

    @Enumerated(EnumType.STRING)
    @Column(name = "dosage_form")
    private DosageForm dosageForm;

    @Column(name = "dose", nullable = false)
    private String dose;

    @Column(name = "frequency", nullable = false)
    private String frequency;

    @Column(name = "duration_days")
    private Short durationDays;

    @Column(name = "quantity", nullable = false, precision = 7, scale = 2)
    private BigDecimal quantity;

    @Column(name = "quantity_unit", nullable = false)
    private String quantityUnit;

    @Column(name = "instructions")
    private String instructions;

    protected PrescriptionItemEntity() {
    }

    public PrescriptionItemEntity(UUID id, short lineNumber, UUID medicineId, String medicineName,
                                  String strength, DosageForm dosageForm, String dose, String frequency,
                                  Short durationDays, BigDecimal quantity, String quantityUnit,
                                  String instructions) {
        this.id = id;
        this.lineNumber = lineNumber;
        this.medicineId = medicineId;
        this.medicineName = medicineName;
        this.strength = strength;
        this.dosageForm = dosageForm;
        this.dose = dose;
        this.frequency = frequency;
        this.durationDays = durationDays;
        this.quantity = quantity;
        this.quantityUnit = quantityUnit;
        this.instructions = instructions;
    }

    public UUID getId() {
        return id;
    }

    public UUID getPrescriptionId() {
        return prescriptionId;
    }

    /** Mirrors the join column in memory; the column itself is written by the parent's collection. */
    void setPrescriptionId(UUID prescriptionId) {
        this.prescriptionId = prescriptionId;
    }

    public short getLineNumber() {
        return lineNumber;
    }

    public UUID getMedicineId() {
        return medicineId;
    }

    public String getMedicineName() {
        return medicineName;
    }

    public String getStrength() {
        return strength;
    }

    public DosageForm getDosageForm() {
        return dosageForm;
    }

    public String getDose() {
        return dose;
    }

    public String getFrequency() {
        return frequency;
    }

    public Short getDurationDays() {
        return durationDays;
    }

    public BigDecimal getQuantity() {
        return quantity;
    }

    public String getQuantityUnit() {
        return quantityUnit;
    }

    public String getInstructions() {
        return instructions;
    }
}
