package com.ota.common.error;

import org.springframework.http.HttpStatus;

/**
 * Canonical error catalog shared across all services. Each code maps to a stable
 * machine-readable identifier plus the HTTP status that the common
 * {@link GlobalExceptionHandler} will attach to the response.
 */
public enum ErrorCode {

    VALIDATION_FAILED(HttpStatus.BAD_REQUEST),
    MALFORMED_REQUEST(HttpStatus.BAD_REQUEST),
    RESOURCE_NOT_FOUND(HttpStatus.NOT_FOUND),
    CONFLICT(HttpStatus.CONFLICT),
    IDEMPOTENT_REPLAY(HttpStatus.CONFLICT),
    INSUFFICIENT_INVENTORY(HttpStatus.CONFLICT),
    UPSTREAM_UNAVAILABLE(HttpStatus.SERVICE_UNAVAILABLE),
    CIRCUIT_OPEN(HttpStatus.SERVICE_UNAVAILABLE),
    INTERNAL_ERROR(HttpStatus.INTERNAL_SERVER_ERROR);

    private final HttpStatus httpStatus;

    ErrorCode(HttpStatus httpStatus) {
        this.httpStatus = httpStatus;
    }

    public HttpStatus getHttpStatus() {
        return httpStatus;
    }
}
