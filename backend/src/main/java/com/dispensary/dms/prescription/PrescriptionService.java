package com.dispensary.dms.prescription;

import com.dispensary.dms.audit.AuditAction;
import com.dispensary.dms.audit.AuditService;
import com.dispensary.dms.audit.EntityType;
import com.dispensary.dms.common.error.ApiException;
import com.dispensary.dms.common.error.BusinessRuleException;
import com.dispensary.dms.common.error.ConflictException;
import com.dispensary.dms.common.error.ErrorCode;
import com.dispensary.dms.common.error.NotFoundException;
import com.dispensary.dms.common.idempotency.IdempotencyResult;
import com.dispensary.dms.common.idempotency.IdempotencyService;
import com.dispensary.dms.common.idempotency.IdempotentHelper;
import com.dispensary.dms.common.time.BusinessClock;
import com.dispensary.dms.common.web.PageResponse;
import com.dispensary.dms.consultation.ConsultationEntity;
import com.dispensary.dms.consultation.ConsultationRepository;
import com.dispensary.dms.prescription.dto.AddPrescriptionRequest;
import com.dispensary.dms.prescription.dto.CancelPrescriptionRequest;
import com.dispensary.dms.prescription.dto.PrescriptionInput;
import com.dispensary.dms.prescription.dto.PrescriptionResponse;
import com.dispensary.dms.prescription.dto.PrescriptionSummary;
import com.dispensary.dms.security.CurrentUser;
import com.dispensary.dms.user.Role;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.util.List;
import java.util.UUID;

/** Prescription creation, re-issue, cancellation and listing (spec section 10.4). */
@Service
public class PrescriptionService {

    private static final Duration CONSULTATION_EDIT_WINDOW = Duration.ofHours(24);

    private final PrescriptionRepository repository;
    private final ConsultationRepository consultationRepository;
    private final PrescriptionItemFactory itemFactory;
    private final PrescriptionAssembler assembler;
    private final PrescriptionStateMachine stateMachine = new PrescriptionStateMachine();
    private final IdempotencyService idempotencyService;
    private final AuditService auditService;
    private final BusinessClock clock;

    public PrescriptionService(PrescriptionRepository repository,
                               ConsultationRepository consultationRepository,
                               PrescriptionItemFactory itemFactory, PrescriptionAssembler assembler,
                               IdempotencyService idempotencyService, AuditService auditService,
                               BusinessClock clock) {
        this.repository = repository;
        this.consultationRepository = consultationRepository;
        this.itemFactory = itemFactory;
        this.assembler = assembler;
        this.idempotencyService = idempotencyService;
        this.auditService = auditService;
        this.clock = clock;
    }

    /** Called within {@code createConsultation}. Persists a PENDING prescription for the consultation. */
    public PrescriptionEntity createForConsultation(ConsultationEntity consultation,
                                                    PrescriptionInput input, CurrentUser actor) {
        PrescriptionEntity prescription = new PrescriptionEntity();
        prescription.setId(UUID.randomUUID());
        prescription.setConsultationId(consultation.getId());
        prescription.setPatientId(consultation.getPatientId());
        prescription.setStatus(PrescriptionStatus.PENDING);
        prescription.setNotes(input.notes());
        prescription.setSubmittedAt(clock.now());
        prescription.setUpdatedAt(clock.now());
        prescription.setItems(itemFactory.build(input.items(), "prescription.items"));
        repository.saveAndFlush(prescription);

        auditService.event(AuditAction.PRESCRIPTION_CREATED, EntityType.PRESCRIPTION)
                .actor(actor).entityId(prescription.getId()).patientId(consultation.getPatientId())
                .detail("prescriptionNumber", PrescriptionAssembler.formatNumber(prescription.getPrescriptionNumber()))
                .detail("itemCount", prescription.getItems().size())
                .save();
        return prescription;
    }

    @Transactional
    public IdempotentHelper.Result<PrescriptionResponse> addToConsultation(
            UUID consultationId, AddPrescriptionRequest request, CurrentUser actor, String idempotencyKey) {
        String path = "/api/v1/consultations/" + consultationId + "/prescriptions";
        String hash = idempotencyService.requestHash("addPrescriptionToConsultation", path, request);
        IdempotencyResult idem = idempotencyService.begin(actor.id(), idempotencyKey,
                "addPrescriptionToConsultation", hash);
        if (idem.isReplay()) {
            return new IdempotentHelper.Result<>(assembler.toResponse(require(idem.resourceId())),
                    idem.responseStatus());
        }

        ConsultationEntity consultation = consultationRepository.findById(consultationId)
                .orElseThrow(() -> new NotFoundException(ErrorCode.CONSULTATION_NOT_FOUND,
                        "Consultation not found"));
        if (!actor.isAdmin() && !consultation.getDoctorId().equals(actor.id())) {
            throw new ApiException(ErrorCode.NOT_RESOURCE_OWNER,
                    "Only the consulting doctor may add a prescription");
        }
        if (Duration.between(consultation.getConsultationAt(), clock.now())
                .compareTo(CONSULTATION_EDIT_WINDOW) > 0) {
            throw new BusinessRuleException(ErrorCode.CONSULTATION_LOCKED,
                    "Prescriptions can only be added within 24 hours of the consultation");
        }

        if (request.supersedesPrescriptionId() != null) {
            validateSupersedes(request.supersedesPrescriptionId(), consultationId);
        }

        PrescriptionEntity prescription = new PrescriptionEntity();
        prescription.setId(UUID.randomUUID());
        prescription.setConsultationId(consultationId);
        prescription.setPatientId(consultation.getPatientId());
        prescription.setStatus(PrescriptionStatus.PENDING);
        prescription.setNotes(request.notes());
        prescription.setSubmittedAt(clock.now());
        prescription.setUpdatedAt(clock.now());
        prescription.setSupersedesPrescriptionId(request.supersedesPrescriptionId());
        prescription.setItems(itemFactory.build(request.items(), "items"));
        repository.saveAndFlush(prescription);

        auditService.event(AuditAction.PRESCRIPTION_CREATED, EntityType.PRESCRIPTION)
                .actor(actor).entityId(prescription.getId()).patientId(consultation.getPatientId())
                .detail("prescriptionNumber", PrescriptionAssembler.formatNumber(prescription.getPrescriptionNumber()))
                .detail("itemCount", prescription.getItems().size())
                .detail("supersedesPrescriptionId",
                        request.supersedesPrescriptionId() == null ? null
                                : request.supersedesPrescriptionId().toString())
                .save();

        idempotencyService.complete(idem.rowId(), prescription.getId(), 201);
        return new IdempotentHelper.Result<>(assembler.toResponse(prescription), 201);
    }

    private void validateSupersedes(UUID supersedesId, UUID consultationId) {
        PrescriptionEntity superseded = repository.findById(supersedesId)
                .orElseThrow(() -> new BusinessRuleException(ErrorCode.INVALID_SUPERSEDED_PRESCRIPTION,
                        "Superseded prescription not found"));
        if (!superseded.getConsultationId().equals(consultationId)
                || superseded.getStatus() != PrescriptionStatus.CANCELLED) {
            throw new BusinessRuleException(ErrorCode.INVALID_SUPERSEDED_PRESCRIPTION,
                    "Superseded prescription must be a cancelled prescription of the same consultation");
        }
    }

    @Transactional
    public PrescriptionResponse cancel(UUID prescriptionId, CancelPrescriptionRequest request,
                                       CurrentUser actor) {
        PrescriptionEntity prescription = repository.findByIdForUpdate(prescriptionId)
                .orElseThrow(() -> new NotFoundException(ErrorCode.PRESCRIPTION_NOT_FOUND,
                        "Prescription not found"));

        // Ownership: a DOCTOR who is not also a PHARMACIST or ADMIN may cancel only their own prescriptions.
        if (actor.hasRole(Role.DOCTOR) && !actor.hasRole(Role.PHARMACIST) && !actor.isAdmin()) {
            ConsultationEntity consultation = consultationRepository.findById(prescription.getConsultationId())
                    .orElseThrow();
            if (!consultation.getDoctorId().equals(actor.id())) {
                throw new ApiException(ErrorCode.NOT_RESOURCE_OWNER,
                        "You can only cancel your own prescriptions");
            }
        }

        PrescriptionStatus from = prescription.getStatus();
        var transition = stateMachine.evaluate(from, PrescriptionStateMachine.Action.CANCEL);
        if (!transition.isValid()) {
            throw new ConflictException(ErrorCode.INVALID_STATUS_TRANSITION,
                    "Cannot cancel a prescription in status " + from,
                    java.util.Map.of("currentStatus", from.name()));
        }
        if (transition.isChange()) {
            prescription.setStatus(PrescriptionStatus.CANCELLED);
            prescription.setCancelledAt(clock.now());
            prescription.setCancelledBy(actor.id());
            prescription.setCancelReason(request.reason());
            prescription.setUpdatedAt(clock.now());
            repository.save(prescription);
            auditService.event(AuditAction.PRESCRIPTION_CANCELLED, EntityType.PRESCRIPTION)
                    .actor(actor).entityId(prescription.getId()).patientId(prescription.getPatientId())
                    .detail("fromStatus", from.name()).detail("toStatus", PrescriptionStatus.CANCELLED.name())
                    .save();
        }
        return assembler.toResponse(prescription);
    }

    @Transactional
    public PrescriptionResponse get(UUID prescriptionId, CurrentUser actor) {
        PrescriptionEntity prescription = require(prescriptionId);
        auditService.event(AuditAction.PRESCRIPTION_VIEWED, EntityType.PRESCRIPTION)
                .actor(actor).entityId(prescription.getId()).patientId(prescription.getPatientId())
                .save();
        return assembler.toResponse(prescription);
    }

    @Transactional(readOnly = true)
    public long versionOf(UUID prescriptionId) {
        return require(prescriptionId).getVersion();
    }

    @Transactional(readOnly = true)
    public PageResponse<PrescriptionSummary> list(List<PrescriptionStatus> statuses, UUID patientId,
                                                  String prescriptionNumber, int page, int size,
                                                  CurrentUser actor) {
        List<PrescriptionStatus> effective = (statuses == null || statuses.isEmpty())
                ? List.of(PrescriptionStatus.PENDING, PrescriptionStatus.IN_PROGRESS) : statuses;
        Long number = parseNumber(prescriptionNumber);
        UUID doctorScope = actor.hasRole(Role.PHARMACIST) || actor.isAdmin() ? null : actor.id();

        var result = repository.search(effective, patientId, number, doctorScope,
                PageRequest.of(page, size, Sort.by("submittedAt").ascending().and(Sort.by("id"))));
        return PageResponse.ofMapped(assembler.toSummaries(result.getContent()), result);
    }

    private Long parseNumber(String prescriptionNumber) {
        if (prescriptionNumber == null || prescriptionNumber.isBlank()) {
            return null;
        }
        String digits = prescriptionNumber.startsWith("RX-")
                ? prescriptionNumber.substring(3) : prescriptionNumber;
        try {
            return Long.parseLong(digits);
        } catch (NumberFormatException e) {
            return -1L; // matches nothing
        }
    }

    PrescriptionEntity require(UUID prescriptionId) {
        return repository.findById(prescriptionId)
                .orElseThrow(() -> new NotFoundException(ErrorCode.PRESCRIPTION_NOT_FOUND,
                        "Prescription not found"));
    }
}
