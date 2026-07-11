package com.ota.common.exception;

import com.ota.common.error.ErrorCode;

public final class Exceptions {

    private Exceptions() {
    }

    public static DomainException notFound(String message, Object... args) {
        return new DomainException(ErrorCode.RESOURCE_NOT_FOUND, message, args);
    }

    public static DomainException badRequest(String message, Object... args) {
        return new DomainException(ErrorCode.MALFORMED_REQUEST, message, args);
    }

    public static DomainException conflict(String message, Object... args) {
        return new DomainException(ErrorCode.CONFLICT, message, args);
    }

    public static DomainException idempotentReplay(String message, Object... args) {
        return new DomainException(ErrorCode.IDEMPOTENT_REPLAY, message, args);
    }

    public static DomainException insufficientInventory(String message, Object... args) {
        return new DomainException(ErrorCode.INSUFFICIENT_INVENTORY, message, args);
    }

    public static DomainException upstreamUnavailable(String message, Throwable cause, Object... args) {
        return new DomainException(ErrorCode.UPSTREAM_UNAVAILABLE, message, cause, args);
    }

    public static DomainException circuitOpen(String message, Object... args) {
        return new DomainException(ErrorCode.CIRCUIT_OPEN, message, args);
    }
}
