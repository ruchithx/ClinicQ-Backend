package com.dispensary.dms.consultation;

import com.dispensary.dms.common.dto.StaffRef;
import com.dispensary.dms.consultation.dto.ConsultationResponse;
import com.dispensary.dms.consultation.dto.Diagnosis;
import com.dispensary.dms.patient.PatientEntity;
import com.dispensary.dms.patient.PatientRepository;
import com.dispensary.dms.patient.dto.PatientRef;
import com.dispensary.dms.prescription.PrescriptionAssembler;
import com.dispensary.dms.prescription.PrescriptionEntity;
import com.dispensary.dms.prescription.PrescriptionRepository;
import com.dispensary.dms.prescription.dto.PrescriptionResponse;
import com.dispensary.dms.user.UserEntity;
import com.dispensary.dms.user.UserRepository;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

/** Assembles {@link ConsultationResponse} objects with batch loading (spec section 6.4). */
@Component
public class ConsultationAssembler {

    private final ConsultationDiagnosisRepository diagnosisRepository;
    private final PrescriptionRepository prescriptionRepository;
    private final PrescriptionAssembler prescriptionAssembler;
    private final PatientRepository patientRepository;
    private final UserRepository userRepository;

    public ConsultationAssembler(ConsultationDiagnosisRepository diagnosisRepository,
                                 PrescriptionRepository prescriptionRepository,
                                 PrescriptionAssembler prescriptionAssembler,
                                 PatientRepository patientRepository, UserRepository userRepository) {
        this.diagnosisRepository = diagnosisRepository;
        this.prescriptionRepository = prescriptionRepository;
        this.prescriptionAssembler = prescriptionAssembler;
        this.patientRepository = patientRepository;
        this.userRepository = userRepository;
    }

    public ConsultationResponse toResponse(ConsultationEntity consultation) {
        return toResponses(List.of(consultation)).get(0);
    }

    public List<ConsultationResponse> toResponses(List<ConsultationEntity> consultations) {
        if (consultations.isEmpty()) {
            return List.of();
        }
        List<UUID> ids = consultations.stream().map(ConsultationEntity::getId).toList();
        Set<UUID> patientIds = consultations.stream()
                .map(ConsultationEntity::getPatientId).collect(Collectors.toSet());
        Set<UUID> doctorIds = consultations.stream()
                .map(ConsultationEntity::getDoctorId).collect(Collectors.toSet());

        Map<UUID, List<ConsultationDiagnosisEntity>> diagnoses =
                diagnosisRepository.findByConsultationIds(ids).stream()
                        .collect(Collectors.groupingBy(ConsultationDiagnosisEntity::getConsultationId));
        Map<UUID, List<PrescriptionEntity>> prescriptionsByConsultation =
                prescriptionRepository.findByConsultationIds(ids).stream()
                        .collect(Collectors.groupingBy(PrescriptionEntity::getConsultationId));

        // Assemble all prescriptions in one batch, then group back by consultation.
        List<PrescriptionEntity> allPrescriptions = prescriptionsByConsultation.values().stream()
                .flatMap(List::stream).toList();
        Map<UUID, PrescriptionResponse> prescriptionResponses = prescriptionAssembler
                .toResponses(allPrescriptions).stream()
                .collect(Collectors.toMap(PrescriptionResponse::id, Function.identity()));

        Map<UUID, PatientEntity> patients = patientRepository.findAllById(patientIds)
                .stream().collect(Collectors.toMap(PatientEntity::getId, Function.identity()));
        Map<UUID, UserEntity> doctors = userRepository.findAllById(doctorIds)
                .stream().collect(Collectors.toMap(UserEntity::getId, Function.identity()));

        List<ConsultationResponse> result = new ArrayList<>(consultations.size());
        for (ConsultationEntity c : consultations) {
            PatientEntity patient = patients.get(c.getPatientId());
            UserEntity doctor = doctors.get(c.getDoctorId());
            List<PrescriptionResponse> prescriptions = prescriptionsByConsultation
                    .getOrDefault(c.getId(), List.of()).stream()
                    .map(p -> prescriptionResponses.get(p.getId()))
                    .filter(java.util.Objects::nonNull)
                    .toList();
            result.add(new ConsultationResponse(
                    c.getId(),
                    patient == null ? null : new PatientRef(patient.getId(), patient.getPatientNumber(),
                            patient.getFullName()),
                    doctor == null ? null : new StaffRef(doctor.getId(), doctor.getFullName()),
                    c.getQueueEntryId(),
                    c.getConsultationAt(),
                    c.getPresentingComplaint(),
                    c.getClinicalNotes(),
                    diagnoses.getOrDefault(c.getId(), List.of()).stream().map(this::toDiagnosis).toList(),
                    prescriptions,
                    c.getCreatedAt()));
        }
        return result;
    }

    private Diagnosis toDiagnosis(ConsultationDiagnosisEntity d) {
        return new Diagnosis(d.getId(), d.getLineNumber(), d.getConditionText(), d.getIcd10Code());
    }
}
