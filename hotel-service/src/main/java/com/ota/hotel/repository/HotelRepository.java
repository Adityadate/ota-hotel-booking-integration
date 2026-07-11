package com.ota.hotel.repository;

import com.ota.hotel.domain.Hotel;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface HotelRepository extends JpaRepository<Hotel, Long> {

    Optional<Hotel> findBySourceAndSourceHotelCode(String source, String sourceHotelCode);

    @Query("""
            select distinct h from Hotel h
            where (:city is null or lower(h.city) = lower(:city))
              and (:name is null or lower(h.name) like lower(concat('%', :name, '%')))
            order by h.name asc
            """)
    List<Hotel> search(@Param("city") String city, @Param("name") String name);
}
