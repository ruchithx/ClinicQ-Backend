package com.dispensary.dms.consultation;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.Immutable;

import java.util.UUID;

/** One condition/diagnosis recorded in a consultation (table {@code consultation_diagnoses}). */
@Entity
@Immutable
@Table(name = "consultation_diagnoses")
public class ConsultationDiagnosisEntity {

    @Id
    @Column(name = "id")
    private UUID id;

    @Column(name = "consultation_id", nullable = false, insertable = false, updatable = false)
    private UUID consultationId;

    @Column(name = "line_number", nullable = false)
    private short lineNumber;

    @Column(name = "condition_text", nullable = false)
    private String conditionText;

    @Column(name = "icd10_code")
    private String icd10Code;

    protected ConsultationDiagnosisEntity() {
    }

    public ConsultationDiagnosisEntity(UUID id, short lineNumber, String conditionText, String icd10Code) {
        this.id = id;
        this.lineNumber = lineNumber;
        this.conditionText = conditionText;
        this.icd10Code = icd10Code;
    }

    public UUID getId() {
        return id;
    }

    public UUID getConsultationId() {
        return consultationId;
    }

    public short getLineNumber() {
        return lineNumber;
    }

    public String getConditionText() {
        return conditionText;
    }

    public String getIcd10Code() {
        return icd10Code;
    }
}
