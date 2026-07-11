package com.ota.integration.client;

import com.ota.common.api.hotel.HotelUpsertRequest;
import java.util.List;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

/** Declarative client to push normalized inventory into the hotel service. */
@FeignClient(name = "hotel-service", url = "${hotel.service.url}")
public interface HotelUpsertClient {

    @PostMapping("/api/v1/hotels/batch")
    void upsertHotels(@RequestBody List<HotelUpsertRequest> requests);
}
