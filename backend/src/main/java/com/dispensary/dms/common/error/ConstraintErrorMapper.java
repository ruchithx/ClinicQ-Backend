package com.dispensary.dms.common.error;

import org.hibernate.exception.ConstraintViolationException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Optional;

/**
 * Maps a database unique-constraint violation to an {@link ApiException} using the stable
 * constraint name (spec section 7.2).
 */
@Component
public class ConstraintErrorMapper {

    public Optional<ApiException> map(DataIntegrityViolationException ex) {
        String constraint = constraintName(ex);
        if (constraint == null) {
            return Optional.empty();
        }
        return switch (constraint) {
            case "uq_patients_patient_number" -> Optional.of(new ConflictException(
                    ErrorCode.DUPLICATE_PATIENT_NUMBER, "Patient number already exists"));
            case "uq_users_username" -> Optional.of(new ConflictException(
                    ErrorCode.DUPLICATE_USERNAME, "Username already exists"));
            case "uq_medicines_name_strength_form" -> Optional.of(new ConflictException(
                    ErrorCode.DUPLICATE_MEDICINE, "Medicine already exists"));
            case "uq_queue_sessions_date_name" -> Optional.of(new ConflictException(
                    ErrorCode.DUPLICATE_QUEUE_SESSION, "Queue session already exists"));
            case "uq_consultations_queue_entry" -> Optional.of(new ConflictException(
                    ErrorCode.QUEUE_ENTRY_ALREADY_LINKED, "Queue entry already linked to a consultation"));
            case "uq_prescriptions_active_per_consultation" -> Optional.of(new ConflictException(
                    ErrorCode.ACTIVE_PRESCRIPTION_EXISTS, "Consultation already has an active prescription"));
            case "uq_prescriptions_supersedes" -> Optional.of(new BusinessRuleException(
                    ErrorCode.INVALID_SUPERSEDED_PRESCRIPTION, "Prescription already superseded"));
            case "uq_dispensings_prescription", "uq_dispensing_items_item" -> Optional.of(
                    new ConflictException(ErrorCode.INVALID_STATUS_TRANSITION,
                            "Prescription already dispensed", Map.of("currentStatus", "DISPENSED")));
            case "uq_queue_entries_session_number" -> Optional.of(new ApiException(
                    ErrorCode.SERVICE_UNAVAILABLE, "Queue numbering conflict; retry"));
            default -> Optional.empty();
        };
    }

    public String constraintName(DataIntegrityViolationException ex) {
        Throwable cause = ex.getCause();
        if (cause instanceof ConstraintViolationException cve && cve.getConstraintName() != null) {
            return stripSchema(cve.getConstraintName());
        }
        return null;
    }

    private String stripSchema(String name) {
        int dot = name.lastIndexOf('.');
        return dot >= 0 ? name.substring(dot + 1) : name;
    }
}
