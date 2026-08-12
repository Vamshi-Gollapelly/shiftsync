package com.shiftsync.common.exception;

/** Thrown for well-formed requests that violate a business rule (e.g. "staff can't create another owner"). */
public class InvalidOperationException extends RuntimeException {
    public InvalidOperationException(String message) {
        super(message);
    }
}