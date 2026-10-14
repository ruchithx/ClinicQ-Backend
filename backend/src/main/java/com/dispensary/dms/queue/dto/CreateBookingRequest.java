package com.dispensary.dms.queue.dto;

import com.dispensary.dms.common.validation.PhoneNumberDeserializer;
import com.dispensary.dms.queue.BookingChannel;
import tools.jackson.databind.annotation.JsonDeserialize;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record CreateBookingRequest(
        @NotNull @Size(min = 1, max = 150) String patientName,
        @JsonDeserialize(using = PhoneNumberDeserializer.class)
        @Pattern(regexp = "^\\+?[0-9]{7,15}$") String contactPhone,
        @NotNull BookingChannel channel,
        @Size(max = 300) String notes,
        Boolean allowDuplicate) {

    public boolean allowDuplicateOrDefault() {
        return Boolean.TRUE.equals(allowDuplicate);
    }
}
