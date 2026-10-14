package com.dispensary.dms.consultation;

import com.dispensary.dms.audit.AuditAction;
import com.dispensary.dms.audit.AuditService;
import com.dispensary.dms.audit.EntityType;
import com.dispensary.dms.common.error.BusinessRuleException;
import com.dispensary.dms.common.error.ConflictException;
import com.dispensary.dms.common.error.ErrorCode;
import com.dispensary.dms.common.error.FieldError;
import com.dispensary.dms.common.error.NotFoundException;
import com.dispensary.dms.common.idempotency.IdempotencyResult;
import com.dispensary.dms.common.idempotency.IdempotencyService;
import com.dispensary.dms.common.idempotency.IdempotentHelper;
import com.dispensary.dms.common.time.BusinessClock;
import com.dispensary.dms.consultation.dto.ConsultationResponse;
import com.dispensary.dms.consultation.dto.CreateConsultationRequest;
import com.dispensary.dms.consultation.dto.DiagnosisInput;
import com.dispensary.dms.patient.PatientRepository;
import com.dispensary.dms.prescription.PrescriptionService;
import com.dispensary.dms.queue.QueueEntryEntity;
import com.dispensary.dms.queue.QueueEntryRepository;
import com.dispensary.dms.queue.QueueEntryStatus;
import com.dispensary.dms.security.CurrentUser;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/** Atomic consultation creation and retrieval (spec section 10.3). */
@Service
public class ConsultationService {

    private final ConsultationRepository repository;
    private final ConsultationAssembler assembler;
    private final PatientRepository patientRepository;
    private final QueueEntryRepository queueEntryRepository;
    private final PrescriptionService prescriptionService;
    private final IdempotencyService idempotencyService;
    private final AuditService auditService;
    private final BusinessClock clock;

    public ConsultationService(ConsultationRepository repository, ConsultationAssembler assembler,
                               PatientRepository patientRepository,
                               QueueEntryRepository queueEntryRepository,
                               PrescriptionService prescriptionService,
                               IdempotencyService idempotencyService, AuditService auditService,
                               BusinessClock clock) {
        this.repository = repository;
        this.assembler = assembler;
        this.patientRepository = patientRepository;
        this.queueEntryRepository = queueEntryRepository;
        this.prescriptionService = prescriptionService;
        this.idempotencyService = idempotencyService;
        this.auditService = auditService;
        this.clock = clock;
    }

    @Transactional
    public IdempotentHelper.Result<ConsultationResponse> create(CreateConsultationRequest request,
                                                                CurrentUser actor, String idempotencyKey) {
        String hash = idempotencyService.requestHash("createConsultation", "/api/v1/consultations", request);
        IdempotencyResult idem = idempotencyService.begin(actor.id(), idempotencyKey,
                "createConsultation", hash);
        if (idem.isReplay()) {
            return new IdempotentHelper.Result<>(assembler.toResponse(require(idem.resourceId())),
                    idem.responseStatus());
        }

        if (!patientRepository.existsById(request.patientId())) {
            throw new BusinessRuleException(ErrorCode.REFERENCED_RESOURCE_NOT_FOUND,
                    "Patient not found", List.of(new FieldError("patientId", "NotFound",
                    "Patient not found", request.patientId().toString())));
        }

        if (request.queueEntryId() != null) {
            QueueEntryEntity entry = queueEntryRepository.findById(request.queueEntryId())
                    .orElseThrow(() -> new BusinessRuleException(ErrorCode.REFERENCED_RESOURCE_NOT_FOUND,
                            "Queue entry not found",
                            List.of(new FieldError("queueEntryId", "NotFound", "Queue entry not found",
                                    request.queueEntryId().toString()))));
            if (entry.getStatus() != QueueEntryStatus.SERVING
                    && entry.getStatus() != QueueEntryStatus.COMPLETED) {
                throw new BusinessRuleException(ErrorCode.QUEUE_ENTRY_NOT_IN_SESSION,
                        "Queue entry must be SERVING or COMPLETED to link a consultation");
            }
            if (repository.existsByQueueEntryId(request.queueEntryId())) {
                throw new ConflictException(ErrorCode.QUEUE_ENTRY_ALREADY_LINKED,
                        "The queue entry is already linked to a consultation");
            }
        }

        ConsultationEntity consultation = new ConsultationEntity();
        consultation.setId(UUID.randomUUID());
        consultation.setPatientId(request.patientId());
        consultation.setDoctorId(actor.id());
        consultation.setQueueEntryId(request.queueEntryId());
        consultation.setConsultationAt(clock.now());
        consultation.setPresentingComplaint(request.presentingComplaint());
        consultation.setClinicalNotes(request.clinicalNotes());
        consultation.setCreatedAt(clock.now());
        consultation.setDiagnoses(buildDiagnoses(request.diagnoses()));
        repository.saveAndFlush(consultation);

        int prescriptionItemCount = 0;
        UUID prescriptionId = null;
        if (request.prescription() != null) {
            var prescription = prescriptionService.createForConsultation(consultation,
                    request.prescription(), actor);
            prescriptionId = prescription.getId();
            prescriptionItemCount = prescription.getItems().size();
        }

        auditService.event(AuditAction.CONSULTATION_CREATED, EntityType.CONSULTATION)
                .actor(actor).entityId(consultation.getId()).patientId(consultation.getPatientId())
                .detail("diagnosisCount", consultation.getDiagnoses().size())
                .detail("prescriptionId", prescriptionId == null ? null : prescriptionId.toString())
                .detail("itemCount", prescriptionItemCount)
                .detail("queueEntryId", request.queueEntryId() == null ? null
                        : request.queueEntryId().toString())
                .save();

        idempotencyService.complete(idem.rowId(), consultation.getId(), 201);
        return new IdempotentHelper.Result<>(assembler.toResponse(consultation), 201);
    }

    @Transactional
    public ConsultationResponse get(UUID consultationId, CurrentUser actor) {
        ConsultationEntity consultation = require(consultationId);
        auditService.event(AuditAction.CONSULTATION_VIEWED, EntityType.CONSULTATION)
                .actor(actor).entityId(consultation.getId()).patientId(consultation.getPatientId())
                .save();
        return assembler.toResponse(consultation);
    }

    private List<ConsultationDiagnosisEntity> buildDiagnoses(List<DiagnosisInput> inputs) {
        List<ConsultationDiagnosisEntity> diagnoses = new ArrayList<>(inputs.size());
        for (int i = 0; i < inputs.size(); i++) {
            DiagnosisInput input = inputs.get(i);
            diagnoses.add(new ConsultationDiagnosisEntity(UUID.randomUUID(), (short) (i + 1),
                    input.conditionText(), input.icd10Code()));
        }
        return diagnoses;
    }

    private ConsultationEntity require(UUID consultationId) {
        return repository.findById(consultationId)
                .orElseThrow(() -> new NotFoundException(ErrorCode.CONSULTATION_NOT_FOUND,
                        "Consultation not found"));
    }
}
