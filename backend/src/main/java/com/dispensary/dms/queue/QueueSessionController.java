package com.dispensary.dms.queue;

import com.dispensary.dms.common.web.PageResponse;
import com.dispensary.dms.queue.dto.CallNextRequest;
import com.dispensary.dms.queue.dto.CallNextResponse;
import com.dispensary.dms.queue.dto.CloseQueueSessionRequest;
import com.dispensary.dms.queue.dto.CreateQueueSessionRequest;
import com.dispensary.dms.queue.dto.QueueSessionResponse;
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
import java.time.LocalDate;
import java.util.UUID;

@RestController
@RequestMapping(path = "/api/v1/queue-sessions", produces = MediaType.APPLICATION_JSON_VALUE)
@Validated
public class QueueSessionController {

    private final QueueSessionService sessionService;

    public QueueSessionController(QueueSessionService sessionService) {
        this.sessionService = sessionService;
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('DOCTOR', 'PHARMACIST', 'ADMIN')")
    public ResponseEntity<PageResponse<QueueSessionResponse>> listQueueSessions(
            @RequestParam(required = false) LocalDate date,
            @RequestParam(required = false) QueueSessionStatus status,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore())
                .body(sessionService.list(date, status, page, size));
    }

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAnyRole('DOCTOR', 'PHARMACIST', 'ADMIN')")
    public ResponseEntity<QueueSessionResponse> createQueueSession(
            @Valid @RequestBody CreateQueueSessionRequest request, CurrentUser principal) {
        QueueSessionResponse created = sessionService.create(request, principal);
        return ResponseEntity.created(URI.create("/api/v1/queue-sessions/" + created.id()))
                .cacheControl(CacheControl.noStore()).body(created);
    }

    @GetMapping("/{queueSessionId}")
    @PreAuthorize("hasAnyRole('DOCTOR', 'PHARMACIST', 'ADMIN')")
    public ResponseEntity<QueueSessionResponse> getQueueSession(
            @PathVariable UUID queueSessionId,
            @RequestHeader(name = "If-None-Match", required = false) String ifNoneMatch) {
        String etag = "W/\"" + queueSessionId + "-" + sessionService.versionOf(queueSessionId) + "\"";
        if (etag.equals(ifNoneMatch)) {
            return ResponseEntity.status(304).eTag(etag).cacheControl(CacheControl.noCache()).build();
        }
        return ResponseEntity.ok().eTag(etag).cacheControl(CacheControl.noCache())
                .body(sessionService.get(queueSessionId));
    }

    @PostMapping("/{queueSessionId}/open")
    @PreAuthorize("hasAnyRole('DOCTOR', 'PHARMACIST', 'ADMIN')")
    public ResponseEntity<QueueSessionResponse> openQueueSession(@PathVariable UUID queueSessionId,
                                                                CurrentUser principal) {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore())
                .body(sessionService.open(queueSessionId, principal));
    }

    @PostMapping(path = "/{queueSessionId}/close", consumes = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAnyRole('DOCTOR', 'PHARMACIST', 'ADMIN')")
    public ResponseEntity<QueueSessionResponse> closeQueueSession(
            @PathVariable UUID queueSessionId,
            @RequestBody(required = false) CloseQueueSessionRequest request,
            CurrentUser principal) {
        CloseQueueSessionRequest effective = request == null
                ? new CloseQueueSessionRequest(false) : request;
        return ResponseEntity.ok().cacheControl(CacheControl.noStore())
                .body(sessionService.close(queueSessionId, effective, principal));
    }

    @PostMapping(path = "/{queueSessionId}/call-next", consumes = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAnyRole('DOCTOR', 'ADMIN')")
    public ResponseEntity<CallNextResponse> callNextPatient(@PathVariable UUID queueSessionId,
                                                           @Valid @RequestBody CallNextRequest request,
                                                           CurrentUser principal) {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore())
                .body(sessionService.callNext(queueSessionId, request, principal));
    }
}
