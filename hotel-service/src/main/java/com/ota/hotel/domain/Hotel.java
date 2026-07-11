package com.ota.hotel.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.persistence.Version;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * A hotel property. Uniquely identified for integration purposes by the
 * (source, sourceHotelCode) pair so repeated syncs from the same OTA upsert
 * rather than duplicate.
 */
@Entity
@Table(name = "hotels",
        uniqueConstraints = @UniqueConstraint(name = "uk_hotel_source",
                columnNames = {"source", "source_hotel_code"}))
@Getter
@Setter
@NoArgsConstructor
public class Hotel {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 64)
    private String source;

    @Column(name = "source_hotel_code", nullable = false, length = 128)
    private String sourceHotelCode;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false, length = 128)
    private String city;

    @Column(length = 128)
    private String country;

    @Column(length = 512)
    private String address;

    @Column(name = "star_rating")
    private Integer starRating;

    @OneToMany(mappedBy = "hotel")
    private List<RoomType> roomTypes = new ArrayList<>();

    @Version
    private Long version;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @PrePersist
    void onCreate() {
        Instant now = Instant.now();
        this.createdAt = now;
        this.updatedAt = now;
    }

    @PreUpdate
    void onUpdate() {
        this.updatedAt = Instant.now();
    }
}
