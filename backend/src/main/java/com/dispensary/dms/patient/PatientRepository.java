package com.dispensary.dms.patient;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface PatientRepository extends JpaRepository<PatientEntity, UUID> {

    boolean existsByPatientNumber(String patientNumber);

    Optional<PatientEntity> findByPatientNumber(String patientNumber);

    @Query("""
            select p from PatientEntity p
            where lower(p.fullName) like lower(concat('%', :term, '%'))
               or upper(p.patientNumber) like upper(concat(:term, '%'))
               or p.phoneNumber = :phone
            order by p.fullName, p.patientNumber
            """)
    Page<PatientEntity> search(@Param("term") String term, @Param("phone") String phone,
                               Pageable pageable);
}
