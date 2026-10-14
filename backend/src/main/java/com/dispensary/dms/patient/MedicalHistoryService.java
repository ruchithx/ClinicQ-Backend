package com.dispensary.dms.patient;

import com.dispensary.dms.audit.AuditAction;
import com.dispensary.dms.audit.AuditService;
import com.dispensary.dms.audit.EntityType;
import com.dispensary.dms.common.web.PageResponse;
import com.dispensary.dms.consultation.ConsultationAssembler;
import com.dispensary.dms.consultation.ConsultationDiagnosisRepository;
import com.dispensary.dms.consultation.ConsultationEntity;
import com.dispensary.dms.consultation.ConsultationRepository;
import com.dispensary.dms.consultation.dto.ConsultationResponse;
import com.dispensary.dms.patient.dto.ConditionSummary;
import com.dispensary.dms.patient.dto.MedicalHistoryResponse;
import com.dispensary.dms.security.CurrentUser;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/** Assembles a patient's medical history (spec section 10.2 / FR-016) with bounded SQL. */
@Service
public class MedicalHistoryService {

    private static final int MAX_CONDITIONS = 50;

    private final PatientService patientService;
    private final ConsultationRepository consultationRepository;
    private final ConsultationAssembler consultationAssembler;
    private final ConsultationDiagnosisRepository diagnosisRepository;
    private final AuditService auditService;

    public MedicalHistoryService(PatientService patientService,
                                 ConsultationRepository consultationRepository,
                                 ConsultationAssembler consultationAssembler,
                                 ConsultationDiagnosisRepository diagnosisRepository,
                                 AuditService auditService) {
        this.patientService = patientService;
        this.consultationRepository = consultationRepository;
        this.consultationAssembler = consultationAssembler;
        this.diagnosisRepository = diagnosisRepository;
        this.auditService = auditService;
    }

    @Transactional
    public MedicalHistoryResponse getMedicalHistory(UUID patientId, int page, int size, CurrentUser actor) {
        PatientEntity patient = patientService.require(patientId);

        Page<ConsultationEntity> consultations = consultationRepository.findByPatient(patientId,
                PageRequest.of(page, size));
        List<ConsultationResponse> responses = consultationAssembler.toResponses(consultations.getContent());
        PageResponse<ConsultationResponse> consultationPage = PageResponse.ofMapped(responses, consultations);

        List<ConditionSummary> conditions = diagnosisRepository.summariseConditions(patientId).stream()
                .limit(MAX_CONDITIONS)
                .map(c -> new ConditionSummary(c.getConditionText(), c.getIcd10Code(),
                        c.getLastRecordedAt(), (int) c.getOccurrences()))
                .toList();

        auditService.event(AuditAction.PATIENT_HISTORY_VIEWED, EntityType.PATIENT)
                .actor(actor).entityId(patientId).patientId(patientId)
                .detail("page", page).detail("size", size).save();

        return new MedicalHistoryResponse(patientService.get(patientId), conditions, consultationPage);
    }
}
