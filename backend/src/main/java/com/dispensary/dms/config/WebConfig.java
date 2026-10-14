package com.dispensary.dms.config;

import com.dispensary.dms.security.CurrentUserArgumentResolver;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.List;

/**
 * CORS is configured only when {@code app.cors.allowed-origins} is non-empty (spec section 8.7).
 * Same-origin deployment through the reverse proxy / Next.js rewrites is the default. Also registers
 * the {@link CurrentUser} argument resolver.
 */
@Configuration(proxyBeanMethods = false)
public class WebConfig implements WebMvcConfigurer {

    private final CurrentUserArgumentResolver currentUserArgumentResolver;

    public WebConfig(CurrentUserArgumentResolver currentUserArgumentResolver) {
        this.currentUserArgumentResolver = currentUserArgumentResolver;
    }

    @Override
    public void addArgumentResolvers(List<HandlerMethodArgumentResolver> resolvers) {
        resolvers.add(currentUserArgumentResolver);
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource(AppProperties props) {
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        String origins = props.cors() == null ? null : props.cors().allowedOrigins();
        if (!StringUtils.hasText(origins)) {
            return source; // no CORS mappings => same-origin only
        }
        CorsConfiguration cfg = new CorsConfiguration();
        cfg.setAllowedOrigins(List.of(origins.split("\\s*,\\s*")));
        cfg.setAllowedMethods(List.of("GET", "POST", "PUT", "OPTIONS"));
        cfg.setAllowedHeaders(List.of("Authorization", "Content-Type", "Idempotency-Key",
                "X-Request-Id", "X-Requested-With", "If-None-Match"));
        cfg.setExposedHeaders(List.of("Location", "ETag", "X-Request-Id", "Retry-After"));
        cfg.setAllowCredentials(true);
        cfg.setMaxAge(3600L);
        source.registerCorsConfiguration("/**", cfg);
        return source;
    }
}
