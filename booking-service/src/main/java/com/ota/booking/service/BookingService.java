package com.ota.booking.service;

import com.ota.booking.client.HotelClient;
import com.ota.booking.domain.Booking;
import com.ota.booking.domain.BookingStatus;
import com.ota.booking.repository.BookingRepository;
import com.ota.booking.web.BookingMapper;
import com.ota.booking.web.BookingResponse;
import com.ota.booking.web.CreateBookingRequest;
import com.ota.common.api.hotel.AvailabilityResponse;
import com.ota.common.api.hotel.ReserveRequest;
import com.ota.common.exception.Exceptions;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Orchestrates a booking: it checks availability with the hotel service, asks the
 * hotel service to atomically reserve the rooms, and records the outcome as an
 * ACID row in its own database. Idempotency is guaranteed by a unique key, so a
 * retried request returns the original result instead of double-booking.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class BookingService {

    private final BookingRepository bookingRepository;
    private final HotelClient hotelClient;

    @Transactional
    public BookingResponse createBooking(CreateBookingRequest req) {
        if (!req.getCheckOut().isAfter(req.getCheckIn())) {
            throw Exceptions.badRequest("checkOut must be strictly after checkIn");
        }

        // 1. Idempotency: a retried request returns the original outcome.
        Optional<Booking> existing = bookingRepository.findByIdempotencyKey(req.getIdempotencyKey());
        if (existing.isPresent()) {
            log.info("Idempotent replay for key {}", req.getIdempotencyKey());
            return BookingMapper.toResponse(existing.get(), true);
        }

        // 2. Availability (read) from the hotel service.
        AvailabilityResponse availability;
        try {
            availability = hotelClient.getAvailability(
                    req.getHotelId(), req.getCheckIn().toString(), req.getCheckOut().toString());
        } catch (Exception e) {
            throw Exceptions.upstreamUnavailable("hotel-service unavailable while checking availability", e);
        }

        AvailabilityResponse.RoomTypeAvailability rta = availability.getRoomTypes().stream()
                .filter(rt -> rt.getRoomTypeId().equals(req.getRoomTypeId()))
                .findFirst()
                .orElseThrow(() -> Exceptions.insufficientInventory(
                        "Room type %s is not available for this stay", req.getRoomTypeId()));

        int capacity = rta.getCapacity() != null ? rta.getCapacity() : 1;
        int rooms = (int) Math.ceil((double) req.getGuests() / capacity);

        // 3. Local guard: if not enough rooms, record a deterministic REJECTED row.
        if (rta.getAvailableRooms() == null || rta.getAvailableRooms() < rooms) {
            Booking rejected = newBooking(req, availability, rta);
            rejected.setStatus(BookingStatus.REJECTED);
            rejected.setReason("Only " + rta.getAvailableRooms() + " room(s) available, need " + rooms);
            return BookingMapper.toResponse(persist(rejected), false);
        }

        // 4. Reserve atomically in the hotel service (its own ACID transaction).
        ReserveRequest reserveRequest = ReserveRequest.builder()
                .checkIn(req.getCheckIn())
                .checkOut(req.getCheckOut())
                .rooms(rooms)
                .build();
        com.ota.common.api.hotel.ReserveResponse reserve;
        try {
            reserve = hotelClient.reserve(req.getHotelId(), req.getRoomTypeId(), reserveRequest);
        } catch (Exception e) {
            throw Exceptions.upstreamUnavailable("hotel-service unavailable while reserving", e);
        }

        Booking booking = newBooking(req, availability, rta);
        booking.setStatus("CONFIRMED".equalsIgnoreCase(reserve.getStatus())
                ? BookingStatus.CONFIRMED : BookingStatus.REJECTED);
        booking.setReservationId(reserve.getReservationId());
        booking.setReason(reserve.getReason());
        if (booking.getStatus() == BookingStatus.CONFIRMED && rta.getTotalPrice() != null) {
            booking.setTotalAmount(rta.getTotalPrice().multiply(BigDecimal.valueOf(rooms)));
        } else {
            booking.setTotalAmount(rta.getTotalPrice());
        }
        return BookingMapper.toResponse(persist(booking), false);
    }

    private Booking newBooking(CreateBookingRequest req, AvailabilityResponse availability,
                               AvailabilityResponse.RoomTypeAvailability rta) {
        Booking b = new Booking();
        b.setIdempotencyKey(req.getIdempotencyKey());
        b.setHotelId(req.getHotelId());
        b.setHotelName(availability.getHotelName());
        b.setRoomTypeId(req.getRoomTypeId());
        b.setRoomTypeName(rta.getName());
        b.setCheckIn(req.getCheckIn());
        b.setCheckOut(req.getCheckOut());
        b.setGuestName(req.getGuestName());
        b.setGuests(req.getGuests());
        b.setCurrency(rta.getCurrency());
        return b;
    }

    /**
     * Persist, tolerating a concurrent insert of the same idempotency key by
     * returning the row the other thread won the race to write.
     */
    private Booking persist(Booking booking) {
        try {
            return bookingRepository.save(booking);
        } catch (DataIntegrityViolationException ex) {
            return bookingRepository.findByIdempotencyKey(booking.getIdempotencyKey())
                    .orElseThrow(() -> ex);
        }
    }

    @Transactional(readOnly = true)
    public BookingResponse getBooking(Long id) {
        Booking b = bookingRepository.findById(id)
                .orElseThrow(() -> Exceptions.notFound("Booking %s not found", id));
        return BookingMapper.toResponse(b, false);
    }

    @Transactional(readOnly = true)
    public List<BookingResponse> list(Long hotelId) {
        List<Booking> bookings = hotelId == null
                ? bookingRepository.findAll()
                : bookingRepository.findByHotelId(hotelId);
        return bookings.stream().map(b -> BookingMapper.toResponse(b, false)).toList();
    }
}
