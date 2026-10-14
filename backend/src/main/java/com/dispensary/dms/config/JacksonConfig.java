package com.dispensary.dms.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import tools.jackson.core.JsonParser;
import tools.jackson.databind.DeserializationContext;
import tools.jackson.databind.JacksonModule;
import tools.jackson.databind.deser.std.StdScalarDeserializer;
import tools.jackson.databind.module.SimpleModule;

/**
 * Jackson customisation (spec section 8.3): trim every incoming string and convert blank optional
 * strings to {@code null}. Registered as a {@link JacksonModule} bean, which Spring Boot applies to
 * the primary {@code ObjectMapper}. Deterministic key ordering for canonical request hashing is set
 * via {@code spring.jackson.*} properties. (Spring Boot 4 uses Jackson 3 under {@code tools.jackson}.)
 */
@Configuration(proxyBeanMethods = false)
public class JacksonConfig {

    @Bean
    public JacksonModule dmsStringTrimmingModule() {
        SimpleModule module = new SimpleModule("dms-string-trimming");
        module.addDeserializer(String.class, new TrimmingStringDeserializer());
        return module;
    }

    /** Trims strings; a string that is blank after trimming becomes {@code null}. */
    static final class TrimmingStringDeserializer extends StdScalarDeserializer<String> {
        TrimmingStringDeserializer() {
            super(String.class);
        }

        @Override
        public String deserialize(JsonParser p, DeserializationContext ctxt) {
            String value = p.getValueAsString();
            if (value == null) {
                return null;
            }
            String trimmed = value.trim();
            return trimmed.isEmpty() ? null : trimmed;
        }
    }
}
