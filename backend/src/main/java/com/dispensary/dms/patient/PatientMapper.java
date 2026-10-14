package com.dispensary.dms.patient;

import com.dispensary.dms.common.time.BusinessClock;
import com.dispensary.dms.patient.dto.PatientRef;
import com.dispensary.dms.patient.dto.PatientResponse;
import com.dispensary.dms.patient.dto.PatientSummary;
import org.springframework.stereotype.Component;

import java.time.Period;

@Component
public class PatientMapper {

    private final BusinessClock clock;

    public PatientMapper(BusinessClock clock) {
        this.clock = clock;
    }

    public int ageYears(PatientEntity p) {
        return Period.between(p.getDateOfBirth(), clock.today()).getYears();
    }

    public PatientResponse toResponse(PatientEntity p) {
        return new PatientResponse(p.getId(), p.getPatientNumber(), p.getFullName(), p.getSex(),
                p.getDateOfBirth(), p.isDateOfBirthEstimated(), ageYears(p), p.getPhoneNumber(),
                p.getAddress(), p.getKnownAllergies(), p.getCreatedAt(), p.getUpdatedAt(), p.getVersion());
    }

    public PatientSummary toSummary(PatientEntity p) {
        return new PatientSummary(p.getId(), p.getPatientNumber(), p.getFullName(), p.getSex(),
                p.getDateOfBirth(), p.isDateOfBirthEstimated(), ageYears(p), p.getPhoneNumber());
    }

    public PatientRef toRef(PatientEntity p) {
        return new PatientRef(p.getId(), p.getPatientNumber(), p.getFullName());
    }
}
