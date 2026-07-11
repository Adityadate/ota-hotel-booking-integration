package com.ota.booking.web;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.time.LocalDate;
import lombok.Builder;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;

@Value
@Builder
@Jacksonized
public class CreateBookingRequest {

    @NotNull
    Long hotelId;

    @NotNull
    Long roomTypeId;

    @NotNull
    @FutureOrPresent
    @JsonFormat(pattern = "yyyy-MM-dd")
    LocalDate checkIn;

    @NotNull
    @FutureOrPresent
    @JsonFormat(pattern = "yyyy-MM-dd")
    LocalDate checkOut;

    @NotBlank
    String guestName;

    @NotNull
    @Positive
    Integer guests;

    @NotBlank
    String idempotencyKey;
}
