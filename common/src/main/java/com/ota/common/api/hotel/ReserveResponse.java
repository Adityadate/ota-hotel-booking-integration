package com.ota.common.api.hotel;

import lombok.Builder;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;

/**
 * Result of an atomic availability-check-and-reserve operation performed by the
 * hotel service. {@code status} is one of CONFIRMED / REJECTED.
 */
@Value
@Builder
@Jacksonized
public class ReserveResponse {

    String reservationId;
    String status;
    Integer availableRooms;
    String reason;
}
