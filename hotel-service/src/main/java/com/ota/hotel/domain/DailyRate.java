package com.ota.hotel.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.math.BigDecimal;
import java.time.LocalDate;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Price and remaining inventory for one room type on one calendar date. The
 * (room_type_id, stay_date) uniqueness is the grain that makes syncs idempotent
 * and makes availability a simple range query.
 */
@Entity
@Table(name = "daily_rates",
        uniqueConstraints = @UniqueConstraint(name = "uk_rate_roomtype_date",
                columnNames = {"room_type_id", "stay_date"}))
@Getter
@Setter
@NoArgsConstructor
public class DailyRate {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "room_type_id", nullable = false)
    private RoomType roomType;

    @Column(name = "stay_date", nullable = false)
    private LocalDate stayDate;

    @Column(name = "price_amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal priceAmount;

    @Column(nullable = false, length = 3)
    private String currency;

    @Column(name = "available_rooms", nullable = false)
    private Integer availableRooms;
}
