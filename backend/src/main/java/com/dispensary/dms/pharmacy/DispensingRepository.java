package com.dispensary.dms.pharmacy;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface DispensingRepository extends JpaRepository<DispensingEntity, UUID> {

    Optional<DispensingEntity> findByPrescriptionId(UUID prescriptionId);

    @Query("select d from DispensingEntity d where d.prescriptionId in :ids")
    List<DispensingEntity> findByPrescriptionIds(@Param("ids") Collection<UUID> ids);
}
