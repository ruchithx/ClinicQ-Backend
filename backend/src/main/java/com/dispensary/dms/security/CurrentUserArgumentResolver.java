package com.dispensary.dms.security;

import com.dispensary.dms.user.Role;
import org.springframework.core.MethodParameter;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Resolves a {@link CurrentUser} controller parameter from the JWT authentication.
 */
@Component
public class CurrentUserArgumentResolver implements HandlerMethodArgumentResolver {

    @Override
    public boolean supportsParameter(MethodParameter parameter) {
        return parameter.getParameterType().equals(CurrentUser.class);
    }

    @Override
    public Object resolveArgument(MethodParameter parameter, ModelAndViewContainer mavContainer,
                                  NativeWebRequest webRequest, WebDataBinderFactory binderFactory) {
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        if (!(authentication instanceof JwtAuthenticationToken jwtAuth)) {
            return null;
        }
        return fromJwt(jwtAuth.getToken());
    }

    public static CurrentUser fromJwt(Jwt jwt) {
        UUID id = UUID.fromString(jwt.getSubject());
        List<String> roleNames = jwt.getClaimAsStringList("roles");
        Set<Role> roles = roleNames == null ? Set.of()
                : roleNames.stream().map(Role::valueOf).collect(Collectors.toUnmodifiableSet());
        Boolean pwdChange = jwt.getClaim("pwd_change");
        return new CurrentUser(id, jwt.getClaimAsString("username"), roles,
                Boolean.TRUE.equals(pwdChange));
    }
}
