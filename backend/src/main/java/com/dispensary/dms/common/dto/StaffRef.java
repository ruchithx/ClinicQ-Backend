package com.dispensary.dms.common.dto;

import java.util.UUID;

/** Minimal reference to a staff user. */
public record StaffRef(UUID id, String fullName) {
}
