package com.dispensary.dms.security;

import com.dispensary.dms.common.error.ErrorCode;
import com.dispensary.dms.common.error.ProblemSupport;
import com.dispensary.dms.config.AppProperties;
import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * In-memory per-IP rate limiting (spec section 9.5): {@code login} for {@code POST /auth/login} and
 * {@code public} for {@code /api/v1/public/**}. Exceeded -> 429 RATE_LIMITED with Retry-After.
 */
@Component
public class RateLimitFilter extends OncePerRequestFilter {

    private final ProblemSupport problems;
    private final int loginPerMinute;
    private final int publicPerMinute;
    private final Map<String, BucketHolder> buckets = new ConcurrentHashMap<>();

    public RateLimitFilter(ProblemSupport problems, AppProperties props) {
        this.problems = problems;
        this.loginPerMinute = props.rateLimit().loginPerMinute();
        this.publicPerMinute = props.rateLimit().publicPerMinute();
    }

    private record BucketHolder(Bucket bucket, long lastAccessMillis) {
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String bucketName = bucketNameFor(request);
        if (bucketName != null) {
            String key = request.getRemoteAddr() + "|" + bucketName;
            int capacity = "login".equals(bucketName) ? loginPerMinute : publicPerMinute;
            Bucket bucket = buckets.compute(key, (k, existing) -> new BucketHolder(
                    existing != null ? existing.bucket() : newBucket(capacity),
                    System.currentTimeMillis())).bucket();
            if (!bucket.tryConsume(1)) {
                long retryAfter = 60;
                response.setHeader("Retry-After", String.valueOf(retryAfter));
                problems.write(response, ErrorCode.RATE_LIMITED, "Too many requests; slow down",
                        request.getRequestURI(), Map.of("retryAfterSeconds", retryAfter));
                return;
            }
        }
        filterChain.doFilter(request, response);
    }

    private String bucketNameFor(HttpServletRequest request) {
        String path = request.getRequestURI();
        if ("POST".equals(request.getMethod()) && "/api/v1/auth/login".equals(path)) {
            return "login";
        }
        if (path.startsWith("/api/v1/public/")) {
            return "public";
        }
        return null;
    }

    private Bucket newBucket(int perMinute) {
        return Bucket.builder()
                .addLimit(Bandwidth.builder().capacity(perMinute)
                        .refillGreedy(perMinute, Duration.ofMinutes(1)).build())
                .build();
    }

    /** Evicts buckets idle for over 10 minutes (spec section 9.5). */
    void evictIdleBuckets(long nowMillis) {
        buckets.entrySet().removeIf(e -> nowMillis - e.getValue().lastAccessMillis() > Duration.ofMinutes(10).toMillis());
    }
}
