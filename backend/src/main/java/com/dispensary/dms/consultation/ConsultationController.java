package com.dispensary.dms.consultation;

import com.dispensary.dms.common.idempotency.IdempotencyService;
import com.dispensary.dms.common.idempotency.IdempotentHelper;
import com.dispensary.dms.consultation.dto.ConsultationResponse;
import com.dispensary.dms.consultation.dto.CreateConsultationRequest;
import com.dispensary.dms.security.CurrentUser;
import jakarta.validation.Valid;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.util.UUID;

@RestController
@RequestMapping(path = "/api/v1/consultations", produces = MediaType.APPLICATION_JSON_VALUE)
public class ConsultationController {

    private final ConsultationService consultationService;
    private final IdempotencyService idempotencyService;

    public ConsultationController(ConsultationService consultationService,
                                  IdempotencyService idempotencyService) {
        this.consultationService = consultationService;
        this.idempotencyService = idempotencyService;
    }

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAnyRole('DOCTOR', 'ADMIN')")
    public ResponseEntity<ConsultationResponse> createConsultation(
            @RequestHeader("Idempotency-Key") String idempotencyKey,
            @Valid @RequestBody CreateConsultationRequest request,
            CurrentUser principal) {
        idempotencyService.validateKeyFormat(idempotencyKey);
        IdempotentHelper.Result<ConsultationResponse> result =
                consultationService.create(request, principal, idempotencyKey);
        return ResponseEntity.status(result.status())
                .location(URI.create("/api/v1/consultations/" + result.body().id()))
                .cacheControl(CacheControl.noStore())
                .body(result.body());
    }

    @GetMapping("/{consultationId}")
    @PreAuthorize("hasAnyRole('DOCTOR', 'ADMIN')")
    public ResponseEntity<ConsultationResponse> getConsultation(@PathVariable UUID consultationId,
                                                               CurrentUser principal) {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore())
                .body(consultationService.get(consultationId, principal));
    }
}
