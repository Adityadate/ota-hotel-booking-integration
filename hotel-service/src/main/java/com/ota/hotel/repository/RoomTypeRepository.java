package com.ota.hotel.repository;

import com.ota.hotel.domain.RoomType;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RoomTypeRepository extends JpaRepository<RoomType, Long> {

    Optional<RoomType> findByHotelIdAndSourceRoomCode(Long hotelId, String sourceRoomCode);

    List<RoomType> findByHotelId(Long hotelId);
}
