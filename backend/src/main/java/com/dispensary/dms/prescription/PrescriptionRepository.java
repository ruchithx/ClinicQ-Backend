package com.dispensary.dms.prescription;

import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

public interface PrescriptionRepository extends JpaRepository<PrescriptionEntity, UUID> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from PrescriptionEntity p where p.id = :id")
    java.util.Optional<PrescriptionEntity> findByIdForUpdate(@Param("id") UUID id);

    @Query("select p from PrescriptionEntity p where p.consultationId in :ids "
            + "order by p.submittedAt desc")
    List<PrescriptionEntity> findByConsultationIds(@Param("ids") Collection<UUID> ids);

    List<PrescriptionEntity> findByConsultationIdOrderBySubmittedAtDesc(UUID consultationId);

    @Query("""
            select p from PrescriptionEntity p
            where (:statuses is null or p.status in :statuses)
              and (:patientId is null or p.patientId = :patientId)
              and (:prescriptionNumber is null or p.prescriptionNumber = :prescriptionNumber)
              and (:doctorId is null or exists (
                    select 1 from ConsultationEntity c
                    where c.id = p.consultationId and c.doctorId = :doctorId))
            """)
    Page<PrescriptionEntity> search(@Param("statuses") Collection<PrescriptionStatus> statuses,
                                    @Param("patientId") UUID patientId,
                                    @Param("prescriptionNumber") Long prescriptionNumber,
                                    @Param("doctorId") UUID doctorId,
                                    Pageable pageable);

    /** Id of the prescription that supersedes the given one, if any. */
    @Query("select p.id from PrescriptionEntity p where p.supersedesPrescriptionId in :ids")
    List<UUID> findSupersededBy(@Param("ids") Collection<UUID> ids);

    @Query("select p.supersedesPrescriptionId, p.id from PrescriptionEntity p "
            + "where p.supersedesPrescriptionId in :ids")
    List<Object[]> findSupersededByPairs(@Param("ids") Collection<UUID> ids);
}
