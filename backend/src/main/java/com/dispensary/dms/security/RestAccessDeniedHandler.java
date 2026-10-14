package com.dispensary.dms.security;

import com.dispensary.dms.audit.AuditService;
import com.dispensary.dms.common.error.ErrorCode;
import com.dispensary.dms.common.error.ProblemSupport;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * Writes {@code 403 FORBIDDEN} Problem Details and, for patient/consultation/prescription paths,
 * records an {@code ACCESS_DENIED} audit event with outcome DENIED (spec section 9.1).
 */
@Component
public class RestAccessDeniedHandler implements AccessDeniedHandler {

    private final ProblemSupport problems;
    private final ObjectProvider<AuditService> auditService;

    public RestAccessDeniedHandler(ProblemSupport problems, ObjectProvider<AuditService> auditService) {
        this.problems = problems;
        this.auditService = auditService;
    }

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response,
                       AccessDeniedException accessDeniedException) throws IOException {
        String path = request.getRequestURI();
        if (touchesProtectedData(path)) {
            AuditService audit = auditService.getIfAvailable();
            if (audit != null) {
                audit.recordAccessDenied(currentUser(), request.getMethod(), path);
            }
        }
        problems.write(response, ErrorCode.FORBIDDEN, "You do not have permission to access this resource",
                path, null);
    }

    private boolean touchesProtectedData(String path) {
        return path.contains("/patients") || path.contains("/consultations")
                || path.contains("/prescriptions");
    }

    private CurrentUser currentUser() {
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication instanceof JwtAuthenticationToken jwtAuth) {
            return CurrentUserArgumentResolver.fromJwt(jwtAuth.getToken());
        }
        return null;
    }
}
