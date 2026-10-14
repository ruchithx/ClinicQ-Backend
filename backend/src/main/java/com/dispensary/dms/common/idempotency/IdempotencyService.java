package com.dispensary.dms.common.idempotency;

import com.dispensary.dms.common.error.BusinessRuleException;
import com.dispensary.dms.common.error.ErrorCode;
import com.dispensary.dms.common.time.BusinessClock;
import org.springframework.stereotype.Service;
import tools.jackson.databind.ObjectMapper;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.util.HexFormat;
import java.util.UUID;

/**
 * De-duplicates retried create requests within the same transaction as the business write
 * (spec section 7.3). Stores ids only, never response bodies.
 */
@Service
public class IdempotencyService {

    private static final Duration TTL = Duration.ofHours(24);
    private static final java.util.regex.Pattern KEY_FORMAT =
            java.util.regex.Pattern.compile("^[A-Za-z0-9_-]{8,100}$");

    /** Validates the Idempotency-Key header format (spec section 7.3); throws 400 if invalid. */
    public void validateKeyFormat(String key) {
        if (key == null || !KEY_FORMAT.matcher(key).matches()) {
            throw new com.dispensary.dms.common.error.ApiException(
                    com.dispensary.dms.common.error.ErrorCode.IDEMPOTENCY_KEY_REQUIRED,
                    "A valid Idempotency-Key header is required");
        }
    }

    private final IdempotencyKeyRepository repository;
    private final BusinessClock clock;
    private final ObjectMapper objectMapper;

    public IdempotencyService(IdempotencyKeyRepository repository, BusinessClock clock,
                              ObjectMapper objectMapper) {
        this.repository = repository;
        this.clock = clock;
        this.objectMapper = objectMapper;
    }

    /**
     * First statement of an idempotent operation. Inserts the key (or detects an existing one).
     */
    public IdempotencyResult begin(UUID userId, String idempotencyKey, String operation, String requestHash) {
        UUID rowId = UUID.randomUUID();
        var now = clock.now();
        int inserted = repository.insertIfAbsent(rowId, userId, idempotencyKey, operation,
                requestHash, now, now.plus(TTL));
        if (inserted == 1) {
            return IdempotencyResult.proceed(rowId);
        }
        // A row already exists (committed, or another transaction that has since committed).
        IdempotencyKeyEntity existing = repository.findForUpdate(userId, idempotencyKey)
                .orElseThrow(() -> new BusinessRuleException(ErrorCode.IDEMPOTENCY_KEY_REUSED,
                        "Idempotency key is being processed; retry shortly"));
        if (!existing.getOperation().equals(operation) || !existing.getRequestHash().equals(requestHash)) {
            throw new BusinessRuleException(ErrorCode.IDEMPOTENCY_KEY_REUSED,
                    "Idempotency key was already used for a different request");
        }
        if (existing.getResourceId() == null || existing.getResponseStatus() == null) {
            // The original transaction has not committed yet.
            throw new BusinessRuleException(ErrorCode.IDEMPOTENCY_KEY_REUSED,
                    "Idempotency key is being processed; retry shortly");
        }
        return IdempotencyResult.replay(existing.getResourceId(), existing.getResponseStatus());
    }

    /** Records the created resource id and status so a later retry can replay the response. */
    public void complete(UUID rowId, UUID resourceId, int status) {
        IdempotencyKeyEntity row = repository.findById(rowId).orElseThrow();
        row.setResourceId(resourceId);
        row.setResponseStatus((short) status);
        repository.save(row);
    }

    /** SHA-256 hex of {@code operationId + "\n" + path + "\n" + canonical JSON of the request}. */
    public String requestHash(String operationId, String path, Object requestDto) {
        try {
            String canonicalBody = requestDto == null ? "" : objectMapper.writeValueAsString(requestDto);
            String payload = operationId + "\n" + path + "\n" + canonicalBody;
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(payload.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("Failed to compute request hash", ex);
        }
    }
}
