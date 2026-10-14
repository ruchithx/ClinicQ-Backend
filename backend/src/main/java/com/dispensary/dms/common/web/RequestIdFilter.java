package com.dispensary.dms.common.web;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;
import java.util.regex.Pattern;

/**
 * Assigns an {@code X-Request-Id} to every request, populates the {@link RequestContext} and MDC,
 * and echoes the id on the response (spec section 8.5 / 12).
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class RequestIdFilter extends OncePerRequestFilter {

    private static final Pattern SAFE_REQUEST_ID = Pattern.compile("^[A-Za-z0-9._-]{1,64}$");
    private static final String HEADER = "X-Request-Id";

    private final RequestContext requestContext;

    public RequestIdFilter(RequestContext requestContext) {
        this.requestContext = requestContext;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String incoming = request.getHeader(HEADER);
        String requestId = (incoming != null && SAFE_REQUEST_ID.matcher(incoming).matches())
                ? incoming : UUID.randomUUID().toString();

        requestContext.setRequestId(requestId);
        requestContext.setIpAddress(request.getRemoteAddr());
        String ua = request.getHeader("User-Agent");
        if (StringUtils.hasText(ua)) {
            requestContext.setUserAgent(ua.length() > 255 ? ua.substring(0, 255) : ua);
        }

        MDC.put("requestId", requestId);
        response.setHeader(HEADER, requestId);
        try {
            filterChain.doFilter(request, response);
        } finally {
            MDC.remove("requestId");
            MDC.remove("userId");
            requestContext.clear();
        }
    }
}
