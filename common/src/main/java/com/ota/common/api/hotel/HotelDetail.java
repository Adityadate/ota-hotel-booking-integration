package com.ota.common.api.hotel;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.ota.common.api.hotel.HotelDetail.RoomTypeDetail;
import com.ota.common.api.hotel.HotelDetail.NightlyRate;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import lombok.Builder;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;

@Value
@Builder
@Jacksonized
public class HotelDetail {

    Long id;
    String name;
    String city;
    String country;
    String address;
    Integer starRating;
    List<RoomTypeDetail> roomTypes;

    @Value
    @Builder
    @Jacksonized
    public static class RoomTypeDetail {

        Long roomTypeId;
        String name;
        Integer capacity;
        Integer totalRooms;
        List<NightlyRate> nextRates;
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
