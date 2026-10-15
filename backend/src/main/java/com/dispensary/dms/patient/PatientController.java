package com.dispensary.dms.patient;

import com.dispensary.dms.common.idempotency.IdempotencyService;
import com.dispensary.dms.common.idempotency.IdempotentHelper;
import com.dispensary.dms.common.web.PageResponse;
import com.dispensary.dms.patient.dto.CreatePatientRequest;
import com.dispensary.dms.patient.dto.MedicalHistoryResponse;
import com.dispensary.dms.patient.dto.PatientResponse;
import com.dispensary.dms.patient.dto.PatientSummary;
import com.dispensary.dms.patient.dto.UpdatePatientRequest;
import com.dispensary.dms.security.CurrentUser;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.util.UUID;

@RestController
@RequestMapping(path = "/api/v1/patients", produces = MediaType.APPLICATION_JSON_VALUE)
@Validated
public class PatientController {

    private final PatientService patientService;
    private final MedicalHistoryService medicalHistoryService;
    private final IdempotencyService idempotencyService;

    public PatientController(PatientService patientService, MedicalHistoryService medicalHistoryService,
                             IdempotencyService idempotencyService) {
        this.patientService = patientService;
        this.medicalHistoryService = medicalHistoryService;
        this.idempotencyService = idempotencyService;
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('DOCTOR', 'ADMIN')")
    public ResponseEntity<PageResponse<PatientSummary>> searchPatients(
            @RequestParam String search,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size,
            CurrentUser principal) {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore())
                .body(patientService.search(search, page, size, principal));
    }

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAnyRole('DOCTOR', 'ADMIN')")
    public ResponseEntity<PatientResponse> createPatient(
            @RequestHeader("Idempotency-Key") String idempotencyKey,
            @Valid @RequestBody CreatePatientRequest request,
            CurrentUser principal) {
        idempotencyService.validateKeyFormat(idempotencyKey);
        IdempotentHelper.Result<PatientResponse> result =
                patientService.create(request, principal, idempotencyKey);
        return ResponseEntity.status(result.status())
                .location(URI.create("/api/v1/patients/" + result.body().id()))
                .cacheControl(CacheControl.noStore())
                .body(result.body());
    }

    @GetMapping("/by-number/{patientNumber}")
    @PreAuthorize("hasAnyRole('DOCTOR', 'ADMIN')")
    public ResponseEntity<PatientResponse> getPatientByNumber(@PathVariable String patientNumber,
                                                              CurrentUser principal) {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore())
                .body(patientService.getByNumber(patientNumber, principal));
    }

    @GetMapping("/{patientId}")
    @PreAuthorize("hasAnyRole('DOCTOR', 'ADMIN')")
    public ResponseEntity<PatientResponse> getPatient(@PathVariable UUID patientId,
                                                      CurrentUser principal) {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore())
                .body(patientService.getById(patientId, principal));
    }

    @PutMapping(path = "/{patientId}", consumes = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAnyRole('DOCTOR', 'ADMIN')")
    public ResponseEntity<PatientResponse> updatePatient(@PathVariable UUID patientId,
                                                         @Valid @RequestBody UpdatePatientRequest request,
                                                         CurrentUser principal) {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore())
                .body(patientService.update(patientId, request, principal));
    }

    @GetMapping("/{patientId}/medical-history")
    @PreAuthorize("hasAnyRole('DOCTOR', 'ADMIN')")
    public ResponseEntity<MedicalHistoryResponse> getMedicalHistory(
            @PathVariable UUID patientId,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size,
            CurrentUser principal) {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore())
                .body(medicalHistoryService.getMedicalHistory(patientId, page, size, principal));
    }
}
