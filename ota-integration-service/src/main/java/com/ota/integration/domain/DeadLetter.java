package com.ota.integration.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.time.Instant;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Parking spot for a unit of work that failed all retries. The scheduled replayer
 * (and the manual replay endpoint) pick these up and try again.
 */
@Entity
@Table(name = "dead_letter")
@Getter
@Setter
@NoArgsConstructor
public class DeadLetter {

    public enum Status {
        PENDING, REPLAYING, FAILED
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 64)
    private String provider;

    @Column(name = "entity_type", length = 64)
    private String entityType;

    @Column(name = "entity_key", length = 128)
    private String entityKey;

    @Column(name = "error_message", length = 1024)
    private String errorMessage;

    @Column(nullable = false)
    private int attempts;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private Status status;

    @Column(name = "first_seen_at", nullable = false, updatable = false)
    private Instant firstSeenAt;

    @Column(name = "last_error_at", nullable = false)
    private Instant lastErrorAt;

    @PrePersist
    void onCreate() {
        Instant now = Instant.now();
        this.firstSeenAt = now;
        this.lastErrorAt = now;
    }

    @PreUpdate
    void onUpdate() {
        this.lastErrorAt = Instant.now();
    }
}
