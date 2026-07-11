package com.ota.hotel.repository;

import com.ota.hotel.domain.DailyRate;
import jakarta.persistence.LockModeType;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface DailyRateRepository extends JpaRepository<DailyRate, Long> {

    Optional<DailyRate> findByRoomTypeIdAndStayDate(Long roomTypeId, LocalDate stayDate);

    @Query("""
            select r from DailyRate r
            where r.roomType.hotel.id = :hotelId
              and r.stayDate >= :from and r.stayDate < :to
            order by r.roomType.id asc, r.stayDate asc
            """)
    List<DailyRate> findForHotelInRange(@Param("hotelId") Long hotelId,
                                        @Param("from") LocalDate from,
                                        @Param("to") LocalDate to);

    @Query("""
            select r from DailyRate r
            where r.roomType.id = :roomTypeId
              and r.stayDate >= :from and r.stayDate < :to
            order by r.stayDate asc
            """)
    List<DailyRate> findForRoomTypeInRange(@Param("roomTypeId") Long roomTypeId,
                                           @Param("from") LocalDate from,
                                           @Param("to") LocalDate to);

    /**
     * Rows for one room type across a stay range, locked FOR UPDATE so a
     * concurrent reservation cannot oversell the same inventory. The transaction
     * that calls this holds the row locks until commit/rollback.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select r from DailyRate r
            where r.roomType.id = :roomTypeId
              and r.stayDate >= :from and r.stayDate < :to
            order by r.stayDate asc
            """)
    List<DailyRate> lockForReservation(@Param("roomTypeId") Long roomTypeId,
                                       @Param("from") LocalDate from,
                                       @Param("to") LocalDate to);

    @Query("""
            select r from DailyRate r
            where r.roomType.id = :roomTypeId and r.stayDate >= :from
            order by r.stayDate asc
            """)
    List<DailyRate> findUpcoming(@Param("roomTypeId") Long roomTypeId, @Param("from") LocalDate from);
}
