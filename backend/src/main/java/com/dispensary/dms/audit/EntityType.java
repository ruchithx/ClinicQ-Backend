package com.dispensary.dms.audit;

/** Affected entity type recorded in {@code audit_events.entity_type}. */
public enum EntityType {
    USER,
    SESSION,
    PATIENT,
    CONSULTATION,
    PRESCRIPTION,
    MEDICINE,
    QUEUE_SESSION,
    QUEUE_ENTRY,
    AUDIT
}
