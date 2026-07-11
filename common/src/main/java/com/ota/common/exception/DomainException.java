package com.ota.common.exception;

import com.ota.common.error.ErrorCode;
import lombok.Getter;

/**
 * Base class for all intentional (domain) exceptions. Runtime so they do not
 * force callers to declare or catch them, while still carrying a canonical
 * {@link ErrorCode} that the global handler turns into a uniform response.
 */
@Getter
public class DomainException extends RuntimeException {

    private final ErrorCode errorCode;
    private final transient Object[] args;

    public DomainException(ErrorCode errorCode, String message, Object... args) {
        super(message);
        this.errorCode = errorCode;
        this.args = args;
    }

    public DomainException(ErrorCode errorCode, String message, Throwable cause, Object... args) {
        super(message, cause);
        this.errorCode = errorCode;
        this.args = args;
    }
}
