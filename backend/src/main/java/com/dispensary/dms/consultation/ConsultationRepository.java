package com.dispensary.dms.consultation;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.UUID;

public interface ConsultationRepository extends JpaRepository<ConsultationEntity, UUID> {

    @Query("select c from ConsultationEntity c where c.patientId = :patientId "
            + "order by c.consultationAt desc, c.id")
    Page<ConsultationEntity> findByPatient(@Param("patientId") UUID patientId, Pageable pageable);

    boolean existsByQueueEntryId(UUID queueEntryId);
}
