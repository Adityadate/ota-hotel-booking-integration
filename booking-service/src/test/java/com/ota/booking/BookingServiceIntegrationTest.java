package com.ota.booking;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.ota.booking.client.HotelClient;
import com.ota.booking.web.BookingResponse;
import com.ota.booking.web.CreateBookingRequest;
import com.ota.common.api.hotel.AvailabilityResponse;
import com.ota.common.api.hotel.ReserveResponse;
import java.math.BigDecimal;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
@ActiveProfiles("h2")
class BookingServiceIntegrationTest {

    @LocalServerPort
    int port;

    @Autowired
    TestRestTemplate rest;

    @MockBean
    HotelClient hotelClient;

    private AvailabilityResponse availability(int available, int capacity, BigDecimal total) {
        AvailabilityResponse.RoomTypeAvailability rta = AvailabilityResponse.RoomTypeAvailability.builder()
                .roomTypeId(10L)
                .name("Standard")
                .capacity(capacity)
                .availableRooms(available)
                .totalPrice(total)
                .currency("EUR")
                .perNight(java.util.List.of())
                .build();
        return AvailabilityResponse.builder()
                .hotelId(1L)
                .hotelName("Test Hotel")
                .checkIn(LocalDate.now().plusDays(1))
                .checkOut(LocalDate.now().plusDays(3))
                .roomTypes(java.util.List.of(rta))
                .build();
    }

    private CreateBookingRequest request(String key, int guests) {
        return CreateBookingRequest.builder()
                .hotelId(1L)
                .roomTypeId(10L)
                .checkIn(LocalDate.now().plusDays(1))
                .checkOut(LocalDate.now().plusDays(3))
                .guestName("Alice")
                .guests(guests)
                .idempotencyKey(key)
                .build();
    }

    private BookingResponse postBooking(CreateBookingRequest req) {
        ResponseEntity<BookingResponse> r = rest.postForEntity(
                "http://localhost:" + port + "/api/v1/bookings", req, BookingResponse.class);
        assertThat(r.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        return r.getBody();
    }

    @Test
    void confirmsBookingThenReplaysIdempotently() {
        when(hotelClient.getAvailability(any(), any(), any())).thenReturn(availability(5, 2, new BigDecimal("300.00")));
        when(hotelClient.reserve(any(), any(), any()))
                .thenReturn(ReserveResponse.builder().status("CONFIRMED").reservationId("RES-1").availableRooms(4).build());

        BookingResponse created = postBooking(request("key-1", 2));
        assertThat(created.getStatus().name()).isEqualTo("CONFIRMED");
        assertThat(created.getReservationId()).isEqualTo("RES-1");
        assertThat(created.getTotalAmount()).isEqualByComparingTo("300.00");
        assertThat(created.isIdempotent()).isFalse();

        // Replay with same idempotency key -> returns the original, no new reservation.
        BookingResponse replay = postBooking(request("key-1", 2));
        assertThat(replay.isIdempotent()).isTrue();
        assertThat(replay.getId()).isEqualTo(created.getId());
        assertThat(replay.getReservationId()).isEqualTo("RES-1");
    }

    @Test
    void rejectsWhenNotEnoughInventory() {
        when(hotelClient.getAvailability(any(), any(), any())).thenReturn(availability(0, 2, new BigDecimal("300.00")));

        BookingResponse rejected = postBooking(request("key-2", 2));
        assertThat(rejected.getStatus().name()).isEqualTo("REJECTED");
        assertThat(rejected.getReason()).contains("Only 0");
    }
}
