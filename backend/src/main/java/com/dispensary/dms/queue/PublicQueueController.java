package com.dispensary.dms.queue;

import com.dispensary.dms.queue.dto.PublicQueueSessionList;
import com.dispensary.dms.queue.dto.PublicQueueStatus;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/** Public, unauthenticated queue display endpoints (spec section 8.1 / 10.7). */
@RestController
@RequestMapping(path = "/api/v1/public/queue-sessions", produces = MediaType.APPLICATION_JSON_VALUE)
public class PublicQueueController {

    private final PublicQueueService publicQueueService;

    public PublicQueueController(PublicQueueService publicQueueService) {
        this.publicQueueService = publicQueueService;
    }

    @GetMapping
    public ResponseEntity<PublicQueueSessionList> listPublicQueueSessions() {
        return ResponseEntity.ok().cacheControl(CacheControl.noCache())
                .body(publicQueueService.listToday());
    }

    @GetMapping("/{queueSessionId}")
    public ResponseEntity<PublicQueueStatus> getPublicQueueStatus(
            @PathVariable UUID queueSessionId,
            @RequestHeader(name = "If-None-Match", required = false) String ifNoneMatch) {
        String etag = "W/\"" + queueSessionId + "-" + publicQueueService.versionOf(queueSessionId) + "\"";
        if (etag.equals(ifNoneMatch)) {
            return ResponseEntity.status(304).eTag(etag).cacheControl(CacheControl.noCache()).build();
        }
        return ResponseEntity.ok().eTag(etag).cacheControl(CacheControl.noCache())
                .body(publicQueueService.getStatus(queueSessionId));
    }
}
