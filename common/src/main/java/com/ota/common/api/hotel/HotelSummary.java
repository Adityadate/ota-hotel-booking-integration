package com.ota.common.api.hotel;

import lombok.Builder;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;

@Value
@Builder
@Jacksonized
public class HotelSummary {

    Long id;
    String name;
    String city;
    String country;
    Integer starRating;
}
