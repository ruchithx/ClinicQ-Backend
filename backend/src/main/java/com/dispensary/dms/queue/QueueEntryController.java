package com.dispensary.dms.queue;

import com.dispensary.dms.queue.dto.QueueEntryResponse;
import com.dispensary.dms.queue.dto.SkipQueueEntryRequest;
import com.dispensary.dms.security.CurrentUser;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping(path = "/api/v1/queue-entries", produces = MediaType.APPLICATION_JSON_VALUE)
public class QueueEntryController {

    private final QueueEntryService entryService;

    public QueueEntryController(QueueEntryService entryService) {
        this.entryService = entryService;
    }

    @GetMapping("/{queueEntryId}")
    @PreAuthorize("hasAnyRole('DOCTOR', 'PHARMACIST', 'ADMIN')")
    public ResponseEntity<QueueEntryResponse> getQueueEntry(@PathVariable UUID queueEntryId) {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore())
                .body(entryService.getEntry(queueEntryId));
    }

    @PostMapping("/{queueEntryId}/call")
    @PreAuthorize("hasAnyRole('DOCTOR', 'ADMIN')")
    public ResponseEntity<QueueEntryResponse> callQueueEntry(@PathVariable UUID queueEntryId,
                                                            CurrentUser principal) {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore())
                .body(entryService.call(queueEntryId, principal));
    }

    @PostMapping("/{queueEntryId}/complete")
    @PreAuthorize("hasAnyRole('DOCTOR', 'ADMIN')")
    public ResponseEntity<QueueEntryResponse> completeQueueEntry(@PathVariable UUID queueEntryId,
                                                                CurrentUser principal) {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore())
                .body(entryService.complete(queueEntryId, principal));
    }

    @PostMapping(path = "/{queueEntryId}/skip", consumes = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAnyRole('DOCTOR', 'ADMIN')")
    public ResponseEntity<QueueEntryResponse> skipQueueEntry(
            @PathVariable UUID queueEntryId,
            @RequestBody(required = false) SkipQueueEntryRequest request,
            CurrentUser principal) {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore())
                .body(entryService.skip(queueEntryId, request, principal));
    }

    @PostMapping("/{queueEntryId}/requeue")
    @PreAuthorize("hasAnyRole('DOCTOR', 'PHARMACIST', 'ADMIN')")
    public ResponseEntity<QueueEntryResponse> requeueQueueEntry(@PathVariable UUID queueEntryId,
                                                               CurrentUser principal) {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore())
                .body(entryService.requeue(queueEntryId, principal));
    }
}
