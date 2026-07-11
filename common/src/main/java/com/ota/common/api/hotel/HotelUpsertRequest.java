package com.ota.common.api.hotel;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.ota.common.api.hotel.HotelUpsertRequest.RoomTypeUpsertRequest;
import com.ota.common.api.hotel.HotelUpsertRequest.RateUpsertRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import lombok.Builder;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;

/**
 * Idempotent upsert payload pushed by the OTA integration service into the hotel
 * service. One payload represents a single hotel (identified by the stable
 * {@code source} + {@code sourceHotelCode} pair) together with its room types and
 * daily rate/inventory rows.
 */
@Value
@Builder
@Jacksonized
public class HotelUpsertRequest {

    @NotBlank
    String source;

    @NotBlank
    String sourceHotelCode;

    @NotBlank
    String name;

    @NotBlank
    String city;

    String country;

    String address;

    Integer starRating;

    @NotEmpty
    @Valid
    List<RoomTypeUpsertRequest> roomTypes;

    @Value
    @Builder
    @Jacksonized
    public static class RoomTypeUpsertRequest {

        @NotBlank
        String sourceRoomCode;

        @NotBlank
        String name;

        @NotNull
        Integer capacity;

        @NotNull
        Integer totalRooms;

        @NotEmpty
        @Valid
        List<RateUpsertRequest> rates;
    }

    @Value
    @Builder
    @Jacksonized
    public static class RateUpsertRequest {

        @NotNull
        @JsonFormat(pattern = "yyyy-MM-dd")
        LocalDate stayDate;

        @NotNull
        BigDecimal priceAmount;

        @NotBlank
        String currency;

        @NotNull
        Integer availableRooms;
    }
}
