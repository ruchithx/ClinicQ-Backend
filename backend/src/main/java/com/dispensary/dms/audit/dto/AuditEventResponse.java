package com.dispensary.dms.audit.dto;

import com.dispensary.dms.audit.AuditAction;
import com.dispensary.dms.audit.AuditOutcome;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

public record AuditEventResponse(
        UUID id,
        Instant occurredAt,
        AuditActor actor,
        AuditAction action,
        String entityType,
        UUID entityId,
        UUID patientId,
        AuditOutcome outcome,
        Map<String, Object> details,
        String requestId,
        String ipAddress) {
}
