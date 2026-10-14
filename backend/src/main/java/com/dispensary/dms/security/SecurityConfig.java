package com.dispensary.dms.security;

import com.dispensary.dms.config.AppProperties;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authorization.AuthorizationDecision;
import org.springframework.security.authorization.AuthorizationManager;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.oauth2.server.resource.web.authentication.BearerTokenAuthenticationFilter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.intercept.AuthorizationFilter;
import org.springframework.security.web.authentication.www.BasicAuthenticationFilter;
import org.springframework.security.web.access.intercept.RequestAuthorizationContext;
import org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter;

import java.util.Collection;
import java.util.List;

/**
 * Stateless bearer-token security chain (spec section 9.1). Access tokens carry role claims;
 * method security ({@code @PreAuthorize}) enforces per-operation roles.
 */
@Configuration(proxyBeanMethods = false)
@EnableMethodSecurity
public class SecurityConfig {

    private final AppProperties props;

    public SecurityConfig(AppProperties props) {
        this.props = props;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        // DelegatingPasswordEncoder with BCrypt strength 12 as default (spec section 9.3).
        @SuppressWarnings("deprecation")
        PasswordEncoder bcrypt12 = new org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder(12);
        var encoders = new java.util.HashMap<String, PasswordEncoder>();
        encoders.put("bcrypt", bcrypt12);
        var delegating = new org.springframework.security.crypto.password.DelegatingPasswordEncoder("bcrypt", encoders);
        return delegating;
    }

    @Bean
    public SecurityFilterChain api(HttpSecurity http,
                                   org.springframework.security.oauth2.jwt.JwtDecoder jwtDecoder,
                                   com.dispensary.dms.common.web.RequestIdFilter requestIdFilter,
                                   RateLimitFilter rateLimitFilter,
                                   PasswordChangeRequiredFilter passwordChangeRequiredFilter,
                                   RestAuthenticationEntryPoint authenticationEntryPoint,
                                   RestAccessDeniedHandler accessDeniedHandler) throws Exception {
        return http
                .csrf(csrf -> csrf.disable())
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .cors(Customizer.withDefaults())
                .authorizeHttpRequests(a -> a
                        .requestMatchers(HttpMethod.POST, "/api/v1/auth/login", "/api/v1/auth/refresh",
                                "/api/v1/auth/logout").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/v1/public/**").permitAll()
                        .requestMatchers("/actuator/health", "/actuator/health/liveness",
                                "/actuator/health/readiness").permitAll()
                        .requestMatchers("/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html")
                        .access(swaggerEnabled())
                        .requestMatchers("/actuator/prometheus").access(privateNetworkOnly())
                        .requestMatchers("/api/v1/**").authenticated()
                        .anyRequest().denyAll())
                .oauth2ResourceServer(o -> o
                        .jwt(j -> j.decoder(jwtDecoder).jwtAuthenticationConverter(rolesConverter()))
                        .authenticationEntryPoint(authenticationEntryPoint))
                .exceptionHandling(e -> e.authenticationEntryPoint(authenticationEntryPoint)
                        .accessDeniedHandler(accessDeniedHandler))
                .addFilterBefore(requestIdFilter, org.springframework.security.web.context.SecurityContextHolderFilter.class)
                .addFilterBefore(rateLimitFilter, BearerTokenAuthenticationFilter.class)
                .addFilterAfter(passwordChangeRequiredFilter, AuthorizationFilter.class)
                .headers(h -> h.contentTypeOptions(Customizer.withDefaults())
                        .frameOptions(f -> f.deny())
                        .referrerPolicy(r -> r.policy(ReferrerPolicyHeaderWriter.ReferrerPolicy.NO_REFERRER)))
                .build();
    }

    private JwtAuthenticationConverter rolesConverter() {
        JwtGrantedAuthoritiesConverter delegate = new JwtGrantedAuthoritiesConverter();
        JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter((Jwt jwt) -> {
            List<String> roles = jwt.getClaimAsStringList("roles");
            if (roles == null) {
                return List.of();
            }
            Collection<GrantedAuthority> authorities = new java.util.ArrayList<>();
            for (String role : roles) {
                authorities.add(new SimpleGrantedAuthority("ROLE_" + role));
            }
            return authorities;
        });
        return converter;
    }

    private AuthorizationManager<RequestAuthorizationContext> swaggerEnabled() {
        return (authentication, context) -> new AuthorizationDecision(props.swagger().enabled());
    }

    private AuthorizationManager<RequestAuthorizationContext> privateNetworkOnly() {
        return (authentication, context) -> {
            HttpServletRequest request = context.getRequest();
            String ip = request.getRemoteAddr();
            return new AuthorizationDecision(isPrivateOrLoopback(ip));
        };
    }

    private boolean isPrivateOrLoopback(String ip) {
        if (ip == null) {
            return false;
        }
        try {
            var address = java.net.InetAddress.getByName(ip);
            return address.isLoopbackAddress() || address.isSiteLocalAddress()
                    || address.isAnyLocalAddress() || address.isLinkLocalAddress();
        } catch (java.net.UnknownHostException e) {
            return false;
        }
    }
}
