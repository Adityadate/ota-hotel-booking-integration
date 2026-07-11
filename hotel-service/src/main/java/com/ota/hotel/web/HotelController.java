package com.ota.hotel.web;

import com.ota.common.api.hotel.AvailabilityResponse;
import com.ota.common.api.hotel.HotelDetail;
import com.ota.common.api.hotel.HotelSummary;
import com.ota.common.api.hotel.HotelUpsertRequest;
import com.ota.common.api.hotel.ReserveRequest;
import com.ota.common.api.hotel.ReserveResponse;
import com.ota.hotel.service.HotelService;
import jakarta.validation.Valid;
import java.time.LocalDate;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/hotels")
@RequiredArgsConstructor
public class HotelController {

    private final HotelService hotelService;

    @PostMapping("/batch")
    @ResponseStatus(HttpStatus.OK)
    public BatchUpsertResponse upsertBatch(@Valid @RequestBody List<HotelUpsertRequest> requests) {
        HotelService.UpsertSummary summary = hotelService.upsertHotels(requests);
        return new BatchUpsertResponse(summary.hotels(), summary.rates());
    }

    @GetMapping
    public List<HotelSummary> search(@RequestParam(required = false) String city,
                                     @RequestParam(required = false) String name) {
        return hotelService.search(city, name);
    }

    @GetMapping("/{id}")
    public HotelDetail get(@PathVariable Long id) {
        return hotelService.getHotel(id);
    }

    @GetMapping("/{id}/availability")
    public AvailabilityResponse availability(@PathVariable Long id,
                                             @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate checkIn,
                                             @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate checkOut) {
        return hotelService.availability(id, checkIn, checkOut);
    }

    @PostMapping("/{hotelId}/room-types/{roomTypeId}/reserve")
    public ReserveResponse reserve(@PathVariable Long hotelId,
                                   @PathVariable Long roomTypeId,
                                   @Valid @RequestBody ReserveRequest request) {
        return hotelService.reserve(roomTypeId, request);
    }

    public record BatchUpsertResponse(int hotels, int rates) {
    }
}
