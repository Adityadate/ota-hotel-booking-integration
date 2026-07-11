-- Booking ledger. MySQL dialect, runs unchanged on H2 in MODE=MySQL.
-- The unique idempotency key is what makes create-booking safe to retry.

CREATE TABLE IF NOT EXISTS bookings (
    id               BIGINT        NOT NULL AUTO_INCREMENT,
    idempotency_key  VARCHAR(128)  NOT NULL,
    hotel_id         BIGINT        NOT NULL,
    hotel_name       VARCHAR(255),
    room_type_id     BIGINT        NOT NULL,
    room_type_name   VARCHAR(255),
    check_in         DATE          NOT NULL,
    check_out        DATE          NOT NULL,
    guest_name       VARCHAR(255)  NOT NULL,
    guests           INT           NOT NULL,
    status           VARCHAR(32)   NOT NULL,
    reservation_id   VARCHAR(128),
    total_amount     DECIMAL(12,2),
    currency         VARCHAR(3),
    reason           VARCHAR(512),
    created_at       TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    CONSTRAINT uk_booking_idempotency UNIQUE (idempotency_key)
);

CREATE INDEX IF NOT EXISTS idx_booking_hotel ON bookings (hotel_id);
