package com.dispensary.dms.medicine;

import com.dispensary.dms.audit.AuditAction;
import com.dispensary.dms.audit.AuditService;
import com.dispensary.dms.audit.EntityType;
import com.dispensary.dms.common.error.ApiException;
import com.dispensary.dms.common.error.ErrorCode;
import com.dispensary.dms.common.error.NotFoundException;
import com.dispensary.dms.common.time.BusinessClock;
import com.dispensary.dms.common.web.PageResponse;
import com.dispensary.dms.medicine.dto.CreateMedicineRequest;
import com.dispensary.dms.medicine.dto.MedicineResponse;
import com.dispensary.dms.medicine.dto.UpdateMedicineRequest;
import com.dispensary.dms.security.CurrentUser;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/** Medicine catalogue management (spec section 10.6, FR-033). */
@Service
public class MedicineService {

    private final MedicineRepository repository;
    private final MedicineMapper mapper;
    private final AuditService auditService;
    private final BusinessClock clock;

    public MedicineService(MedicineRepository repository, MedicineMapper mapper,
                           AuditService auditService, BusinessClock clock) {
        this.repository = repository;
        this.mapper = mapper;
        this.auditService = auditService;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public PageResponse<MedicineResponse> list(String search, Boolean active, int page, int size) {
        var result = repository.search(search, active,
                PageRequest.of(page, size, Sort.by("name").ascending()
                        .and(Sort.by("strength")).and(Sort.by("id"))));
        return PageResponse.of(result, mapper::toResponse);
    }

    @Transactional(readOnly = true)
    public MedicineResponse get(UUID medicineId) {
        return mapper.toResponse(require(medicineId));
    }

    @Transactional
    public MedicineResponse create(CreateMedicineRequest request, CurrentUser actor) {
        MedicineEntity medicine = new MedicineEntity();
        medicine.setId(UUID.randomUUID());
        medicine.setName(request.name());
        medicine.setStrength(request.strength());
        medicine.setDosageForm(request.dosageForm());
        medicine.setActive(true);
        medicine.setCreatedAt(clock.now());
        medicine.setUpdatedAt(clock.now());
        medicine.setCreatedBy(actor.id());
        medicine.setUpdatedBy(actor.id());
        repository.saveAndFlush(medicine);

        auditService.event(AuditAction.MEDICINE_CREATED, EntityType.MEDICINE)
                .actor(actor).entityId(medicine.getId())
                .detail("name", request.name())
                .detail("strength", request.strength())
                .detail("dosageForm", request.dosageForm().name())
                .save();
        return mapper.toResponse(medicine);
    }

    @Transactional
    public MedicineResponse update(UUID medicineId, UpdateMedicineRequest request, CurrentUser actor) {
        MedicineEntity medicine = require(medicineId);
        if (medicine.getVersion() != request.version()) {
            throw new ApiException(ErrorCode.CONCURRENT_MODIFICATION,
                    "The medicine was changed by someone else; reload and retry");
        }
        List<String> changed = new ArrayList<>();
        if (!medicine.getName().equals(request.name())) {
            changed.add("name");
        }
        if (!java.util.Objects.equals(medicine.getStrength(), request.strength())) {
            changed.add("strength");
        }
        if (medicine.getDosageForm() != request.dosageForm()) {
            changed.add("dosageForm");
        }
        if (medicine.isActive() != request.active()) {
            changed.add("active");
        }
        medicine.setName(request.name());
        medicine.setStrength(request.strength());
        medicine.setDosageForm(request.dosageForm());
        medicine.setActive(request.active());
        medicine.setUpdatedAt(clock.now());
        medicine.setUpdatedBy(actor.id());
        repository.saveAndFlush(medicine);

        auditService.event(AuditAction.MEDICINE_UPDATED, EntityType.MEDICINE)
                .actor(actor).entityId(medicine.getId())
                .detail("changedFields", changed)
                .detail("active", request.active())
                .save();
        return mapper.toResponse(medicine);
    }

    private MedicineEntity require(UUID medicineId) {
        return repository.findById(medicineId)
                .orElseThrow(() -> new NotFoundException(ErrorCode.MEDICINE_NOT_FOUND, "Medicine not found"));
    }
}
