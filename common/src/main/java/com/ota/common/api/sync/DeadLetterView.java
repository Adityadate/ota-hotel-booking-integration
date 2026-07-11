package com.ota.common.api.sync;

import com.fasterxml.jackson.annotation.JsonFormat;
import java.time.OffsetDateTime;
import lombok.Builder;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;

/**
 * Read-only view of a dead-letter entry: a unit of work (one hotel/rate batch from
 * a provider) that exhausted its retries and is parked for later replay.
 */
@Value
@Builder
@Jacksonized
public class DeadLetterView {

    Long id;
    String provider;
    String entityType;
    String entityKey;
    String errorMessage;
    int attempts;
    String status;            // PENDING | REPLAYING | FAILED

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ssXXX")
    OffsetDateTime firstSeenAt;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ssXXX")
    OffsetDateTime lastErrorAt;
}
