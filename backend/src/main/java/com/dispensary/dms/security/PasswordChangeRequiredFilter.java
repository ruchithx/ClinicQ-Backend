package com.dispensary.dms.security;

import com.dispensary.dms.common.error.ErrorCode;
import com.dispensary.dms.common.error.ProblemSupport;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Blocks all endpoints except {@code GET /auth/me}, {@code POST /auth/change-password} and
 * {@code POST /auth/logout} while the token carries {@code pwd_change=true} (spec section 9.1).
 */
@Component
public class PasswordChangeRequiredFilter extends OncePerRequestFilter {

    private final ProblemSupport problems;

    public PasswordChangeRequiredFilter(ProblemSupport problems) {
        this.problems = problems;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication instanceof JwtAuthenticationToken jwtAuth
                && Boolean.TRUE.equals(jwtAuth.getToken().getClaim("pwd_change"))
                && !isAllowedWhilePasswordChangeRequired(request)) {
            problems.write(response, ErrorCode.PASSWORD_CHANGE_REQUIRED,
                    "You must change your password before continuing", request.getRequestURI(), null);
            return;
        }
        filterChain.doFilter(request, response);
    }

    private boolean isAllowedWhilePasswordChangeRequired(HttpServletRequest request) {
        String method = request.getMethod();
        String path = request.getRequestURI();
        return ("GET".equals(method) && "/api/v1/auth/me".equals(path))
                || ("POST".equals(method) && "/api/v1/auth/change-password".equals(path))
                || ("POST".equals(method) && "/api/v1/auth/logout".equals(path));
    }
}
