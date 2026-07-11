package com.ota.booking.client;

import com.ota.common.api.hotel.AvailabilityResponse;
import com.ota.common.api.hotel.ReserveRequest;
import com.ota.common.api.hotel.ReserveResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * Declarative client to the hotel service. Dates are passed as ISO strings
 * because query parameters are not LocalDate-aware over the wire.
 */
@FeignClient(name = "hotel-service", url = "${hotel.service.url}")
public interface HotelClient {

    @GetMapping("/api/v1/hotels/{hotelId}/availability")
    AvailabilityResponse getAvailability(@PathVariable("hotelId") Long hotelId,
                                         @RequestParam("checkIn") String checkIn,
                                         @RequestParam("checkOut") String checkOut);

    @PostMapping("/api/v1/hotels/{hotelId}/room-types/{roomTypeId}/reserve")
    ReserveResponse reserve(@PathVariable("hotelId") Long hotelId,
                            @PathVariable("roomTypeId") Long roomTypeId,
                            @RequestBody ReserveRequest request);
}
