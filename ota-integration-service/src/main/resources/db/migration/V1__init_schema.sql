-- Dead-letter store for the integration pipeline. MySQL dialect, runs on H2 MODE=MySQL.
CREATE TABLE IF NOT EXISTS dead_letter (
    id           BIGINT       NOT NULL AUTO_INCREMENT,
    provider     VARCHAR(64)  NOT NULL,
    entity_type  VARCHAR(64),
    entity_key   VARCHAR(128),
    error_message VARCHAR(1024),
    attempts     INT          NOT NULL DEFAULT 0,
    status       VARCHAR(16)  NOT NULL,
    first_seen_at TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    last_error_at TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id)
);

CREATE INDEX IF NOT EXISTS idx_deadletter_status ON dead_letter (status);
CREATE INDEX IF NOT EXISTS idx_deadletter_provider ON dead_letter (provider);
