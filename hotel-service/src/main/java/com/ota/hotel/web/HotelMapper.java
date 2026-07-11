package com.ota.hotel.web;

import com.ota.common.api.hotel.AvailabilityResponse;
import com.ota.common.api.hotel.AvailabilityResponse.RoomTypeAvailability;
import com.ota.common.api.hotel.HotelDetail;
import com.ota.common.api.hotel.HotelSummary;
import com.ota.hotel.domain.DailyRate;
import com.ota.hotel.domain.Hotel;
import com.ota.hotel.domain.RoomType;
import java.math.BigDecimal;
import java.util.List;

/** Pure functions that turn persistence entities into the shared API DTOs. */
public final class HotelMapper {

    private HotelMapper() {
    }

    public static HotelSummary toSummary(Hotel h) {
        return HotelSummary.builder()
                .id(h.getId())
                .name(h.getName())
                .city(h.getCity())
                .country(h.getCountry())
                .starRating(h.getStarRating())
                .build();
    }

    public static HotelDetail.RoomTypeDetail toDetail(RoomType rt, List<DailyRate> upcoming) {
        List<HotelDetail.NightlyRate> rates = upcoming.stream()
                .map(r -> HotelDetail.NightlyRate.builder()
                        .date(r.getStayDate())
                        .price(r.getPriceAmount())
                        .currency(r.getCurrency())
                        .availableRooms(r.getAvailableRooms())
                        .build())
                .toList();
        return HotelDetail.RoomTypeDetail.builder()
                .roomTypeId(rt.getId())
                .name(rt.getName())
                .capacity(rt.getCapacity())
                .totalRooms(rt.getTotalRooms())
                .nextRates(rates)
                .build();
    }

    public static HotelDetail toDetail(Hotel h, List<HotelDetail.RoomTypeDetail> roomTypes) {
        return HotelDetail.builder()
                .id(h.getId())
                .name(h.getName())
                .city(h.getCity())
                .country(h.getCountry())
                .address(h.getAddress())
                .starRating(h.getStarRating())
                .roomTypes(roomTypes)
                .build();
    }

    public static RoomTypeAvailability toAvailability(RoomType rt, List<DailyRate> rows) {
        List<AvailabilityResponse.NightlyRate> perNight = rows.stream()
                .map(r -> AvailabilityResponse.NightlyRate.builder()
                        .date(r.getStayDate())
                        .price(r.getPriceAmount())
                        .currency(r.getCurrency())
                        .availableRooms(r.getAvailableRooms())
                        .build())
                .toList();

        int available = rows.isEmpty() ? 0 : rows.stream().mapToInt(DailyRate::getAvailableRooms).min().orElse(0);
        BigDecimal total = rows.stream().map(DailyRate::getPriceAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
        String currency = rows.isEmpty() ? "USD" : rows.get(0).getCurrency();

        return RoomTypeAvailability.builder()
                .roomTypeId(rt.getId())
                .name(rt.getName())
                .capacity(rt.getCapacity())
                .totalRooms(rt.getTotalRooms())
                .availableRooms(available)
                .totalPrice(total)
                .currency(currency)
                .perNight(perNight)
                .build();
    }
}
