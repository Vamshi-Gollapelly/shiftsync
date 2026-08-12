package com.shiftsync.common.exception;

/** Thrown when a new shift would overlap an existing shift already assigned to the same staff member. */
public class ShiftConflictException extends RuntimeException {
    public ShiftConflictException(String message) {
        super(message);
    }
}