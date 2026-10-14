package com.dispensary.dms.audit.dto;

import java.util.UUID;

public record AuditActor(UUID id, String username) {
}
