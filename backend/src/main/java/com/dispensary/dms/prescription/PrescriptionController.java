package com.dispensary.dms.prescription;

import com.dispensary.dms.common.idempotency.IdempotencyService;
import com.dispensary.dms.common.idempotency.IdempotentHelper;
import com.dispensary.dms.common.web.PageResponse;
import com.dispensary.dms.prescription.dto.AddPrescriptionRequest;
import com.dispensary.dms.prescription.dto.CancelPrescriptionRequest;
import com.dispensary.dms.prescription.dto.PrescriptionResponse;
import com.dispensary.dms.prescription.dto.PrescriptionSummary;
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
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping(path = "/api/v1", produces = MediaType.APPLICATION_JSON_VALUE)
@Validated
public class PrescriptionController {

    private final PrescriptionService prescriptionService;
    private final IdempotencyService idempotencyService;

    public PrescriptionController(PrescriptionService prescriptionService,
                                  IdempotencyService idempotencyService) {
        this.prescriptionService = prescriptionService;
        this.idempotencyService = idempotencyService;
    }

    @PostMapping(path = "/consultations/{consultationId}/prescriptions",
            consumes = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAnyRole('DOCTOR')")
    public ResponseEntity<PrescriptionResponse> addPrescriptionToConsultation(
            @PathVariable UUID consultationId,
            @RequestHeader("Idempotency-Key") String idempotencyKey,
            @Valid @RequestBody AddPrescriptionRequest request,
            CurrentUser principal) {
        idempotencyService.validateKeyFormat(idempotencyKey);
        IdempotentHelper.Result<PrescriptionResponse> result =
                prescriptionService.addToConsultation(consultationId, request, principal, idempotencyKey);
        return ResponseEntity.status(result.status())
                .location(URI.create("/api/v1/prescriptions/" + result.body().id()))
                .cacheControl(CacheControl.noStore())
                .body(result.body());
    }

    @GetMapping("/prescriptions")
    @PreAuthorize("hasAnyRole('PHARMACIST', 'DOCTOR')")
    public ResponseEntity<PageResponse<PrescriptionSummary>> listPrescriptions(
            @RequestParam(required = false) List<PrescriptionStatus> status,
            @RequestParam(required = false) UUID patientId,
            @RequestParam(required = false) String prescriptionNumber,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size,
            CurrentUser principal) {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore())
                .body(prescriptionService.list(status, patientId, prescriptionNumber, page, size, principal));
    }

    @GetMapping("/prescriptions/{prescriptionId}")
    @PreAuthorize("hasAnyRole('PHARMACIST', 'DOCTOR')")
    public ResponseEntity<PrescriptionResponse> getPrescription(@PathVariable UUID prescriptionId,
                                                               CurrentUser principal) {
        PrescriptionResponse response = prescriptionService.get(prescriptionId, principal);
        return ResponseEntity.ok()
                .eTag("\"" + response.id() + "-" + response.version() + "\"")
                .cacheControl(CacheControl.noStore())
                .body(response);
    }

    @PostMapping(path = "/prescriptions/{prescriptionId}/cancel",
            consumes = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAnyRole('DOCTOR', 'PHARMACIST')")
    public ResponseEntity<PrescriptionResponse> cancelPrescription(
            @PathVariable UUID prescriptionId,
            @Valid @RequestBody CancelPrescriptionRequest request,
            CurrentUser principal) {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore())
                .body(prescriptionService.cancel(prescriptionId, request, principal));
    }
}
