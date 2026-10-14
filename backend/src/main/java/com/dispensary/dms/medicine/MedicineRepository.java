package com.dispensary.dms.medicine;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.UUID;

public interface MedicineRepository extends JpaRepository<MedicineEntity, UUID> {

    @Query("""
            select m from MedicineEntity m
            where (:search is null or lower(m.name) like lower(concat('%', :search, '%')))
              and (:active is null or m.active = :active)
            """)
    Page<MedicineEntity> search(@Param("search") String search,
                                @Param("active") Boolean active,
                                Pageable pageable);
}
