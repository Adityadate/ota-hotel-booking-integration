package com.ota.common.api.hotel;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.ota.common.api.hotel.AvailabilityResponse.RoomTypeAvailability;
import com.ota.common.api.hotel.AvailabilityResponse.NightlyRate;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import lombok.Builder;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;

/**
 * Availability for a hotel across a date range. For each room type we report the
 * minimum rooms available over the stay (the binding constraint) and the summed
 * nightly price, plus the per-night breakdown.
 */
@Value
@Builder
@Jacksonized
public class AvailabilityResponse {

    Long hotelId;
    String hotelName;
    String city;

    @JsonFormat(pattern = "yyyy-MM-dd")
    LocalDate checkIn;

    @JsonFormat(pattern = "yyyy-MM-dd")
    LocalDate checkOut;

    List<RoomTypeAvailability> roomTypes;

    @Value
    @Builder
    @Jacksonized
    public static class RoomTypeAvailability {

        Long roomTypeId;
        String name;
        Integer capacity;
        Integer totalRooms;
        Integer availableRooms;
        BigDecimal totalPrice;
        String currency;
        List<NightlyRate> perNight;
    }

    @Value
    @Builder
    @Jacksonized
    public static class NightlyRate {

        @JsonFormat(pattern = "yyyy-MM-dd")
        LocalDate date;

        BigDecimal price;
        String currency;
        Integer availableRooms;
    }
}
