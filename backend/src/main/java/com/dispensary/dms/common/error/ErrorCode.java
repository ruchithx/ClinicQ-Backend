package com.dispensary.dms.common.error;

import org.springframework.http.HttpStatus;

/**
 * Stable machine-readable error codes with their HTTP status and stable title (spec section 11).
 */
public enum ErrorCode {
    MALFORMED_REQUEST(HttpStatus.BAD_REQUEST, "Malformed request"),
    VALIDATION_FAILED(HttpStatus.BAD_REQUEST, "Validation failed"),
    IDEMPOTENCY_KEY_REQUIRED(HttpStatus.BAD_REQUEST, "Idempotency key required"),
    UNAUTHENTICATED(HttpStatus.UNAUTHORIZED, "Authentication required"),
    TOKEN_EXPIRED(HttpStatus.UNAUTHORIZED, "Access token expired"),
    INVALID_CREDENTIALS(HttpStatus.UNAUTHORIZED, "Invalid credentials"),
    ACCOUNT_LOCKED(HttpStatus.UNAUTHORIZED, "Account temporarily locked"),
    ACCOUNT_DISABLED(HttpStatus.UNAUTHORIZED, "Account disabled"),
    REFRESH_TOKEN_INVALID(HttpStatus.UNAUTHORIZED, "Session expired"),
    REFRESH_TOKEN_ROTATED(HttpStatus.UNAUTHORIZED, "Refresh token already rotated"),
    SESSION_REVOKED(HttpStatus.UNAUTHORIZED, "Session revoked"),
    FORBIDDEN(HttpStatus.FORBIDDEN, "Access denied"),
    PASSWORD_CHANGE_REQUIRED(HttpStatus.FORBIDDEN, "Password change required"),
    NOT_RESOURCE_OWNER(HttpStatus.FORBIDDEN, "Not the owner of this resource"),
    RESOURCE_NOT_FOUND(HttpStatus.NOT_FOUND, "Resource not found"),
    PATIENT_NOT_FOUND(HttpStatus.NOT_FOUND, "Patient not found"),
    CONSULTATION_NOT_FOUND(HttpStatus.NOT_FOUND, "Consultation not found"),
    PRESCRIPTION_NOT_FOUND(HttpStatus.NOT_FOUND, "Prescription not found"),
    QUEUE_SESSION_NOT_FOUND(HttpStatus.NOT_FOUND, "Queue session not found"),
    QUEUE_ENTRY_NOT_FOUND(HttpStatus.NOT_FOUND, "Queue entry not found"),
    USER_NOT_FOUND(HttpStatus.NOT_FOUND, "User not found"),
    MEDICINE_NOT_FOUND(HttpStatus.NOT_FOUND, "Medicine not found"),
    DUPLICATE_PATIENT_NUMBER(HttpStatus.CONFLICT, "Patient number already exists"),
    DUPLICATE_USERNAME(HttpStatus.CONFLICT, "Username already exists"),
    DUPLICATE_MEDICINE(HttpStatus.CONFLICT, "Medicine already exists"),
    DUPLICATE_QUEUE_SESSION(HttpStatus.CONFLICT, "Queue session already exists"),
    DUPLICATE_BOOKING(HttpStatus.CONFLICT, "Possible duplicate booking"),
    CONCURRENT_MODIFICATION(HttpStatus.CONFLICT, "Record changed by someone else"),
    INVALID_STATUS_TRANSITION(HttpStatus.CONFLICT, "Invalid status transition"),
    QUEUE_STATE_CHANGED(HttpStatus.CONFLICT, "Queue changed"),
    QUEUE_SESSION_NOT_OPEN(HttpStatus.CONFLICT, "Queue session not open"),
    QUEUE_SESSION_CLOSED(HttpStatus.CONFLICT, "Queue session closed"),
    QUEUE_SESSION_FULL(HttpStatus.CONFLICT, "Queue session full"),
    QUEUE_SESSION_HAS_ACTIVE_ENTRIES(HttpStatus.CONFLICT, "Queue session has active entries"),
    QUEUE_ENTRY_ALREADY_LINKED(HttpStatus.CONFLICT, "Queue entry already linked"),
    ACTIVE_PRESCRIPTION_EXISTS(HttpStatus.CONFLICT, "Active prescription exists"),
    REFERENCED_RESOURCE_NOT_FOUND(HttpStatus.UNPROCESSABLE_ENTITY, "Referenced resource not found"),
    MEDICINE_INACTIVE(HttpStatus.UNPROCESSABLE_ENTITY, "Medicine is not available"),
    CONSULTATION_LOCKED(HttpStatus.UNPROCESSABLE_ENTITY, "Consultation locked"),
    INVALID_SUPERSEDED_PRESCRIPTION(HttpStatus.UNPROCESSABLE_ENTITY, "Invalid superseded prescription"),
    QUEUE_ENTRY_NOT_IN_SESSION(HttpStatus.UNPROCESSABLE_ENTITY, "Queue entry not in session"),
    DISPENSE_ITEMS_MISMATCH(HttpStatus.UNPROCESSABLE_ENTITY, "Dispense items do not match"),
    DISPENSE_QUANTITY_EXCEEDS_PRESCRIBED(HttpStatus.UNPROCESSABLE_ENTITY, "Quantity exceeds prescribed"),
    NOTHING_DISPENSED(HttpStatus.UNPROCESSABLE_ENTITY, "Nothing dispensed"),
    DISPENSE_NOTE_REQUIRED(HttpStatus.UNPROCESSABLE_ENTITY, "Note required"),
    INVALID_SESSION_DATE(HttpStatus.UNPROCESSABLE_ENTITY, "Invalid session date"),
    ASSIGNED_USER_NOT_DOCTOR(HttpStatus.UNPROCESSABLE_ENTITY, "Assigned user is not a doctor"),
    SELF_MODIFICATION_NOT_ALLOWED(HttpStatus.UNPROCESSABLE_ENTITY, "Cannot change own account this way"),
    LAST_ADMIN_REQUIRED(HttpStatus.UNPROCESSABLE_ENTITY, "At least one admin required"),
    CURRENT_PASSWORD_INCORRECT(HttpStatus.UNPROCESSABLE_ENTITY, "Current password incorrect"),
    PASSWORD_POLICY_VIOLATION(HttpStatus.UNPROCESSABLE_ENTITY, "Password does not meet policy"),
    IDEMPOTENCY_KEY_REUSED(HttpStatus.UNPROCESSABLE_ENTITY, "Idempotency key reused"),
    RATE_LIMITED(HttpStatus.TOO_MANY_REQUESTS, "Too many requests"),
    INTERNAL_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "Internal server error"),
    SERVICE_UNAVAILABLE(HttpStatus.SERVICE_UNAVAILABLE, "Service unavailable");

    private final HttpStatus status;
    private final String title;

    ErrorCode(HttpStatus status, String title) {
        this.status = status;
        this.title = title;
    }

    public HttpStatus status() {
        return status;
    }

    public String title() {
        return title;
    }

    /** {@code https://dms.example/problems/<code-in-kebab-case>}. */
    public String typeUri() {
        return "https://dms.example/problems/" + name().toLowerCase().replace('_', '-');
    }
}
