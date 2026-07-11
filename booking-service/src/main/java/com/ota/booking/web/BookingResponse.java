package com.ota.booking.web;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.ota.booking.domain.BookingStatus;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import lombok.Builder;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;

@Value
@Builder
@Jacksonized
public class BookingResponse {

    Long id;
    String idempotencyKey;
    Long hotelId;
    String hotelName;
    Long roomTypeId;
    String roomTypeName;
    LocalDate checkIn;
    LocalDate checkOut;
    String guestName;
    Integer guests;
    BookingStatus status;
    String reservationId;
    BigDecimal totalAmount;
    String currency;
    String reason;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ssXXX")
    OffsetDateTime createdAt;

    boolean idempotent;
}
