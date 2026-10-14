package com.dispensary.dms.pharmacy;

import com.dispensary.dms.common.idempotency.IdempotencyService;
import com.dispensary.dms.common.idempotency.IdempotentHelper;
import com.dispensary.dms.pharmacy.dto.DispenseRequest;
import com.dispensary.dms.prescription.dto.PrescriptionResponse;
import com.dispensary.dms.security.CurrentUser;
import jakarta.validation.Valid;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping(path = "/api/v1/prescriptions", produces = MediaType.APPLICATION_JSON_VALUE)
public class DispensingController {

    private final DispensingService dispensingService;
    private final IdempotencyService idempotencyService;

    public DispensingController(DispensingService dispensingService,
                                IdempotencyService idempotencyService) {
        this.dispensingService = dispensingService;
        this.idempotencyService = idempotencyService;
    }

    @PostMapping("/{prescriptionId}/start")
    @PreAuthorize("hasAnyRole('PHARMACIST')")
    public ResponseEntity<PrescriptionResponse> startPrescription(@PathVariable UUID prescriptionId,
                                                                 CurrentUser principal) {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore())
                .body(dispensingService.start(prescriptionId, principal));
    }

    @PostMapping(path = "/{prescriptionId}/dispense", consumes = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAnyRole('PHARMACIST')")
    public ResponseEntity<PrescriptionResponse> dispensePrescription(
            @PathVariable UUID prescriptionId,
            @RequestHeader("Idempotency-Key") String idempotencyKey,
            @Valid @RequestBody DispenseRequest request,
            CurrentUser principal) {
        idempotencyService.validateKeyFormat(idempotencyKey);
        IdempotentHelper.Result<PrescriptionResponse> result =
                dispensingService.dispense(prescriptionId, request, principal, idempotencyKey);
        return ResponseEntity.status(result.status()).cacheControl(CacheControl.noStore())
                .body(result.body());
    }
}
