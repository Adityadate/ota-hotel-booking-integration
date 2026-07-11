-- Hotel inventory schema. Written in MySQL dialect; runs unchanged on H2 in
-- MODE=MySQL for local development and tests. Flyway applies this idempotently.

CREATE TABLE IF NOT EXISTS hotels (
    id                BIGINT       NOT NULL AUTO_INCREMENT,
    source            VARCHAR(64)  NOT NULL,
    source_hotel_code VARCHAR(128) NOT NULL,
    name              VARCHAR(255) NOT NULL,
    city              VARCHAR(128) NOT NULL,
    country           VARCHAR(128),
    address           VARCHAR(512),
    star_rating       INT,
    version           BIGINT       NOT NULL DEFAULT 0,
    created_at        TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at        TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    CONSTRAINT uk_hotel_source UNIQUE (source, source_hotel_code)
);

CREATE TABLE IF NOT EXISTS room_types (
    id               BIGINT      NOT NULL AUTO_INCREMENT,
    hotel_id         BIGINT      NOT NULL,
    source_room_code VARCHAR(128) NOT NULL,
    name             VARCHAR(255) NOT NULL,
    capacity         INT         NOT NULL,
    total_rooms      INT         NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_roomtype_source UNIQUE (hotel_id, source_room_code),
    CONSTRAINT fk_roomtype_hotel FOREIGN KEY (hotel_id) REFERENCES hotels (id)
);

CREATE TABLE IF NOT EXISTS daily_rates (
    id              BIGINT       NOT NULL AUTO_INCREMENT,
    room_type_id    BIGINT       NOT NULL,
    stay_date       DATE         NOT NULL,
    price_amount    DECIMAL(12,2) NOT NULL,
    currency        VARCHAR(3)   NOT NULL,
    available_rooms INT          NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_rate_roomtype_date UNIQUE (room_type_id, stay_date),
    CONSTRAINT fk_rate_roomtype FOREIGN KEY (room_type_id) REFERENCES room_types (id)
);

CREATE INDEX IF NOT EXISTS idx_hotel_city        ON hotels (city);
CREATE INDEX IF NOT EXISTS idx_rate_roomtype_date ON daily_rates (room_type_id, stay_date);
CREATE INDEX IF NOT EXISTS idx_rate_hotel_date    ON daily_rates (room_type_id, stay_date);
