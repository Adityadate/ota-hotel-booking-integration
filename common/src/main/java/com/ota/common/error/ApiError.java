package com.ota.common.error;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.time.OffsetDateTime;
import java.util.List;
import lombok.Builder;
import lombok.Value;

/**
 * Standard error envelope returned by every service. Keeping the shape identical
 * across services lets the API gateway and clients parse failures uniformly.
 */
@Value
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ApiError {

    String code;
    String message;
    int status;
    OffsetDateTime timestamp;
    String path;
    List<FieldError> errors;

    @Value
    @Builder
    public static class FieldError {
        String field;
        String reason;
    }
}
