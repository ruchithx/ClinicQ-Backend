package com.dispensary.dms.common.web;

import org.springframework.stereotype.Component;

/**
 * Per-request holder for the request id, client IP and truncated user agent. Backed by thread-locals
 * (populated by {@link RequestIdFilter} at the start of the servlet filter chain and cleared at the
 * end) rather than a request-scoped bean, because filters run before Spring binds request-scope.
 */
@Component
public class RequestContext {

    private final ThreadLocal<String> requestId = new ThreadLocal<>();
    private final ThreadLocal<String> ipAddress = new ThreadLocal<>();
    private final ThreadLocal<String> userAgent = new ThreadLocal<>();

    public String getRequestId() {
        return requestId.get();
    }

    public void setRequestId(String value) {
        requestId.set(value);
    }

    public String getIpAddress() {
        return ipAddress.get();
    }

    public void setIpAddress(String value) {
        ipAddress.set(value);
    }

    public String getUserAgent() {
        return userAgent.get();
    }

    public void setUserAgent(String value) {
        userAgent.set(value);
    }

    /** Must be called at the end of each request to avoid leaking state across pooled threads. */
    public void clear() {
        requestId.remove();
        ipAddress.remove();
        userAgent.remove();
    }
}
