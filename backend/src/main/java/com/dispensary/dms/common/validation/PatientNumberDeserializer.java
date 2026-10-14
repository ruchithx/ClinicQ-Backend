package com.dispensary.dms.common.validation;

import tools.jackson.core.JsonParser;
import tools.jackson.databind.DeserializationContext;
import tools.jackson.databind.ValueDeserializer;

import java.util.Locale;

/**
 * Trims and upper-cases a patient number before validation (spec section 8.3).
 */
public class PatientNumberDeserializer extends ValueDeserializer<String> {

    @Override
    public String deserialize(JsonParser p, DeserializationContext ctxt) {
        String raw = p.getValueAsString();
        if (raw == null) {
            return null;
        }
        String normalised = raw.trim().toUpperCase(Locale.ROOT);
        return normalised.isEmpty() ? null : normalised;
    }
}
