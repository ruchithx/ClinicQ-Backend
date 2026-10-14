package com.dispensary.dms.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Defines the {@code bearerAuth} and {@code refreshCookie} security schemes and the info block
 * (spec section 8.6).
 */
@Configuration(proxyBeanMethods = false)
public class OpenApiConfig {

    @Bean
    public OpenAPI dmsOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("Dispensary Management System API")
                        .version("1.0.0")
                        .description("Patient records, consultations, prescriptions, pharmacy dispensing, "
                                + "telephone bookings and a public queue display."))
                .components(new Components()
                        .addSecuritySchemes("bearerAuth", new SecurityScheme()
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT"))
                        .addSecuritySchemes("refreshCookie", new SecurityScheme()
                                .type(SecurityScheme.Type.APIKEY)
                                .in(SecurityScheme.In.COOKIE)
                                .name("dms_refresh")));
    }
}
