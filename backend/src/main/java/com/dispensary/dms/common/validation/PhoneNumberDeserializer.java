package com.dispensary.dms.common.validation;

import tools.jackson.core.JsonParser;
import tools.jackson.databind.DeserializationContext;
import tools.jackson.databind.ValueDeserializer;

/**
 * Normalises a phone number before validation: removes spaces, hyphens and parentheses
 * (spec section 8.3). Blank becomes {@code null}.
 */
public class PhoneNumberDeserializer extends ValueDeserializer<String> {

    @Override
    public String deserialize(JsonParser p, DeserializationContext ctxt) {
        String raw = p.getValueAsString();
        if (raw == null) {
            return null;
        }
        String normalised = raw.replaceAll("[\\s()\\-]", "");
        return normalised.isEmpty() ? null : normalised;
    }
}
