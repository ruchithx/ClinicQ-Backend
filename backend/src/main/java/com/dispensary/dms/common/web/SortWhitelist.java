package com.dispensary.dms.common.web;

import com.dispensary.dms.common.error.ApiException;
import com.dispensary.dms.common.error.ErrorCode;
import com.dispensary.dms.common.error.FieldError;
import org.springframework.data.domain.Sort;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Per-endpoint whitelist mapping a {@code field,direction} sort token to a safe {@link Sort},
 * always adding {@code id} as a stable tie-breaker (spec section 8.4). Unknown tokens -> 400.
 */
public final class SortWhitelist {

    private final Map<String, Sort> allowed = new LinkedHashMap<>();
    private final String defaultToken;

    private SortWhitelist(String defaultToken) {
        this.defaultToken = defaultToken;
    }

    public static SortWhitelist withDefault(String defaultToken) {
        return new SortWhitelist(defaultToken);
    }

    public SortWhitelist allow(String token, Sort sort) {
        allowed.put(token, sort.and(Sort.by("id")));
        return this;
    }

    public Sort resolve(String token) {
        String effective = (token == null || token.isBlank()) ? defaultToken : token;
        Sort sort = allowed.get(effective);
        if (sort == null) {
            throw new ApiException(ErrorCode.VALIDATION_FAILED, "Unsupported sort value",
                    Map.of(), List.of(new FieldError("sort", "Sort",
                    "Allowed values: " + String.join(", ", allowed.keySet()), effective)));
        }
        return sort;
    }
}
