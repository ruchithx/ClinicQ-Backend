package com.dispensary.dms.patient;

import com.dispensary.dms.audit.AuditAction;
import com.dispensary.dms.audit.AuditService;
import com.dispensary.dms.audit.EntityType;
import com.dispensary.dms.common.error.ApiException;
import com.dispensary.dms.common.error.BusinessRuleException;
import com.dispensary.dms.common.error.ConflictException;
import com.dispensary.dms.common.error.ErrorCode;
import com.dispensary.dms.common.error.FieldError;
import com.dispensary.dms.common.error.NotFoundException;
import com.dispensary.dms.common.time.BusinessClock;
import com.dispensary.dms.common.web.PageResponse;
import com.dispensary.dms.common.idempotency.IdempotentHelper;
import com.dispensary.dms.patient.dto.CreatePatientRequest;
import com.dispensary.dms.patient.dto.PatientResponse;
import com.dispensary.dms.patient.dto.PatientSummary;
import com.dispensary.dms.patient.dto.UpdatePatientRequest;
import com.dispensary.dms.common.idempotency.IdempotencyResult;
import com.dispensary.dms.common.idempotency.IdempotencyService;
import com.dispensary.dms.security.CurrentUser;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

/** Patient registration, lookup, search and demographics (spec section 10.2). */
@Service
public class PatientService {

    private static final LocalDate MIN_DOB = LocalDate.of(1900, 1, 1);

    private final PatientRepository repository;
    private final PatientMapper mapper;
    private final IdempotencyService idempotencyService;
    private final AuditService auditService;
    private final BusinessClock clock;

    public PatientService(PatientRepository repository, PatientMapper mapper,
                          IdempotencyService idempotencyService, AuditService auditService,
                          BusinessClock clock) {
        this.repository = repository;
        this.mapper = mapper;
        this.idempotencyService = idempotencyService;
        this.auditService = auditService;
        this.clock = clock;
    }

    @Transactional
    public IdempotentHelper.Result<PatientResponse> create(CreatePatientRequest request,
                                                           CurrentUser actor, String idempotencyKey) {
        String hash = idempotencyService.requestHash("createPatient", "/api/v1/patients", request);
        IdempotencyResult idem = idempotencyService.begin(actor.id(), idempotencyKey, "createPatient", hash);
        if (idem.isReplay()) {
            return new IdempotentHelper.Result<>(get(idem.resourceId()), idem.responseStatus());
        }

        String number = request.patientNumber().trim().toUpperCase(Locale.ROOT);
        if (repository.existsByPatientNumber(number)) {
            UUID existingId = repository.findByPatientNumber(number).map(PatientEntity::getId).orElse(null);
            throw new ConflictException(ErrorCode.DUPLICATE_PATIENT_NUMBER,
                    "A patient with this patient number already exists",
                    existingId == null ? Map.of() : Map.of("existingPatientId", existingId.toString()));
        }

        DobResult dob = resolveDob(request.dateOfBirth(), request.estimatedAgeYears(), false);

        PatientEntity patient = new PatientEntity();
        patient.setId(UUID.randomUUID());
        patient.setPatientNumber(number);
        patient.setFullName(request.fullName());
        patient.setSex(request.sex());
        patient.setDateOfBirth(dob.dateOfBirth());
        patient.setDateOfBirthEstimated(dob.estimated());
        patient.setPhoneNumber(request.phoneNumber());
        patient.setAddress(request.address());
        patient.setKnownAllergies(request.knownAllergies());
        patient.setCreatedAt(clock.now());
        patient.setUpdatedAt(clock.now());
        patient.setCreatedBy(actor.id());
        patient.setUpdatedBy(actor.id());
        repository.saveAndFlush(patient);

        auditService.event(AuditAction.PATIENT_CREATED, EntityType.PATIENT)
                .actor(actor).entityId(patient.getId()).patientId(patient.getId())
                .detail("patientNumber", number).save();

        idempotencyService.complete(idem.rowId(), patient.getId(), 201);
        return new IdempotentHelper.Result<>(mapper.toResponse(patient), 201);
    }

    @Transactional
    public PatientResponse getById(UUID patientId, CurrentUser actor) {
        PatientEntity patient = require(patientId);
        auditService.event(AuditAction.PATIENT_VIEWED, EntityType.PATIENT)
                .actor(actor).entityId(patient.getId()).patientId(patient.getId())
                .detail("via", "ID").save();
        return mapper.toResponse(patient);
    }

    /** Internal read without audit (used when assembling other responses). */
    @Transactional(readOnly = true)
    public PatientResponse get(UUID patientId) {
        return mapper.toResponse(require(patientId));
    }

    @Transactional
    public PatientResponse getByNumber(String patientNumber, CurrentUser actor) {
        String number = patientNumber.trim().toUpperCase(Locale.ROOT);
        PatientEntity patient = repository.findByPatientNumber(number)
                .orElseThrow(() -> new NotFoundException(ErrorCode.PATIENT_NOT_FOUND,
                        "No patient with patient number " + number));
        auditService.event(AuditAction.PATIENT_VIEWED, EntityType.PATIENT)
                .actor(actor).entityId(patient.getId()).patientId(patient.getId())
                .detail("via", "NUMBER").save();
        return mapper.toResponse(patient);
    }

    @Transactional
    public PageResponse<PatientSummary> search(String search, int page, int size, CurrentUser actor) {
        String term = search == null ? "" : search.trim();
        if (term.length() < 2 || term.length() > 100) {
            throw new BusinessRuleException(ErrorCode.VALIDATION_FAILED, "Search term must be 2-100 characters",
                    List.of(new FieldError("search", "Size", "Search term must be 2-100 characters")));
        }
        String sanitized = term.replace("%", "").replace("_", "");
        String phone = term.replaceAll("[\\s()\\-]", "");
        var result = repository.search(sanitized, phone, PageRequest.of(page, size));
        PageResponse<PatientSummary> response = PageResponse.of(result, mapper::toSummary);
        auditService.event(AuditAction.PATIENT_SEARCHED, EntityType.PATIENT)
                .actor(actor).detail("search", term).detail("resultCount", result.getTotalElements()).save();
        return response;
    }

    @Transactional
    public PatientResponse update(UUID patientId, UpdatePatientRequest request, CurrentUser actor) {
        PatientEntity patient = require(patientId);
        if (patient.getVersion() != request.version()) {
            throw new ApiException(ErrorCode.CONCURRENT_MODIFICATION,
                    "The patient was changed by someone else; reload and retry");
        }
        DobResult dob = resolveDob(request.dateOfBirth(), request.estimatedAgeYears(),
                request.dateOfBirthEstimated() != null && request.dateOfBirthEstimated());

        List<String> changed = new ArrayList<>();
        if (!patient.getFullName().equals(request.fullName())) {
            changed.add("fullName");
        }
        if (patient.getSex() != request.sex()) {
            changed.add("sex");
        }
        if (!patient.getDateOfBirth().equals(dob.dateOfBirth())
                || patient.isDateOfBirthEstimated() != dob.estimated()) {
            changed.add("dateOfBirth");
        }
        if (!java.util.Objects.equals(patient.getPhoneNumber(), request.phoneNumber())) {
            changed.add("phoneNumber");
        }
        if (!java.util.Objects.equals(patient.getAddress(), request.address())) {
            changed.add("address");
        }
        if (!java.util.Objects.equals(patient.getKnownAllergies(), request.knownAllergies())) {
            changed.add("knownAllergies");
        }

        patient.setFullName(request.fullName());
        patient.setSex(request.sex());
        patient.setDateOfBirth(dob.dateOfBirth());
        patient.setDateOfBirthEstimated(dob.estimated());
        patient.setPhoneNumber(request.phoneNumber());
        patient.setAddress(request.address());
        patient.setKnownAllergies(request.knownAllergies());
        patient.setUpdatedAt(clock.now());
        patient.setUpdatedBy(actor.id());
        repository.saveAndFlush(patient);

        auditService.event(AuditAction.PATIENT_UPDATED, EntityType.PATIENT)
                .actor(actor).entityId(patient.getId()).patientId(patient.getId())
                .detail("changedFields", changed).save();
        return mapper.toResponse(patient);
    }

    PatientEntity require(UUID patientId) {
        return repository.findById(patientId)
                .orElseThrow(() -> new NotFoundException(ErrorCode.PATIENT_NOT_FOUND, "Patient not found"));
    }

    private record DobResult(LocalDate dateOfBirth, boolean estimated) {
    }

    private DobResult resolveDob(LocalDate dateOfBirth, Integer estimatedAgeYears, boolean estimatedFlag) {
        LocalDate dob;
        boolean estimated;
        if (estimatedAgeYears != null) {
            dob = clock.today().minusYears(estimatedAgeYears);
            estimated = true;
        } else {
            dob = dateOfBirth;
            estimated = estimatedFlag;
        }
        if (dob == null || dob.isBefore(MIN_DOB) || dob.isAfter(clock.today())) {
            throw new BusinessRuleException(ErrorCode.VALIDATION_FAILED,
                    "Date of birth must be between 1900-01-01 and today",
                    List.of(new FieldError("dateOfBirth", "Range",
                            "Date of birth must be between 1900-01-01 and today")));
        }
        return new DobResult(dob, estimated);
    }
}
