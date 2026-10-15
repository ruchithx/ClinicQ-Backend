package com.dispensary.dms.consultation;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
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
 * A doctor-patient encounter (table {@code consultations}); insert-only. Implements
 * {@link Persistable} because the id is assigned up front and there is no {@code @Version}, so
 * Spring Data would otherwise merge instead of persist and fail to cascade the new diagnoses.
 */
@Entity
@Immutable
@Table(name = "consultations")
public class ConsultationEntity implements Persistable<UUID> {

    @Transient
    private boolean persisted;

    @Id
    @Column(name = "id")
    private UUID id;

    @Column(name = "patient_id", nullable = false)
    private UUID patientId;

    @Column(name = "doctor_id", nullable = false)
    private UUID doctorId;

    @Column(name = "queue_entry_id")
    private UUID queueEntryId;

    @Column(name = "consultation_at", nullable = false)
    private Instant consultationAt;

    @Column(name = "presenting_complaint")
    private String presentingComplaint;

    @Column(name = "clinical_notes")
    private String clinicalNotes;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @OneToMany(cascade = CascadeType.PERSIST)
    @JoinColumn(name = "consultation_id", nullable = false, updatable = false)
    @OrderBy("lineNumber")
    private List<ConsultationDiagnosisEntity> diagnoses = new ArrayList<>();

    protected ConsultationEntity() {
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
    void linkDiagnoses() {
        diagnoses.forEach(d -> d.setConsultationId(id));
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public UUID getPatientId() {
        return patientId;
    }

    public void setPatientId(UUID patientId) {
        this.patientId = patientId;
    }

    public UUID getDoctorId() {
        return doctorId;
    }

    public void setDoctorId(UUID doctorId) {
        this.doctorId = doctorId;
    }

    public UUID getQueueEntryId() {
        return queueEntryId;
    }

    public void setQueueEntryId(UUID queueEntryId) {
        this.queueEntryId = queueEntryId;
    }

    public Instant getConsultationAt() {
        return consultationAt;
    }

    public void setConsultationAt(Instant consultationAt) {
        this.consultationAt = consultationAt;
    }

    public String getPresentingComplaint() {
        return presentingComplaint;
    }

    public void setPresentingComplaint(String presentingComplaint) {
        this.presentingComplaint = presentingComplaint;
    }

    public String getClinicalNotes() {
        return clinicalNotes;
    }

    public void setClinicalNotes(String clinicalNotes) {
        this.clinicalNotes = clinicalNotes;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public List<ConsultationDiagnosisEntity> getDiagnoses() {
        return diagnoses;
    }

    public void setDiagnoses(List<ConsultationDiagnosisEntity> diagnoses) {
        this.diagnoses = diagnoses;
    }
}
