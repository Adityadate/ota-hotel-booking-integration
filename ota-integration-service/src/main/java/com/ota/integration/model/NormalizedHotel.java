package com.ota.integration.model;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Provider-agnostic view of one hotel. Every adapter converts its own vendor
 * JSON into this shape so the rest of the pipeline only ever sees one schema.
 */
public record NormalizedHotel(
        String source,
        String sourceHotelCode,
        String name,
        String city,
        String country,
        String address,
        Integer starRating,
        List<NormalizedRoomType> roomTypes) {

    public record NormalizedRoomType(
            String sourceRoomCode,
            String name,
            int capacity,
            int totalRooms,
            List<NormalizedRate> rates) {
    }

    public record NormalizedRate(
            LocalDate stayDate,
            BigDecimal priceAmount,
            String currency,
            int availableRooms) {
    }
}
