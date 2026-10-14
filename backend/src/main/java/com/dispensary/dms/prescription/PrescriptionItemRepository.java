package com.dispensary.dms.prescription;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

public interface PrescriptionItemRepository extends JpaRepository<PrescriptionItemEntity, UUID> {

    @Query("select i from PrescriptionItemEntity i where i.prescriptionId in :ids order by i.lineNumber")
    List<PrescriptionItemEntity> findByPrescriptionIds(@Param("ids") Collection<UUID> ids);

    List<PrescriptionItemEntity> findByPrescriptionIdOrderByLineNumber(UUID prescriptionId);

    @Query("select count(i) from PrescriptionItemEntity i where i.prescriptionId = :id")
    long countByPrescriptionId(@Param("id") UUID id);
}
