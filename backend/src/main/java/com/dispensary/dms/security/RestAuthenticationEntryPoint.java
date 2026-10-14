package com.dispensary.dms.security;

import com.dispensary.dms.common.error.ErrorCode;
import com.dispensary.dms.common.error.ProblemSupport;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.oauth2.server.resource.InvalidBearerTokenException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * Writes Problem Details for unauthenticated requests (spec section 9.1): distinguishes an expired
 * token ({@code TOKEN_EXPIRED}) from any other authentication failure ({@code UNAUTHENTICATED}).
 */
@Component
public class RestAuthenticationEntryPoint implements AuthenticationEntryPoint {

    private final ProblemSupport problems;

    public RestAuthenticationEntryPoint(ProblemSupport problems) {
        this.problems = problems;
    }

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response,
                         AuthenticationException authException) throws IOException {
        ErrorCode code = isExpired(authException) ? ErrorCode.TOKEN_EXPIRED : ErrorCode.UNAUTHENTICATED;
        String detail = code == ErrorCode.TOKEN_EXPIRED
                ? "The access token has expired" : "Authentication is required";
        problems.write(response, code, detail, request.getRequestURI(), null);
    }

    private boolean isExpired(AuthenticationException ex) {
        String msg = ex.getMessage();
        if (ex instanceof InvalidBearerTokenException && msg != null) {
            return msg.contains("expired") || msg.contains("Jwt expired");
        }
        return msg != null && msg.toLowerCase().contains("expired");
    }
}
