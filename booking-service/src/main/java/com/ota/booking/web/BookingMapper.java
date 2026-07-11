package com.ota.booking.web;

import com.ota.booking.domain.Booking;
import com.ota.booking.domain.BookingStatus;
import java.time.ZoneOffset;

public final class BookingMapper {

    private BookingMapper() {
    }

    public static BookingResponse toResponse(Booking b, boolean idempotent) {
        return BookingResponse.builder()
                .id(b.getId())
                .idempotencyKey(b.getIdempotencyKey())
                .hotelId(b.getHotelId())
                .hotelName(b.getHotelName())
                .roomTypeId(b.getRoomTypeId())
                .roomTypeName(b.getRoomTypeName())
                .checkIn(b.getCheckIn())
                .checkOut(b.getCheckOut())
                .guestName(b.getGuestName())
                .guests(b.getGuests())
                .status(b.getStatus())
                .reservationId(b.getReservationId())
                .totalAmount(b.getTotalAmount())
                .currency(b.getCurrency())
                .reason(b.getReason())
                .createdAt(b.getCreatedAt().atOffset(ZoneOffset.UTC))
                .idempotent(idempotent)
                .build();
    }

    public static BookingStatus fromReserveStatus(String status) {
        return "CONFIRMED".equalsIgnoreCase(status) ? BookingStatus.CONFIRMED : BookingStatus.REJECTED;
    }
}
