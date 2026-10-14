package com.dispensary.dms.common.error;

import com.dispensary.dms.audit.AuditService;
import com.dispensary.dms.security.CurrentUser;
import com.dispensary.dms.security.CurrentUserArgumentResolver;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authorization.AuthorizationDeniedException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.dao.CannotAcquireLockException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.dao.PessimisticLockingFailureException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.method.MethodValidationException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingRequestHeaderException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Translates exceptions into RFC 9457 Problem Details (spec section 11).
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);
    private static final Set<String> SENSITIVE_FIELDS = Set.of(
            "password", "currentPassword", "newPassword", "temporaryPassword",
            "clinicalNotes", "presentingComplaint", "conditionText", "knownAllergies",
            "notes", "note", "reason", "cancelReason", "skipReason");

    private final ProblemSupport problems;
    private final ConstraintErrorMapper constraintMapper;
    private final AuditService auditService;

    public GlobalExceptionHandler(ProblemSupport problems, ConstraintErrorMapper constraintMapper,
                                  AuditService auditService) {
        this.problems = problems;
        this.constraintMapper = constraintMapper;
        this.auditService = auditService;
    }

    @ExceptionHandler(ApiException.class)
    public ResponseEntity<ProblemDetail> handleApi(ApiException ex, HttpServletRequest request) {
        return respond(ex.code(), ex.getMessage(), request, ex.context(), ex.errors(),
                extraHeaders(ex));
    }

    @ExceptionHandler({AuthorizationDeniedException.class, AccessDeniedException.class})
    public ResponseEntity<ProblemDetail> handleAccessDenied(AccessDeniedException ex,
                                                           HttpServletRequest request) {
        String path = request.getRequestURI();
        if (path.contains("/patients") || path.contains("/consultations") || path.contains("/prescriptions")) {
            auditService.recordAccessDenied(currentUser(), request.getMethod(), path);
        }
        return respond(ErrorCode.FORBIDDEN, "You do not have permission to access this resource",
                request, null, null, null);
    }

    private CurrentUser currentUser() {
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication instanceof JwtAuthenticationToken jwtAuth) {
            return CurrentUserArgumentResolver.fromJwt(jwtAuth.getToken());
        }
        return null;
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ProblemDetail> handleBodyValidation(MethodArgumentNotValidException ex,
                                                              HttpServletRequest request) {
        List<FieldError> errors = new ArrayList<>();
        ex.getBindingResult().getFieldErrors().forEach(fe -> errors.add(new FieldError(
                fe.getField(), codeOf(fe.getCode()), fe.getDefaultMessage(),
                redact(fe.getField(), fe.getRejectedValue()))));
        ex.getBindingResult().getGlobalErrors().forEach(ge -> errors.add(new FieldError(
                ge.getObjectName(), codeOf(ge.getCode()), ge.getDefaultMessage())));
        return respond(ErrorCode.VALIDATION_FAILED, "Validation failed", request, null, errors, null);
    }

    @ExceptionHandler({HandlerMethodValidationException.class, MethodValidationException.class})
    public ResponseEntity<ProblemDetail> handleHandlerValidation(Exception ex, HttpServletRequest request) {
        return respond(ErrorCode.VALIDATION_FAILED, "Validation failed", request, null,
                List.of(new FieldError("request", "Validation", ex.getMessage())), null);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ProblemDetail> handleConstraintViolation(ConstraintViolationException ex,
                                                                   HttpServletRequest request) {
        List<FieldError> errors = new ArrayList<>();
        for (ConstraintViolation<?> v : ex.getConstraintViolations()) {
            String field = v.getPropertyPath().toString();
            errors.add(new FieldError(field, v.getConstraintDescriptor().getAnnotation()
                    .annotationType().getSimpleName(), v.getMessage(),
                    redact(field, v.getInvalidValue())));
        }
        return respond(ErrorCode.VALIDATION_FAILED, "Validation failed", request, null, errors, null);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ProblemDetail> handleUnreadable(HttpMessageNotReadableException ex,
                                                          HttpServletRequest request) {
        return respond(ErrorCode.MALFORMED_REQUEST, "Request body could not be read", request,
                null, null, null);
    }

    @ExceptionHandler(MissingRequestHeaderException.class)
    public ResponseEntity<ProblemDetail> handleMissingHeader(MissingRequestHeaderException ex,
                                                            HttpServletRequest request) {
        if ("Idempotency-Key".equalsIgnoreCase(ex.getHeaderName())) {
            return respond(ErrorCode.IDEMPOTENCY_KEY_REQUIRED, "Idempotency-Key header is required",
                    request, null, null, null);
        }
        return respond(ErrorCode.VALIDATION_FAILED, "Missing required header: " + ex.getHeaderName(),
                request, null, null, null);
    }

    @ExceptionHandler({NoResourceFoundException.class, HttpRequestMethodNotSupportedException.class})
    public ResponseEntity<ProblemDetail> handleNotFound(Exception ex, HttpServletRequest request) {
        return respond(ErrorCode.RESOURCE_NOT_FOUND, "Resource not found", request, null, null, null);
    }

    @ExceptionHandler(OptimisticLockingFailureException.class)
    public ResponseEntity<ProblemDetail> handleOptimistic(OptimisticLockingFailureException ex,
                                                          HttpServletRequest request) {
        return respond(ErrorCode.CONCURRENT_MODIFICATION, "Record changed by someone else; reload",
                request, null, null, null);
    }

    @ExceptionHandler({PessimisticLockingFailureException.class, CannotAcquireLockException.class})
    public ResponseEntity<ProblemDetail> handleLockTimeout(Exception ex, HttpServletRequest request) {
        return respond(ErrorCode.SERVICE_UNAVAILABLE, "The resource is busy; please retry", request,
                null, null, null);
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ProblemDetail> handleIntegrity(DataIntegrityViolationException ex,
                                                         HttpServletRequest request) {
        return constraintMapper.map(ex)
                .map(mapped -> respond(mapped.code(), mapped.getMessage(), request,
                        mapped.context(), mapped.errors(), null))
                .orElseGet(() -> {
                    log.error("Unmapped data integrity violation (constraint={})",
                            constraintMapper.constraintName(ex), ex);
                    return respond(ErrorCode.INTERNAL_ERROR, "Internal server error", request,
                            null, null, null);
                });
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ProblemDetail> handleUnexpected(Exception ex, HttpServletRequest request) {
        log.error("Unexpected error handling {} {}", request.getMethod(), request.getRequestURI(), ex);
        return respond(ErrorCode.INTERNAL_ERROR, "Internal server error", request, null, null, null);
    }

    private ResponseEntity<ProblemDetail> respond(ErrorCode code, String detail,
                                                  HttpServletRequest request,
                                                  java.util.Map<String, Object> context,
                                                  List<FieldError> errors,
                                                  java.util.Map<String, String> headers) {
        ProblemDetail problem = problems.build(code, detail, request.getRequestURI(), context, errors);
        ResponseEntity.BodyBuilder builder = ResponseEntity.status(code.status());
        if (headers != null) {
            headers.forEach(builder::header);
        }
        return builder.body(problem);
    }

    private java.util.Map<String, String> extraHeaders(ApiException ex) {
        if (ex.code() == ErrorCode.RATE_LIMITED && ex.context().get("retryAfterSeconds") != null) {
            return java.util.Map.of("Retry-After", String.valueOf(ex.context().get("retryAfterSeconds")));
        }
        return null;
    }

    private Object redact(String field, Object value) {
        if (value == null) {
            return null;
        }
        String leaf = field.contains(".") ? field.substring(field.lastIndexOf('.') + 1) : field;
        if (leaf.contains("[")) {
            leaf = leaf.substring(0, leaf.indexOf('['));
        }
        return SENSITIVE_FIELDS.contains(leaf) ? null : value;
    }

    private String codeOf(String springCode) {
        return springCode == null ? "Invalid" : springCode;
    }

    HttpStatus statusFor(ErrorCode code) {
        return code.status();
    }
}
