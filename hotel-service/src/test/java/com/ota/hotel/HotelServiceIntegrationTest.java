package com.ota.hotel;

import static org.assertj.core.api.Assertions.assertThat;

import com.ota.common.api.hotel.AvailabilityResponse;
import com.ota.common.api.hotel.HotelDetail;
import com.ota.common.api.hotel.HotelSummary;
import com.ota.common.api.hotel.HotelUpsertRequest;
import com.ota.common.api.hotel.ReserveRequest;
import com.ota.common.api.hotel.ReserveResponse;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("h2")
class HotelServiceIntegrationTest {

    @LocalServerPort
    int port;

    @Autowired
    TestRestTemplate rest;

    private String baseUrl() {
        return "http://localhost:" + port + "/api/v1/hotels";
    }

    private List<HotelUpsertRequest> samplePayload() {
        LocalDate today = LocalDate.now();
        List<HotelUpsertRequest.RateUpsertRequest> rates = List.of(
                rate(today.plusDays(1), new BigDecimal("120.00"), 5),
                rate(today.plusDays(2), new BigDecimal("120.00"), 5),
                rate(today.plusDays(3), new BigDecimal("150.00"), 5));
        HotelUpsertRequest.RoomTypeUpsertRequest roomType = HotelUpsertRequest.RoomTypeUpsertRequest
                .builder()
                .sourceRoomCode("STD")
                .name("Standard Double")
                .capacity(2)
                .totalRooms(10)
                .rates(rates)
                .build();
        return List.of(HotelUpsertRequest.builder()
                .source("TEST")
                .sourceHotelCode("H1")
                .name("Grand Test Hotel")
                .city("Paris")
                .country("FR")
                .starRating(4)
                .roomTypes(List.of(roomType))
                .build());
    }

    private HotelUpsertRequest.RateUpsertRequest rate(LocalDate d, BigDecimal p, int avail) {
        return HotelUpsertRequest.RateUpsertRequest.builder()
                .stayDate(d).priceAmount(p).currency("EUR").availableRooms(avail).build();
    }

    private <T> ResponseEntity<T> post(String url, Object body, Class<T> type) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        return rest.exchange(url, HttpMethod.POST, new HttpEntity<>(body, headers), type);
    }

    @Test
    void fullUpsertAvailabilityReserveFlow() {
        // 1. Upsert inventory
        ResponseEntity<String> upsert = post(baseUrl() + "/batch", samplePayload(), String.class);
        assertThat(upsert.getStatusCode()).isEqualTo(HttpStatus.OK);

        // 2. Search finds it
        ResponseEntity<HotelSummary[]> search = rest.getForEntity(baseUrl() + "?city=Paris", HotelSummary[].class);
        assertThat(search.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(search.getBody()).isNotNull().hasSize(1);
        Long hotelId = search.getBody()[0].getId();

        // 3. Detail
        ResponseEntity<HotelDetail> detail = rest.getForEntity(baseUrl() + "/" + hotelId, HotelDetail.class);
        assertThat(detail.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(detail.getBody().getRoomTypes()).isNotEmpty();

        // 4. Availability for the 3-night stay
        LocalDate in = LocalDate.now().plusDays(1);
        LocalDate out = LocalDate.now().plusDays(4);
        ResponseEntity<AvailabilityResponse> avail = rest.getForEntity(
                baseUrl() + "/" + hotelId + "/availability?checkIn=" + in + "&checkOut=" + out,
                AvailabilityResponse.class);
        assertThat(avail.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(avail.getBody().getRoomTypes()).hasSize(1);
        AvailabilityResponse.RoomTypeAvailability rta = avail.getBody().getRoomTypes().get(0);
        assertThat(rta.getAvailableRooms()).isEqualTo(5);
        assertThat(rta.getTotalPrice()).isEqualByComparingTo("390.00");
        assertThat(rta.getPerNight()).hasSize(3);

        Long roomTypeId = rta.getRoomTypeId();

        // 5. First reservation (2 of 5 rooms) -> CONFIRMED
        ReserveResponse first = post(baseUrl() + "/" + hotelId + "/room-types/" + roomTypeId + "/reserve",
                ReserveRequest.builder().checkIn(in).checkOut(out).rooms(2).build(),
                ReserveResponse.class).getBody();
        assertThat(first.getStatus()).isEqualTo("CONFIRMED");
        assertThat(first.getReservationId()).isNotBlank();

        // 6. Remaining inventory dropped to 3
        AvailabilityResponse.RoomTypeAvailability after = rest.getForEntity(
                baseUrl() + "/" + hotelId + "/availability?checkIn=" + in + "&checkOut=" + out,
                AvailabilityResponse.class).getBody().getRoomTypes().get(0);
        assertThat(after.getAvailableRooms()).isEqualTo(3);

        // 7. Try to reserve 4 (more than remaining 3) -> REJECTED, no overbook
        ReserveResponse rejected = post(baseUrl() + "/" + hotelId + "/room-types/" + roomTypeId + "/reserve",
                ReserveRequest.builder().checkIn(in).checkOut(out).rooms(4).build(),
                ReserveResponse.class).getBody();
        assertThat(rejected.getStatus()).isEqualTo("REJECTED");
    }
}
