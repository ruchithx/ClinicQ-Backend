package com.dispensary.dms.pharmacy;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

public interface DispensingItemRepository extends JpaRepository<DispensingItemEntity, UUID> {

    @Query("select i from DispensingItemEntity i where i.dispensingId in :ids")
    List<DispensingItemEntity> findByDispensingIds(@Param("ids") Collection<UUID> ids);
}
