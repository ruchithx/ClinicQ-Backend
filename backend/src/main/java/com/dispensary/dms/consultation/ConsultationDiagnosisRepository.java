package com.dispensary.dms.consultation;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

public interface ConsultationDiagnosisRepository extends JpaRepository<ConsultationDiagnosisEntity, UUID> {

    @Query("select d from ConsultationDiagnosisEntity d where d.consultationId in :ids order by d.lineNumber")
    List<ConsultationDiagnosisEntity> findByConsultationIds(@Param("ids") Collection<UUID> ids);

    /** Conditions summary (example query Q5): grouped counts per condition for a patient. */
    @Query("""
            select d.conditionText as conditionText, d.icd10Code as icd10Code,
                   max(c.consultationAt) as lastRecordedAt, count(d) as occurrences
            from ConsultationDiagnosisEntity d
            join ConsultationEntity c on c.id = d.consultationId
            where c.patientId = :patientId
            group by d.conditionText, d.icd10Code
            order by max(c.consultationAt) desc
            """)
    List<ConditionSummaryProjection> summariseConditions(@Param("patientId") UUID patientId);

    interface ConditionSummaryProjection {
        String getConditionText();

        String getIcd10Code();

        Instant getLastRecordedAt();

        long getOccurrences();
    }
}
