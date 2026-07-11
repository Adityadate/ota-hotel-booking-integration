package com.ota.integration.service;

import com.ota.common.api.hotel.HotelUpsertRequest;
import com.ota.integration.client.HotelUpsertClient;
import com.ota.integration.model.NormalizedHotel;
import com.ota.integration.provider.ProviderAdapter;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Recover;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;

/**
 * The actual (retryable) hops to external systems. Kept in its own bean so the
 * @Retryable advice is applied through the Spring proxy — self-invocation would
 * bypass it. Each adapter fetch is retried with backoff; each push to the hotel
 * service is retried too.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ProviderCallService {

    private final HotelUpsertClient hotelClient;

    @Retryable(retryFor = Exception.class, maxAttempts = 3, backoff = @Backoff(delay = 400, multiplier = 1.5))
    public List<NormalizedHotel> fetch(ProviderAdapter adapter) {
        log.info("Fetching inventory from provider {}", adapter.getName());
        return adapter.fetchAndNormalize();
    }

    @Retryable(retryFor = Exception.class, maxAttempts = 2, backoff = @Backoff(delay = 300))
    public void push(NormalizedHotel hotel) {
        hotelClient.upsertHotels(List.of(toRequest(hotel)));
    }

    @Recover
    public List<NormalizedHotel> recoverFetch(Exception e, ProviderAdapter adapter) {
        throw new IllegalStateException("Provider " + adapter.getName() + " fetch failed after retries", e);
    }

    @Recover
    public void recoverPush(Exception e, NormalizedHotel hotel) {
        throw new IllegalStateException("Push to hotel-service failed for " + hotel.source() + "/"
                + hotel.sourceHotelCode() + " after retries", e);
    }

    private HotelUpsertRequest toRequest(NormalizedHotel h) {
        List<HotelUpsertRequest.RoomTypeUpsertRequest> roomTypes = h.roomTypes().stream().map(rt ->
                HotelUpsertRequest.RoomTypeUpsertRequest.builder()
                        .sourceRoomCode(rt.sourceRoomCode())
                        .name(rt.name())
                        .capacity(rt.capacity())
                        .totalRooms(rt.totalRooms())
                        .rates(rt.rates().stream().map(r ->
                                HotelUpsertRequest.RateUpsertRequest.builder()
                                        .stayDate(r.stayDate())
                                        .priceAmount(r.priceAmount())
                                        .currency(r.currency())
                                        .availableRooms(r.availableRooms())
                                        .build()).toList())
                        .build()).toList();
        return HotelUpsertRequest.builder()
                .source(h.source())
                .sourceHotelCode(h.sourceHotelCode())
                .name(h.name())
                .city(h.city())
                .country(h.country())
                .address(h.address())
                .starRating(h.starRating())
                .roomTypes(roomTypes)
                .build();
    }
}
