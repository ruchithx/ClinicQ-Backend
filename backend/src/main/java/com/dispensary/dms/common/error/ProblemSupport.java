package com.dispensary.dms.common.error;

import com.dispensary.dms.common.time.BusinessClock;
import com.dispensary.dms.common.web.RequestContext;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.net.URI;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Builds RFC 9457 {@link ProblemDetail} instances (spec section 11) and can write them directly to
 * a servlet response (used by security filters and entry points that run outside the MVC advice).
 */
@Component
public class ProblemSupport {

    private final ObjectProvider<RequestContext> requestContext;
    private final BusinessClock clock;
    private final ObjectMapper objectMapper;

    public ProblemSupport(ObjectProvider<RequestContext> requestContext, BusinessClock clock,
                          ObjectMapper objectMapper) {
        this.requestContext = requestContext;
        this.clock = clock;
        this.objectMapper = objectMapper;
    }

    public ProblemDetail build(ErrorCode code, String detail, String instance,
                               Map<String, Object> context, List<FieldError> errors) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(code.status(), detail);
        problem.setType(URI.create(code.typeUri()));
        problem.setTitle(code.title());
        if (instance != null) {
            problem.setInstance(URI.create(instance));
        }
        problem.setProperty("code", code.name());
        problem.setProperty("requestId", currentRequestId());
        problem.setProperty("timestamp", clock.now().toString());
        if (errors != null && !errors.isEmpty()) {
            problem.setProperty("errors", errors);
        }
        if (context != null) {
            context.forEach(problem::setProperty);
        }
        return problem;
    }

    /** Writes a Problem JSON response directly (for filters / security entry points). */
    public void write(HttpServletResponse response, ErrorCode code, String detail, String instance,
                      Map<String, Object> context) throws IOException {
        response.setStatus(code.status().value());
        response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("type", code.typeUri());
        body.put("title", code.title());
        body.put("status", code.status().value());
        body.put("detail", detail);
        if (instance != null) {
            body.put("instance", instance);
        }
        body.put("code", code.name());
        body.put("requestId", currentRequestId());
        body.put("timestamp", Instant.now().toString());
        if (context != null) {
            body.putAll(context);
        }
        objectMapper.writeValue(response.getWriter(), body);
    }

    private String currentRequestId() {
        RequestContext ctx = requestContext.getIfAvailable();
        return ctx == null || ctx.getRequestId() == null ? "unknown" : ctx.getRequestId();
    }
}
