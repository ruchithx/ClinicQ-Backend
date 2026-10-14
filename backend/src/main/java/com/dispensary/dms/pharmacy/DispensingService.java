package com.dispensary.dms.pharmacy;

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
import com.dispensary.dms.pharmacy.dto.DispenseItemInput;
import com.dispensary.dms.pharmacy.dto.DispenseRequest;
import com.dispensary.dms.prescription.PrescriptionAssembler;
import com.dispensary.dms.prescription.PrescriptionEntity;
import com.dispensary.dms.prescription.PrescriptionItemEntity;
import com.dispensary.dms.prescription.PrescriptionItemRepository;
import com.dispensary.dms.prescription.PrescriptionRepository;
import com.dispensary.dms.prescription.PrescriptionStateMachine;
import com.dispensary.dms.prescription.PrescriptionStatus;
import com.dispensary.dms.prescription.dto.PrescriptionResponse;
import com.dispensary.dms.security.CurrentUser;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

/** Pharmacy start / dispense operations with exactly-once guarantees (spec section 10.5). */
@Service
public class DispensingService {

    private final PrescriptionRepository prescriptionRepository;
    private final PrescriptionItemRepository itemRepository;
    private final DispensingRepository dispensingRepository;
    private final PrescriptionAssembler assembler;
    private final PrescriptionStateMachine stateMachine = new PrescriptionStateMachine();
    private final IdempotencyService idempotencyService;
    private final AuditService auditService;
    private final BusinessClock clock;

    public DispensingService(PrescriptionRepository prescriptionRepository,
                             PrescriptionItemRepository itemRepository,
                             DispensingRepository dispensingRepository,
                             PrescriptionAssembler assembler, IdempotencyService idempotencyService,
                             AuditService auditService, BusinessClock clock) {
        this.prescriptionRepository = prescriptionRepository;
        this.itemRepository = itemRepository;
        this.dispensingRepository = dispensingRepository;
        this.assembler = assembler;
        this.idempotencyService = idempotencyService;
        this.auditService = auditService;
        this.clock = clock;
    }

    @Transactional
    public PrescriptionResponse start(UUID prescriptionId, CurrentUser actor) {
        PrescriptionEntity prescription = lock(prescriptionId);
        PrescriptionStatus from = prescription.getStatus();
        var transition = stateMachine.evaluate(from, PrescriptionStateMachine.Action.START);
        if (!transition.isValid()) {
            throw new ConflictException(ErrorCode.INVALID_STATUS_TRANSITION,
                    "Cannot start a prescription in status " + from,
                    Map.of("currentStatus", from.name()));
        }
        if (transition.isChange()) {
            prescription.setStatus(PrescriptionStatus.IN_PROGRESS);
            prescription.setStartedAt(clock.now());
            prescription.setStartedBy(actor.id());
            prescription.setUpdatedAt(clock.now());
            prescriptionRepository.save(prescription);
            auditService.event(AuditAction.PRESCRIPTION_STARTED, EntityType.PRESCRIPTION)
                    .actor(actor).entityId(prescription.getId()).patientId(prescription.getPatientId())
                    .detail("fromStatus", from.name()).detail("toStatus", PrescriptionStatus.IN_PROGRESS.name())
                    .save();
        }
        return assembler.toResponse(prescription);
    }

    @Transactional
    public IdempotentHelper.Result<PrescriptionResponse> dispense(UUID prescriptionId,
                                                                  DispenseRequest request, CurrentUser actor,
                                                                  String idempotencyKey) {
        String path = "/api/v1/prescriptions/" + prescriptionId + "/dispense";
        String hash = idempotencyService.requestHash("dispensePrescription", path, request);
        IdempotencyResult idem = idempotencyService.begin(actor.id(), idempotencyKey,
                "dispensePrescription", hash);
        if (idem.isReplay()) {
            return new IdempotentHelper.Result<>(
                    assembler.toResponse(prescriptionRepository.findById(idem.resourceId()).orElseThrow()),
                    idem.responseStatus());
        }

        PrescriptionEntity prescription = lock(prescriptionId);
        PrescriptionStatus from = prescription.getStatus();
        if (from != PrescriptionStatus.PENDING && from != PrescriptionStatus.IN_PROGRESS) {
            throw new ConflictException(ErrorCode.INVALID_STATUS_TRANSITION,
                    "Cannot dispense a prescription in status " + from,
                    Map.of("currentStatus", from.name()));
        }

        List<PrescriptionItemEntity> items = itemRepository
                .findByPrescriptionIdOrderByLineNumber(prescriptionId);
        validateItemSet(request.items(), items);

        Map<UUID, PrescriptionItemEntity> itemsById = items.stream()
                .collect(Collectors.toMap(PrescriptionItemEntity::getId, Function.identity()));

        List<FieldError> errors = new ArrayList<>();
        List<DispensingItemEntity> dispensingItems = new ArrayList<>(request.items().size());
        boolean allFull = true;
        boolean allZero = true;

        for (int i = 0; i < request.items().size(); i++) {
            DispenseItemInput input = request.items().get(i);
            PrescriptionItemEntity prescribed = itemsById.get(input.prescriptionItemId());
            BigDecimal qty = input.quantityDispensed();
            BigDecimal prescribedQty = prescribed.getQuantity();

            if (qty.compareTo(prescribedQty) > 0) {
                errors.add(new FieldError("items[" + i + "].quantityDispensed", "Max",
                        "Quantity exceeds the prescribed quantity", qty));
                continue;
            }
            DispensedItemStatus status = qty.compareTo(prescribedQty) == 0 ? DispensedItemStatus.FULL
                    : qty.signum() == 0 ? DispensedItemStatus.NOT_DISPENSED : DispensedItemStatus.PARTIAL;
            if (status != DispensedItemStatus.FULL
                    && (input.note() == null || input.note().isBlank())) {
                errors.add(new FieldError("items[" + i + "].note", "NotBlank",
                        "A note is required when the quantity is below the prescribed amount"));
                continue;
            }
            if (status != DispensedItemStatus.FULL) {
                allFull = false;
            }
            if (qty.signum() != 0) {
                allZero = false;
            }
            dispensingItems.add(new DispensingItemEntity(UUID.randomUUID(), prescriptionId,
                    input.prescriptionItemId(), qty, status, input.note()));
        }

        if (!errors.isEmpty()) {
            ErrorCode code = errors.stream().allMatch(e -> e.code().equals("Max"))
                    ? ErrorCode.DISPENSE_QUANTITY_EXCEEDS_PRESCRIBED : ErrorCode.DISPENSE_NOTE_REQUIRED;
            throw new BusinessRuleException(code, "One or more dispensed items are invalid", errors);
        }
        if (allZero) {
            throw new BusinessRuleException(ErrorCode.NOTHING_DISPENSED,
                    "Nothing was dispensed; cancel the prescription instead");
        }

        DispensingOutcome outcome = allFull ? DispensingOutcome.FULL : DispensingOutcome.PARTIAL;
        DispensingEntity dispensing = new DispensingEntity(UUID.randomUUID(), prescriptionId, actor.id(),
                clock.now(), outcome, request.notes());
        dispensing.setItems(dispensingItems);
        dispensingRepository.saveAndFlush(dispensing);

        if (from == PrescriptionStatus.PENDING) {
            prescription.setStartedAt(clock.now());
            prescription.setStartedBy(actor.id());
        }
        prescription.setStatus(PrescriptionStatus.DISPENSED);
        prescription.setUpdatedAt(clock.now());
        prescriptionRepository.saveAndFlush(prescription);

        auditService.event(AuditAction.PRESCRIPTION_DISPENSED, EntityType.PRESCRIPTION)
                .actor(actor).entityId(prescription.getId()).patientId(prescription.getPatientId())
                .detail("fromStatus", from.name()).detail("toStatus", PrescriptionStatus.DISPENSED.name())
                .detail("outcome", outcome.name()).detail("dispensingId", dispensing.getId().toString())
                .save();

        idempotencyService.complete(idem.rowId(), prescription.getId(), 200);
        return new IdempotentHelper.Result<>(assembler.toResponse(prescription), 200);
    }

    private void validateItemSet(List<DispenseItemInput> inputs, List<PrescriptionItemEntity> items) {
        Set<UUID> required = items.stream().map(PrescriptionItemEntity::getId).collect(Collectors.toSet());
        Set<UUID> provided = new HashSet<>();
        for (DispenseItemInput input : inputs) {
            if (!provided.add(input.prescriptionItemId())) {
                throw new BusinessRuleException(ErrorCode.DISPENSE_ITEMS_MISMATCH,
                        "Each prescription item must appear exactly once");
            }
        }
        if (!provided.equals(required)) {
            throw new BusinessRuleException(ErrorCode.DISPENSE_ITEMS_MISMATCH,
                    "The dispensed items must match the prescription items exactly");
        }
    }

    private PrescriptionEntity lock(UUID prescriptionId) {
        return prescriptionRepository.findByIdForUpdate(prescriptionId)
                .orElseThrow(() -> new NotFoundException(ErrorCode.PRESCRIPTION_NOT_FOUND,
                        "Prescription not found"));
    }
}
