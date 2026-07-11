package com.ota.hotel.service;

import com.ota.common.api.hotel.AvailabilityResponse;
import com.ota.common.api.hotel.HotelDetail;
import com.ota.common.api.hotel.HotelSummary;
import com.ota.common.api.hotel.HotelUpsertRequest;
import com.ota.common.api.hotel.ReserveRequest;
import com.ota.common.api.hotel.ReserveResponse;
import com.ota.common.exception.Exceptions;
import com.ota.hotel.domain.DailyRate;
import com.ota.hotel.domain.Hotel;
import com.ota.hotel.domain.RoomType;
import com.ota.hotel.repository.DailyRateRepository;
import com.ota.hotel.repository.HotelRepository;
import com.ota.hotel.repository.RoomTypeRepository;
import com.ota.hotel.web.HotelMapper;
import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class HotelService {

    private final HotelRepository hotelRepository;
    private final RoomTypeRepository roomTypeRepository;
    private final DailyRateRepository dailyRateRepository;
    private final Clock clock;

    /**
     * Idempotent upsert of hotel inventory coming from an OTA provider. Each call
     * reconciles the supplied (hotel, room type, per-day rate) tuples against what
     * is already persisted, inserting or updating so repeated syncs converge.
     */
    @Transactional
    public UpsertSummary upsertHotels(List<HotelUpsertRequest> requests) {
        int hotels = 0;
        int rates = 0;
        for (HotelUpsertRequest req : requests) {
            Hotel hotel = hotelRepository
                    .findBySourceAndSourceHotelCode(req.getSource(), req.getSourceHotelCode())
                    .orElseGet(Hotel::new);
            hotel.setSource(req.getSource());
            hotel.setSourceHotelCode(req.getSourceHotelCode());
            hotel.setName(req.getName());
            hotel.setCity(req.getCity());
            hotel.setCountry(req.getCountry());
            hotel.setAddress(req.getAddress());
            hotel.setStarRating(req.getStarRating());
            hotel = hotelRepository.save(hotel);

            for (HotelUpsertRequest.RoomTypeUpsertRequest rtReq : req.getRoomTypes()) {
                RoomType rt = roomTypeRepository
                        .findByHotelIdAndSourceRoomCode(hotel.getId(), rtReq.getSourceRoomCode())
                        .orElseGet(RoomType::new);
                rt.setHotel(hotel);
                rt.setSourceRoomCode(rtReq.getSourceRoomCode());
                rt.setName(rtReq.getName());
                rt.setCapacity(rtReq.getCapacity());
                rt.setTotalRooms(rtReq.getTotalRooms());
                rt = roomTypeRepository.save(rt);

                for (HotelUpsertRequest.RateUpsertRequest rateReq : rtReq.getRates()) {
                    DailyRate rate = dailyRateRepository
                            .findByRoomTypeIdAndStayDate(rt.getId(), rateReq.getStayDate())
                            .orElseGet(DailyRate::new);
                    rate.setRoomType(rt);
                    rate.setStayDate(rateReq.getStayDate());
                    rate.setPriceAmount(rateReq.getPriceAmount());
                    rate.setCurrency(rateReq.getCurrency());
                    rate.setAvailableRooms(rateReq.getAvailableRooms());
                    dailyRateRepository.save(rate);
                    rates++;
                }
            }
            hotels++;
        }
        log.info("Upserted {} hotels / {} daily rate rows", hotels, rates);
        return new UpsertSummary(hotels, rates);
    }

    @Transactional(readOnly = true)
    public List<HotelSummary> search(String city, String name) {
        return hotelRepository.search(city, name).stream()
                .map(HotelMapper::toSummary)
                .toList();
    }

    @Transactional(readOnly = true)
    public HotelDetail getHotel(Long id) {
        Hotel hotel = hotelRepository.findById(id)
                .orElseThrow(() -> Exceptions.notFound("Hotel %s not found", id));
        LocalDate from = LocalDate.now(clock);
        List<HotelDetail.RoomTypeDetail> details = hotel.getRoomTypes().stream()
                .map(rt -> {
                    List<DailyRate> upcoming = dailyRateRepository.findUpcoming(rt.getId(), from);
                    List<DailyRate> next7 = upcoming.stream().limit(7).toList();
                    return HotelMapper.toDetail(rt, next7);
                })
                .toList();
        return HotelMapper.toDetail(hotel, details);
    }

    @Transactional(readOnly = true)
    public AvailabilityResponse availability(Long id, LocalDate checkIn, LocalDate checkOut) {
        if (checkIn == null || checkOut == null || !checkOut.isAfter(checkIn)) {
            throw Exceptions.badRequest("checkOut must be strictly after checkIn");
        }
        Hotel hotel = hotelRepository.findById(id)
                .orElseThrow(() -> Exceptions.notFound("Hotel %s not found", id));
        List<RoomTypeAvailabilityHolder> holders = hotel.getRoomTypes().stream()
                .map(rt -> {
                    List<DailyRate> rows = dailyRateRepository
                            .findForRoomTypeInRange(rt.getId(), checkIn, checkOut);
                    return new RoomTypeAvailabilityHolder(rt, rows);
                })
                .toList();

        List<AvailabilityResponse.RoomTypeAvailability> roomTypes = holders.stream()
                .map(h -> HotelMapper.toAvailability(h.roomType(), h.rates()))
                .toList();

        return AvailabilityResponse.builder()
                .hotelId(hotel.getId())
                .hotelName(hotel.getName())
                .city(hotel.getCity())
                .checkIn(checkIn)
                .checkOut(checkOut)
                .roomTypes(roomTypes)
                .build();
    }

    /**
     * Atomic check-and-reserve. Rows for the stay are locked pessimistically so two
     * concurrent bookings cannot sell the same room. The whole operation is one
     * database transaction => ACID. Returns CONFIRMED with a reservation id on
     * success, or REJECTED with a reason when inventory is insufficient.
     */
    @Transactional
    public ReserveResponse reserve(Long roomTypeId, ReserveRequest req) {
        LocalDate from = req.getCheckIn();
        LocalDate to = req.getCheckOut();
        if (from == null || to == null || !to.isAfter(from)) {
            throw Exceptions.badRequest("checkOut must be strictly after checkIn");
        }
        int nights = (int) from.until(to).getDays();
        List<DailyRate> rows = dailyRateRepository.lockForReservation(roomTypeId, from, to);
        if (rows.size() != nights) {
            return ReserveResponse.builder()
                    .status("REJECTED")
                    .reason("Inventory not loaded for the full stay range")
                    .build();
        }
        int minAvailable = rows.stream().mapToInt(DailyRate::getAvailableRooms).min().orElse(0);
        if (minAvailable < req.getRooms()) {
            return ReserveResponse.builder()
                    .status("REJECTED")
                    .availableRooms(minAvailable)
                    .reason("Only " + minAvailable + " room(s) available for the stay")
                    .build();
        }
        for (DailyRate row : rows) {
            row.setAvailableRooms(row.getAvailableRooms() - req.getRooms());
        }
        dailyRateRepository.saveAll(rows);
        String reservationId = UUID.randomUUID().toString();
        log.info("Reserved {} room(s) for roomType {} (reservation {})", req.getRooms(), roomTypeId, reservationId);
        return ReserveResponse.builder()
                .reservationId(reservationId)
                .status("CONFIRMED")
                .availableRooms(minAvailable - req.getRooms())
                .build();
    }

    public record UpsertSummary(int hotels, int rates) {
    }

    private record RoomTypeAvailabilityHolder(RoomType roomType, List<DailyRate> rates) {
    }
}
