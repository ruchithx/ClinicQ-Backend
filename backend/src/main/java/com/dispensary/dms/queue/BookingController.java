package com.dispensary.dms.queue;

import com.dispensary.dms.common.idempotency.IdempotencyService;
import com.dispensary.dms.common.idempotency.IdempotentHelper;
import com.dispensary.dms.common.web.PageResponse;
import com.dispensary.dms.queue.dto.CancelBookingRequest;
import com.dispensary.dms.queue.dto.CreateBookingRequest;
import com.dispensary.dms.queue.dto.QueueEntryResponse;
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
import java.util.UUID;

@RestController
@RequestMapping(path = "/api/v1", produces = MediaType.APPLICATION_JSON_VALUE)
@Validated
public class BookingController {

    private final QueueEntryService entryService;
    private final IdempotencyService idempotencyService;

    public BookingController(QueueEntryService entryService, IdempotencyService idempotencyService) {
        this.entryService = entryService;
        this.idempotencyService = idempotencyService;
    }

    @GetMapping("/queue-sessions/{queueSessionId}/entries")
    @PreAuthorize("hasAnyRole('DOCTOR', 'PHARMACIST', 'ADMIN')")
    public ResponseEntity<PageResponse<QueueEntryResponse>> listQueueEntries(
            @PathVariable UUID queueSessionId,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "50") @Min(1) @Max(100) int size) {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore())
                .body(entryService.listEntries(queueSessionId, page, size));
    }

    @PostMapping(path = "/queue-sessions/{queueSessionId}/entries", consumes = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAnyRole('DOCTOR', 'PHARMACIST', 'ADMIN')")
    public ResponseEntity<QueueEntryResponse> createBooking(
            @PathVariable UUID queueSessionId,
            @RequestHeader("Idempotency-Key") String idempotencyKey,
            @Valid @RequestBody CreateBookingRequest request,
            CurrentUser principal) {
        idempotencyService.validateKeyFormat(idempotencyKey);
        IdempotentHelper.Result<QueueEntryResponse> result =
                entryService.createBooking(queueSessionId, request, principal, idempotencyKey);
        return ResponseEntity.status(result.status())
                .location(URI.create("/api/v1/queue-entries/" + result.body().id()))
                .cacheControl(CacheControl.noStore()).body(result.body());
    }

    @PostMapping(path = "/queue-entries/{queueEntryId}/cancel", consumes = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAnyRole('DOCTOR', 'PHARMACIST', 'ADMIN')")
    public ResponseEntity<QueueEntryResponse> cancelBooking(@PathVariable UUID queueEntryId,
                                                           @Valid @RequestBody CancelBookingRequest request,
                                                           CurrentUser principal) {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore())
                .body(entryService.cancelBooking(queueEntryId, request, principal));
    }
}
